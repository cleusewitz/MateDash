import ActivityKit
import Foundation

/// UserDefaults(App Group)를 감시해서 Live Activity를 자동 관리
class ChargingLiveActivityManager {
    static let shared = ChargingLiveActivityManager()
    private init() {}

    private var currentActivity: Activity<ChargingActivityAttributes>?
    private var pollTimer: Timer?

    private let suiteName = "group.com.soooool.matedash"
    private let debugKey = "la_dbg_charging_last"

    private func log(_ msg: String) {
        print("[MateDash] Charging: \(msg)")
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        defaults.set("\(f.string(from: Date())) \(msg)", forKey: debugKey)
    }

    // MARK: - UserDefaults에서 상태 읽기

    private func readState() -> (name: String, state: ChargingActivityAttributes.ContentState, isCharging: Bool)? {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return nil }

        let chargingState = defaults.string(forKey: "la_charging_state") ?? ""
        let batteryLevel = defaults.integer(forKey: "la_battery_level")
        let displayName = defaults.string(forKey: "la_display_name") ?? "Tesla"

        let state = ChargingActivityAttributes.ContentState(
            batteryLevel: batteryLevel,
            chargerPower: defaults.integer(forKey: "la_charger_power"),
            chargeLimitSoc: defaults.integer(forKey: "la_charge_limit_soc"),
            timeToFullCharge: defaults.double(forKey: "la_time_to_full"),
            chargingState: chargingState,
            energyAdded: defaults.double(forKey: "la_energy_added")
        )

        let isCharging = chargingState.lowercased() == "charging"
        return (displayName, state, isCharging)
    }

    // MARK: - 폴링 시작/중지

    /// 앱 시작 시 호출 - 30초마다 UserDefaults를 확인
    func startMonitoring() {
        stopMonitoring()
        print("[MateDash] Live Activity monitoring started")

        // 즉시 1회 체크
        checkAndUpdate()

        // 30초마다 반복
        pollTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.checkAndUpdate()
        }

        // Kotlin에서 보내는 즉시 체크 알림 수신
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(forceCheck),
            name: Notification.Name("com.soooool.matedash.forceCheckLiveActivity"),
            object: nil
        )

        // 즉시 종료 알림 수신
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(forceEndAll),
            name: Notification.Name("com.soooool.matedash.forceEndAllLiveActivities"),
            object: nil
        )
    }

    func stopMonitoring() {
        pollTimer?.invalidate()
        pollTimer = nil
        NotificationCenter.default.removeObserver(self)
    }

    /// Kotlin에서 테스트 데이터 작성 후 즉시 호출
    @objc func forceCheck() {
        print("[MateDash] Force check triggered")
        checkAndUpdate()
    }

    // MARK: - 상태 확인 & Live Activity 관리

    private func checkAndUpdate() {
        guard let (name, state, isCharging) = readState() else {
            log("check skipped (no defaults)")
            return
        }

        if isCharging {
            if currentActivity != nil {
                updateActivity(state: state)
            } else {
                startActivity(vehicleName: name, state: state)
            }
        } else {
            if currentActivity != nil {
                endActivity(state: state)
            } else {
                log("idle (not charging, no activity)")
            }
        }
    }

    // MARK: - Activity 관리

    private func startActivity(vehicleName: String, state: ChargingActivityAttributes.ContentState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            log("LA disabled by system")
            return
        }

        let attributes = ChargingActivityAttributes(vehicleName: vehicleName)

        do {
            let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(120))
            let activity = try Activity.request(
                attributes: attributes,
                content: content,
                pushType: nil
            )
            currentActivity = activity
            log("started id=\(activity.id) bat=\(state.batteryLevel)")
        } catch {
            log("start FAILED: \(error.localizedDescription)")
        }
    }

    private func updateActivity(state: ChargingActivityAttributes.ContentState) {
        guard let activity = currentActivity else { return }

        Task {
            let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(120))
            await activity.update(content)
            log("updated bat=\(state.batteryLevel) kW=\(state.chargerPower)")
        }
    }

    private func endActivity(state: ChargingActivityAttributes.ContentState) {
        guard let activity = currentActivity else { return }

        let finalState = ChargingActivityAttributes.ContentState(
            batteryLevel: state.batteryLevel,
            chargerPower: 0,
            chargeLimitSoc: state.chargeLimitSoc,
            timeToFullCharge: 0,
            chargingState: "Complete",
            energyAdded: state.energyAdded
        )

        Task {
            let content = ActivityContent(state: finalState, staleDate: nil)
            await activity.end(content, dismissalPolicy: .immediate)
            log("ended bat=\(state.batteryLevel)")
            currentActivity = nil
        }
    }

    /// Kotlin에서 테스트 종료 시 즉시 호출
    @objc func forceEndAll() {
        print("[MateDash] Force end all triggered")
        endAllActivities()
    }

    /// 모든 활동 정리
    func endAllActivities() {
        Task {
            for activity in Activity<ChargingActivityAttributes>.activities {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
            currentActivity = nil
            print("[MateDash] All Live Activities ended")
        }
    }
}
