import UIKit
import Social
import UniformTypeIdentifiers

/// 공유시트에서 텍스트/URL을 받아 App Group UserDefaults에 저장한 뒤 즉시 닫는다.
class ShareViewController: UIViewController {

    private let suiteName = "group.com.soooool.matedash"
    private let keyRaw = "share_text_raw"
    private let keyUpdated = "share_text_updated"
    private let keyDbgLast = "share_dbg_last"
    private let keyDbgLog = "share_dbg_log"

    private func log(_ msg: String) {
        NSLog("[MateDash] Share: %@", msg)
        print("[MateDash] Share: \(msg)")
        guard let defaults = UserDefaults(suiteName: suiteName) else { return }
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        let line = "\(f.string(from: Date())) \(msg)"
        defaults.set(line, forKey: keyDbgLast)
        var history = defaults.stringArray(forKey: keyDbgLog) ?? []
        history.append(line)
        if history.count > 20 { history.removeFirst(history.count - 20) }
        defaults.set(history, forKey: keyDbgLog)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        log("viewDidLoad")
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        log("viewDidAppear")
        handleIncoming()
    }

    private func handleIncoming() {
        guard let items = extensionContext?.inputItems as? [NSExtensionItem] else {
            log("no inputItems → abort")
            complete()
            return
        }
        log("items=\(items.count)")

        var textPieces: [String] = []
        var urlPieces: [String] = []
        let group = DispatchGroup()

        for (idx, item) in items.enumerated() {
            if let attributed = item.attributedContentText?.string, !attributed.isEmpty {
                log("item[\(idx)] attrText len=\(attributed.count)")
                textPieces.append(attributed)
            }
            guard let providers = item.attachments else {
                log("item[\(idx)] no attachments")
                continue
            }
            log("item[\(idx)] providers=\(providers.count)")
            for (pIdx, provider) in providers.enumerated() {
                let types = provider.registeredTypeIdentifiers
                log("  provider[\(pIdx)] types=\(types.joined(separator: ","))")
                if provider.hasItemConformingToTypeIdentifier(UTType.plainText.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.plainText.identifier, options: nil) { [weak self] item, err in
                        if let s = item as? String, !s.isEmpty {
                            self?.log("  loaded plainText len=\(s.count)")
                            textPieces.append(s)
                        } else {
                            self?.log("  plainText empty/err=\(err?.localizedDescription ?? "nil")")
                        }
                        group.leave()
                    }
                } else if provider.hasItemConformingToTypeIdentifier(UTType.text.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.text.identifier, options: nil) { [weak self] item, err in
                        if let s = item as? String, !s.isEmpty {
                            self?.log("  loaded text len=\(s.count)")
                            textPieces.append(s)
                        } else {
                            self?.log("  text empty/err=\(err?.localizedDescription ?? "nil")")
                        }
                        group.leave()
                    }
                }
                if provider.hasItemConformingToTypeIdentifier(UTType.url.identifier) {
                    group.enter()
                    provider.loadItem(forTypeIdentifier: UTType.url.identifier, options: nil) { [weak self] item, err in
                        if let url = item as? URL {
                            self?.log("  loaded URL=\(url.absoluteString)")
                            urlPieces.append(url.absoluteString)
                        } else if let s = item as? String {
                            self?.log("  loaded URL-as-string len=\(s.count)")
                            urlPieces.append(s)
                        } else {
                            self?.log("  url empty/err=\(err?.localizedDescription ?? "nil")")
                        }
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
            self.log("combined len=\(combined.count) text=\(textPieces.count) urls=\(urlPieces.count)")
            self.save(combined)
            self.complete()
        }
    }

    private func save(_ text: String) {
        guard let defaults = UserDefaults(suiteName: suiteName) else {
            log("save FAILED (no defaults)")
            return
        }
        defaults.set(text, forKey: keyRaw)
        defaults.set(Date().timeIntervalSinceReferenceDate, forKey: keyUpdated)
        defaults.synchronize()
        log("saved keyRaw len=\(text.count)")
    }

    private func complete() {
        log("complete")
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
