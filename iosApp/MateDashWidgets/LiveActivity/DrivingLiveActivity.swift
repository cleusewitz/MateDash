import ActivityKit
import SwiftUI
import WidgetKit

struct DrivingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: DrivingActivityAttributes.self) { context in
            DrivingLockScreenView(context: context)
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
                            Image(systemName: "speedometer")
                                .foregroundStyle(.cyan)
                            Text("\(context.state.speed)")
                                .font(.title2.bold())
                            Text("km/h")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    VStack(alignment: .trailing, spacing: 2) {
                        HStack(spacing: 3) {
                            Image(systemName: "battery.75percent")
                                .foregroundStyle(batteryColor(context.state.batteryLevel))
                                .font(.caption)
                            Text("\(context.state.batteryLevel)%")
                                .font(.caption.bold())
                        }
                        Text(formatDistance(context.state.distanceKm))
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Label(context.attributes.startAddress, systemImage: "location.fill")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                        Spacer()
                        Label(formatDuration(context.state.durationMin), systemImage: "clock.fill")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                DynamicIslandExpandedRegion(.center) {}
            } compactLeading: {
                HStack(spacing: 3) {
                    Image(systemName: "car.fill")
                        .foregroundStyle(.cyan)
                        .font(.caption)
                    Text("\(context.state.speed)")
                        .font(.caption.bold())
                }
            } compactTrailing: {
                HStack(spacing: 2) {
                    Text("\(context.state.batteryLevel)%")
                        .font(.caption.bold())
                        .foregroundStyle(batteryColor(context.state.batteryLevel))
                }
            } minimal: {
                Image(systemName: "car.fill")
                    .foregroundStyle(.cyan)
                    .font(.caption)
            }
        }
    }
}

// MARK: - Lock Screen View

private struct DrivingLockScreenView: View {
    let context: ActivityViewContext<DrivingActivityAttributes>

    var body: some View {
        VStack(spacing: 12) {
            // 상단: 차량 이름 + 주행 중 배지
            HStack {
                HStack(spacing: 6) {
                    Image(systemName: "car.fill")
                        .foregroundStyle(.cyan)
                    Text(context.attributes.vehicleName)
                        .font(.subheadline.bold())
                }
                Spacer()
                Text("주행 중")
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 3)
                    .background(Color.cyan.opacity(0.2))
                    .foregroundStyle(.cyan)
                    .clipShape(Capsule())
            }

            // 중간: 속도 + 배터리
            HStack(alignment: .bottom) {
                // 속도
                HStack(alignment: .lastTextBaseline, spacing: 4) {
                    Text("\(context.state.speed)")
                        .font(.system(size: 42, weight: .bold, design: .rounded))
                    Text("km/h")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .padding(.bottom, 6)
                }
                Spacer()
                // 배터리
                VStack(alignment: .trailing, spacing: 2) {
                    HStack(spacing: 4) {
                        Image(systemName: "battery.75percent")
                            .foregroundStyle(batteryColor(context.state.batteryLevel))
                        Text("\(context.state.batteryLevel)%")
                            .font(.title3.bold())
                    }
                    if context.state.power != 0 {
                        Text("\(context.state.power) kW")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            // 하단: 주행 정보
            HStack {
                DrivingInfoItem(icon: "road.lanes", value: formatDistance(context.state.distanceKm), color: .cyan)
                Spacer()
                DrivingInfoItem(icon: "clock.fill", value: formatDuration(context.state.durationMin), color: .orange)
                Spacer()
                DrivingInfoItem(icon: "location.fill", value: context.attributes.startAddress, color: .green)
            }
        }
    }
}

// MARK: - Components

private struct DrivingInfoItem: View {
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
                .lineLimit(1)
        }
    }
}

// MARK: - Helpers

private func batteryColor(_ level: Int) -> Color {
    if level < 20 { return .red }
    if level < 50 { return .orange }
    return .green
}

private func formatDistance(_ km: Double) -> String {
    if km < 1 {
        return String(format: "%.0f m", km * 1000)
    }
    return String(format: "%.1f km", km)
}

private func formatDuration(_ minutes: Int) -> String {
    if minutes < 60 {
        return "\(minutes)분"
    }
    let h = minutes / 60
    let m = minutes % 60
    return "\(h)시간 \(m)분"
}

// MARK: - Preview

#Preview("Driving Lock Screen", as: .content, using: DrivingActivityAttributes(vehicleName: "Model Y", startAddress: "집")) {
    DrivingLiveActivity()
} contentStates: {
    DrivingActivityAttributes.ContentState(
        speed: 82,
        batteryLevel: 75,
        distanceKm: 23.4,
        power: -18,
        durationMin: 35,
        drivingState: "Driving"
    )
}
