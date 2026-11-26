import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic of ``MonarchAiStructuredAnswer``: how
/// values become text, which fields are wide, the grid rows and what
/// VoiceOver reads.
enum MonarchAiStructuredAnswerTokens {
    /// Padding inside the answer's border.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the header, the fields and the actions.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the provenance label and the title.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between fields.
    static let fieldSpacing: CGFloat = MonarchTokens.Space.space3
    /// Padding inside a field card in the grid layout.
    static let cardPadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between list items and between badges.
    static let itemSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between a bullet and its item.
    static let bulletSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The corner radius of the answer.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.lg
    /// The corner radius of a field card.
    static let cardRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of borders.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// Shown for a field with no value; VoiceOver reads the empty label instead.
    static let emptyMark = "—"
    /// The bullet before a list item, hidden from VoiceOver.
    static let bullet = "\u{2022}"
    /// Columns of the grid layout at standard text sizes.
    static let gridColumns = 2

    /// The colour roles of the answer.
    enum Role {
        static var surface: MonarchColorRole { \.bgSurface }
        static var border: MonarchColorRole { \.borderDefault }
        static var title: MonarchColorRole { \.textDefault }
        static var cardSurface: MonarchColorRole { \.bgSubtle }
        static var cardBorder: MonarchColorRole { \.borderSubtle }
        static var bullet: MonarchColorRole { \.textMuted }
        static var empty: MonarchColorRole { \.textSubtle }
    }

    // MARK: Values

    /// Whether a value is missing: absent, null, empty text or an empty array.
    static func isMissing(_ value: MonarchAiJSON?) -> Bool {
        switch value {
        case .none, .some(.null): true
        case .some(.string(let text)): text.isEmpty
        case .some(.array(let items)): items.isEmpty
        default: false
        }
    }

    /// The value as plain text: text as it is, numbers without a needless
    /// point, objects and arrays as compact JSON.
    static func text(_ value: MonarchAiJSON?) -> String {
        MonarchAiTableAnswerTokens.text(value)
    }

    /// The items of a list or badge field: every element of an array that
    /// is not missing, or the single value.
    static func items(_ value: MonarchAiJSON?) -> [String] {
        let values: [MonarchAiJSON?]
        if case .array(let elements) = value {
            values = elements
        } else {
            values = [value]
        }
        return values.filter { !isMissing($0) }.map(text)
    }

    /// A number field's text: a JSON number or numeric text such as "1,200"
    /// written with the format in the locale, anything else as text.
    static func number(_ value: MonarchAiJSON?, format: MonarchMetricFormat, locale: Locale) -> String {
        if let number = MonarchAiTableAnswerTokens.number(value) {
            if case .string(let raw) = value, raw.trimmingCharacters(in: .whitespaces).hasSuffix("%") {
                return text(value)
            }
            return MonarchMetric.formatted(number, format: format, locale: locale)
        }
        return text(value)
    }

    /// The tone of one badge.
    static func tone(_ item: String, field: MonarchAiStructuredField) -> MonarchTone {
        field.toneForValue?(item) ?? field.tone
    }

    // MARK: Layout

    /// Whether a field is wide: as set, or by default `list` and `markdown` fields.
    static func isWide(_ field: MonarchAiStructuredField) -> Bool {
        field.isWide ?? (field.type == .list || field.type == .markdown)
    }

    /// The fields of each row of the grid, by index: a wide field alone, the
    /// others side by side up to `columns`, in schema order.
    static func gridRows(_ fields: [MonarchAiStructuredField], columns: Int) -> [[Int]] {
        var rows: [[Int]] = []
        var current: [Int] = []
        for (index, field) in fields.enumerated() {
            if isWide(field) || columns <= 1 {
                if !current.isEmpty { rows.append(current) }
                current = []
                rows.append([index])
            } else {
                current.append(index)
                if current.count == columns {
                    rows.append(current)
                    current = []
                }
            }
        }
        if !current.isEmpty { rows.append(current) }
        return rows
    }

    // MARK: Accessibility

    /// What a field shows and VoiceOver reads for its value.
    enum State: Equatable {
        /// The value has not arrived and the answer is still streaming.
        case pending
        /// The answer is complete and the field has no value.
        case empty
        /// The field has a value.
        case value
    }

    /// The state of a field.
    static func state(_ value: MonarchAiJSON?, isStreaming: Bool) -> State {
        guard isMissing(value) else { return .value }
        return isStreaming ? .pending : .empty
    }

    /// What VoiceOver reads after a field's label: its value, its items
    /// separated by commas, "Still generating" or "Not provided".
    static func spokenValue(
        _ value: MonarchAiJSON?,
        field: MonarchAiStructuredField,
        isStreaming: Bool,
        locale: Locale,
        pending: String,
        empty: String
    ) -> String {
        switch state(value, isStreaming: isStreaming) {
        case .pending: return pending
        case .empty: return empty
        case .value:
            switch field.type {
            case .number: return number(value, format: field.format, locale: locale)
            case .list, .badge: return items(value).joined(separator: ", ")
            case .text, .markdown: return text(value)
            }
        }
    }
}
