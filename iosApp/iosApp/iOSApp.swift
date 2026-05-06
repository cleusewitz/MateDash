import SwiftUI

@main
struct iOSApp: App {
    init() {
        ChargingLiveActivityManager.shared.startMonitoring()
        DrivingLiveActivityManager.shared.startMonitoring()
        NowPlayingMonitor.shared.startMonitoring()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
        }
    }
}
