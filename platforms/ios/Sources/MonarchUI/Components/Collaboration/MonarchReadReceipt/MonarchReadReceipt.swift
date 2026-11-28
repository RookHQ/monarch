import MonarchTokens
import SwiftUI

/// "Seen" under the newest message that the other people have read.
///
/// ```swift
/// MonarchReadReceipt([MonarchReadReceiptReader(name: "Ana")])
/// MonarchReadReceipt(readers, display: .count) { showReaders = true }
/// ```
///
/// The product decides which message carries the receipt and who has read
/// it; leave the sender out. Nothing is drawn while `readers` is empty. The
/// receipt shows "Seen", "Seen by 3" or the readers' avatars. Place it in
/// the conversation's column: under your own messages it is usually aligned
/// to the trailing edge.
///
/// Accessibility: the receipt is one element that always reads the full
/// sentence, "Seen by Ana, Ben and Chen", whatever it shows. With
/// `onShowReaders` it is a button with a touch target at least 44 points
/// high. The mark and the avatars are decorative.
public struct MonarchReadReceipt: View {
    private let readers: [MonarchReadReceiptReader]
    private let display: MonarchReadReceiptDisplay
    private let maxAvatars: Int
    private let labels: MonarchReadReceiptLabels
    private let onShowReaders: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A read receipt.
    ///
    /// - Parameters:
    ///   - readers: The people who have read the message, without the sender.
    ///   - display: Shows "Seen", "Seen by 3" or the avatars.
    ///   - maxAvatars: With the avatars display, at most this many faces, then "+N".
    ///   - labels: Replacements for the built-in strings.
    ///   - onShowReaders: Makes the receipt a button, for example to list the readers with times.
    public init(
        _ readers: [MonarchReadReceiptReader],
        display: MonarchReadReceiptDisplay = .text,
        maxAvatars: Int = 3,
        labels: MonarchReadReceiptLabels = MonarchReadReceiptLabels(),
        onShowReaders: (@MainActor () -> Void)? = nil
    ) {
        self.readers = readers
        self.display = display
        self.maxAvatars = maxAvatars
        self.labels = labels
        self.onShowReaders = onShowReaders
    }

    /// The content of the view.
    public var body: some View {
        if !readers.isEmpty {
            let sentence = MonarchReadReceiptTokens.sentence(readers, labels: labels, locale: locale)
            Group {
                if let onShowReaders {
                    Button(action: onShowReaders) {
                        visual.frame(minHeight: MonarchAccessibility.minimumHitTarget).contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                } else {
                    visual
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(sentence)
            .accessibilityAddTraits(onShowReaders == nil ? [.isStaticText] : [.isButton])
            .help(sentence)
        }
    }

    private var visual: some View {
        HStack(spacing: MonarchReadReceiptTokens.spacing) {
            if display == .avatars {
                // Overlapping faces with a surface ring, then "+N" as text, as the web.
                let shown = MonarchReadReceiptTokens.visibleAvatars(total: readers.count, maxAvatars: maxAvatars)
                HStack(spacing: -MonarchTokens.Space.space0_5) {
                    ForEach(Array(readers.prefix(shown))) { reader in
                        MonarchAvatar(
                            url: reader.avatarURL,
                            name: reader.name,
                            seed: reader.id,
                            size: MonarchReadReceiptTokens.avatarSize,
                            isDecorative: true
                        )
                        .overlay(Circle().strokeBorder(theme.colors.bgSurface, lineWidth: 1))
                    }
                }
                .accessibilityHidden(true)
                if readers.count > shown {
                    Text("+\(readers.count - shown)")
                        .monarchTextStyle(MonarchReadReceiptTokens.textStyle)
                        .monospacedDigit()
                        .accessibilityHidden(true)
                }
            } else {
                // Two overlapping ticks, as the web draws "Seen".
                HStack(spacing: -MonarchTokens.Space.space2) {
                    MonarchIcon(systemName: MonarchReadReceiptTokens.seenSymbol, size: .sm)
                    MonarchIcon(systemName: MonarchReadReceiptTokens.seenSymbol, size: .sm)
                }
                .accessibilityHidden(true)
            }
            Text(MonarchReadReceiptTokens.visibleText(display, count: readers.count, labels: labels))
                .monarchTextStyle(MonarchReadReceiptTokens.textStyle)
        }
        .foregroundStyle(theme.colors[role: MonarchReadReceiptTokens.foreground])
    }
}

#if DEBUG
    #Preview("MonarchReadReceipt") {
        let readers = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth"].map {
            MonarchReadReceiptReader(name: $0)
        }
        MonarchPreviewMatrix {
            VStack(alignment: .trailing) {
                MonarchReadReceipt(Array(readers.prefix(1)))
                MonarchReadReceipt(readers, display: .count)
                MonarchReadReceipt(readers, display: .avatars)
                MonarchReadReceipt(Array(readers.prefix(4)), display: .avatars, onShowReaders: {})
            }
        }
    }
#endif
