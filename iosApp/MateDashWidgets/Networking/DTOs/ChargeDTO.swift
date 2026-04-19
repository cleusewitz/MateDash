import Foundation

struct ChargesResponse: Codable {
    let data: ChargesData?
}

struct ChargesData: Codable {
    let charges: [ChargeDTO]?
}

struct ChargeDTO: Codable {
    let chargeId: Int?
    let startDate: String?
    let endDate: String?
    let address: String?
    let chargeEnergyAdded: Double?
    let chargeEnergyUsed: Double?
    let cost: Double?
    let durationMin: Int?
    let durationStr: String?
    let batteryDetails: ChargeBatteryDetails?
    let outsideTempAvg: Double?
    let odometer: Double?

    struct ChargeBatteryDetails: Codable {
        let startBatteryLevel: Int?
        let endBatteryLevel: Int?
    }
}
