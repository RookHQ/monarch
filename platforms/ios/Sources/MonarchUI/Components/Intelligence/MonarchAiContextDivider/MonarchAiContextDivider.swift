import SwiftUI

/// A labelled rule between messages in a thread: a calendar day, a context
/// reset, or a point where earlier messages were summarized.
///
/// ```swift
/// MonarchAiContextDivider(.day, date: message.date)
/// MonarchAiContextDivider(.contextReset)
/// MonarchAiContextDivider(.summarized, summary: summary) {
///     loadEarlierMessages()
/// }
/// ```
///
/// The day label is "Today", "Yesterday", a weekday and date for this year,
/// or a full date, in the locale, calendar and time zone of the environment.
/// A summarized divider shows the summary behind a standard `DisclosureGroup`
/// and, with `onExpand`, a button that shows the original messages.
///
/// Accessibility: a divider without controls is one element, read as its
/// label and then its description. A context reset has dashed lines, so it
/// does not depend on colour. With controls, the label, the disclosure and
/// the button are separate elements, the summary is read after "AI-generated
/// summary:", and each control has a 44-point touch target.
public struct MonarchAiContextDivider: View {
    private let kind: MonarchAiContextDividerKind
    private let date: Date?
    private let summary: String?
    private let now: Date?
    private let label: String?
    private let detail: String?
    private let summaryTitle: String?
    private let summaryPrefix: String?
    private let expandTitle: String?
    private let onExpand: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.locale) private var locale
    @State private var isSummaryExpanded = false

    /// A context divider.
    ///
    /// - Parameters:
    ///   - kind: What the divider marks.
    ///   - date: The day, for a `day` divider.
    ///   - summary: For `summarized`: the summary the assistant now works from, shown behind a disclosure.
    ///   - now: The reference time for "Today" and "Yesterday". The default is the current time.
    ///   - label: Replaces the built-in label of the kind.
    ///   - detail: Replaces the built-in description of a context reset.
    ///   - summaryTitle: Replaces the built-in "Show summary".
    ///   - summaryPrefix: Replaces the built-in "AI-generated summary:" that VoiceOver reads before the summary.
    ///   - expandTitle: Replaces the built-in "Show earlier messages".
    ///   - onExpand: For `summarized`: shows a button that runs this to reveal the original messages.
    public init(
        _ kind: MonarchAiContextDividerKind,
        date: Date? = nil,
        summary: String? = nil,
        now: Date? = nil,
        label: String? = nil,
        detail: String? = nil,
        summaryTitle: String? = nil,
        summaryPrefix: String? = nil,
        expandTitle: String? = nil,
        onExpand: (@MainActor () -> Void)? = nil
    ) {
        self.kind = kind
        self.date = date
        self.summary = summary
        self.now = now
        self.label = label
        self.detail = detail
        self.summaryTitle = summaryTitle
        self.summaryPrefix = summaryPrefix
        self.expandTitle = expandTitle
        self.onExpand = onExpand
    }

    private var text: String {
        var zoned = calendar
        zoned.timeZone = timeZone
        return MonarchAiContextDividerTokens.label(
            kind: kind,
            date: date,
            now: now ?? Date(),
            calendar: zoned,
            locale: locale,
            override: label
        )
    }

    private var detailText: String? {
        kind == .contextReset ? (detail ?? MonarchAiContextDividerStrings.contextResetDescription) : nil
    }

    /// The content of the view.
    public var body: some View {
        let isInteractive = MonarchAiContextDividerTokens.isInteractive(
            kind: kind,
            hasSummary: summary != nil,
            hasExpand: onExpand != nil
        )
        Group {
            if isInteractive {
                VStack(spacing: MonarchAiContextDividerTokens.stackSpacing) {
                    rule
                    controls
                }
                .accessibilityElement(children: .contain)
            } else {
                VStack(spacing: MonarchAiContextDividerTokens.stackSpacing) {
                    rule
                    if let detailText {
                        Text(detailText)
                            .monarchTextStyle(.caption)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchAiContextDividerTokens.accessibilityLabel(label: text, detail: detailText)
                )
            }
        }
        .padding(.vertical, MonarchAiContextDividerTokens.verticalPadding)
        .frame(maxWidth: .infinity)
    }

    private var rule: some View {
        HStack(spacing: MonarchAiContextDividerTokens.ruleSpacing) {
            line
            Text(text)
                .monarchTextStyle(.caption, weight: MonarchAiContextDividerTokens.labelWeight)
                .multilineTextAlignment(.center)
                .foregroundStyle(theme.colors[role: MonarchAiContextDividerTokens.labelRole(kind)])
                .layoutPriority(1)
            line
        }
    }

    private var line: some View {
        let dash = MonarchAiContextDividerTokens.dash
        return MonarchAiContextDividerLine()
            .stroke(
                theme.colors[role: MonarchAiContextDividerTokens.lineRole(kind)],
                style: StrokeStyle(
                    lineWidth: MonarchAiContextDividerTokens.lineWidth,
                    dash: MonarchAiContextDividerTokens.isDashed(kind) ? [dash, dash] : []
                )
            )
            .frame(minWidth: MonarchAiContextDividerTokens.lineMinWidth)
            .frame(height: MonarchAiContextDividerTokens.lineWidth)
            .accessibilityHidden(true)
    }

    @ViewBuilder private var controls: some View {
        if let summary {
            DisclosureGroup(isExpanded: $isSummaryExpanded) {
                summaryBody(summary)
            } label: {
                Text(summaryTitle ?? MonarchAiContextDividerStrings.showSummary)
                    .monarchTextStyle(.label)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            }
            .tint(theme.colors.textLink)
            .foregroundStyle(theme.colors.textLink)
        }
        if let onExpand {
            Button(expandTitle ?? MonarchAiContextDividerStrings.expand, action: onExpand)
                .buttonStyle(.monarch(.ghost, size: .sm))
        }
    }

    private func summaryBody(_ summary: String) -> some View {
        let shape = RoundedRectangle(
            cornerRadius: MonarchAiContextDividerTokens.summaryCornerRadius,
            style: .continuous
        )
        return Text(summary)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, MonarchAiContextDividerTokens.summaryHorizontalPadding)
            .padding(.vertical, MonarchAiContextDividerTokens.summaryVerticalPadding)
            .background(shape.fill(theme.colors.aiSubtle))
            .overlay(
                shape.strokeBorder(theme.colors.aiBorder, lineWidth: MonarchAiContextDividerTokens.lineWidth)
            )
            .accessibilityLabel(
                MonarchAiContextDividerTokens.summaryAccessibilityLabel(
                    prefix: summaryPrefix ?? MonarchAiContextDividerStrings.summaryPrefix,
                    summary: summary
                )
            )
    }
}

/// A horizontal line through the middle of its frame, so it can be dashed.
private struct MonarchAiContextDividerLine: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.minX, y: rect.midY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.midY))
        return path
    }
}

#if DEBUG
    #Preview("MonarchAiContextDivider") {
        MonarchPreviewMatrix {
            VStack {
                MonarchAiContextDivider(.day, date: Date())
                MonarchAiContextDivider(.contextReset)
                MonarchAiContextDivider(.summarized)
                MonarchAiContextDivider(.summarized, summary: "The team agreed to ship the report on Friday.") {}
            }
        }
    }
#endif
