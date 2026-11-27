import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchTableBlockFixtures: ComponentFixtures {
    static let id = "table-block"

    private static let plans = MonarchTableBlockData(rows: [
        ["Plan", "Seats", "Price"],
        ["Team", "10", "12"],
        ["Business", "Unlimited", "30"],
    ])

    static let fixtures = [
        Fixture("read-only with caption and header") {
            MonarchTableBlock("Plans", data: plans)
        },
        Fixture("read-only without header or caption") {
            MonarchTableBlock(data: MonarchTableBlockData(rows: [["A", "B"], ["C", "D"]], hasHeaderRow: false))
        },
        Fixture("wide table with long text and empty cells") {
            MonarchTableBlock(
                "Comparison",
                data: MonarchTableBlockData(rows: [
                    ["Feature", "Starter", "Team", "Business", "Enterprise", "Notes"],
                    [
                        "Audit log", "", "30 days", "1 year", "Unlimited",
                        "A long note that has to wrap inside its column instead of widening the table without limit.",
                    ],
                ]),
                label: "Plan comparison"
            )
        },
        Fixture("single cell") {
            MonarchTableBlock(data: MonarchTableBlockData(rows: [["Only"]]))
        },
        Fixture("editable with controls and delete") {
            MonarchTableBlock("Plans", data: .constant(plans)) {}
        },
        Fixture("editable, empty, custom labels") {
            MonarchTableBlock(
                data: .constant(MonarchTableBlockData(rowCount: 2, columnCount: 2)),
                labels: MonarchTableBlockLabels(controls: "Tabelle", cellPosition: { "Zeile \($0), Spalte \($1)" })
            )
        },
        Fixture("editable, disabled") {
            MonarchTableBlock(data: .constant(plans)).disabled(true)
        },
    ]
}

@Suite("MonarchTableBlock")
struct MonarchTableBlockTests {
    @Test("Rows are padded to a rectangle and an empty input is one empty cell")
    func normalizes() {
        let ragged = MonarchTableBlockData(rows: [["a"], ["b", "c", "d"], []])
        #expect(ragged.rows == [["a", "", ""], ["b", "c", "d"], ["", "", ""]])
        #expect(ragged.rowCount == 3)
        #expect(ragged.columnCount == 3)
        #expect(MonarchTableBlockData(rows: []).rows == [[""]])
        #expect(MonarchTableBlockData(rows: [[]]).rows == [[""]])
    }

    @Test("Tables are clamped to 50 rows and 20 columns")
    func limits() {
        let large = MonarchTableBlockData(rowCount: 500, columnCount: 500)
        #expect(large.rowCount == MonarchTableBlockData.maximumRowCount)
        #expect(large.columnCount == MonarchTableBlockData.maximumColumnCount)
        #expect(!large.canAddRow)
        #expect(!large.canAddColumn)
        let small = MonarchTableBlockData(rowCount: 0, columnCount: -4)
        #expect(small.rowCount == 1 && small.columnCount == 1)
        let wide = MonarchTableBlockData(rows: Array(repeating: Array(repeating: "x", count: 30), count: 60))
        #expect(wide.rowCount == 50 && wide.columnCount == 20)
        var full = large
        let changed1 = full.addRow(after: 0)
        #expect(!changed1)
        let changed2 = full.addColumn(after: 0)
        #expect(!changed2)
        #expect(full == large)
    }

    @Test("The default table is three by three with a header row")
    func defaults() {
        let table = MonarchTableBlockData()
        #expect(table.rowCount == 3 && table.columnCount == 3)
        #expect(table.hasHeaderRow)
        #expect(table.isHeader(row: 0))
        #expect(!table.isHeader(row: 1))
        var plain = table
        plain.hasHeaderRow = false
        #expect(!plain.isHeader(row: 0))
    }

    @Test("Cells are read and written by position, and positions outside the table are ignored")
    func cells() {
        var table = MonarchTableBlockData(rows: [["a", "b"], ["c", "d"]])
        #expect(table[1, 0] == "c")
        table[1, 0] = "changed"
        #expect(table.rows == [["a", "b"], ["changed", "d"]])
        #expect(table[5, 5].isEmpty)
        #expect(table[-1, 0].isEmpty)
        table[5, 5] = "ignored"
        table[0, -1] = "ignored"
        #expect(table.rows == [["a", "b"], ["changed", "d"]])
    }

    @Test("Rows and columns are added after a position")
    func adds() {
        var table = MonarchTableBlockData(rows: [["a", "b"], ["c", "d"]])
        let changed3 = table.addRow(after: 0)
        #expect(changed3)
        #expect(table.rows == [["a", "b"], ["", ""], ["c", "d"]])
        let changed4 = table.addColumn(after: 1)
        #expect(changed4)
        #expect(table.rows == [["a", "b", ""], ["", "", ""], ["c", "d", ""]])
        let changed5 = table.addColumn(after: 0)
        #expect(changed5)
        #expect(table.rows[0] == ["a", "", "b", ""])
        let changed6 = table.addRow(after: 9)
        #expect(!changed6)
        let changed7 = table.addColumn(after: -1)
        #expect(!changed7)
        #expect(table.rowCount == 3 && table.columnCount == 4)
    }

    @Test("Rows and columns are deleted, but never the last one")
    func deletes() {
        var table = MonarchTableBlockData(rows: [["a", "b"], ["c", "d"]])
        let changed8 = table.deleteRow(at: 0)
        #expect(changed8)
        #expect(table.rows == [["c", "d"]])
        #expect(!table.canDeleteRow)
        let changed9 = table.deleteRow(at: 0)
        #expect(!changed9)
        let changed10 = table.deleteColumn(at: 1)
        #expect(changed10)
        #expect(table.rows == [["c"]])
        #expect(!table.canDeleteColumn)
        let changed11 = table.deleteColumn(at: 0)
        #expect(!changed11)
        #expect(table.rows == [["c"]])
        var other = MonarchTableBlockData(rows: [["a", "b"], ["c", "d"]])
        let changed12 = other.deleteRow(at: 2)
        #expect(!changed12)
        let changed13 = other.deleteColumn(at: 2)
        #expect(!changed13)
    }

    @Test("The controls act on the cell that last had focus, or on the last cell")
    func target() {
        let table = MonarchTableBlockData(rowCount: 3, columnCount: 4)
        #expect(MonarchTableBlockTokens.target(active: nil, in: table) == MonarchTableBlockCell(row: 2, column: 3))
        let inside = MonarchTableBlockCell(row: 1, column: 2)
        #expect(MonarchTableBlockTokens.target(active: inside, in: table) == inside)
        let stale = MonarchTableBlockCell(row: 9, column: 9)
        #expect(MonarchTableBlockTokens.target(active: stale, in: table) == MonarchTableBlockCell(row: 2, column: 3))
        let negative = MonarchTableBlockCell(row: -1, column: 0)
        #expect(MonarchTableBlockTokens.target(active: negative, in: table) == MonarchTableBlockCell(row: 2, column: 3))
    }

    @Test("The table is named by the label, then the caption, then the built-in name")
    func regionLabel() {
        #expect(MonarchTableBlockTokens.regionLabel(label: "Plan comparison", caption: "Plans") == "Plan comparison")
        #expect(MonarchTableBlockTokens.regionLabel(label: nil, caption: "Plans") == "Plans")
        #expect(MonarchTableBlockTokens.regionLabel(label: "", caption: "") == "Table")
        #expect(MonarchTableBlockTokens.regionLabel(label: nil, caption: nil) == "Table")
        #expect(MonarchTableBlockTokens.captionIsSpokenName(label: nil))
        #expect(MonarchTableBlockTokens.captionIsSpokenName(label: ""))
        #expect(!MonarchTableBlockTokens.captionIsSpokenName(label: "Plan comparison"))
    }

    @Test("VoiceOver reads a cell's text and its position counted from one")
    func cellAccessibility() {
        let labels = MonarchTableBlockLabels()
        let cell = MonarchTableBlockCell(row: 1, column: 0)
        #expect(MonarchTableBlockTokens.position(of: cell, labels: labels) == "Row 2, column 1")
        #expect(MonarchTableBlockTokens.cellLabel(text: "Team", labels: labels) == "Team")
        #expect(MonarchTableBlockTokens.cellLabel(text: "", labels: labels) == "Empty")
        #expect(MonarchTableBlockTokens.cellLabel(text: "  ", labels: labels) == "Empty")
        let german = MonarchTableBlockLabels(emptyCell: "Leer", cellPosition: { "Zeile \($0), Spalte \($1)" })
        #expect(MonarchTableBlockTokens.position(of: cell, labels: german) == "Zeile 2, Spalte 1")
        #expect(MonarchTableBlockTokens.cellLabel(text: "", labels: german) == "Leer")
    }

    @Test("Header cells differ in fill and in weight; disabled text uses the disabled role")
    func appearance() {
        #expect(MonarchTableBlockTokens.background(isHeader: true) == \MonarchColorTokens.bgSubtle)
        #expect(MonarchTableBlockTokens.background(isHeader: false) == \MonarchColorTokens.bgSurface)
        #expect(MonarchTableBlockTokens.textStyle(isHeader: true) == .bodyStrong)
        #expect(MonarchTableBlockTokens.textStyle(isHeader: false) == .body)
        #expect(MonarchTableBlockTokens.foreground(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchTableBlockTokens.foreground(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchTableBlockTokens.line == \MonarchColorTokens.borderDefault)
    }

    @Test("Editable cells are at least as tall as the touch target and columns can wrap")
    func sizes() {
        #expect(MonarchTableBlockTokens.editableCellMinHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(MonarchTableBlockTokens.maxColumnWidth > MonarchTableBlockTokens.minColumnWidth)
        #expect(MonarchTableBlockTokens.editableColumnWidth >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("The built-in strings resolve")
    func strings() {
        let strings = [
            MonarchTableBlockStrings.region, MonarchTableBlockStrings.controls, MonarchTableBlockStrings.addRow,
            MonarchTableBlockStrings.addColumn, MonarchTableBlockStrings.deleteRow,
            MonarchTableBlockStrings.deleteColumn, MonarchTableBlockStrings.headerRow,
            MonarchTableBlockStrings.deleteTable, MonarchTableBlockStrings.emptyCell,
        ]
        #expect(strings.allSatisfy { !$0.isEmpty && !$0.hasPrefix("monarch.") })
        #expect(MonarchTableBlockStrings.addRow == "Add row below")
        #expect(MonarchTableBlockStrings.cellPosition(row: 12, column: 3) == "Row 12, column 3")
    }

    #if os(iOS)
        // Touch targets and Dynamic Type apply on the simulator.
        @Test("An editable table with one cell is at least as tall as its controls and a 44-point cell")
        @MainActor
        func editableHeight() {
            let table = MonarchTableBlock(data: .constant(MonarchTableBlockData(rowCount: 1, columnCount: 1)))
            #expect(Host.layout(table).height >= 2 * MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
