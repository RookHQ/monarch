import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private enum TableSamples {
    static let columns = [
        MonarchAiTableColumn("region", label: "Region"),
        MonarchAiTableColumn("revenue", label: "Revenue"),
        MonarchAiTableColumn("updated", label: "Updated"),
        MonarchAiTableColumn("note", label: "Note"),
    ]
    static let rows: [[String: MonarchAiJSON]] = [
        ["region": "North", "revenue": 1200, "updated": "2024-10-14", "note": "On track"],
        ["region": "South", "revenue": "980.5", "updated": "2024-09-30", "note": nil],
        ["region": "East", "revenue": nil, "updated": "2024-10-01T09:30Z", "note": "=SUM(A1:A3)"],
        ["region": "west", "revenue": "-3", "updated": "2024-10-02", "note": "Says \"hello\", then leaves"],
    ]
    static let wide: [[String: MonarchAiJSON]] = (1...12).map { index in
        [
            "id": .number(Double(index)),
            "customer": .string("Customer \(index) with a fairly long name"),
            "plan": .string(index.isMultiple(of: 2) ? "Team" : "Enterprise"),
            "seats": .number(Double(index * 7)),
            "renews": .string("2025-0\(index % 9 + 1)-15"),
        ]
    }
}

enum MonarchAiTableAnswerFixtures: ComponentFixtures {
    static let id = "ai-table-answer"
    static let fixtures = [
        Fixture("typed columns, every action") {
            MonarchAiTableAnswer(
                TableSamples.rows,
                title: "Revenue by region",
                columns: TableSamples.columns,
                onOpenAsArtifact: {}
            )
        },
        Fixture("inferred columns, wide, row limit") {
            MonarchAiTableAnswer(TableSamples.wide, visibleRows: 5)
        },
        Fixture("streaming") {
            MonarchAiTableAnswer(
                Array(TableSamples.rows.prefix(2)),
                title: "Revenue by region",
                columns: TableSamples.columns,
                isStreaming: true,
                onOpenAsArtifact: {}
            )
        },
        Fixture("no rows, not sortable, not copyable") {
            MonarchAiTableAnswer([], title: "Nothing found", isSortable: false, isCopyable: false)
        },
        Fixture("custom labels") {
            MonarchAiTableAnswer(
                TableSamples.rows,
                columns: TableSamples.columns,
                labels: MonarchAiTableAnswerLabels(aiTable: "Generated", rowCount: { "\($0) lines" }, copyCSV: "Copy")
            )
        },
    ]
}

@Suite("MonarchAiTableAnswer")
struct MonarchAiTableAnswerTests {
    @Test("Numbers and numeric text are read as numbers")
    func numbers() {
        #expect(MonarchAiTableAnswerTokens.number(1200) == 1200)
        #expect(MonarchAiTableAnswerTokens.number("1,200") == 1200)
        #expect(MonarchAiTableAnswerTokens.number("-3.5") == -3.5)
        #expect(MonarchAiTableAnswerTokens.number("−3") == -3)
        #expect(MonarchAiTableAnswerTokens.number("+4") == 4)
        #expect(MonarchAiTableAnswerTokens.number("12%") == 12)
        #expect(MonarchAiTableAnswerTokens.number(".5") == 0.5)
        #expect(MonarchAiTableAnswerTokens.number("12,34") == nil)
        #expect(MonarchAiTableAnswerTokens.number("abc") == nil)
        #expect(MonarchAiTableAnswerTokens.number("%") == nil)
        #expect(MonarchAiTableAnswerTokens.number(true) == nil)
        #expect(MonarchAiTableAnswerTokens.number(nil) == nil)
    }

    @Test("ISO dates and times are read as times, in UTC without a zone")
    func dates() {
        let day = MonarchAiTableAnswerTokens.date("2024-10-14")
        #expect(day == Date(timeIntervalSince1970: 1_728_864_000))
        #expect(MonarchAiTableAnswerTokens.date("2024-10-14T09:30Z") == Date(timeIntervalSince1970: 1_728_898_200))
        #expect(MonarchAiTableAnswerTokens.date("2024-10-14 09:30:00") == Date(timeIntervalSince1970: 1_728_898_200))
        #expect(
            MonarchAiTableAnswerTokens.date("2024-10-14T11:30:00+02:00") == Date(timeIntervalSince1970: 1_728_898_200)
        )
        #expect(MonarchAiTableAnswerTokens.date("2024-10-14T09:30:00.250Z") != nil)
        #expect(MonarchAiTableAnswerTokens.date("14/10/2024") == nil)
        #expect(MonarchAiTableAnswerTokens.date(20_241_014) == nil)
    }

    @Test("A column's type is inferred from its values, ignoring blanks")
    func inference() {
        #expect(MonarchAiTableAnswerTokens.inferType([1, "2,000", nil, ""]) == .number)
        #expect(MonarchAiTableAnswerTokens.inferType(["2024-10-14", nil, "2024-01-01T00:00Z"]) == .date)
        #expect(MonarchAiTableAnswerTokens.inferType(["North", 1]) == .text)
        #expect(MonarchAiTableAnswerTokens.inferType([nil, .null]) == .text)
    }

    @Test("Columns default to every key in alphabetical order, numbers at the trailing edge")
    func columns() {
        let resolved = MonarchAiTableAnswerTokens.resolveColumns(nil, rows: TableSamples.rows)
        #expect(resolved.map(\.key) == ["note", "region", "revenue", "updated"])
        #expect(resolved.map(\.label) == ["note", "region", "revenue", "updated"])
        #expect(resolved.first { $0.key == "revenue" }?.type == .number)
        #expect(resolved.first { $0.key == "revenue" }?.alignment == .trailing)
        #expect(resolved.first { $0.key == "updated" }?.type == .date)
        #expect(resolved.first { $0.key == "region" }?.alignment == .leading)

        let given = MonarchAiTableAnswerTokens.resolveColumns(
            [MonarchAiTableColumn("revenue", label: "Revenue", type: .text, alignment: .leading)],
            rows: TableSamples.rows
        )
        #expect(given.map(\.label) == ["Revenue"])
        #expect(given.first?.type == .text)
        #expect(given.first?.alignment == .leading)
    }

    @Test("A header sorts ascending first, then flips")
    func toggling() {
        let first = MonarchAiTableAnswerTokens.toggled(nil, key: "revenue")
        #expect(first == .init(key: "revenue", ascending: true))
        let second = MonarchAiTableAnswerTokens.toggled(first, key: "revenue")
        #expect(second == .init(key: "revenue", ascending: false))
        #expect(MonarchAiTableAnswerTokens.toggled(second, key: "revenue").ascending)
        #expect(MonarchAiTableAnswerTokens.toggled(second, key: "region") == .init(key: "region", ascending: true))
    }

    @Test("Numbers sort by value, blanks last in both directions")
    func sortNumbers() {
        let columns = MonarchAiTableAnswerTokens.resolveColumns(TableSamples.columns, rows: TableSamples.rows)
        let up = MonarchAiTableAnswerTokens.order(
            TableSamples.rows,
            by: .init(key: "revenue", ascending: true),
            columns: columns,
            locale: Locale(identifier: "en_US")
        )
        #expect(up == [3, 1, 0, 2])
        let down = MonarchAiTableAnswerTokens.order(
            TableSamples.rows,
            by: .init(key: "revenue", ascending: false),
            columns: columns,
            locale: Locale(identifier: "en_US")
        )
        #expect(down == [0, 1, 3, 2])
    }

    @Test("Dates sort by time and text ignores case")
    func sortDatesAndText() {
        let columns = MonarchAiTableAnswerTokens.resolveColumns(TableSamples.columns, rows: TableSamples.rows)
        let locale = Locale(identifier: "en_US")
        let byDate = MonarchAiTableAnswerTokens.order(
            TableSamples.rows,
            by: .init(key: "updated", ascending: true),
            columns: columns,
            locale: locale
        )
        #expect(byDate == [1, 2, 3, 0])
        let byRegion = MonarchAiTableAnswerTokens.order(
            TableSamples.rows,
            by: .init(key: "region", ascending: true),
            columns: columns,
            locale: locale
        )
        #expect(byRegion == [2, 0, 1, 3])
        // No sort, or an unknown column, keeps the model's order.
        #expect(
            MonarchAiTableAnswerTokens.order(TableSamples.rows, by: nil, columns: columns, locale: locale) == [
                0, 1, 2, 3,
            ]
        )
        #expect(
            MonarchAiTableAnswerTokens.order(
                TableSamples.rows,
                by: .init(key: "missing", ascending: true),
                columns: columns,
                locale: locale
            ) == [0, 1, 2, 3]
        )
    }

    @Test("CSV quotes, doubles quotes, neutralises formulas and keeps negative numbers")
    func csv() {
        let columns = MonarchAiTableAnswerTokens.resolveColumns(TableSamples.columns, rows: TableSamples.rows)
        let csv = MonarchAiTableAnswerTokens.csv(TableSamples.rows, columns: columns)
        let lines = csv.components(separatedBy: "\r\n")
        #expect(lines.count == 5)
        #expect(lines[0] == "Region,Revenue,Updated,Note")
        #expect(lines[1] == "North,1200,2024-10-14,On track")
        #expect(lines[2] == "South,980.5,2024-09-30,")
        #expect(lines[3] == "East,,2024-10-01T09:30Z,'=SUM(A1:A3)")
        #expect(lines[4] == "west,-3,2024-10-02,\"Says \"\"hello\"\", then leaves\"")
        #expect(MonarchAiTableAnswerTokens.csvField(" padded") == "\" padded\"")
        #expect(MonarchAiTableAnswerTokens.csvField("@user") == "'@user")
        #expect(MonarchAiTableAnswerTokens.csvField(-3) == "-3")
    }

    @Test("Values become text: whole numbers without a point, objects as compact JSON")
    func text() {
        #expect(MonarchAiTableAnswerTokens.text(1200) == "1200")
        #expect(MonarchAiTableAnswerTokens.text(2.5) == "2.5")
        #expect(MonarchAiTableAnswerTokens.text(true) == "true")
        #expect(MonarchAiTableAnswerTokens.text(["b": 1, "a": "x"]) == #"{"a":"x","b":1}"#)
        #expect(MonarchAiTableAnswerTokens.text(nil) == "")
        #expect(MonarchAiTableAnswerTokens.display(1200, type: .number, locale: Locale(identifier: "en_US")) == "1,200")
        #expect(MonarchAiTableAnswerTokens.display(1200, type: .text, locale: Locale(identifier: "en_US")) == "1200")
    }

    @Test("VoiceOver reads each row as headers with their values, and empty cells as Empty")
    func rowLabel() {
        let columns = MonarchAiTableAnswerTokens.resolveColumns(TableSamples.columns, rows: TableSamples.rows)
        let label = MonarchAiTableAnswerTokens.rowLabel(
            TableSamples.rows[1],
            columns: columns,
            locale: Locale(identifier: "en_US"),
            emptyLabel: MonarchAiTableAnswerStrings.empty
        )
        #expect(label == "Region: South, Revenue: 980.5, Updated: 2024-09-30, Note: Empty")
    }

    @Test("Long tables draw a bounded number of rows until shown in full")
    func rowLimit() {
        #expect(MonarchAiTableAnswerTokens.shownCount(total: 250, visibleRows: 200, isShowingAll: false) == 200)
        #expect(MonarchAiTableAnswerTokens.shownCount(total: 250, visibleRows: 200, isShowingAll: true) == 250)
        #expect(MonarchAiTableAnswerTokens.shownCount(total: 3, visibleRows: 0, isShowingAll: false) == 1)
        #expect(MonarchAiTableAnswerTokens.shownCount(total: 0, visibleRows: 200, isShowingAll: false) == 0)
    }

    @Test("The sort direction has its own icon, so it does not depend on colour")
    func sortSymbols() {
        let symbols = Set([true, false, nil].map { MonarchAiTableAnswerTokens.sortSymbol(ascending: $0) })
        #expect(symbols.count == 3)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiTableAnswerStrings.aiTable == "AI table")
        #expect(MonarchAiTableAnswerStrings.rowCount(1) == "1 row")
        #expect(MonarchAiTableAnswerStrings.rowCount(12) == "12 rows")
        #expect(MonarchAiTableAnswerStrings.copyCSV == "Copy as CSV")
        #expect(MonarchAiTableAnswerStrings.copied == "Copied")
        #expect(MonarchAiTableAnswerStrings.openAsArtifact == "Open as artifact")
        #expect(MonarchAiTableAnswerStrings.sorted("Revenue", ascending: true) == "Sorted by Revenue, ascending.")
        #expect(MonarchAiTableAnswerStrings.sorted("Revenue", ascending: false) == "Sorted by Revenue, descending.")
        #expect(MonarchAiTableAnswerStrings.sortValue(ascending: true) == "Sorted ascending")
        #expect(MonarchAiTableAnswerStrings.empty == "Empty")
        #expect(MonarchAiTableAnswerStrings.noRows == "No rows.")
        #expect(MonarchAiTableAnswerStrings.loadingRows == "More rows are being generated")
        #expect(MonarchAiTableAnswerStrings.complete == "Table complete.")
        #expect(MonarchAiTableAnswerStrings.showAllRows(250) == "Show all 250 rows")
    }
}
