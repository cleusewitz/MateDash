import Foundation

struct DrivesResponse: Codable {
    let data: DrivesData?
}

struct DrivesData: Codable {
    let drives: [DriveDTO]?
}

struct DriveDTO: Codable {
    let driveId: Int?
    let startDate: String?
    let endDate: String?
    let startAddress: String?
    let endAddress: String?
    let odometerDetails: DriveOdometerDetails?
    let durationMin: Int?
    let durationStr: String?
    let speedMax: Int?
    let speedAvg: Double?
    let batteryDetails: DriveBatteryDetails?
    let outsideTempAvg: Double?
    let energyConsumedNet: Double?

    struct DriveOdometerDetails: Codable {
        let odometerDistance: Double?
    }

    struct DriveBatteryDetails: Codable {
        let startBatteryLevel: Int?
        let endBatteryLevel: Int?
    }
}
