// UIKit (AppKit on the macOS build host) because SwiftUI has no API that
// writes to the pasteboard, and none that sets pasteboard options.
#if canImport(UIKit)
    import UIKit
    import UniformTypeIdentifiers
#elseif canImport(AppKit)
    import AppKit
#endif

/// Writes a secret to the system pasteboard, marked as sensitive where the
/// platform allows it.
enum MonarchSecretFieldPasteboard {
    /// The pasteboard type that macOS clipboard managers read as "do not
    /// record". It is a community convention, not an Apple API.
    static let concealedType = "org.nspasteboard.ConcealedType"

    /// Replaces the contents of the general pasteboard with `text`.
    ///
    /// On iOS the item is local only, so it is not offered to the user's
    /// other devices through Universal Clipboard. On macOS it also carries
    /// the concealed type.
    @MainActor
    static func write(_ text: String) {
        #if canImport(UIKit)
            UIPasteboard.general.setItems(
                [[UTType.utf8PlainText.identifier: text]],
                options: [.localOnly: true]
            )
        #elseif canImport(AppKit)
            let concealed = NSPasteboard.PasteboardType(concealedType)
            NSPasteboard.general.clearContents()
            NSPasteboard.general.declareTypes([.string, concealed], owner: nil)
            NSPasteboard.general.setString(text, forType: .string)
            NSPasteboard.general.setString("", forType: concealed)
        #endif
    }
}
