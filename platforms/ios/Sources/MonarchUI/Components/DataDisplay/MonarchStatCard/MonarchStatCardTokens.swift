import CoreGraphics
import MonarchTokens

/// Component tokens and pure logic for ``MonarchStatCard``.
enum MonarchStatCardTokens {
    /// The gap between the parts of the card.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the icon, the metric and the trailing controls.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the content of the card variant.
    static let padding: CGFloat = MonarchCardTokens.padding
    /// The corner radius of the card variant.
    static let cornerRadius: CGFloat = MonarchCardTokens.cornerRadius
    /// The border width of the card variant.
    static let borderWidth: CGFloat = MonarchCardTokens.borderWidth
    /// The SF Symbol after a stat that opens its details.
    static let disclosureSymbol = "chevron.forward"
    /// The value shown under the placeholder while loading, so the redacted
    /// shape has the width of a figure.
    static let placeholderValue = "000,000"

    /// What the value area shows.
    enum Phase: Equatable {
        /// The metric.
        case value
        /// A redacted placeholder.
        case loading
        /// The error message.
        case error
    }

    /// The phase: loading wins over an error, and an empty message is no error.
    static func phase(isLoading: Bool, errorMessage: String?) -> Phase {
        if isLoading { return .loading }
        if let errorMessage, !errorMessage.isEmpty { return .error }
        return .value
    }

    /// The border role: the card border, or the tone's border.
    static func border(_ tone: MonarchTone) -> MonarchColorRole {
        MonarchToneChrome.border(tone, neutral: MonarchCardTokens.border)
    }

    /// The tone's icon beside the label, so a tone never depends on colour alone. None for neutral.
    static func toneSymbol(_ tone: MonarchTone) -> String? {
        MonarchToneChrome.symbol(tone, override: nil)
    }

    /// The colour of the tone icon.
    static func toneColor(_ tone: MonarchTone) -> MonarchColorRole {
        MonarchToneChrome.titleColor(tone)
    }

    /// The tone of the sparkline: the card's tone, or the sparkline's default for neutral.
    static func sparklineTone(_ tone: MonarchTone) -> MonarchTone? {
        tone == .neutral ? nil : tone
    }

    /// Whether the card variant draws a surface.
    static func hasSurface(_ variant: MonarchStatCardVariant) -> Bool {
        variant == .card
    }
}
