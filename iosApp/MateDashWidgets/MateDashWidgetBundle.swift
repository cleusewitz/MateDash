import WidgetKit
import SwiftUI

@main
struct MateDashWidgetBundle: WidgetBundle {
    var body: some Widget {
        RecentChargingWidget()
        ChargingLiveActivity()
        DrivingLiveActivity()
    }
}
