import WidgetKit
import SwiftUI

struct RecentChargingWidget: Widget {
    let kind = "RecentChargingWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: RecentChargingProvider()) { entry in
            RecentChargingWidgetView(entry: entry)
                .containerBackground(
                    Color(red: 0.043, green: 0.043, blue: 0.043),
                    for: .widget
                )
        }
        .configurationDisplayName("최근 충전")
        .description("마지막 충전 정보와 이번 달 충전 요약을 보여줍니다.")
        .supportedFamilies([.systemMedium])
    }
}

#Preview(as: .systemMedium) {
    RecentChargingWidget()
} timeline: {
    RecentChargingEntry.placeholder
}
