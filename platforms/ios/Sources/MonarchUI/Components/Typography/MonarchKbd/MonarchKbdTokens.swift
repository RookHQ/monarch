import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchKbd``.
enum MonarchKbdTokens {
    /// Padding before and after the keys.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space1_5
    /// Padding above and below the keys.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// The smallest width and height of the key cap before Dynamic Type is
    /// applied, so that a single letter gives a square cap.
    static let minimumSize: CGFloat = MonarchTokens.Space.space6
    /// The width of the bottom edge, which is heavier than the other edges
    /// so the cap reads as a key.
    static let bottomEdgeWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The type role of the keys.
    static let textStyle: MonarchTextStyle = .code
    /// What separates the keys of a spoken shortcut.
    static let spokenSeparator = " "

    /// The colour roles of the key cap.
    static var appearance: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.bgSubtle, foreground: \.textMuted, border: \.borderDefault)
    }

    /// Whether the key cap is drawn: always when asked, otherwise only
    /// while a hardware keyboard is attached.
    static func isVisible(isAlwaysVisible: Bool, isKeyboardAttached: Bool) -> Bool {
        isAlwaysVisible || isKeyboardAttached
    }

    /// The spoken name of a key that is written as a glyph, or `nil` for
    /// any other character.
    static func name(of glyph: Character, labels: MonarchKbdLabels) -> String? {
        switch glyph {
        case "\u{2318}": labels.command ?? MonarchKbdStrings.command
        case "\u{21E7}": labels.shift ?? MonarchKbdStrings.shift
        case "\u{2325}": labels.option ?? MonarchKbdStrings.option
        case "\u{2303}": labels.control ?? MonarchKbdStrings.control
        case "\u{21A9}", "\u{23CE}": labels.return ?? MonarchKbdStrings.return
        case "\u{238B}": labels.escape ?? MonarchKbdStrings.escape
        case "\u{232B}": labels.delete ?? MonarchKbdStrings.delete
        case "\u{21E5}": labels.tab ?? MonarchKbdStrings.tab
        case "\u{2423}": labels.space ?? MonarchKbdStrings.space
        case "\u{21EA}": labels.capsLock ?? MonarchKbdStrings.capsLock
        case "\u{2191}": labels.upArrow ?? MonarchKbdStrings.upArrow
        case "\u{2193}": labels.downArrow ?? MonarchKbdStrings.downArrow
        case "\u{2190}": labels.leftArrow ?? MonarchKbdStrings.leftArrow
        case "\u{2192}": labels.rightArrow ?? MonarchKbdStrings.rightArrow
        default: nil
        }
    }

    /// What VoiceOver reads for a shortcut: each key glyph by its name and
    /// the other characters as they are, such as "Command Shift P" for
    /// "⌘⇧P". Text without glyphs is returned unchanged apart from white
    /// space at its ends.
    static func spokenText(for keys: String, labels: MonarchKbdLabels) -> String {
        var parts: [String] = []
        var pending = ""
        func flush() {
            let text = pending.trimmingCharacters(in: .whitespacesAndNewlines)
            if !text.isEmpty { parts.append(text) }
            pending = ""
        }
        for character in keys {
            if let name = name(of: character, labels: labels) {
                flush()
                parts.append(name)
            } else {
                pending.append(character)
            }
        }
        flush()
        return parts.joined(separator: spokenSeparator)
    }
}
