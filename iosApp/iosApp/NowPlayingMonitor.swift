import Foundation

/// MediaRemote (프라이빗 프레임워크)를 dlopen해서 시스템 전역 NowPlaying 정보를
/// 수집하고 App Group UserDefaults에 기록한다. Kotlin/Native 측에서 이를 읽는다.
///
/// ⚠️ 프라이빗 API 사용 — App Store 심사 거부됨. 사이드로드/TestFlight/Ad-hoc 한정.
/// 어떤 단계에서 실패해도 앱이 죽지 않도록 모든 호출을 보수적으로 처리한다.
class NowPlayingMonitor {
    static let shared = NowPlayingMonitor()
    private init() {}

    private let suiteName = "group.com.soooool.matedash"
    private var bundle: CFBundle?
    private var register: MRMediaRemoteRegisterForNowPlayingNotifications?
    private var getInfo: MRMediaRemoteGetNowPlayingInfo?
    private var getIsPlaying: MRMediaRemoteGetNowPlayingApplicationIsPlaying?
    private var started = false

    // ABI: CFDictionaryRef(=__CFDictionary*) → NSDictionary? (toll-free, 포인터 1개로 매칭)
    // 이전 버전에서 [String: Any]?로 잘못 선언해 ABI 불일치로 크래시 → 검은 화면 발생.
    private typealias MRMediaRemoteRegisterForNowPlayingNotifications =
        @convention(c) (DispatchQueue) -> Void
    private typealias MRMediaRemoteGetNowPlayingInfo =
        @convention(c) (DispatchQueue, @convention(block) (NSDictionary?) -> Void) -> Void
    private typealias MRMediaRemoteGetNowPlayingApplicationIsPlaying =
        @convention(c) (DispatchQueue, @convention(block) (Bool) -> Void) -> Void

    /// ContentView.onAppear에서 약간의 지연 후 호출되어야 한다.
    /// SwiftUI App.init() 시점에서는 framework loader가 완전치 않아 위험.
    func startMonitoring() {
        if started { return }
        started = true

        // iOS 17.4+ 가드: Apple이 MediaRemote 접근을 강하게 차단했으므로 시도 자체를 스킵
        let v = ProcessInfo.processInfo.operatingSystemVersion
        if (v.majorVersion > 17) || (v.majorVersion == 17 && v.minorVersion >= 4) {
            log("iOS \(v.majorVersion).\(v.minorVersion) — MediaRemote 차단된 버전, 스킵")
            return
        }

        let path = "/System/Library/PrivateFrameworks/MediaRemote.framework"
        let url = NSURL(fileURLWithPath: path) as CFURL
        guard let bundle = CFBundleCreate(kCFAllocatorDefault, url) else {
            log("MediaRemote bundle 로드 실패")
            return
        }
        self.bundle = bundle

        guard let regPtr = CFBundleGetFunctionPointerForName(
            bundle, "MRMediaRemoteRegisterForNowPlayingNotifications" as CFString) else {
            log("Register fn 못 찾음")
            return
        }
        guard let infoPtr = CFBundleGetFunctionPointerForName(
            bundle, "MRMediaRemoteGetNowPlayingInfo" as CFString) else {
            log("GetNowPlayingInfo fn 못 찾음")
            return
        }
        let playPtr = CFBundleGetFunctionPointerForName(
            bundle, "MRMediaRemoteGetNowPlayingApplicationIsPlaying" as CFString)

        register = unsafeBitCast(regPtr, to: MRMediaRemoteRegisterForNowPlayingNotifications.self)
        getInfo = unsafeBitCast(infoPtr, to: MRMediaRemoteGetNowPlayingInfo.self)
        if let playPtr = playPtr {
            getIsPlaying = unsafeBitCast(playPtr, to: MRMediaRemoteGetNowPlayingApplicationIsPlaying.self)
        }

        // Notification 옵저버를 register 전에 먼저 걸어두기.
        let nc = NotificationCenter.default
        nc.addObserver(
            forName: NSNotification.Name("kMRMediaRemoteNowPlayingInfoDidChangeNotification"),
            object: nil, queue: .main) { [weak self] _ in self?.refresh() }
        nc.addObserver(
            forName: NSNotification.Name("kMRMediaRemoteNowPlayingApplicationIsPlayingDidChangeNotification"),
            object: nil, queue: .main) { [weak self] _ in self?.refresh() }
        nc.addObserver(
            forName: NSNotification.Name("kMRMediaRemoteNowPlayingApplicationDidChangeNotification"),
            object: nil, queue: .main) { [weak self] _ in self?.refresh() }

        // register 호출. 이게 실패해도 앱은 계속 돌아야 함.
        register?(DispatchQueue.main)
        log("monitoring 시작 (ABI 수정본, register 완료)")

        // 초기 fetch는 register 직후 즉시 호출하지 말고 약간 미룬다.
        // (register가 내부적으로 세션 셋업하는 시간을 둠)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
            self?.refresh()
        }
    }

    private func refresh() {
        guard let getInfo = getInfo else { return }
        getInfo(DispatchQueue.main) { [weak self] info in
            self?.write(info: info)
        }
        if let getIsPlaying = getIsPlaying {
            getIsPlaying(DispatchQueue.main) { [weak self] playing in
                self?.writeIsPlaying(playing)
            }
        }
    }

    private func write(info: NSDictionary?) {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        if let info = info,
           let title = info["kMRMediaRemoteNowPlayingInfoTitle"] as? String,
           !title.isEmpty {
            let artist = info["kMRMediaRemoteNowPlayingInfoArtist"] as? String ?? ""
            let rate = (info["kMRMediaRemoteNowPlayingInfoPlaybackRate"] as? NSNumber)?.doubleValue ?? 0
            defaults.set(title, forKey: "now_playing_title")
            defaults.set(artist, forKey: "now_playing_artist")
            if rate > 0 {
                defaults.set(true, forKey: "now_playing_is_playing")
            }
        } else {
            defaults.set("", forKey: "now_playing_title")
            defaults.set("", forKey: "now_playing_artist")
            defaults.set(false, forKey: "now_playing_is_playing")
        }
        defaults.set(Date().timeIntervalSinceReferenceDate, forKey: "now_playing_updated")
        defaults.synchronize()
    }

    private func writeIsPlaying(_ playing: Bool) {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        defaults.set(playing, forKey: "now_playing_is_playing")
        defaults.synchronize()
    }

    private func log(_ msg: String) {
        print("[MateDash] NowPlaying: \(msg)")
    }
}
