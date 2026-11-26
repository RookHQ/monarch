import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiInsightCallout``.
enum MonarchAiInsightCalloutTokens {
    /// The gap between the icon, the body and the dismiss button.
    static let spacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the parts of the body.
    static let bodySpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the title and the origin label.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the callout.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// Extra padding on the leading side, past the tone bar.
    static let leadingPadding: CGFloat = MonarchTokens.Space.space4
    /// The width of the tone bar on the leading edge.
    static let barWidth: CGFloat = MonarchTokens.Space.space1
    /// The gap between the origin symbol and its text.
    static let originSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the origin label's text.
    static let originHorizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below the origin label's text.
    static let originVerticalPadding: CGFloat = MonarchTokens.Space.px
    /// The symbol of a rule-made insight: a checklist, not an assistant mark.
    static let ruleSymbol = "checklist"
    /// The symbol of the dismiss button.
    static let dismissSymbol = "xmark"

    /// The roles of the callout for a tone: the tone's subtle fill and
    /// border, and its strong colour for the icon and the bar.
    static func surface(tone: MonarchTone) -> (
        fill: MonarchColorRole, border: MonarchColorRole, accent: MonarchColorRole
    ) {
        let palette = MonarchTonePalette.palette(for: tone)
        return (palette.subtle, palette.border, palette.strong)
    }

    /// The symbol of a tone.
    static func symbol(tone: MonarchTone) -> String {
        MonarchTonePalette.palette(for: tone).symbol
    }

    /// The roles of the origin label. Only a model-made insight uses the
    /// assistant colours.
    static func origin(_ origin: MonarchAiInsightOrigin) -> MonarchSurfaceAppearance {
        switch origin {
        case .ai: MonarchSurfaceAppearance(background: \.aiSubtle, foreground: \.aiStrong, border: \.aiBorder)
        case .rule: MonarchSurfaceAppearance(background: \.bgSurface, foreground: \.textMuted, border: \.borderDefault)
        }
    }

    /// The text of the origin label.
    static func originText(_ origin: MonarchAiInsightOrigin, labels: MonarchAiInsightCalloutLabels) -> String {
        switch origin {
        case .ai: labels.ai ?? MonarchAiInsightCalloutStrings.ai
        case .rule: labels.rule ?? MonarchAiInsightCalloutStrings.rule
        }
    }

    /// What VoiceOver reads for the title: the tone name first, so the tone
    /// is never colour alone. The neutral tone has no name.
    static func spokenTitle(_ title: String, tone: MonarchTone, labels: MonarchAiInsightCalloutLabels) -> String {
        guard let name = labels.tone ?? MonarchToneStrings.name(tone), !name.isEmpty else { return title }
        return MonarchAiInsightCalloutStrings.titled(tone: name, title: title)
    }
}
