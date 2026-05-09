package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.MqttConfig
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSDate
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplicationWillEnterForegroundNotification
import platform.posix.AF_UNSPEC
import platform.posix.SO_NOSIGPIPE
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.addrinfo
import platform.posix.close
import platform.posix.connect
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.memset
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.socket

actual fun createMqttService(): MqttService = IosMqttService()

@OptIn(ExperimentalForeignApi::class)
private class IosMqttService : MqttService {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var sockfd: Int = -1
    private val subscribers = mutableMapOf<String, (String, String) -> Unit>()
    private var readJob: Job? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private val keepAliveSec: Int = 60
    private var packetId: Int = 1

    private var lastConfig: MqttConfig? = null
    private var explicitDisconnect = false

    init {
        // 앱이 백그라운드에서 다시 포그라운드로 올라올 때 연결 점검.
        // 백그라운드 동안 PINGREQ가 멈춰 broker/NAT가 만료시킨 경우 즉시 재연결.
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationWillEnterForegroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            ensureConnected()
        }
    }

    override fun connect(config: MqttConfig, onConnected: () -> Unit, onError: (String) -> Unit) {
        explicitDisconnect = false
        lastConfig = config
        scope.launch {
            _connectionState.value = MqttConnectionState.CONNECTING
            if (attemptConnect(config)) {
                onConnected()
            } else {
                onError("MQTT 첫 연결 실패 — 자동 재시도 중")
                scheduleReconnect()
            }
        }
    }

    override fun subscribe(topic: String, onMessage: (String, String) -> Unit) {
        subscribers[topic] = onMessage
        val fd = sockfd
        if (fd >= 0) {
            scope.launch {
                try {
                    sendAll(fd, buildSubscribe(topic, nextPacketId()))
                } catch (_: Exception) {}
            }
        }
    }

    override fun disconnect() {
        explicitDisconnect = true
        reconnectJob?.cancel(); reconnectJob = null
        scope.launch {
            val fd = sockfd
            if (fd >= 0) {
                try { sendAll(fd, byteArrayOf(0xE0.toByte(), 0x00)) } catch (_: Exception) {}
            }
            cleanupSocket()
        }
    }

    /** 외부 신호(앱 포그라운드 등)로 호출. 끊겨있고 재연결 진행 중이 아니면 즉시 재시도. */
    private fun ensureConnected() {
        if (explicitDisconnect || lastConfig == null) return
        if (sockfd >= 0) return
        if (reconnectJob?.isActive == true) return
        scheduleReconnect()
    }

    private fun cleanupSocket() {
        readJob?.cancel(); readJob = null
        pingJob?.cancel(); pingJob = null
        if (sockfd >= 0) {
            close(sockfd)
            sockfd = -1
        }
        if (_connectionState.value !in setOf(MqttConnectionState.RECONNECTING, MqttConnectionState.ERROR)) {
            _connectionState.value = MqttConnectionState.DISCONNECTED
        }
    }

    private fun nextPacketId(): Int {
        val id = packetId
        packetId = if (packetId >= 0xFFFF) 1 else packetId + 1
        return id
    }

    /** 끊긴 후 백오프 재시도(1→2→5→10→30s 캡). 명시적 disconnect 또는 성공 시 종료. */
    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        val cfg = lastConfig ?: return
        if (explicitDisconnect) return
        reconnectJob = scope.launch {
            var backoff = 1_000L
            while (isActive && !explicitDisconnect && sockfd < 0) {
                _connectionState.value = MqttConnectionState.RECONNECTING
                delay(backoff)
                if (!isActive || explicitDisconnect || sockfd >= 0) break
                if (attemptConnect(cfg)) break
                backoff = (backoff * 2).coerceAtMost(30_000L)
            }
        }
    }

    /** 한 번의 연결 시도. 성공 시 read/ping 루프 시작 + 기존 구독 복구. */
    private suspend fun attemptConnect(config: MqttConfig): Boolean {
        if (sockfd >= 0) return true
        return try {
            val fd = openSocket(config.host, config.port) ?: return false
            disableSigpipe(fd)
            sockfd = fd
            val clientId = "MateDash-${NSDate().timeIntervalSince1970.toLong()}"
            sendAll(fd, buildConnect(clientId, config.username, config.password, keepAliveSec))
            val ack = readPacket(fd)
            if (ack.type != 0x20) {
                close(fd); sockfd = -1
                return false
            }
            val rc = ack.payload.getOrNull(1)?.toInt() ?: -1
            if (rc != 0) {
                close(fd); sockfd = -1
                return false
            }
            _connectionState.value = MqttConnectionState.CONNECTED
            // 기존 구독 복구
            for (topic in subscribers.keys) {
                try { sendAll(fd, buildSubscribe(topic, nextPacketId())) } catch (_: Exception) {}
            }
            startReadLoop(fd)
            startPingLoop(fd)
            true
        } catch (e: Exception) {
            if (sockfd >= 0) {
                close(sockfd)
                sockfd = -1
            }
            false
        }
    }

    private fun startReadLoop(fd: Int) {
        readJob = scope.launch {
            while (isActive && fd == sockfd) {
                val pkt = try {
                    readPacket(fd)
                } catch (e: Exception) {
                    if (isActive && sockfd == fd) {
                        cleanupSocket()
                        if (!explicitDisconnect) scheduleReconnect()
                    }
                    break
                }
                when (pkt.type) {
                    0x30 -> handlePublish(pkt.payload)
                    0x90, 0xD0 -> { /* SUBACK / PINGRESP — ignore */ }
                }
            }
        }
    }

    private fun startPingLoop(fd: Int) {
        pingJob = scope.launch {
            val intervalMs = (keepAliveSec / 2L).coerceAtLeast(15L) * 1000L
            while (isActive && fd == sockfd) {
                delay(intervalMs)
                if (!isActive || fd != sockfd) break
                try {
                    sendAll(fd, byteArrayOf(0xC0.toByte(), 0x00))
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun handlePublish(payload: ByteArray) {
        if (payload.size < 2) return
        val topicLen = ((payload[0].toInt() and 0xFF) shl 8) or (payload[1].toInt() and 0xFF)
        if (payload.size < 2 + topicLen) return
        val topic = payload.copyOfRange(2, 2 + topicLen).decodeToString()
        // QoS 0 → no packet identifier; payload follows immediately
        val message = payload.copyOfRange(2 + topicLen, payload.size).decodeToString()
        for ((pattern, callback) in subscribers) {
            if (topicMatches(pattern, topic)) callback(topic, message)
        }
    }

    private fun topicMatches(pattern: String, topic: String): Boolean {
        if (pattern == topic) return true
        val p = pattern.split('/')
        val t = topic.split('/')
        for (i in p.indices) {
            val seg = p[i]
            if (seg == "#") return true
            if (i >= t.size) return false
            if (seg == "+") continue
            if (seg != t[i]) return false
        }
        return p.size == t.size
    }

    // ── Socket I/O ──────────────────────────────────────────────────

    private fun openSocket(host: String, port: Int): Int? = memScoped {
        val hints = alloc<addrinfo>()
        memset(hints.ptr, 0, sizeOf<addrinfo>().convert())
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        val resultPtr = allocPointerTo<addrinfo>()
        val rc = getaddrinfo(host, port.toString(), hints.ptr, resultPtr.ptr)
        if (rc != 0) return@memScoped null
        val first = resultPtr.value ?: return@memScoped null
        var fd = -1
        var p: CPointer<addrinfo>? = first
        while (p != null) {
            val info = p.pointed
            fd = socket(info.ai_family, info.ai_socktype, info.ai_protocol)
            if (fd >= 0) {
                if (connect(fd, info.ai_addr, info.ai_addrlen) == 0) break
                close(fd)
                fd = -1
            }
            p = info.ai_next
        }
        freeaddrinfo(first)
        if (fd < 0) null else fd
    }

    private fun disableSigpipe(fd: Int) {
        memScoped {
            val one = alloc<IntVar>()
            one.value = 1
            setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, one.ptr, sizeOf<IntVar>().convert())
        }
    }

    private fun sendAll(fd: Int, bytes: ByteArray) {
        if (bytes.isEmpty()) return
        bytes.usePinned { pinned ->
            var sent = 0
            while (sent < bytes.size) {
                val r = send(
                    fd,
                    pinned.addressOf(sent),
                    (bytes.size - sent).convert(),
                    0,
                ).toInt()
                if (r <= 0) throw RuntimeException("send 실패")
                sent += r
            }
        }
    }

    private fun recvFully(fd: Int, n: Int): ByteArray {
        val buf = ByteArray(n)
        if (n == 0) return buf
        buf.usePinned { pinned ->
            var got = 0
            while (got < n) {
                val r = recv(
                    fd,
                    pinned.addressOf(got),
                    (n - got).convert(),
                    0,
                ).toInt()
                if (r <= 0) throw RuntimeException("recv 종료")
                got += r
            }
        }
        return buf
    }

    private data class Packet(val type: Int, val flags: Int, val payload: ByteArray)

    private fun readPacket(fd: Int): Packet {
        val header = recvFully(fd, 1)[0].toInt() and 0xFF
        val type = header and 0xF0
        val flags = header and 0x0F
        val remLen = readRemainingLength(fd)
        val payload = if (remLen > 0) recvFully(fd, remLen) else ByteArray(0)
        return Packet(type, flags, payload)
    }

    private fun readRemainingLength(fd: Int): Int {
        var multiplier = 1
        var value = 0
        var loops = 0
        while (true) {
            val b = recvFully(fd, 1)[0].toInt() and 0xFF
            value += (b and 0x7F) * multiplier
            if ((b and 0x80) == 0) break
            multiplier *= 128
            loops++
            if (loops > 3) throw RuntimeException("Remaining Length 인코딩 손상")
        }
        return value
    }

    // ── MQTT 3.1.1 패킷 빌더 ────────────────────────────────────────

    private fun buildConnect(clientId: String, username: String, password: String, keepAlive: Int): ByteArray {
        val varHeader = mutableListOf<Byte>()
        appendString(varHeader, "MQTT")
        varHeader += 0x04.toByte() // Protocol level (3.1.1)
        var flags = 0x02 // clean session
        if (username.isNotEmpty()) flags = flags or 0x80
        if (password.isNotEmpty()) flags = flags or 0x40
        varHeader += flags.toByte()
        varHeader += (keepAlive shr 8 and 0xFF).toByte()
        varHeader += (keepAlive and 0xFF).toByte()

        val payload = mutableListOf<Byte>()
        appendString(payload, clientId)
        if (username.isNotEmpty()) appendString(payload, username)
        if (password.isNotEmpty()) appendString(payload, password)

        val body = (varHeader + payload).toByteArray()
        return wrap(0x10, body)
    }

    private fun buildSubscribe(topic: String, packetId: Int): ByteArray {
        val body = mutableListOf<Byte>()
        body += (packetId shr 8 and 0xFF).toByte()
        body += (packetId and 0xFF).toByte()
        appendString(body, topic)
        body += 0x00.toByte() // QoS 0
        return wrap(0x82, body.toByteArray())
    }

    private fun wrap(firstByte: Int, body: ByteArray): ByteArray {
        val remLen = encodeRemainingLength(body.size)
        val packet = ByteArray(1 + remLen.size + body.size)
        packet[0] = firstByte.toByte()
        remLen.copyInto(packet, 1)
        body.copyInto(packet, 1 + remLen.size)
        return packet
    }

    private fun appendString(out: MutableList<Byte>, s: String) {
        val bytes = s.encodeToByteArray()
        require(bytes.size <= 0xFFFF) { "MQTT 문자열은 65535바이트 이하여야 합니다" }
        out += (bytes.size shr 8 and 0xFF).toByte()
        out += (bytes.size and 0xFF).toByte()
        for (b in bytes) out += b
    }

    private fun encodeRemainingLength(len: Int): ByteArray {
        require(len in 0..(128 * 128 * 128 * 128 - 1)) { "Remaining Length 범위 초과" }
        var x = len
        val out = mutableListOf<Byte>()
        do {
            var b = x and 0x7F
            x = x ushr 7
            if (x > 0) b = b or 0x80
            out += b.toByte()
        } while (x > 0)
        return out.toByteArray()
    }
}
