import Foundation

/// MediaRemote (프라이빗 프레임워크)를 dlopen해서 시스템 전역 NowPlaying 정보를
/// 수집하고 App Group UserDefaults에 기록한다. Kotlin/Native 측에서 이를 읽는다.
///
/// ⚠️ 프라이빗 API 사용 — App Store 심사 거부됨. 사이드로드/TestFlight/Ad-hoc 한정.
class NowPlayingMonitor {
    static let shared = NowPlayingMonitor()
    private init() {}

    private let suiteName = "group.com.soooool.matedash"
    private var bundle: CFBundle?
    private var register: MRMediaRemoteRegisterForNowPlayingNotifications?
    private var getInfo: MRMediaRemoteGetNowPlayingInfo?
    private var getIsPlaying: MRMediaRemoteGetNowPlayingApplicationIsPlaying?

    private typealias MRMediaRemoteRegisterForNowPlayingNotifications =
        @convention(c) (DispatchQueue) -> Void
    private typealias MRMediaRemoteGetNowPlayingInfo =
        @convention(c) (DispatchQueue, @convention(block) ([String: Any]?) -> Void) -> Void
    private typealias MRMediaRemoteGetNowPlayingApplicationIsPlaying =
        @convention(c) (DispatchQueue, @convention(block) (Bool) -> Void) -> Void

    func startMonitoring() {
        let path = "/System/Library/PrivateFrameworks/MediaRemote.framework"
        guard let url = NSURL(fileURLWithPath: path) as CFURL?,
              let bundle = CFBundleCreate(kCFAllocatorDefault, url) else {
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

        register?(DispatchQueue.main)

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

        refresh()
        log("monitoring 시작")
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

    private func write(info: [String: Any]?) {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        if let info = info,
           let title = info["kMRMediaRemoteNowPlayingInfoTitle"] as? String,
           !title.isEmpty {
            let artist = info["kMRMediaRemoteNowPlayingInfoArtist"] as? String ?? ""
            let rate = info["kMRMediaRemoteNowPlayingInfoPlaybackRate"] as? Double ?? 0
            defaults.set(title, forKey: "now_playing_title")
            defaults.set(artist, forKey: "now_playing_artist")
            // 명시적 isPlaying 콜백이 따로 갱신하지만, 메타 정보 갱신 시점에 rate>0 만 신뢰
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
