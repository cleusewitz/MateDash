import WidgetKit

struct RecentChargingEntry: TimelineEntry {
    let date: Date
    let lastCharge: ChargeDTO?
    let monthlyChargeCount: Int
    let monthlyTotalEnergy: Double
    let monthlyTotalCost: Double?
    let monthLabel: String
    let isPlaceholder: Bool
    let errorMessage: String?

    static var placeholder: RecentChargingEntry {
        RecentChargingEntry(
            date: .now,
            lastCharge: ChargeDTO(
                chargeId: 1,
                startDate: "2026-04-11 14:00:00",
                endDate: "2026-04-11 14:45:00",
                address: "Tesla Supercharger Gangnam",
                chargeEnergyAdded: 18.5,
                chargeEnergyUsed: 20.1,
                cost: 8500,
                durationMin: 45,
                durationStr: "45 min",
                batteryDetails: .init(startBatteryLevel: 42, endBatteryLevel: 80),
                outsideTempAvg: 18.0,
                odometer: 12345.6
            ),
            monthlyChargeCount: 8,
            monthlyTotalEnergy: 142.3,
            monthlyTotalCost: 45000,
            monthLabel: "4월",
            isPlaceholder: true,
            errorMessage: nil
        )
    }

    static var noConfig: RecentChargingEntry {
        RecentChargingEntry(
            date: .now,
            lastCharge: nil,
            monthlyChargeCount: 0,
            monthlyTotalEnergy: 0,
            monthlyTotalCost: nil,
            monthLabel: "",
            isPlaceholder: false,
            errorMessage: "앱에서 서버를 연결하세요"
        )
    }
}
