import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let chooserColumns = [
    MonarchColumnChooserColumn("Name", isHideable: false),
    MonarchColumnChooserColumn("Status"),
    MonarchColumnChooserColumn("Owner"),
    MonarchColumnChooserColumn("Updated"),
]

private let manyChooserColumns =
    chooserColumns + ["Priority", "Region", "Created", "Due date", "Customer"].map { MonarchColumnChooserColumn($0) }

private let chooserHeight = MonarchTokens.Space.space24 * 4

enum MonarchColumnChooserFixtures: ComponentFixtures {
    static let id = "column-chooser"
    static let fixtures = [
        Fixture("required column, one hidden, reorderable, reset") {
            MonarchColumnChooser(
                chooserColumns,
                hiddenColumns: .constant(["Owner"]),
                order: .constant(["Updated"]),
                onReset: {}
            )
            .frame(height: chooserHeight)
        },
        Fixture("all shown, no reordering") {
            MonarchColumnChooser(chooserColumns, hiddenColumns: .constant([]))
                .frame(height: chooserHeight)
        },
        Fixture("searchable, minimum reached") {
            MonarchColumnChooser(
                manyChooserColumns,
                hiddenColumns: .constant(Set(manyChooserColumns.dropFirst(2).map(\.id))),
                minimumVisible: 2
            )
            .frame(height: chooserHeight)
        },
        Fixture("custom labels and long names") {
            MonarchColumnChooser(
                [
                    MonarchColumnChooserColumn("Date of the last contact with the customer's account manager"),
                    MonarchColumnChooserColumn("Region"),
                ],
                hiddenColumns: .constant([]),
                labels: MonarchColumnChooserLabels(title: "Visible fields", shownCount: { "\($0)/\($1)" })
            )
            .frame(height: chooserHeight)
        },
    ]
}

@Suite("MonarchColumnChooser")
struct MonarchColumnChooserTests {
    private typealias Tokens = MonarchColumnChooserTokens

    @Test("Order puts listed ids first, skips unknown ids, and appends the rest")
    func ordered() {
        let ids = { (order: [String]?) in Tokens.ordered(chooserColumns, order: order).map(\.id) }
        #expect(ids(nil) == ["Name", "Status", "Owner", "Updated"])
        #expect(ids([]) == ["Name", "Status", "Owner", "Updated"])
        #expect(ids(["Updated", "Gone", "Status"]) == ["Updated", "Status", "Name", "Owner"])
        #expect(ids(["Owner", "Owner"]) == ["Owner", "Name", "Status", "Updated"])
    }

    @Test("A column that cannot be hidden is always shown and its switch is off limits")
    func required() {
        let name = chooserColumns[0]
        #expect(Tokens.isShown(name, hidden: ["Name"]))
        #expect(Tokens.shownCount(chooserColumns, hidden: ["Name", "Owner"]) == 3)
        #expect(Tokens.isToggleDisabled(name, hidden: [], shownCount: 4, minimumVisible: 1))
    }

    @Test("The last shown columns cannot be switched off; hidden ones can be switched on")
    func minimum() {
        let status = chooserColumns[1]
        #expect(!Tokens.isToggleDisabled(status, hidden: [], shownCount: 4, minimumVisible: 1))
        #expect(Tokens.isToggleDisabled(status, hidden: [], shownCount: 2, minimumVisible: 2))
        #expect(!Tokens.isToggleDisabled(status, hidden: ["Status"], shownCount: 1, minimumVisible: 2))
    }

    @Test("Switching updates the hidden set")
    func setting() {
        #expect(Tokens.setting("Owner", isShown: false, in: []) == ["Owner"])
        #expect(Tokens.setting("Owner", isShown: true, in: ["Owner", "Status"]) == ["Status"])
    }

    @Test("Search matches names ignoring case and diacritics, from the threshold")
    func search() {
        let columns = [MonarchColumnChooserColumn("Créé le"), MonarchColumnChooserColumn("Status")]
        #expect(Tokens.isSearchable(columnCount: 8, threshold: 8))
        #expect(!Tokens.isSearchable(columnCount: 7, threshold: 8))
        #expect(Tokens.filtered(columns, query: "cree", isSearchable: true).map(\.id) == ["Créé le"])
        #expect(Tokens.filtered(columns, query: " STAT ", isSearchable: true).map(\.id) == ["Status"])
        #expect(Tokens.filtered(columns, query: "zzz", isSearchable: true).isEmpty)
        #expect(Tokens.filtered(columns, query: "zzz", isSearchable: false).count == 2)
        #expect(Tokens.isFiltering(query: "st", isSearchable: true))
        #expect(!Tokens.isFiltering(query: "  ", isSearchable: true))
        #expect(!Tokens.isFiltering(query: "st", isSearchable: false))
    }

    @Test("Moves by a step stay inside the list; drags use onMove offsets")
    func moves() {
        let ids = ["a", "b", "c"]
        #expect(Tokens.moving("a", by: 1, in: ids) == ["b", "a", "c"])
        #expect(Tokens.moving("c", by: -1, in: ids) == ["a", "c", "b"])
        #expect(Tokens.moving("a", by: -1, in: ids) == nil)
        #expect(Tokens.moving("c", by: 1, in: ids) == nil)
        #expect(Tokens.moving("z", by: 1, in: ids) == nil)
        #expect(Tokens.moving(fromOffsets: IndexSet(integer: 0), toOffset: 3, in: ids) == ["b", "c", "a"])
    }

    @Test("Count, announcement and Show all")
    func texts() {
        let labels = MonarchColumnChooserLabels()
        #expect(Tokens.countText(shown: 5, total: 8, labels: labels) == "5 of 8 shown")
        #expect(
            Tokens.movedAnnouncement("Status", to: 2, count: 8, labels: labels) == "Status moved to position 3 of 8."
        )
        #expect(Tokens.showsShowAll(shown: 3, total: 4))
        #expect(!Tokens.showsShowAll(shown: 4, total: 4))
        let custom = MonarchColumnChooserLabels(moved: { "\($0) → \($1)/\($2)" })
        #expect(Tokens.movedAnnouncement("Status", to: 0, count: 2, labels: custom) == "Status → 1/2")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchColumnChooserStrings.title == "Columns")
        #expect(MonarchColumnChooserStrings.search == "Find a column")
        #expect(MonarchColumnChooserStrings.noMatches == "No columns match.")
        #expect(MonarchColumnChooserStrings.required == "Always shown")
        #expect(MonarchColumnChooserStrings.showAll == "Show all")
        #expect(MonarchColumnChooserStrings.reset == "Reset")
        #expect(MonarchColumnChooserStrings.clearSearch == "Clear search")
        #expect(MonarchColumnChooserStrings.moveUp("Status") == "Move Status up")
        #expect(MonarchColumnChooserStrings.moveDown("Status") == "Move Status down")
    }
}
