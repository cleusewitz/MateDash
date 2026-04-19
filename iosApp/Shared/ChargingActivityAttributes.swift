import ActivityKit
import Foundation

struct ChargingActivityAttributes: ActivityAttributes {
    /// 충전 시작 시 고정되는 정보
    struct ContentState: Codable, Hashable {
        var batteryLevel: Int
        var chargerPower: Int        // kW
        var chargeLimitSoc: Int      // 목표 %
        var timeToFullCharge: Double  // 시간 (예: 1.5 = 1시간 30분)
        var chargingState: String    // Charging, Complete, Stopped 등
        var energyAdded: Double      // kWh
    }

    let vehicleName: String
}
