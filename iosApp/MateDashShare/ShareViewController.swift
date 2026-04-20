import UIKit
import Social
import UniformTypeIdentifiers

/// 공유시트에서 텍스트/URL을 받아 App Group UserDefaults에 저장한 뒤 즉시 닫는다.
class ShareViewController: UIViewController {

    private let suiteName = "group.com.soooool.matedash"
    private let keyRaw = "share_text_raw"
    private let keyUpdated = "share_text_updated"

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        handleIncoming()
    }

    private func handleIncoming() {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem] else {
            complete()
            return
        }

        var textPieces: [String] = []
        var urlPieces: [String] = []
        let group = DispatchGroup()

        for item in items {
            if let attributed = item.attributedContentText?.string, !attributed.isEmpty {
                textPieces.append(attributed)
            }
            guard let providers = item.attachments else { continue }
            for provider in providers {
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { item, _ in
                        if let s = item as? String, !s.isEmpty { textPieces.append(s) }
                        group.leave()
                    }
                } else if provider.hasItemConformingToTypeIdentifier(UTType.text.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.text.identifier, options: nil) { item, _ in
                        if let s = item as? String, !s.isEmpty { textPieces.append(s) }
                        group.leave()
                    }
                }
                if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { item, _ in
                        if let url = item as? URL { urlPieces.append(url.absoluteString) }
                        else if let s = item as? String { urlPieces.append(s) }
                        group.leave()
                    }
                }
            }
        }

        group.notify(queue: .main) { [weak self] in
            guard let self else { return }
            var combined = textPieces.joined(separator: "\n").trimmingCharacters(in: .whitespacesAndNewlines)
            for u in urlPieces where !combined.contains(u) {
                combined = combined.isEmpty ? u : "\(combined)\n\(u)"
            }
            self.save(combined)
            self.complete()
        }
    }

    private func save(_ text: String) {
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        defaults.set(text, forKey: keyRaw)
        defaults.set(Date().timeIntervalSinceReferenceDate, forKey: keyUpdated)
        defaults.synchronize()
    }

    private func complete() {
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
