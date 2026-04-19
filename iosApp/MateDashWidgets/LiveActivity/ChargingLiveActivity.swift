import ActivityKit
import SwiftUI
import WidgetKit

// MARK: - Live Activity Widget

struct ChargingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: ChargingActivityAttributes.self) { context in
            // 잠금 화면 UI
            LockScreenView(context: context)
                .padding(16)
                .activityBackgroundTint(.black)
                .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.attributes.vehicleName)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        HStack(spacing: 4) {
                            Image(systemName: "bolt.fill")
                                .foregroundStyle(.green)
                            Text("\(context.state.batteryLevel)%")
                                .font(.title2.bold())
                        }
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("\(context.state.chargerPower) kW")
                            .font(.caption.bold())
                            .foregroundStyle(.cyan)
                        Text(timeRemainingText(context.state.timeToFullCharge))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ChargingProgressBar(
                        level: context.state.batteryLevel,
                        limit: context.state.chargeLimitSoc
                    )
                }
                DynamicIslandExpandedRegion(.center) {}
            } compactLeading: {
                // Compact Leading
                HStack(spacing: 3) {
                    Image(systemName: "bolt.fill")
                        .foregroundStyle(.green)
                        .font(.caption)
                    Text("\(context.state.batteryLevel)%")
                        .font(.caption.bold())
                }
            } compactTrailing: {
                // Compact Trailing
                Text(timeRemainingShort(context.state.timeToFullCharge))
                    .font(.caption.bold())
                    .foregroundStyle(.cyan)
            } minimal: {
                // Minimal
                Image(systemName: "bolt.fill")
                    .foregroundStyle(.green)
                    .font(.caption)
            }
        }
    }
}

// MARK: - Lock Screen View

private struct LockScreenView: View {
    let context: ActivityViewContext<ChargingActivityAttributes>

    var body: some View {
        VStack(spacing: 12) {
            // 상단: 차량 이름 + 상태
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "bolt.car.fill")
                        .foregroundStyle(.green)
                    Text(context.attributes.vehicleName)
                        .font(.subheadline.bold())
                }
                Spacer()
                Text(chargingStateText(context.state.chargingState))
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(chargingStateColor(context.state.chargingState).opacity(0.2))
                    .foregroundStyle(chargingStateColor(context.state.chargingState))
                    .clipShape(Capsule())
            }

            // 중간: 배터리 프로그레스 바
            ChargingProgressBar(
                level: context.state.batteryLevel,
                limit: context.state.chargeLimitSoc
            )

            // 하단: 상세 정보
            HStack {
                InfoItem(icon: "bolt.fill", value: "\(context.state.chargerPower) kW", color: .cyan)
                Spacer()
                InfoItem(icon: "plus.circle.fill", value: String(format: "%.1f kWh", context.state.energyAdded), color: .green)
                Spacer()
                InfoItem(icon: "clock.fill", value: timeRemainingText(context.state.timeToFullCharge), color: .orange)
            }
        }
    }
}

// MARK: - Components

private struct ChargingProgressBar: View {
    let level: Int
    let limit: Int

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                // 배경
                RoundedRectangle(cornerRadius: 6)
                    .fill(Color.white.opacity(0.1))

                // 현재 충전량
                RoundedRectangle(cornerRadius: 6)
                    .fill(batteryColor)
                    .frame(width: geo.size.width * CGFloat(level) / 100.0)

                // 목표 라인
                Rectangle()
                    .fill(Color.white.opacity(0.6))
                    .frame(width: 2)
                    .offset(x: geo.size.width * CGFloat(limit) / 100.0 - 1)

                // 퍼센트 텍스트
                HStack {
                    Text("\(level)%")
                        .font(.caption.bold())
                        .padding(.leading, 8)
                    Spacer()
                    Text("목표 \(limit)%")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.trailing, 8)
                }
            }
        }
        .frame(height: 24)
    }

    private var batteryColor: Color {
        if level < 20 { return .red }
        if level < 50 { return .orange }
        return .green
    }
}

private struct InfoItem: View {
    let icon: String
    let value: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
                .foregroundStyle(color)
            Text(value)
                .font(.caption.bold())
        }
    }
}

// MARK: - Helpers

private func timeRemainingText(_ hours: Double) -> String {
    if hours <= 0 { return "완료" }
    let h = Int(hours)
    let m = Int((hours - Double(h)) * 60)
    if h > 0 {
        return "\(h)시간 \(m)분"
    }
    return "\(m)분"
}

private func timeRemainingShort(_ hours: Double) -> String {
    if hours <= 0 { return "Done" }
    let h = Int(hours)
    let m = Int((hours - Double(h)) * 60)
    if h > 0 {
        return "\(h)h\(m)m"
    }
    return "\(m)m"
}

private func chargingStateText(_ state: String) -> String {
    switch state.lowercased() {
    case "charging": return "충전 중"
    case "complete": return "완료"
    case "stopped": return "중단됨"
    case "disconnected": return "미연결"
    default: return state
    }
}

private func chargingStateColor(_ state: String) -> Color {
    switch state.lowercased() {
    case "charging": return .green
    case "complete": return .cyan
    case "stopped": return .orange
    default: return .secondary
    }
}

// MARK: - Preview

#Preview("Lock Screen", as: .content, using: ChargingActivityAttributes(vehicleName: "Model Y")) {
    ChargingLiveActivity()
} contentStates: {
    ChargingActivityAttributes.ContentState(
        batteryLevel: 67,
        chargerPower: 11,
        chargeLimitSoc: 80,
        timeToFullCharge: 0.75,
        chargingState: "Charging",
        energyAdded: 12.4
    )
}
