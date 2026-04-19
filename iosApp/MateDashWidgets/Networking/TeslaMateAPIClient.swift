import Foundation

struct TeslaMateAPIClient {
    let baseURL: String
    let apiToken: String
    let carId: Int

    init(config: ApiWidgetConfig) {
        self.baseURL = config.baseURL
        self.apiToken = config.apiToken
        self.carId = config.carId
    }

    private var decoder: JSONDecoder {
        let d = JSONDecoder()
        d.keyDecodingStrategy = .convertFromSnakeCase
        return d
    }

    private func request(path: String) -> URLRequest {
        let url = URL(string: "\(baseURL)\(path)")!
        var req = URLRequest(url: url, timeoutInterval: 15)
        if !apiToken.isEmpty {
            req.setValue("Bearer \(apiToken)", forHTTPHeaderField: "Authorization")
        }
        return req
    }

    func fetchCharges(limit: Int = 100) async throws -> [ChargeDTO] {
        let req = request(path: "/api/v1/cars/\(carId)/charges?limit=\(limit)")
        let (data, _) = try await URLSession.shared.data(for: req)
        let response = try decoder.decode(ChargesResponse.self, from: data)
        return response.data?.charges ?? []
    }

    func fetchCarStatus() async throws -> CarStatusDTO {
        let req = request(path: "/api/v1/cars/\(carId)/status")
        let (data, _) = try await URLSession.shared.data(for: req)
        let response = try decoder.decode(ApiStatusResponse.self, from: data)
        guard let status = response.data?.status else {
            throw URLError(.cannotParseResponse)
        }
        return status
    }

    func fetchDrives(limit: Int = 20) async throws -> [DriveDTO] {
        let req = request(path: "/api/v1/cars/\(carId)/drives?limit=\(limit)")
        let (data, _) = try await URLSession.shared.data(for: req)
        let response = try decoder.decode(DrivesResponse.self, from: data)
        return response.data?.drives ?? []
    }
}
