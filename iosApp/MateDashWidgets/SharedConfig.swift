import Foundation

struct ApiWidgetConfig {
    let baseURL: String
    let apiToken: String
    let carId: Int
}

enum SharedConfig {
    private static let suiteName = "group.com.soooool.matedash"
    private static let keyBaseURL = "api_base_url"
    private static let keyApiToken = "api_token"
    private static let keyCarId = "api_car_id"

    static func load() -> ApiWidgetConfig? {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return nil }
        guard let baseURL = defaults.string(forKey: keyBaseURL), !baseURL.isEmpty else { return nil }
        let apiToken = defaults.string(forKey: keyApiToken) ?? ""
        let carId = defaults.integer(forKey: keyCarId)
        return ApiWidgetConfig(
            baseURL: baseURL,
            apiToken: apiToken,
            carId: carId == 0 ? 1 : carId
        )
    }
}
