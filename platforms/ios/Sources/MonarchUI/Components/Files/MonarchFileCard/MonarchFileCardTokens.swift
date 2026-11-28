import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchFileCard``.
enum MonarchFileCardTokens {
    /// Padding inside the card.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the preview, the text and the badges in the card layout.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the parts of a row.
    static let rowSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the name and the meta line.
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between badges.
    static let badgeSpacing: CGFloat = MonarchTokens.Space.space1
    /// How far the checkbox and the menu sit from the card's corners.
    static let cornerInset: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of the card.
    static let radius: CGFloat = MonarchTokens.Radius.lg
    /// The corner radius of the preview.
    static let previewRadius: CGFloat = MonarchTokens.Radius.md
    /// The side of the preview in a row, before Dynamic Type.
    static let rowPreviewSize: CGFloat = MonarchTokens.Control.lg
    /// The width-to-height ratio of the preview in the card layout.
    static let previewAspectRatio: CGFloat = 4.0 / 3.0
    /// The border width of an unselected card.
    static let borderWidth: CGFloat = MonarchShapes.borderWidth
    /// The border width of a selected card, so selection is not shown by colour alone.
    static let selectedBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The SF Symbol of the actions menu.
    static let menuSymbol = "ellipsis"
    /// The separator between the parts of the meta line.
    static let metaSeparator = " · "

    /// The colour roles of the card surface.
    struct Appearance: Equatable {
        /// The fill.
        let background: MonarchColorRole
        /// The border.
        let border: MonarchColorRole
    }

    /// The surface for a state. Selected wins over pressed; disabled wins over both.
    static func appearance(isSelected: Bool, isPressed: Bool, isEnabled: Bool) -> Appearance {
        if !isEnabled { return Appearance(background: \.bgSubtle, border: \.borderSubtle) }
        if isSelected { return Appearance(background: \.accentSubtle, border: \.accentBorder) }
        if isPressed { return Appearance(background: \.bgSubtle, border: \.borderDefault) }
        return Appearance(background: \.bgSurface, border: \.borderSubtle)
    }

    /// The border width for a state.
    static func borderWidth(isSelected: Bool) -> CGFloat {
        isSelected ? selectedBorderWidth : borderWidth
    }

    /// The colour of the name.
    static func nameRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour of the meta line.
    static func metaRole(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The parts of the meta line: the type, the size in the locale's units and the app's meta.
    static func metaParts(
        family: MonarchFileTypeFamily,
        size: Int64?,
        meta: String?,
        typeLabel: String?,
        locale: Locale
    ) -> [String] {
        var parts = [typeLabel ?? MonarchFileTypeIconStrings.name(family)]
        if let size { parts.append(MonarchFormats.fileSize(Int64(size), locale: locale)) }
        if let meta, !meta.isEmpty { parts.append(meta) }
        return parts.filter { !$0.isEmpty }
    }

    /// The visible meta line.
    static func metaLine(_ parts: [String]) -> String {
        parts.joined(separator: metaSeparator)
    }

    /// What VoiceOver reads after the name: the meta parts, then the badges.
    static func accessibilityValue(metaParts: [String], badges: [String]) -> String {
        (metaParts + badges.filter { !$0.isEmpty }).joined(separator: ", ")
    }
}
