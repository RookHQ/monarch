import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchTypingIndicator``.
enum MonarchTypingIndicatorTokens {
    /// The number of dots.
    static let dotCount = 3
    /// The diameter of a dot before Dynamic Type is applied.
    static let dotSize: CGFloat = MonarchTokens.Space.space1_5
    /// The gap between two dots.
    static let dotSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the dots and the text.
    static let textSpacing: CGFloat = MonarchTokens.Space.space2
    /// How far a dot rises, before Dynamic Type is applied.
    static let bounce: CGFloat = MonarchTokens.Space.space1
    /// How long a dot takes to rise or fall, in seconds.
    static let bounceDuration: TimeInterval = TimeInterval(MonarchTokens.Duration.slower) / 1000
    /// The delay between one dot and the next, in seconds.
    static let stagger: TimeInterval = TimeInterval(MonarchTokens.Duration.fast) / 1000
    /// The type role of the text.
    static let textStyle: MonarchTextStyle = .caption
    /// The role of the dots.
    static var dot: MonarchColorRole { \.textSubtle }
    /// The role of the text.
    static var text: MonarchColorRole { \.textMuted }
    /// A string with the line height of the text and no glyph, which keeps
    /// the height of the line while nobody is typing.
    static let lineBox = " "

    /// The sentence for the people who are typing, or an empty string for
    /// nobody. Empty names are left out.
    static func sentence(names: [String], text: String?) -> String {
        let people = names.filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
        guard let first = people.first else { return "" }
        if let text, !text.isEmpty { return text }
        switch people.count {
        case 1: return MonarchTypingIndicatorStrings.one(first)
        case 2: return MonarchTypingIndicatorStrings.two(first, people[1])
        default: return MonarchTypingIndicatorStrings.several
        }
    }

    /// What is announced when the sentence changes, or `nil` for nothing.
    static func announcement(sentence: String, announcesChanges: Bool) -> String? {
        announcesChanges && !sentence.isEmpty ? sentence : nil
    }
}
