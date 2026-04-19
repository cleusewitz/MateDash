import WidgetKit

struct RecentChargingProvider: TimelineProvider {
    func placeholder(in context: Context) -> RecentChargingEntry {
        .placeholder
    }

    func getSnapshot(in context: Context, completion: @escaping (RecentChargingEntry) -> Void) {
        if context.isPreview {
            completion(.placeholder)
            return
        }
        Task {
            let entry = await fetchEntry()
            completion(entry)
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<RecentChargingEntry>) -> Void) {
        Task {
            let entry = await fetchEntry()
            let refreshDate = Calendar.current.date(byAdding: .minute, value: 30, to: .now)!
            let timeline = Timeline(entries: [entry], policy: .after(refreshDate))
            completion(timeline)
        }
    }

    private func fetchEntry() async -> RecentChargingEntry {
        guard let config = SharedConfig.load() else {
            return .noConfig
        }

        let client = TeslaMateAPIClient(config: config)

        do {
            let charges = try await client.fetchCharges(limit: 100)
            let sorted = charges.sorted { ($0.startDate ?? "") > ($1.startDate ?? "") }

            let now = Date()
            let calendar = Calendar.current
            let year = calendar.component(.year, from: now)
            let month = calendar.component(.month, from: now)
            let monthPrefix = String(format: "%04d-%02d", year, month)

            let monthCharges = sorted.filter { $0.startDate?.hasPrefix(monthPrefix) == true }

            let lastCharge = sorted.first
            let monthlyCount = monthCharges.count
            let monthlyEnergy = monthCharges.compactMap(\.chargeEnergyAdded).reduce(0, +)
            let costs = monthCharges.compactMap(\.cost)
            let monthlyCost: Double? = costs.isEmpty ? nil : costs.reduce(0, +)

            return RecentChargingEntry(
                date: .now,
                lastCharge: lastCharge,
                monthlyChargeCount: monthlyCount,
                monthlyTotalEnergy: monthlyEnergy,
                monthlyTotalCost: monthlyCost,
                monthLabel: "\(month)월",
                isPlaceholder: false,
                errorMessage: nil
            )
        } catch {
            let refreshDate = Calendar.current.date(byAdding: .minute, value: 15, to: .now)!
            return RecentChargingEntry(
                date: .now,
                lastCharge: nil,
                monthlyChargeCount: 0,
                monthlyTotalEnergy: 0,
                monthlyTotalCost: nil,
                monthLabel: "",
                isPlaceholder: false,
                errorMessage: "서버 연결 안됨"
            )
        }
    }
}
