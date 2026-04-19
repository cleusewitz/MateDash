import ActivityKit
import Foundation

struct DrivingActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var speed: Int              // km/h
        var batteryLevel: Int       // %
        var distanceKm: Double      // 주행 거리
        var power: Int              // kW (현재 소비 전력)
        var durationMin: Int        // 주행 시간 (분)
        var drivingState: String    // Driving, Parked 등
    }

    let vehicleName: String
    let startAddress: String
}
