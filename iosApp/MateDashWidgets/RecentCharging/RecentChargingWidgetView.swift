import SwiftUI
import WidgetKit

struct RecentChargingWidgetView: View {
    let entry: RecentChargingEntry

    private let bgColor = Color(red: 0.043, green: 0.043, blue: 0.043)
    private let cardColor = Color(red: 0.102, green: 0.102, blue: 0.102)
    private let teslaRed = Color(red: 0.890, green: 0.098, blue: 0.216)
    private let textSecondary = Color(red: 0.557, green: 0.557, blue: 0.576)

    var body: some View {
        if let error = entry.errorMessage {
            errorView(error)
        } else if let charge = entry.lastCharge {
            contentView(charge: charge)
        } else {
            errorView("충전 기록 없음")
        }
    }

    @ViewBuilder
    private func contentView(charge: ChargeDTO) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            // Header
            HStack {
                Text("최근 충전")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.white)
                Spacer()
                Text(entry.monthLabel)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(textSecondary)
            }

            Spacer(minLength: 2)

            // Last charge info
            VStack(alignment: .leading, spacing: 3) {
                Text(charge.address ?? "알 수 없는 위치")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(.white)
                    .lineLimit(1)

                HStack(spacing: 8) {
                    // Battery change
                    if let bd = charge.batteryDetails,
                       let start = bd.startBatteryLevel,
                       let end = bd.endBatteryLevel {
                        HStack(spacing: 2) {
                            Image(systemName: "battery.50percent")
                                .font(.system(size: 10))
                                .foregroundColor(teslaRed)
                            Text("\(start) → \(end)%")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white)
                        }
                    }

                    // Energy added
                    if let energy = charge.chargeEnergyAdded {
                        Text("+\(String(format: "%.1f", energy)) kWh")
                            .font(.system(size: 11))
                            .foregroundColor(textSecondary)
                    }

                    // Duration
                    if let dur = charge.durationMin {
                        Text("\(dur)분")
                            .font(.system(size: 11))
                            .foregroundColor(textSecondary)
                    }
                }
            }

            Spacer(minLength: 2)

            // Monthly summary
            HStack(spacing: 0) {
                Image(systemName: "bolt.fill")
                    .font(.system(size: 9))
                    .foregroundColor(teslaRed)

                Text(" 이번 달: ")
                    .font(.system(size: 10))
                    .foregroundColor(textSecondary)

                Text("\(entry.monthlyChargeCount)회")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.white)

                Text(" · ")
                    .font(.system(size: 10))
                    .foregroundColor(textSecondary)

                Text("\(String(format: "%.1f", entry.monthlyTotalEnergy)) kWh")
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(.white)

                if let cost = entry.monthlyTotalCost, cost > 0 {
                    Text(" · ")
                        .font(.system(size: 10))
                        .foregroundColor(textSecondary)

                    Text(formatCost(cost))
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white)
                }
            }
        }
        .padding(14)
    }

    @ViewBuilder
    private func errorView(_ message: String) -> some View {
        VStack(spacing: 8) {
            Image(systemName: "bolt.car")
                .font(.system(size: 24))
                .foregroundColor(textSecondary)
            Text(message)
                .font(.system(size: 13))
                .foregroundColor(textSecondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(14)
    }

    private func formatCost(_ cost: Double) -> String {
        let won = Int(cost)
        if won >= 10000 {
            let man = won / 10000
            let remainder = (won % 10000) / 1000
            if remainder > 0 {
                return "₩\(man).\(remainder)만"
            }
            return "₩\(man)만"
        }
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        return "₩" + (formatter.string(from: NSNumber(value: won)) ?? "\(won)")
    }
}
