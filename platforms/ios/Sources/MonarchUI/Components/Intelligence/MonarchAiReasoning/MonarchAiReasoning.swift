import SwiftUI

/// The model's reasoning for one answer, as a disclosure: "Thinking…" with a
/// live timer while it streams, then "Thought for 12s" once it finishes.
///
/// ```swift
/// if case .reasoning(let reasoning) = part {
///     MonarchAiReasoning(reasoning)
/// }
/// ```
///
/// It shows a ``MonarchAiReasoningPart`` from the message model. While the
/// part streams the disclosure is open, the text grows live and the header
/// is a ``MonarchAiThinkingIndicator`` with the elapsed time. When it
/// finishes the disclosure closes, unless the person opened or closed it
/// meanwhile, and the header reads "Thought for" the part's `durationMs`, or
/// the time this view measured. Redacted or empty reasoning is a still row
/// reading "Reasoning hidden" or "Thought", with no disclosure.
///
/// Accessibility: the header is the disclosure button of
/// ``MonarchDisclosureGroupStyle``, named "Assistant reasoning: Thought for
/// 12s" with an expanded or collapsed value. The still row is one element
/// with the same name. The live timer is not read on every tick; the header
/// is read again when VoiceOver reaches it. With Reduce Motion the thinking
/// mark stops pulsing and the content opens without animation.
public struct MonarchAiReasoning: View {
    private let part: MonarchAiReasoningPart
    private let labels: MonarchAiReasoningLabels
    private let onExpandedChange: (@MainActor (Bool) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var timeline: MonarchAiReasoningTimeline
    @State private var isExpanded: Bool
    @State private var isTouched = false

    /// A reasoning disclosure.
    ///
    /// - Parameters:
    ///   - part: The reasoning part from the message model.
    ///   - startedAt: When the reasoning started. Pass it when the view can be
    ///     rebuilt while the part streams (for example in a lazy list), so the
    ///     timer does not restart. The moment the view appears when `nil`.
    ///   - startsExpanded: Whether finished reasoning starts open, for history
    ///     or a debugging view. Streaming reasoning always starts open.
    ///   - labels: Replacements for the built-in strings.
    ///   - onExpandedChange: Called when the disclosure opens or closes, by the
    ///     person or automatically when the reasoning finishes.
    public init(
        _ part: MonarchAiReasoningPart,
        startedAt: Date? = nil,
        startsExpanded: Bool = false,
        labels: MonarchAiReasoningLabels = MonarchAiReasoningLabels(),
        onExpandedChange: (@MainActor (Bool) -> Void)? = nil
    ) {
        self.part = part
        self.labels = labels
        self.onExpandedChange = onExpandedChange
        let isStreaming = part.state == .streaming
        _timeline = State(
            initialValue: MonarchAiReasoningTimeline(
                isStreaming: isStreaming,
                start: startedAt ?? (isStreaming ? Date() : nil)
            )
        )
        _isExpanded = State(initialValue: isStreaming || startsExpanded)
    }

    private var isStreaming: Bool { part.state == .streaming }

    /// The content of the view.
    public var body: some View {
        let summary = MonarchAiReasoningTokens.summary(
            part: part,
            measuredMs: timeline.measuredMs,
            locale: locale,
            labels: labels
        )
        let spokenLabel = MonarchAiReasoningTokens.accessibilityLabel(summary: summary, labels: labels)
        Group {
            if MonarchAiReasoningTokens.hasBody(part) {
                DisclosureGroup(isExpanded: expansion) {
                    reasoningText
                } label: {
                    header(summary: summary)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(spokenLabel)
                }
                .disclosureGroupStyle(.monarch)
                .environment(\.monarchDisclosureChevron, .leading(\.textMuted))
            } else {
                stillRow(summary: summary)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(spokenLabel)
            }
        }
        .onChange(of: isStreaming) { _, streaming in
            update(isStreaming: streaming, now: Date())
        }
    }

    private var expansion: Binding<Bool> {
        Binding(
            get: { isExpanded },
            set: { newValue in
                if isStreaming { isTouched = true }
                guard newValue != isExpanded else { return }
                isExpanded = newValue
                onExpandedChange?(newValue)
            }
        )
    }

    @ViewBuilder private func header(summary: String) -> some View {
        if isStreaming {
            MonarchAiThinkingIndicator(
                label: summary,
                startedAt: timeline.start,
                variant: .mark,
                isAnnounced: false,
                elapsedLabel: labels.duration
            )
        } else {
            Text(summary)
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors[role: MonarchAiReasoningTokens.summaryText])
        }
    }

    @ViewBuilder private func stillRow(summary: String) -> some View {
        Group {
            if isStreaming {
                // No text yet: the live timer without a disclosure.
                header(summary: summary)
            } else {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiReasoningTokens.iconSpacing) {
                    MonarchAiBotGlyph(size: .sm)
                        .foregroundStyle(theme.colors[role: MonarchAiReasoningTokens.glyph])
                    Text(summary)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: MonarchAiReasoningTokens.summaryText])
                }
            }
        }
        .frame(maxWidth: .infinity, minHeight: MonarchAiReasoningTokens.rowMinHeight, alignment: .leading)
    }

    private var reasoningText: some View {
        Text(part.text)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors[role: MonarchAiReasoningTokens.bodyText])
            .textSelection(.enabled)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading, MonarchAiReasoningTokens.bodyInset)
            .overlay(alignment: .leading) {
                Rectangle()
                    .fill(theme.colors[role: MonarchAiReasoningTokens.rule])
                    .frame(width: MonarchAiReasoningTokens.ruleWidth)
                    .accessibilityHidden(true)
            }
    }

    private func update(isStreaming streaming: Bool, now: Date) {
        let transition = timeline.update(isStreaming: streaming, now: now)
        if transition == .started { isTouched = false }
        let next = MonarchAiReasoningTokens.expanded(
            after: transition,
            isExpanded: isExpanded,
            isTouched: isTouched
        )
        guard next != isExpanded else { return }
        isExpanded = next
        onExpandedChange?(next)
    }
}

#if DEBUG
    #Preview("MonarchAiReasoning") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiReasoning(
                    MonarchAiReasoningPart(
                        text: "The person asks for the cheapest flight. I should compare the three fares first.",
                        state: .streaming
                    ),
                    startedAt: Date(timeIntervalSinceNow: -8)
                )
                MonarchAiReasoning(
                    MonarchAiReasoningPart(text: "Compared three fares and picked the lowest.", durationMs: 12_000)
                )
                MonarchAiReasoning(
                    MonarchAiReasoningPart(text: "Checked the dates.", durationMs: 65_000),
                    startsExpanded: true
                )
                MonarchAiReasoning(MonarchAiReasoningPart(text: "", durationMs: 4_000, isRedacted: true))
            }
        }
    }
#endif
