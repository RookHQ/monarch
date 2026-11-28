import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchFileTypeIcon``.
enum MonarchFileTypeIconTokens {
    /// The side of the tile behind a large icon, before Dynamic Type.
    static let tileSize: CGFloat = MonarchTokens.Space.space10
    /// The fill of the tile behind a large icon.
    static var tileBackground: MonarchColorRole { \.bgMuted }
    /// The border of the tile, so it keeps its edge in the high-contrast themes.
    static var tileBorder: MonarchColorRole { \.borderSubtle }

    /// The glyph size for an icon size.
    static func iconSize(_ size: MonarchFileTypeIconSize) -> MonarchIconSize {
        switch size {
        case .sm: .sm
        case .md: .md
        case .lg: .lg
        }
    }

    /// Whether the size draws a tile behind the glyph.
    static func hasTile(_ size: MonarchFileTypeIconSize) -> Bool {
        size == .lg
    }

    /// The SF Symbols of a family: a file outline, and the glyph drawn inside it, as the web's file icons (a file
    /// with an image, a play mark, a note, and so on). Every family has its own shape, so the type never depends
    /// on the tint.
    static func symbols(_ family: MonarchFileTypeFamily) -> (outline: String, inner: String?) {
        switch family {
        case .image: ("doc", "photo")
        case .video: ("doc", "play.fill")
        case .audio: ("doc", "music.note")
        case .pdf: ("doc.plaintext", nil)
        case .document: ("doc", "text.alignleft")
        case .spreadsheet: ("doc", "tablecells")
        case .presentation: ("doc", "display")
        case .archive: ("doc.zipper", nil)
        case .code: ("doc", "chevron.left.forwardslash.chevron.right")
        case .text: ("doc", "line.3.horizontal")
        case .other: ("doc", nil)
        }
    }

    /// The SF Symbol of a family that tells it apart: the glyph inside the file, else the outline.
    static func symbol(_ family: MonarchFileTypeFamily) -> String {
        let pair = symbols(family)
        return pair.inner ?? pair.outline
    }

    /// The tint of a family, from the data palette as on the web.
    static func color(_ family: MonarchFileTypeFamily) -> MonarchColorRole {
        switch family {
        case .image, .video: \.data1
        case .document, .text: \.data2
        case .spreadsheet, .code: \.data3
        case .audio, .presentation: \.data4
        case .pdf: \.data5
        case .archive, .other: \.textMuted
        }
    }

    /// What VoiceOver reads: the override if there is one, otherwise the
    /// built-in name of the family.
    static func accessibilityLabel(family: MonarchFileTypeFamily, label: String?) -> String {
        if let label, !label.isEmpty { return label }
        return MonarchFileTypeIconStrings.name(family)
    }
}
