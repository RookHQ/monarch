import CoreGraphics
import MonarchTokens

/// Component tokens and logic for ``MonarchSectionCard``. The surface,
/// padding and radius are the card's.
enum MonarchSectionCardTokens {
    /// Padding around the header, the body and the footer.
    static let padding: CGFloat = MonarchCardTokens.padding
    /// The corner radius.
    static let cornerRadius: CGFloat = MonarchCardTokens.cornerRadius
    /// The width of the border and the footer's top rule.
    static let borderWidth: CGFloat = MonarchCardTokens.borderWidth
    /// The gap between the icon, the title and the chevron.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the lines of the header text.
    static let textSpacing: CGFloat = MonarchSectionHeaderTokens.textSpacing
    /// The gap between the header text and the actions.
    static let actionSpacing: CGFloat = MonarchSectionHeaderTokens.barSpacing
    /// The gap between the items of the content.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space2
    /// The skeleton rows while loading.
    static let loadingRows = 3

    /// The border: the card's for the neutral tone, the tone's border otherwise.
    static func border(_ tone: MonarchTone) -> MonarchColorRole {
        MonarchToneChrome.border(tone, neutral: MonarchCardTokens.border)
    }

    /// The title colour: default text for the neutral tone, the tone's strong role otherwise.
    static func titleColor(_ tone: MonarchTone) -> MonarchColorRole {
        MonarchToneChrome.titleColor(tone)
    }

    /// The SF Symbol before the title: the one the app gives, none otherwise.
    static func symbol(_ tone: MonarchTone, override: String?) -> String? {
        // As the web card: a tone colours the border and the title; only an icon the app passes is drawn.
        override
    }

    /// Whether the body and the footer show: always, unless a collapsible card is closed.
    static func showsBody(isCollapsible: Bool, isExpanded: Bool) -> Bool {
        !isCollapsible || isExpanded
    }

    /// The padding of the body: none when it is flush, for a list that runs to the edges.
    static func bodyPadding(isFlush: Bool) -> CGFloat {
        isFlush ? 0 : padding
    }

    /// Whether the header is busy: refreshing in place, or loading.
    static func isBusy(_ isBusy: Bool, state: MonarchSectionCardState) -> Bool {
        isBusy || state == .loading
    }

    /// The spoken value of the title: open or closed for a collapsible card,
    /// then "Loading" while busy.
    static func titleValue(
        isCollapsible: Bool,
        isExpanded: Bool,
        isBusy: Bool,
        expanded: String,
        collapsed: String,
        loading: String
    ) -> String {
        var parts: [String] = []
        if isCollapsible { parts.append(isExpanded ? expanded : collapsed) }
        if isBusy { parts.append(loading) }
        return parts.joined(separator: ", ")
    }
}
