import SwiftUI

/// Shows that the assistant has the request and is working before its first
/// token arrives: "Thinking…" with a soft shimmer or a pulsing assistant
/// mark, and after a few seconds how long it has been working.
///
/// ```swift
/// if conversation.status == .submitted {
///     MonarchAiThinkingIndicator()
/// }
/// MonarchAiThinkingIndicator(label: "Searching your files…", startedAt: start, variant: .mark)
/// ```
///
/// Pass `startedAt` when the view can be rebuilt during one turn (for
/// example in a lazy list), so the elapsed time does not restart.
///
/// Accessibility: one element read as the label, with the elapsed time as
/// its value and the updates-frequently trait. The label is announced when
/// the indicator appears and again when it changes; the elapsed time is not
/// announced. Under Reduce Motion the shimmer and the pulse stop and the
/// clock still ticks once a second.
public struct MonarchAiThinkingIndicator: View {
    private let label: String?
    private let startedAt: Date?
    private let variant: MonarchAiThinkingIndicatorVariant
    private let showsElapsedAfter: TimeInterval
    private let isAnnounced: Bool
    private let elapsedLabel: (@Sendable (Int) -> String)?

    @State private var appearedAt = Date()
    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// A thinking indicator.
    ///
    /// - Parameters:
    ///   - label: Replaces "Thinking…", for example "Searching your files…".
    ///   - startedAt: When the assistant started working. The moment the
    ///     indicator first appeared when `nil`.
    ///   - variant: A shimmer across the label, or a pulsing mark.
    ///   - showsElapsedAfter: Seconds before the elapsed time appears.
    ///     `.infinity` never shows it.
    ///   - isAnnounced: Whether VoiceOver announces the label when it appears
    ///     and when it changes.
    ///   - elapsedLabel: Formats whole elapsed seconds. "1m 5s" in the
    ///     environment's locale by default.
    public init(
        label: String? = nil,
        startedAt: Date? = nil,
        variant: MonarchAiThinkingIndicatorVariant = .shimmer,
        showsElapsedAfter: TimeInterval = 3,
        isAnnounced: Bool = true,
        elapsedLabel: (@Sendable (Int) -> String)? = nil
    ) {
        self.label = label
        self.startedAt = startedAt
        self.variant = variant
        self.showsElapsedAfter = showsElapsedAfter
        self.isAnnounced = isAnnounced
        self.elapsedLabel = elapsedLabel
    }

    private var text: String { label ?? MonarchAiThinkingIndicatorStrings.thinking }

    /// The content of the view.
    public var body: some View {
        TimelineView(.animation(minimumInterval: reduceMotion ? 1 : nil)) { context in
            let now = context.date
            let time: TimeInterval? = reduceMotion ? nil : now.timeIntervalSinceReferenceDate
            let seconds = MonarchAiThinkingIndicatorTokens.elapsedSeconds(now: now, start: startedAt ?? appearedAt)
            let elapsed =
                MonarchAiThinkingIndicatorTokens.showsElapsed(seconds: seconds, after: showsElapsedAfter)
                ? (elapsedLabel?(seconds)
                    ?? MonarchAiThinkingIndicatorTokens.elapsedText(seconds: seconds, locale: locale))
                : nil
            content(time: time, elapsed: elapsed)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(text)
                .accessibilityValue(elapsed ?? "")
                .accessibilityAddTraits(.updatesFrequently)
        }
        .onAppear { announce() }
        .onChange(of: text) { announce() }
    }

    private func announce() {
        if isAnnounced {
            AccessibilityNotification.Announcement(text).post()
        }
    }

    private func content(time: TimeInterval?, elapsed: String?) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchAiThinkingIndicatorTokens.spacing) {
            if variant == .mark {
                MonarchAiBotGlyph(size: .sm)
                    .foregroundStyle(theme.colors[role: MonarchAiThinkingIndicatorTokens.accent])
                    .opacity(MonarchAiThinkingIndicatorTokens.markOpacity(at: time))
                    .alignmentGuide(.firstTextBaseline) { $0[VerticalAlignment.center] }
            }
            labelText(time: time)
            if let elapsed {
                Text(elapsed)
                    .monarchTextStyle(.caption)
                    .monarchTabularNumber()
                    .foregroundStyle(theme.colors[role: MonarchAiThinkingIndicatorTokens.elapsed])
            }
        }
    }

    @ViewBuilder private func labelText(time: TimeInterval?) -> some View {
        let base = theme.colors[role: MonarchAiThinkingIndicatorTokens.label]
        if variant == .shimmer, let time {
            let band = MonarchAiThinkingIndicatorTokens.bandLocations(
                centre: MonarchAiThinkingIndicatorTokens.bandCentre(at: time)
            )
            let accent = theme.colors[role: MonarchAiThinkingIndicatorTokens.accent]
            let isRightToLeft = layoutDirection == .rightToLeft
            Text(text)
                .monarchTextStyle(.body)
                .foregroundStyle(
                    LinearGradient(
                        stops: [
                            .init(color: base, location: band.start),
                            .init(color: accent, location: band.centre),
                            .init(color: base, location: band.end),
                        ],
                        // A gradient's unit points are not mirrored, so the
                        // sweep is turned round for right-to-left text.
                        startPoint: isRightToLeft ? .trailing : .leading,
                        endPoint: isRightToLeft ? .leading : .trailing
                    )
                )
        } else {
            Text(text)
                .monarchTextStyle(.body)
                .foregroundStyle(base)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiThinkingIndicator") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiThinkingIndicator(isAnnounced: false)
                MonarchAiThinkingIndicator(
                    label: "Searching your files…",
                    startedAt: Date(timeIntervalSinceNow: -65),
                    variant: .mark,
                    isAnnounced: false
                )
            }
        }
    }
#endif
