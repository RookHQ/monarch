// UIKit (AppKit on the macOS build host) because SwiftUI has no API that
// writes to the pasteboard.
import UniformTypeIdentifiers

#if canImport(UIKit)
    import UIKit
#elseif canImport(AppKit)
    import AppKit
#endif

/// Writes text to the system pasteboard.
enum MonarchCopyButtonPasteboard {
    /// How long sensitive text stays on the pasteboard: two minutes.
    static let sensitiveLifetime: TimeInterval = 120

    /// Replaces the contents of the general pasteboard with `text`.
    ///
    /// Sensitive text (a secret, a recovery code) stays on this device, so
    /// Universal Clipboard does not send it to the person's other devices,
    /// and is removed from the pasteboard after ``sensitiveLifetime``.
    @MainActor
    static func write(_ text: String, isSensitive: Bool = false, now: Date = Date()) {
        #if canImport(UIKit)
            if isSensitive {
                UIPasteboard.general.setItems(
                    [[UTType.utf8PlainText.identifier: text]],
                    options: options(isSensitive: true, now: now)
                )
            } else {
                UIPasteboard.general.string = text
            }
        #elseif canImport(AppKit)
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(text, forType: .string)
            if isSensitive {
                // The convention password managers use: clipboard history apps skip it.
                let concealed = NSPasteboard.PasteboardType("org.nspasteboard.ConcealedType")
                NSPasteboard.general.setString("", forType: concealed)
            }
        #endif
    }

    #if canImport(UIKit)
        /// The pasteboard options of a write: local only and expiring for sensitive text.
        static func options(isSensitive: Bool, now: Date) -> [UIPasteboard.OptionsKey: Any] {
            guard isSensitive else { return [:] }
            return [.localOnly: true, .expirationDate: now.addingTimeInterval(sensitiveLifetime)]
        }
    #endif
}
