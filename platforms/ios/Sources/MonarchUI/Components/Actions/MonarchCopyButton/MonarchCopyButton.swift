import SwiftUI

/// A button that copies text to the pasteboard and confirms it.
///
/// ```swift
/// MonarchCopyButton(value: webhookURL, target: "webhook URL")
/// ```
///
/// After a copy the icon becomes a checkmark and the title becomes "Copied"
/// for two seconds, the device plays the success haptic, and VoiceOver
/// announces "Copied to clipboard". The button keeps the width of the longer
/// title, so the layout does not move.
///
/// Accessibility: it is a standard `Button`. `target` is added to the spoken
/// name ("Copy webhook URL"), so several copy buttons on one screen have
/// different names. With `isIconOnly` the title is still the spoken name.
///
/// With `isSensitive`, for secrets and recovery codes, the copied text stays
/// on this device (no Universal Clipboard) and leaves the pasteboard after
/// two minutes.
public struct MonarchCopyButton: View {
    private let title: String?
    private let value: String
    private let target: String?
    private let systemImage: String?
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let isIconOnly: Bool
    private let isSensitive: Bool
    private let copiedTitle: String?
    private let copiedAnnouncement: String?
    private let resetAfter: Duration
    private let onCopy: (@MainActor (String) -> Void)?

    @State private var isCopied = false
    @State private var copyCount = 0

    /// A copy button.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in title "Copy". Already localised.
    ///   - value: The text to copy.
    ///   - target: What is copied, for VoiceOver, such as "webhook URL".
    ///   - systemImage: Replaces the copy symbol shown before a copy.
    ///   - variant: The visual weight.
    ///   - size: The height and padding.
    ///   - isIconOnly: Whether only the icon is shown.
    ///   - isSensitive: Keeps the text on this device and removes it from the pasteboard after two minutes.
    ///   - copiedTitle: Replaces the built-in title "Copied".
    ///   - copiedAnnouncement: Replaces the built-in announcement "Copied to clipboard".
    ///   - resetAfter: How long the copied state lasts. Two seconds by default; zero keeps it.
    ///   - onCopy: Runs with the copied text after each copy.
    public init(
        _ title: String? = nil,
        value: String,
        target: String? = nil,
        systemImage: String? = nil,
        variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .md,
        isIconOnly: Bool = false,
        isSensitive: Bool = false,
        copiedTitle: String? = nil,
        copiedAnnouncement: String? = nil,
        resetAfter: Duration = .seconds(2),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.title = title
        self.value = value
        self.target = target
        self.systemImage = systemImage
        self.variant = variant
        self.size = size
        self.isIconOnly = isIconOnly
        self.isSensitive = isSensitive
        self.copiedTitle = copiedTitle
        self.copiedAnnouncement = copiedAnnouncement
        self.resetAfter = resetAfter
        self.onCopy = onCopy
    }

    /// The content of the view.
    public var body: some View {
        let idleTitle = title ?? MonarchCopyButtonStrings.copy
        let doneTitle = copiedTitle ?? MonarchCopyButtonStrings.copied
        Button {
            copy()
        } label: {
            HStack(spacing: MonarchButtonTokens.iconSpacing) {
                MonarchIcon(
                    systemName: MonarchCopyButtonTokens.symbol(isCopied: isCopied, custom: systemImage),
                    size: .sm
                )
                if !isIconOnly {
                    // Both titles share one cell, so the width does not change.
                    ZStack {
                        Text(idleTitle).opacity(isCopied ? 0 : 1)
                        Text(doneTitle).opacity(isCopied ? 1 : 0)
                    }
                }
            }
        }
        .buttonStyle(.monarch(variant, size: size))
        .accessibilityLabel(
            MonarchCopyButtonTokens.accessibilityLabel(title: isCopied ? doneTitle : idleTitle, target: target)
        )
        .sensoryFeedback(.success, trigger: copyCount)
        .task(id: copyCount) { await resetWhenDue() }
    }

    private func copy() {
        MonarchCopyButtonPasteboard.write(value, isSensitive: isSensitive)
        isCopied = true
        copyCount += 1
        AccessibilityNotification.Announcement(copiedAnnouncement ?? MonarchCopyButtonStrings.announcement).post()
        onCopy?(value)
    }

    /// Returns to the idle look after `resetAfter`. A newer copy cancels this
    /// task and starts another, so only the last copy ends the state.
    private func resetWhenDue() async {
        guard MonarchCopyButtonTokens.resets(copyCount: copyCount, after: resetAfter) else { return }
        try? await Task.sleep(for: resetAfter)
        if !Task.isCancelled {
            isCopied = false
        }
    }
}

#if DEBUG
    #Preview("MonarchCopyButton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCopyButton(value: "https://example.com/hook", target: "webhook URL")
                MonarchCopyButton("Copy key", value: "sk_test", variant: .primary, size: .lg)
                MonarchCopyButton(value: "sk_test", target: "API key", variant: .ghost, isIconOnly: true)
                MonarchCopyButton(value: "sk_test").disabled(true)
            }
        }
    }
#endif
