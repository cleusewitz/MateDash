import Foundation

struct ApiStatusResponse: Codable {
    let data: ApiStatusData?
}

struct ApiStatusData: Codable {
    let status: CarStatusDTO?
}

struct CarStatusDTO: Codable {
    let displayName: String?
    let state: String?
    let batteryDetails: BatteryDetailsDTO?
    let climateDetails: ClimateDetailsDTO?
    let chargingDetails: ChargingDetailsDTO?

    struct BatteryDetailsDTO: Codable {
        let batteryLevel: Int?
        let estBatteryRange: Double?
        let ratedBatteryRange: Double?
    }

    struct ClimateDetailsDTO: Codable {
        let insideTemp: Double?
        let outsideTemp: Double?
        let isClimateOn: Bool?
    }

    struct ChargingDetailsDTO: Codable {
        let chargingState: String?
        let pluggedIn: Bool?
        let chargerPower: Int?
        let timeToFullCharge: Double?
    }
}
