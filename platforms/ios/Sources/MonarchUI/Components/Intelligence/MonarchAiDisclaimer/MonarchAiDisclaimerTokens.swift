import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiDisclaimer``.
enum MonarchAiDisclaimerTokens {
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1_5
    /// The SF Symbol of the medium risk. The low risk uses the AI bot glyph.
    static let mediumSymbol = "info.circle"
    /// The SF Symbol of the high risk.
    static let highSymbol = "exclamationmark.triangle"
    /// The weight of the medium-risk text.
    static let mediumWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.regular)

    /// The colour roles of a low or medium disclaimer. The high risk is a warning ``MonarchInlineNotice``.
    struct Appearance: Equatable {
        /// The text.
        let text: MonarchColorRole
        /// The icon.
        let icon: MonarchColorRole
    }

    /// The colour roles for a risk. Pure, so it is tested without a view.
    static func appearance(_ risk: MonarchAiDisclaimerRisk) -> Appearance {
        switch risk {
        case .low: Appearance(text: \.textMuted, icon: \.aiStrong)
        case .medium: Appearance(text: \.textDefault, icon: \.infoStrong)
        case .high: Appearance(text: \.textDefault, icon: \.warningStrong)
        }
    }

    /// The type role for a risk.
    static func textStyle(_ risk: MonarchAiDisclaimerRisk) -> MonarchTextStyle {
        risk == .low ? .caption : .label
    }

    /// Whether the line is centred: only the quiet low-risk line, as on the web.
    static func isCentred(_ risk: MonarchAiDisclaimerRisk) -> Bool {
        risk == .low
    }

    /// The text that is shown: the app's own wording, else the built-in wording of the risk.
    static func text(_ custom: String?, risk: MonarchAiDisclaimerRisk) -> String {
        if let custom, !custom.isEmpty { return custom }
        return MonarchAiDisclaimerStrings.text(risk)
    }
}
