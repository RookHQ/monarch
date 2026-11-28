import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let previewColumns = [
    MonarchPreviewRowsColumn("Name", id: "name"),
    MonarchPreviewRowsColumn("Email", id: "email"),
    MonarchPreviewRowsColumn("Seats", id: "seats", isNumeric: true),
]

private let previewRows = [
    MonarchPreviewRow(row: 1, values: ["name": "Ada Lovelace", "email": "ada@example.com", "seats": "3"]),
    MonarchPreviewRow(
        row: 2,
        values: ["name": "Alan Turing", "email": "alan@", "seats": "1"],
        errors: [MonarchPreviewRowIssue("Not a valid address", field: "Email")]
    ),
    MonarchPreviewRow(
        row: 3,
        values: ["name": "Grace Hopper", "email": ""],
        warnings: [MonarchPreviewRowIssue("Missing email"), MonarchPreviewRowIssue("Seats set to 1", field: "Seats")]
    ),
    MonarchPreviewRow(
        row: 4,
        values: ["name": "Ada Lovelace", "email": "ada@example.com"],
        status: .duplicate,
        note: "Matches an existing contact"
    ),
]

enum MonarchPreviewRowsFixtures: ComponentFixtures {
    static let id = "preview-rows"
    static let fixtures = [
        Fixture("every result, filter tabs") {
            MonarchPreviewRows("Preview of contacts.csv", rows: previewRows, columns: previewColumns)
        },
        Fixture("every row ready, no tabs") {
            MonarchPreviewRows(
                "Preview",
                rows: [previewRows[0], MonarchPreviewRow(row: 2, values: ["name": "Edsger Dijkstra"])],
                columns: previewColumns
            )
        },
        Fixture("filtered to errors") {
            MonarchPreviewRows(
                "Preview",
                rows: previewRows,
                columns: previewColumns,
                filter: .constant(.status(.error))
            )
        },
        Fixture("filter matches nothing") {
            MonarchPreviewRows(
                "Preview",
                rows: [previewRows[0]],
                columns: previewColumns,
                filter: .constant(.status(.warning))
            )
        },
        Fixture("empty file") {
            MonarchPreviewRows("Preview", rows: [], columns: previewColumns)
        },
        Fixture("paged, long text, custom labels") {
            MonarchPreviewRows(
                "Preview",
                rows: (1...5).map {
                    MonarchPreviewRow(
                        row: $0,
                        values: ["name": "A very long organisation name that has to wrap onto another line \($0)"],
                        errors: [
                            MonarchPreviewRowIssue(
                                "This value is longer than the field allows and will be cut",
                                field: "Name"
                            )
                        ]
                    )
                },
                columns: previewColumns,
                isFilterable: false,
                pageSize: 2,
                labels: MonarchPreviewRowsLabels(row: { "Line \($0)" })
            )
        },
    ]
}

@Suite("MonarchPreviewRows")
@MainActor
struct MonarchPreviewRowsTests {
    private let labels = MonarchPreviewRowsLabels()

    @Test("The status comes from the override, then errors, then warnings")
    func status() {
        #expect(previewRows.map(\.resolvedStatus) == [.ready, .error, .warning, .duplicate])
        let both = MonarchPreviewRow(
            row: 9,
            values: [:],
            errors: [MonarchPreviewRowIssue("e")],
            warnings: [MonarchPreviewRowIssue("w")]
        )
        #expect(both.resolvedStatus == .error)
        let overridden = MonarchPreviewRow(row: 9, values: [:], status: .ready, errors: [MonarchPreviewRowIssue("e")])
        #expect(overridden.resolvedStatus == .ready)
    }

    @Test("Rows are counted by status")
    func counts() {
        let counts = MonarchPreviewRowsTokens.counts(previewRows)
        #expect(counts == [.ready: 1, .warning: 1, .error: 1, .duplicate: 1])
        #expect(MonarchPreviewRowsTokens.counts([]) == [.ready: 0, .warning: 0, .error: 0, .duplicate: 0])
    }

    @Test("A filter keeps only its rows, in order")
    func filtering() {
        #expect(MonarchPreviewRowsTokens.filtered(previewRows, by: .all).map(\.row) == [1, 2, 3, 4])
        #expect(MonarchPreviewRowsTokens.filtered(previewRows, by: .status(.error)).map(\.row) == [2])
        #expect(MonarchPreviewRowsTokens.filtered([previewRows[0]], by: .status(.warning)).isEmpty)
    }

    @Test("Tabs show when some row is not ready or a filter is chosen")
    func showsFilter() {
        let problems = MonarchPreviewRowsTokens.counts(previewRows)
        let clean = MonarchPreviewRowsTokens.counts([previewRows[0]])
        #expect(MonarchPreviewRowsTokens.showsFilter(isFilterable: true, counts: problems, filter: .all))
        #expect(!MonarchPreviewRowsTokens.showsFilter(isFilterable: true, counts: clean, filter: .all))
        #expect(MonarchPreviewRowsTokens.showsFilter(isFilterable: true, counts: clean, filter: .status(.ready)))
        #expect(!MonarchPreviewRowsTokens.showsFilter(isFilterable: false, counts: problems, filter: .all))
    }

    @Test("Tabs are All, then problems first, only statuses with rows")
    func options() {
        let options = MonarchPreviewRowsTokens.options(rows: previewRows, filter: .all, labels: labels)
        #expect(options.map(\.title) == ["All", "Error", "Warning", "Duplicate", "Ready"])
        #expect(options.map(\.count) == [4, 1, 1, 1, 1])
        #expect(options.map(\.countTone) == [.neutral, .danger, .warning, .neutral, .neutral])
        let clean = MonarchPreviewRowsTokens.options(rows: [previewRows[0]], filter: .status(.error), labels: labels)
        #expect(clean.map(\.filter) == [.all, .status(.error), .status(.ready)])
        #expect(clean[1].count == 0)
    }

    @Test("Statuses map to tones and distinct symbols")
    func tones() {
        #expect(
            MonarchPreviewRowStatus.allCases.map(MonarchPreviewRowsTokens.tone) == [.success, .warning, .danger, .info]
        )
        let symbols = Set(MonarchPreviewRowStatus.allCases.map(MonarchPreviewRowsTokens.symbol))
        #expect(symbols.count == MonarchPreviewRowStatus.allCases.count)
        #expect(MonarchPreviewRowsTokens.issueRole(isError: true) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchPreviewRowsTokens.issueRole(isError: false) == \MonarchColorTokens.warningStrong)
    }

    @Test("Paging shows a page at a time, zero shows all")
    func paging() {
        #expect(MonarchPreviewRowsTokens.visibleCount(total: 120, pageSize: 50, pages: 1) == 50)
        #expect(MonarchPreviewRowsTokens.visibleCount(total: 120, pageSize: 50, pages: 3) == 120)
        #expect(MonarchPreviewRowsTokens.visibleCount(total: 120, pageSize: 0, pages: 1) == 120)
        #expect(MonarchPreviewRowsTokens.visibleCount(total: 10, pageSize: 50, pages: 0) == 10)
    }

    @Test("Values and issues read as drawn")
    func text() {
        #expect(MonarchPreviewRowsTokens.value(nil) == nil)
        #expect(MonarchPreviewRowsTokens.value("") == nil)
        #expect(MonarchPreviewRowsTokens.value("3") == "3")
        #expect(MonarchPreviewRowsTokens.issueText(MonarchPreviewRowIssue("Bad", field: "Email")) == "Email: Bad")
        #expect(MonarchPreviewRowsTokens.issueText(MonarchPreviewRowIssue("Bad")) == "Bad")
    }

    @Test("A row is read as its title, then result, values, issues and note")
    func spoken() {
        #expect(MonarchPreviewRowsTokens.rowTitle(4, labels: labels) == "Row 4")
        #expect(
            MonarchPreviewRowsTokens.spokenValue(previewRows[2], columns: previewColumns, labels: labels)
                == "Warning. Name: Grace Hopper. Email: Empty. Seats: Empty. Warning: Missing email. "
                + "Warning: Seats: Seats set to 1"
        )
        #expect(
            MonarchPreviewRowsTokens.spokenValue(previewRows[3], columns: [previewColumns[0]], labels: labels)
                == "Duplicate. Name: Ada Lovelace. Matches an existing contact"
        )
        let custom = MonarchPreviewRowsLabels(error: "Skipped", row: { "Line \($0)" }, emptyValue: "Blank")
        #expect(MonarchPreviewRowsTokens.rowTitle(2, labels: custom) == "Line 2")
        #expect(
            MonarchPreviewRowsTokens.spokenValue(previewRows[1], columns: [previewColumns[1]], labels: custom)
                == "Skipped. Email: alan@. Skipped: Email: Not a valid address"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchPreviewRowsStrings.filter, MonarchPreviewRowsStrings.all, MonarchPreviewRowsStrings.ready,
            MonarchPreviewRowsStrings.warning, MonarchPreviewRowsStrings.error, MonarchPreviewRowsStrings.duplicate,
            MonarchPreviewRowsStrings.row(1), MonarchPreviewRowsStrings.emptyValue,
            MonarchPreviewRowsStrings.emptyTitle, MonarchPreviewRowsStrings.emptyDescription,
            MonarchPreviewRowsStrings.filteredTitle,
        ]
        for string in all { #expect(!string.hasPrefix("monarch.")) }
        #expect(MonarchPreviewRowsStrings.row(12) == "Row 12")
    }
}
