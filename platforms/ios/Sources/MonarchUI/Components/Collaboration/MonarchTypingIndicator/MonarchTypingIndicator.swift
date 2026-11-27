import SwiftUI

/// "Ana is typing…" under a conversation, with three bouncing dots.
///
/// Pass the names of the people who are typing now, without the current
/// user. The indicator words the sentence for one, two or several people.
///
/// ```swift
/// MonarchTypingIndicator(["Ana"])
/// MonarchTypingIndicator(typingNames, text: myLocalisedSentence)
/// ```
///
/// With an empty list nothing is drawn. By default the line keeps its
/// height, so the conversation above does not jump each time someone starts
/// or stops typing.
///
/// Accessibility: the dots are decorative and the sentence is one element.
/// A new sentence is announced once, without moving focus, unless
/// `announcesChanges` is false. The dots stand still when Reduce Motion is
/// on. The text and the dots scale with Dynamic Type.
public struct MonarchTypingIndicator: View {
    private let names: [String]
    private let text: String?
    private let reservesSpace: Bool
    private let announcesChanges: Bool

    @Environment(\.monarchTheme) private var theme

    /// A typing indicator.
    ///
    /// - Parameters:
    ///   - names: The display names of the people typing now, newest last.
    ///   - text: Replaces the built-in sentence, for a product that words it
    ///     itself. It is shown only while somebody is typing.
    ///   - reservesSpace: Whether the line keeps its height while nobody is
    ///     typing.
    ///   - announcesChanges: Whether a new sentence is announced to
    ///     VoiceOver.
    public init(
        _ names: [String],
        text: String? = nil,
        reservesSpace: Bool = true,
        announcesChanges: Bool = true
    ) {
        self.names = names
        self.text = text
        self.reservesSpace = reservesSpace
        self.announcesChanges = announcesChanges
    }

    /// The content of the view.
    public var body: some View {
        let sentence = MonarchTypingIndicatorTokens.sentence(names: names, text: text)
        ZStack(alignment: .leading) {
            if reservesSpace {
                Text(MonarchTypingIndicatorTokens.lineBox)
                    .monarchTextStyle(MonarchTypingIndicatorTokens.textStyle)
                    .hidden()
            }
            if !sentence.isEmpty {
                HStack(spacing: MonarchTypingIndicatorTokens.textSpacing) {
                    MonarchTypingDots()
                    Text(sentence)
                        .monarchTextStyle(MonarchTypingIndicatorTokens.textStyle)
                        .foregroundStyle(theme.colors[role: MonarchTypingIndicatorTokens.text])
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(sentence)
        .accessibilityHidden(sentence.isEmpty)
        .onChange(of: sentence) { _, newSentence in
            let announcement = MonarchTypingIndicatorTokens.announcement(
                sentence: newSentence,
                announcesChanges: announcesChanges
            )
            if let announcement {
                AccessibilityNotification.Announcement(announcement).post()
            }
        }
    }
}

/// The three dots. They bounce one after the other, or stand still when
/// Reduce Motion is on.
private struct MonarchTypingDots: View {
    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchTypingIndicatorTokens.dotSize
    @ScaledMetric(relativeTo: .caption) private var bounce = MonarchTypingIndicatorTokens.bounce
    @State private var isUp = false

    var body: some View {
        HStack(spacing: MonarchTypingIndicatorTokens.dotSpacing) {
            ForEach(0..<MonarchTypingIndicatorTokens.dotCount, id: \.self) { index in
                Circle()
                    .fill(theme.colors[role: MonarchTypingIndicatorTokens.dot])
                    .frame(width: dotSize, height: dotSize)
                    .offset(y: isUp && !reduceMotion ? -bounce : 0)
                    .animation(animation(index: index), value: isUp)
            }
        }
        .accessibilityHidden(true)
        .onAppear { isUp = true }
    }

    private func animation(index: Int) -> Animation? {
        guard !reduceMotion else { return nil }
        return .easeInOut(duration: MonarchTypingIndicatorTokens.bounceDuration)
            .repeatForever(autoreverses: true)
            .delay(MonarchTypingIndicatorTokens.stagger * TimeInterval(index))
    }
}

#if DEBUG
    #Preview("MonarchTypingIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTypingIndicator(["Ana"])
                MonarchTypingIndicator(["Ana", "Ben"])
                MonarchTypingIndicator(["Ana", "Ben", "Chen"])
                MonarchTypingIndicator([])
            }
        }
    }
#endif
