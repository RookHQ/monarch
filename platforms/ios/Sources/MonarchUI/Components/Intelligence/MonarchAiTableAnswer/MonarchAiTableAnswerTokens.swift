import Foundation
import MonarchTokens

/// Component tokens and logic for ``MonarchAiTableAnswer``: type inference,
/// sorting, CSV and what VoiceOver reads. Every function is pure.
enum MonarchAiTableAnswerTokens {
    /// The gap between the header, the table and what follows it.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3
    /// The gap between the lines of the header.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the bot glyph and the provenance label.
    static let eyebrowSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between the header actions.
    static let actionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between columns.
    static let columnSpacing: CGFloat = MonarchTokens.Space.space4
    /// Padding above and below a cell.
    static let cellVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding around the table inside its border.
    static let tablePadding: CGFloat = MonarchTokens.Space.space3
    /// The gap between a header label and its sort icon.
    static let sortIconSpacing: CGFloat = MonarchTokens.Space.space1
    /// The corner radius of the table's border.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the table's border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The rows shown before "Show all rows", by default.
    static let defaultVisibleRows = 200
    /// Shown for an empty cell, hidden from VoiceOver.
    static let emptyMark = "—"

    /// The provenance label colour.
    static var eyebrowColor: MonarchColorRole { \.aiStrong }
    /// The header text colour.
    static var headerColor: MonarchColorRole { \.textMuted }
    /// The colour of supporting text: the row count, empty cells.
    static var supportingColor: MonarchColorRole { \.textMuted }
    /// The table's border and row separators.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The header row's fill.
    static var headerBackground: MonarchColorRole { \.bgSubtle }

    /// The SF Symbol after a header: the direction it is sorted in, or a neutral pair of arrows.
    static func sortSymbol(ascending: Bool?) -> String {
        switch ascending {
        case .some(true): "chevron.up"
        case .some(false): "chevron.down"
        case .none: "chevron.up.chevron.down"
        }
    }

    // MARK: Values

    /// Whether a value is missing: null or blank text.
    static func isBlank(_ value: MonarchAiJSON?) -> Bool {
        switch value {
        case .none, .some(.null): true
        case .string(let text): text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        default: false
        }
    }

    private static let numberPattern = #"^[-+−]?(\d{1,3}(,\d{3})+|\d+)?(\.\d+)?%?$"#
    private static let datePattern =
        #"^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})?)?$"#

    /// The value as a number: a JSON number, or text such as "1,200", "-3.5" or "12%".
    static func number(_ value: MonarchAiJSON?) -> Double? {
        switch value {
        case .number(let number): return number.isFinite ? number : nil
        case .string(let raw):
            let text = raw.trimmingCharacters(in: .whitespaces)
            guard !text.isEmpty, text.contains(where: \.isNumber),
                text.range(of: numberPattern, options: .regularExpression) != nil
            else { return nil }
            var cleaned = text.replacingOccurrences(of: ",", with: "").replacingOccurrences(of: "−", with: "-")
            if cleaned.hasSuffix("%") { cleaned.removeLast() }
            if cleaned.hasPrefix("+") { cleaned.removeFirst() }
            guard let number = Double(cleaned), number.isFinite else { return nil }
            return number
        default: return nil
        }
    }

    /// The value as a time: ISO 8601 text such as "2024-10-14" or "2024-10-14T09:30:00Z".
    /// A date or time without a zone is read as UTC.
    static func date(_ value: MonarchAiJSON?) -> Date? {
        guard case .string(let raw) = value else { return nil }
        let text = raw.trimmingCharacters(in: .whitespaces)
        guard text.range(of: datePattern, options: .regularExpression) != nil else { return nil }
        var iso = text.replacingOccurrences(of: " ", with: "T")
        if iso.count == 10 { iso += "T00:00:00" }
        let hasZone = iso.hasSuffix("Z") || iso.dropFirst(11).contains(where: { $0 == "+" || $0 == "-" })
        if !hasZone { iso += "Z" }
        // "T09:30Z" has no seconds; ISO8601DateFormatter needs them.
        if let time = iso.range(of: #"T\d{2}:\d{2}(?=Z|[+-])"#, options: .regularExpression) {
            iso.insert(contentsOf: ":00", at: time.upperBound)
        }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: iso) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: iso)
    }

    /// The type of a column from its values, ignoring blanks: `number` when
    /// every value is a number, `date` when every value is an ISO date, otherwise `text`.
    static func inferType(_ values: [MonarchAiJSON?]) -> MonarchAiTableColumnType {
        let present = values.filter { !isBlank($0) }
        if present.isEmpty { return .text }
        if present.allSatisfy({ number($0) != nil }) { return .number }
        if present.allSatisfy({ date($0) != nil }) { return .date }
        return .text
    }

    /// The text of a value: text as is, whole numbers without a decimal point,
    /// and arrays and objects as compact JSON.
    static func text(_ value: MonarchAiJSON?) -> String {
        guard let value, !isBlank(value) else { return "" }
        switch value {
        case .null: return ""
        case .string(let text): return text
        case .bool(let flag): return flag ? "true" : "false"
        case .number(let number):
            if number.rounded() == number, abs(number) < 1e15 { return String(Int64(number)) }
            return String(number)
        case .array, .object:
            let encoder = JSONEncoder()
            encoder.outputFormatting = [.sortedKeys, .withoutEscapingSlashes]
            return (try? encoder.encode(value)).flatMap { String(data: $0, encoding: .utf8) } ?? ""
        }
    }

    /// The text shown in a cell: JSON numbers in a number column are formatted
    /// for the locale; everything else is ``text(_:)``.
    static func display(_ value: MonarchAiJSON?, type: MonarchAiTableColumnType, locale: Locale) -> String {
        if case .number(let number) = value, type == .number, number.isFinite {
            return number.formatted(.number.locale(locale))
        }
        return text(value)
    }

    // MARK: Columns

    /// A column with every default decided.
    struct ResolvedColumn: Identifiable, Hashable {
        /// The key of the value in each row.
        let key: String
        /// The header text.
        let label: String
        /// How values compare and format.
        let type: MonarchAiTableColumnType
        /// Where values sit.
        let alignment: MonarchAiTableColumnAlignment
        /// The key, which identifies the column.
        var id: String { key }
    }

    /// The columns to show: the given ones, or every key of the rows in
    /// alphabetical order, with labels, types and alignments filled in.
    static func resolveColumns(
        _ columns: [MonarchAiTableColumn]?,
        rows: [[String: MonarchAiJSON]]
    ) -> [ResolvedColumn] {
        let base =
            columns
            ?? Set(rows.flatMap(\.keys)).sorted().map { MonarchAiTableColumn($0) }
        return base.map { column in
            let type = column.type ?? inferType(rows.map { $0[column.key] })
            return ResolvedColumn(
                key: column.key,
                label: column.label ?? column.key,
                type: type,
                alignment: column.alignment ?? (type == .number ? .trailing : .leading)
            )
        }
    }

    // MARK: Sorting

    /// The column the table is sorted by and the direction.
    struct Sort: Hashable {
        /// The key of the column.
        let key: String
        /// Whether the order is ascending.
        let ascending: Bool
    }

    /// The sort after a header is activated: ascending for a new column, and
    /// the other direction for the column already sorted ascending.
    static func toggled(_ sort: Sort?, key: String) -> Sort {
        Sort(key: key, ascending: !(sort?.key == key && sort?.ascending == true))
    }

    /// The order of the rows as indexes. Blanks always go last, whatever the
    /// direction, and equal values keep the order the model gave.
    static func order(
        _ rows: [[String: MonarchAiJSON]],
        by sort: Sort?,
        columns: [ResolvedColumn],
        locale: Locale
    ) -> [Int] {
        let indexes = Array(rows.indices)
        guard let sort, let column = columns.first(where: { $0.key == sort.key }) else { return indexes }
        let values = rows.map { $0[column.key] }
        let blanks = values.map(isBlank)
        let numbers = column.type == .number ? values.map(number) : []
        let dates = column.type == .date ? values.map(date) : []
        let texts = column.type == .text ? values.map(text) : []
        func compare(_ a: Int, _ b: Int) -> ComparisonResult {
            switch column.type {
            case .number:
                let x = numbers[a] ?? 0
                let y = numbers[b] ?? 0
                return x == y ? .orderedSame : (x < y ? .orderedAscending : .orderedDescending)
            case .date:
                let x = dates[a] ?? .distantPast
                let y = dates[b] ?? .distantPast
                return x == y ? .orderedSame : (x < y ? .orderedAscending : .orderedDescending)
            case .text:
                return texts[a].compare(
                    texts[b],
                    options: [.caseInsensitive, .diacriticInsensitive, .numeric],
                    range: nil,
                    locale: locale
                )
            }
        }
        return indexes.sorted { a, b in
            if blanks[a] != blanks[b] { return !blanks[a] }
            if blanks[a] { return a < b }
            let result = compare(a, b)
            if result == .orderedSame { return a < b }
            return sort.ascending ? result == .orderedAscending : result == .orderedDescending
        }
    }

    // MARK: CSV

    /// One CSV field: quoted when it holds a comma, a quote, a line break or
    /// space at either end, with quotes doubled. Text a spreadsheet would run
    /// as a formula (`=`, `+`, `-`, `@`, tab, return) is prefixed with `'`,
    /// but numbers such as "-3" are kept.
    static func csvField(_ value: MonarchAiJSON?) -> String {
        var field = text(value)
        if case .string = value, let first = field.first, "=+-@\t\r".contains(first), number(value) == nil {
            field = "'" + field
        }
        let needsQuotes =
            field.contains(where: { $0 == "," || $0 == "\"" || $0 == "\n" || $0 == "\r" })
            || field.first?.isWhitespace == true || field.last?.isWhitespace == true
        return needsQuotes ? "\"" + field.replacingOccurrences(of: "\"", with: "\"\"") + "\"" : field
    }

    /// The rows as RFC 4180 CSV: a header row of labels, then one line per row, with CRLF line endings.
    static func csv(_ rows: [[String: MonarchAiJSON]], columns: [ResolvedColumn]) -> String {
        var lines = [columns.map { csvField(.string($0.label)) }.joined(separator: ",")]
        for row in rows {
            lines.append(columns.map { csvField(row[$0.key]) }.joined(separator: ","))
        }
        return lines.joined(separator: "\r\n")
    }

    // MARK: Accessibility

    /// What VoiceOver reads for a row: each header and its value, with
    /// `emptyLabel` for a missing value.
    static func rowLabel(
        _ row: [String: MonarchAiJSON],
        columns: [ResolvedColumn],
        locale: Locale,
        emptyLabel: String
    ) -> String {
        columns.map { column in
            let value = row[column.key]
            let text = isBlank(value) ? emptyLabel : display(value, type: column.type, locale: locale)
            return "\(column.label): \(text)"
        }
        .joined(separator: ", ")
    }

    /// How many rows are rendered: all of them once shown, otherwise at most `visibleRows` (at least 1).
    static func shownCount(total: Int, visibleRows: Int, isShowingAll: Bool) -> Int {
        isShowingAll ? total : min(total, max(1, visibleRows))
    }
}
