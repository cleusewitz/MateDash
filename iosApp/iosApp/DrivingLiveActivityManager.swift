import ActivityKit
import Foundation

/// UserDefaults(App Group)를 감시해서 주행 Live Activity를 자동 관리
class DrivingLiveActivityManager {
    static let shared = DrivingLiveActivityManager()
    private init() {}

    private var currentActivity: Activity<DrivingActivityAttributes>?
    private var pollTimer: Timer?

    private let suiteName = "group.com.soooool.matedash"

    // MARK: - UserDefaults에서 상태 읽기

    private func readState() -> (name: String, startAddr: String, state: DrivingActivityAttributes.ContentState, isDriving: Bool)? {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return nil }

        let drivingState = defaults.string(forKey: "la_driving_state") ?? ""
        let displayName = defaults.string(forKey: "la_display_name") ?? "Tesla"
        let startAddress = defaults.string(forKey: "la_driving_start_address") ?? ""

        let state = DrivingActivityAttributes.ContentState(
            speed: defaults.integer(forKey: "la_driving_speed"),
            batteryLevel: defaults.integer(forKey: "la_driving_battery"),
            distanceKm: defaults.double(forKey: "la_driving_distance"),
            power: defaults.integer(forKey: "la_driving_power"),
            durationMin: defaults.integer(forKey: "la_driving_duration_min"),
            drivingState: drivingState
        )

        let isDriving = drivingState.lowercased() == "driving"
        return (displayName, startAddress, state, isDriving)
    }

    // MARK: - 폴링 시작/중지

    func startMonitoring() {
        stopMonitoring()
        print("[MateDash] Driving Live Activity monitoring started")

        checkAndUpdate()

        pollTimer = Timer.scheduledTimer(withTimeInterval: 30, repeats: true) { [weak self] _ in
            self?.checkAndUpdate()
        }

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(forceCheck),
            name: Notification.Name("com.soooool.matedash.forceCheckLiveActivity"),
            object: nil
        )

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

    @objc func forceCheck() {
        print("[MateDash] Driving force check triggered")
        checkAndUpdate()
    }

    @objc func forceEndAll() {
        print("[MateDash] Driving force end all triggered")
        endAllActivities()
    }

    // MARK: - 상태 확인 & Live Activity 관리

    private func checkAndUpdate() {
        guard let (name, startAddr, state, isDriving) = readState() else { return }

        if isDriving {
            if currentActivity != nil {
                updateActivity(state: state)
            } else {
                startActivity(vehicleName: name, startAddress: startAddr, state: state)
            }
        } else {
            if currentActivity != nil {
                endActivity(state: state)
            }
        }
    }

    // MARK: - Activity 관리

    private func startActivity(vehicleName: String, startAddress: String, state: DrivingActivityAttributes.ContentState) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            print("[MateDash] Live Activities not enabled")
            return
        }

        let attributes = DrivingActivityAttributes(
            vehicleName: vehicleName,
            startAddress: startAddress.isEmpty ? "출발" : startAddress
        )

        do {
            let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(120))
            let activity = try Activity.request(
                attributes: attributes,
                content: content,
                pushType: nil
            )
            currentActivity = activity
            print("[MateDash] Driving Live Activity started: \(activity.id), speed=\(state.speed)km/h")
        } catch {
            print("[MateDash] Failed to start Driving Live Activity: \(error)")
        }
    }

    private func updateActivity(state: DrivingActivityAttributes.ContentState) {
        guard let activity = currentActivity else { return }

        Task {
            let content = ActivityContent(state: state, staleDate: Date().addingTimeInterval(120))
            await activity.update(content)
        }
    }

    private func endActivity(state: DrivingActivityAttributes.ContentState) {
        guard let activity = currentActivity else { return }

        let finalState = DrivingActivityAttributes.ContentState(
            speed: 0,
            batteryLevel: state.batteryLevel,
            distanceKm: state.distanceKm,
            power: 0,
            durationMin: state.durationMin,
            drivingState: "Parked"
        )

        Task {
            let content = ActivityContent(state: finalState, staleDate: nil)
            await activity.end(content, dismissalPolicy: .immediate)
            print("[MateDash] Driving Live Activity ended")
            currentActivity = nil
        }
    }

    func endAllActivities() {
        Task {
            for activity in Activity<DrivingActivityAttributes>.activities {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
            currentActivity = nil
            print("[MateDash] All Driving Live Activities ended")
        }
    }
}
