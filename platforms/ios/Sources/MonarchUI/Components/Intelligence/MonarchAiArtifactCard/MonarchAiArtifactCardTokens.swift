import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchAiArtifactCard``.
enum MonarchAiArtifactCardTokens {
    /// Padding before and after the content.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// The gap between the icon, the text and the action.
    static let contentSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the action text and its chevron.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space1
    /// The side of the icon tile before Dynamic Type is applied.
    static let iconTileSize: CGFloat = MonarchTokens.Control.md
    /// The corner radius of the icon tile.
    static let iconTileRadius: CGFloat = MonarchTokens.Radius.md
    /// The corner radius of the card.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The greatest width of the card: 448 points, 28rem on the web.
    static let maxWidth: CGFloat = MonarchTokens.Space.space24 * 4 + MonarchTokens.Space.space16
    /// The SF Symbol after the action text. It mirrors in right-to-left layouts.
    static let chevronSymbol = "chevron.forward"

    /// The SF Symbol of a kind.
    static func symbol(_ kind: MonarchAiArtifactKind) -> String {
        switch kind {
        case .document: "doc.text"
        case .code: "chevron.left.forwardslash.chevron.right"
        case .table: "tablecells"
        case .chart: "chart.bar"
        case .image: "photo"
        case .html: "globe"
        }
    }

    /// The colour roles of the card in one state.
    struct Appearance: Equatable {
        let background: MonarchColorRole
        let border: MonarchColorRole
        let title: MonarchColorRole
        let secondary: MonarchColorRole
        let accent: MonarchColorRole
        let iconTile: MonarchColorRole
    }

    /// The roles for a state. Pure, so it is tested without a view.
    static func appearance(isActive: Bool, isPressed: Bool, isEnabled: Bool) -> Appearance {
        guard isEnabled else {
            return Appearance(
                background: \.bgSubtle,
                border: \.borderSubtle,
                title: \.textDisabled,
                secondary: \.textDisabled,
                accent: \.textDisabled,
                iconTile: \.bgMuted
            )
        }
        let background: MonarchColorRole = isActive ? \.aiSubtle : (isPressed ? \.bgSubtle : \.bgSurface)
        return Appearance(
            background: background,
            border: isActive || isPressed ? \.aiBorder : \.borderDefault,
            title: \.textDefault,
            secondary: \.textMuted,
            accent: \.aiStrong,
            iconTile: isActive ? \.bgSurface : \.aiSubtle
        )
    }

    /// The width of the border. The artifact on show has a heavier border.
    static func borderWidth(isActive: Bool) -> CGFloat {
        isActive ? MonarchTokens.Space.space0_5 : MonarchTokens.Space.px
    }

    /// The visible line under the title, such as "Document · v3".
    static func meta(kind: MonarchAiArtifactKind, version: Int, kindLabel: String?, versionLabel: String?) -> String {
        let kindText = kindLabel ?? MonarchAiArtifactCardStrings.kind(kind)
        let versionText = versionLabel ?? MonarchAiArtifactCardStrings.version(version)
        return [kindText, versionText].filter { !$0.isEmpty }.joined(separator: " · ")
    }

    /// The action text: "Open", or "Showing" for the artifact on show.
    static func actionText(isActive: Bool, actionLabel: String?) -> String {
        actionLabel ?? (isActive ? MonarchAiArtifactCardStrings.opened : MonarchAiArtifactCardStrings.open)
    }

    /// What VoiceOver reads for the card: the title, the kind, the version
    /// in words, the description and the action.
    static func accessibilityLabel(
        title: String,
        kind: MonarchAiArtifactKind,
        version: Int,
        description: String?,
        isActive: Bool,
        kindLabel: String? = nil,
        versionLabel: String? = nil,
        actionLabel: String? = nil
    ) -> String {
        let parts: [String?] = [
            title,
            kindLabel ?? MonarchAiArtifactCardStrings.kind(kind),
            versionLabel ?? MonarchAiArtifactCardStrings.versionLong(version),
            description,
            actionText(isActive: isActive, actionLabel: actionLabel),
        ]
        return parts.compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
    }
}
