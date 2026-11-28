import MonarchTokens
import SwiftUI

/// Component tokens and the pure logic of ``MonarchEmojiPicker``.
enum MonarchEmojiPickerTokens {
    /// One section of the grid: a category, the recents or the search results.
    struct Section: Equatable, Identifiable {
        let id: String
        let label: String
        let symbol: String
        let entries: [Entry]
    }

    /// One cell: the emoji as shown (with the skin tone) and its item.
    struct Entry: Equatable, Identifiable {
        let item: MonarchEmojiPickerItem
        let display: String
        var id: String { display }
    }

    /// The least size of a cell, which is also its touch target. Scaled
    /// with Dynamic Type.
    static let cellSize: CGFloat = MonarchAccessibility.minimumHitTarget
    /// The height of the scrolling grid, three times the largest space token.
    static let gridHeight: CGFloat = MonarchTokens.Space.space24 * gridHeightRatio
    /// How many of the largest space token make the grid's height.
    static let gridHeightRatio: CGFloat = 3
    /// The gap between the parts of the picker.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between cells.
    static let cellSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The type role of an emoji.
    static let emojiStyle: MonarchTextStyle = .title
    /// The type role of the emoji on a category button.
    static let categoryStyle: MonarchTextStyle = .heading
    /// The type role of a section heading.
    static let headingStyle: MonarchTextStyle = .caption
    /// The SF Symbol of the search field.
    static let searchSymbol = "magnifyingglass"
    /// The SF Symbol of the clear button.
    static let clearSymbol = "xmark.circle.fill"
    /// The emoji that shows the chosen skin tone on its menu.
    static let skinToneSample = "✋"
    /// The emoji on the recents button.
    static let recentSymbol = "🕘"
    /// The most recents shown by default.
    static let defaultMaxRecents = 24
    /// The colour of a section heading.
    static var headingColor: MonarchColorRole { \.textMuted }
    /// The fill of the selected category button.
    static var selectedCategoryFill: MonarchColorRole { \.bgEmphasis }

    private static let modifiers: Set<Unicode.Scalar> = [
        "\u{1F3FB}", "\u{1F3FC}", "\u{1F3FD}", "\u{1F3FE}", "\u{1F3FF}", "\u{FE0F}",
    ]

    /// An item from a row of the built-in index: `emoji|name|keywords|1`.
    static func item(fromRow row: Substring) -> MonarchEmojiPickerItem? {
        let fields = row.trimmingCharacters(in: .whitespaces).split(separator: "|", omittingEmptySubsequences: false)
        guard fields.count >= 2, !fields[0].isEmpty else { return nil }
        let keywords = fields.count > 2 ? fields[2].split(separator: " ").map(String.init) : []
        return MonarchEmojiPickerItem(
            String(fields[0]),
            name: String(fields[1]),
            keywords: keywords,
            acceptsSkinTone: fields.count > 3 && fields[3] == "1"
        )
    }

    /// The emoji with a skin tone modifier after its first character and any
    /// other modifier or variation selector removed. `none` keeps it as it is.
    static func applying(_ tone: MonarchEmojiSkinTone, to emoji: String) -> String {
        guard let modifier = tone.modifier, let first = emoji.unicodeScalars.first else { return emoji }
        var scalars = String.UnicodeScalarView()
        scalars.append(first)
        scalars.append(modifier)
        scalars.append(contentsOf: emoji.unicodeScalars.dropFirst().filter { !modifiers.contains($0) })
        return String(scalars)
    }

    /// The emoji without skin tone modifiers or variation selectors.
    static func base(of emoji: String) -> String {
        var scalars = String.UnicodeScalarView()
        scalars.append(contentsOf: emoji.unicodeScalars.filter { !modifiers.contains($0) })
        return String(scalars)
    }

    /// The emoji whose name or keywords match every word of the query, each
    /// word as the start of a word of the name or of a keyword. Names that
    /// start with the query come first.
    static func search(
        _ query: String,
        in categories: [MonarchEmojiPickerCategory],
        limit: Int = .max
    ) -> [MonarchEmojiPickerItem] {
        let words = query.lowercased().split(whereSeparator: \.isWhitespace).map(String.init)
        guard !words.isEmpty else { return [] }
        let phrase = words.joined(separator: " ")
        var seen = Set<String>()
        var starts: [MonarchEmojiPickerItem] = []
        var others: [MonarchEmojiPickerItem] = []
        for item in categories.flatMap(\.emojis) where !seen.contains(item.emoji) {
            let name = item.name.lowercased()
            let tokens =
                name.split(whereSeparator: { " :,-".contains($0) }).map(String.init)
                + item.keywords.map { $0.lowercased() }
            guard words.allSatisfy({ word in tokens.contains { $0.hasPrefix(word) } }) else { continue }
            seen.insert(item.emoji)
            if name.hasPrefix(phrase) { starts.append(item) } else { others.append(item) }
        }
        return Array((starts + others).prefix(max(0, limit)))
    }

    /// A recents list with the emoji first, without duplicates, at most
    /// `max` long.
    static func pushingRecent(_ emoji: String, to recents: [String], max: Int) -> [String] {
        Array(([emoji] + recents.filter { $0 != emoji }).prefix(Swift.max(0, max)))
    }

    /// The name of a category.
    static func categoryName(_ category: MonarchEmojiPickerCategory, labels: MonarchEmojiPickerLabels) -> String {
        category.label ?? labels.categoryName?(category.id) ?? MonarchEmojiPickerStrings.category(category.id)
            ?? category.id
    }

    /// The sections shown: the search results while there is a query,
    /// otherwise the recents and every category that has emoji.
    static func sections(
        query: String,
        categories: [MonarchEmojiPickerCategory],
        recents: [String],
        maxRecents: Int,
        tone: MonarchEmojiSkinTone,
        labels: MonarchEmojiPickerLabels
    ) -> [Section] {
        func entry(_ item: MonarchEmojiPickerItem) -> Entry {
            Entry(item: item, display: item.acceptsSkinTone ? applying(tone, to: item.emoji) : item.emoji)
        }
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty {
            return [
                Section(
                    id: "search",
                    label: labels.searchResults ?? MonarchEmojiPickerStrings.searchResults,
                    symbol: "",
                    entries: search(trimmed, in: categories).map(entry)
                )
            ]
        }
        var result: [Section] = []
        if !recents.isEmpty, maxRecents > 0 {
            var byBase: [String: MonarchEmojiPickerItem] = [:]
            for item in categories.flatMap(\.emojis) where byBase[base(of: item.emoji)] == nil {
                byBase[base(of: item.emoji)] = item
            }
            let entries = recents.prefix(maxRecents).map { emoji in
                Entry(item: byBase[base(of: emoji)] ?? MonarchEmojiPickerItem(emoji, name: emoji), display: emoji)
            }
            let label = labels.categoryName?("recent") ?? MonarchEmojiPickerStrings.category("recent") ?? "recent"
            result.append(Section(id: "recent", label: label, symbol: recentSymbol, entries: entries))
        }
        for category in categories where !category.emojis.isEmpty {
            result.append(
                Section(
                    id: category.id,
                    label: categoryName(category, labels: labels),
                    symbol: category.symbol ?? category.emojis[0].emoji,
                    entries: category.emojis.map(entry)
                )
            )
        }
        return result
    }

    /// What is announced after a search.
    static func resultsAnnouncement(_ count: Int, labels: MonarchEmojiPickerLabels) -> String {
        labels.resultsCount?(count) ?? MonarchEmojiPickerStrings.resultsCount(count)
    }

    /// The name of a skin tone.
    static func skinToneName(_ tone: MonarchEmojiSkinTone, labels: MonarchEmojiPickerLabels) -> String {
        labels.skinToneName?(tone) ?? MonarchEmojiPickerStrings.skinToneName(tone)
    }

    /// The colour roles of the search field, from the text field's tokens.
    static func fieldBorder(isEnabled: Bool) -> MonarchColorRole {
        MonarchTextFieldTokens.border(hasError: false, isEnabled: isEnabled)
    }
}
