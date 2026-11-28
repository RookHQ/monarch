import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchInlineNotice``.
enum MonarchInlineNoticeTokens {
    /// The gap between the icon and the text.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the text and the action or the dismiss button.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the content of the subtle variant.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content of the subtle variant.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The SF Symbol of the dismiss button.
    static let dismissSymbol = "xmark"

    /// The colour roles of a notice.
    struct Appearance: Equatable {
        /// The fill, or `nil` for no fill.
        let background: MonarchColorRole?
        /// The icon.
        let icon: MonarchColorRole
        /// The text.
        let text: MonarchColorRole
    }

    /// The roles for a tone and variant. The subtle variant is a tinted
    /// strip with default text; the plain variant has no fill and its text
    /// takes the strong colour of the tone.
    static func appearance(tone: MonarchTone, variant: MonarchInlineNoticeVariant) -> Appearance {
        let palette = MonarchTonePalette.palette(for: tone)
        switch variant {
        case .subtle:
            return Appearance(background: palette.subtle, icon: palette.strong, text: \.textDefault)
        case .plain:
            return Appearance(background: nil, icon: palette.strong, text: palette.strong)
        }
    }

    /// The SF Symbol: the app's, or the one of the tone.
    static func symbol(tone: MonarchTone, override: String?) -> String {
        if let override, !override.isEmpty { return override }
        // The web notice draws its info circle for the neutral tone.
        if tone == .neutral { return "info.circle" }
        return MonarchTonePalette.palette(for: tone).symbol
    }

    /// The title, strongly emphasised, a space, then the message, as one
    /// run of text so they wrap together. Only the message without a title.
    static func attributedText(title: String?, message: String) -> AttributedString {
        guard let title, !title.isEmpty else { return AttributedString(message) }
        var lead = AttributedString(title)
        lead.inlinePresentationIntent = .stronglyEmphasized
        return lead + AttributedString(" " + message)
    }

    /// What VoiceOver reads for the notice: the tone name, the title, then
    /// the message. Empty parts are left out.
    static func accessibilityLabel(toneName: String?, title: String?, message: String) -> String {
        [toneName, title, message]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .joined(separator: ", ")
    }
}
