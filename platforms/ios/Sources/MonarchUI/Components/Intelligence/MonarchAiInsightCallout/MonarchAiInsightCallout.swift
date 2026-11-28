import SwiftUI

/// A proactive insight about the current record or screen: what was
/// noticed, why, the evidence and one action, with its origin shown
/// honestly as "AI insight" or "Rule-based".
///
/// ```swift
/// MonarchAiInsightCallout(
///     "Renewal at risk",
///     why: "Usage fell 40% in the last 30 days and two admins left.",
///     origin: .ai,
///     tone: .warning,
///     actionTitle: "Plan outreach",
///     action: { planOutreach() },
///     evidence: { Text("Weekly active users: 120 → 72") }
/// )
/// ```
///
/// The tone sets the fill, the border, the leading bar and the icon:
/// `info` for something to know, `warning` for a risk, `danger` for
/// something that needs action now, `success` for good news. Only the AI
/// origin uses the assistant colours and mark.
///
/// Accessibility: the title is read with the tone name first ("Warning:
/// Renewal at risk") and the origin label after it; the reason follows as
/// text. Evidence is a disclosure group, the action a button, and the
/// dismiss button is named "Dismiss insight". The app removes the callout
/// on dismiss and decides where focus goes.
public struct MonarchAiInsightCallout<Evidence: View>: View {
    private let title: String
    private let why: String
    private let origin: MonarchAiInsightOrigin
    private let tone: MonarchTone
    private let actionTitle: String?
    private let isHeading: Bool
    private let labels: MonarchAiInsightCalloutLabels
    private let action: (@MainActor () -> Void)?
    private let onDismiss: (@MainActor () -> Void)?
    private let evidence: Evidence?

    @Environment(\.monarchTheme) private var theme
    @State private var showsEvidence = false

    /// An insight callout with evidence.
    ///
    /// - Parameters:
    ///   - title: The insight in one line, already localised.
    ///   - why: Why it was raised, in plain words.
    ///   - origin: Whether a model or a fixed rule raised it. Always shown.
    ///   - tone: How much it matters.
    ///   - actionTitle: The title of the one follow-up action. Needs `action`.
    ///   - isHeading: Whether the title is a heading for VoiceOver navigation.
    ///   - labels: Replaces the built-in strings.
    ///   - action: The follow-up action.
    ///   - onDismiss: Adds a dismiss button.
    ///   - evidence: Supporting data, shown in a collapsed disclosure.
    public init(
        _ title: String,
        why: String,
        origin: MonarchAiInsightOrigin,
        tone: MonarchTone = .info,
        actionTitle: String? = nil,
        isHeading: Bool = false,
        labels: MonarchAiInsightCalloutLabels = MonarchAiInsightCalloutLabels(),
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil,
        @ViewBuilder evidence: () -> Evidence
    ) {
        self.title = title
        self.why = why
        self.origin = origin
        self.tone = tone
        self.actionTitle = actionTitle
        self.isHeading = isHeading
        self.labels = labels
        self.action = action
        self.onDismiss = onDismiss
        self.evidence = evidence()
    }

    /// The content of the view.
    public var body: some View {
        let surface = MonarchAiInsightCalloutTokens.surface(tone: tone)
        HStack(alignment: .top, spacing: MonarchAiInsightCalloutTokens.spacing) {
            MonarchIcon(systemName: MonarchAiInsightCalloutTokens.symbol(tone: tone), size: .sm)
                .foregroundStyle(theme.colors[role: surface.accent])
            VStack(alignment: .leading, spacing: MonarchAiInsightCalloutTokens.bodySpacing) {
                header
                // 14 points and muted, under the 16 point title: the title leads.
                Text(why)
                    .monarchTextStyle(.label, weight: .regular)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
                if let evidence {
                    DisclosureGroup(
                        labels.evidence ?? MonarchAiInsightCalloutStrings.evidence,
                        isExpanded: $showsEvidence
                    ) {
                        evidence
                    }
                    .disclosureGroupStyle(.monarch)
                }
                if let actionTitle, let action {
                    MonarchButton(actionTitle, size: .sm, action: action)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let onDismiss {
                Button(action: onDismiss) {
                    MonarchIcon(systemName: MonarchAiInsightCalloutTokens.dismissSymbol, size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                        .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
                .accessibilityLabel(labels.dismiss ?? MonarchAiInsightCalloutStrings.dismiss)
            }
        }
        .padding(.vertical, MonarchAiInsightCalloutTokens.padding)
        .padding(.leading, MonarchAiInsightCalloutTokens.leadingPadding)
        .padding(.trailing, MonarchAiInsightCalloutTokens.padding)
        .background(MonarchShapes.container.fill(theme.colors[role: surface.fill]))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(theme.colors[role: surface.border])
                .frame(width: MonarchAiInsightCalloutTokens.barWidth)
        }
        .clipShape(MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: surface.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        MonarchFlowLayout(
            spacing: MonarchAiInsightCalloutTokens.headerSpacing,
            rowSpacing: MonarchAiInsightCalloutTokens.originSpacing
        ) {
            Text(title)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityLabel(MonarchAiInsightCalloutTokens.spokenTitle(title, tone: tone, labels: labels))
                .accessibilityAddTraits(isHeading ? .isHeader : [])
            MonarchAiInsightOriginLabel(
                origin: origin,
                text: MonarchAiInsightCalloutTokens.originText(origin, labels: labels)
            )
        }
    }
}

extension MonarchAiInsightCallout where Evidence == EmptyView {
    /// An insight callout without evidence.
    ///
    /// - Parameters:
    ///   - title: The insight in one line, already localised.
    ///   - why: Why it was raised, in plain words.
    ///   - origin: Whether a model or a fixed rule raised it. Always shown.
    ///   - tone: How much it matters.
    ///   - actionTitle: The title of the one follow-up action. Needs `action`.
    ///   - isHeading: Whether the title is a heading for VoiceOver navigation.
    ///   - labels: Replaces the built-in strings.
    ///   - action: The follow-up action.
    ///   - onDismiss: Adds a dismiss button.
    public init(
        _ title: String,
        why: String,
        origin: MonarchAiInsightOrigin,
        tone: MonarchTone = .info,
        actionTitle: String? = nil,
        isHeading: Bool = false,
        labels: MonarchAiInsightCalloutLabels = MonarchAiInsightCalloutLabels(),
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.why = why
        self.origin = origin
        self.tone = tone
        self.actionTitle = actionTitle
        self.isHeading = isHeading
        self.labels = labels
        self.action = action
        self.onDismiss = onDismiss
        self.evidence = nil
    }
}

/// The origin chip: the assistant mark and "AI insight", or a checklist and "Rule-based".
private struct MonarchAiInsightOriginLabel: View {
    let origin: MonarchAiInsightOrigin
    let text: String

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(spacing: MonarchAiInsightCalloutTokens.originSpacing) {
            switch origin {
            case .ai: MonarchAiBotGlyph(size: .sm)
            case .rule: MonarchIcon(systemName: MonarchAiInsightCalloutTokens.ruleSymbol, size: .sm)
            }
            Text(text)
                .monarchTextStyle(.caption)
        }
        .padding(.horizontal, MonarchAiInsightCalloutTokens.originHorizontalPadding)
        .padding(.vertical, MonarchAiInsightCalloutTokens.originVerticalPadding)
        .monarchSurface(MonarchAiInsightCalloutTokens.origin(origin), in: Capsule())
    }
}

#if DEBUG
    #Preview("MonarchAiInsightCallout") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiInsightCallout(
                    "Renewal at risk",
                    why: "Usage fell 40% in the last 30 days.",
                    origin: .ai,
                    tone: .warning,
                    actionTitle: "Plan outreach",
                    action: {},
                    onDismiss: {},
                    evidence: { Text("Weekly active users: 120 to 72") }
                )
                MonarchAiInsightCallout("Invoice overdue", why: "Unpaid for 30 days.", origin: .rule, tone: .danger)
            }
        }
    }
#endif
