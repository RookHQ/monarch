import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

@MainActor
private let sampleActions = [
    MonarchBulkAction(id: "archive", title: "Archive", systemImage: "archivebox") {},
    MonarchBulkAction(id: "tag", title: "Add tag", systemImage: "tag") {},
    MonarchBulkAction(id: "export", title: "Export") {},
    MonarchBulkAction(id: "delete", title: "Delete", systemImage: "trash", isDestructive: true) {},
]

enum MonarchBulkActionBarFixtures: ComponentFixtures {
    static let id = "bulk-action-bar"
    static let fixtures = [
        Fixture("count of a total, overflow menu, destructive action") {
            MonarchBulkActionBar(count: 3, totalCount: 120, actions: sampleActions, onClear: {})
        },
        Fixture("every action visible, no total") {
            MonarchBulkActionBar(count: 1, actions: sampleActions, maxVisibleActions: 4, onClear: {})
        },
        Fixture("one action running, one disabled, busy") {
            VStack {
                MonarchBulkActionBar(
                    count: 12,
                    actions: [
                        MonarchBulkAction(id: "archive", title: "Archive") {},
                        MonarchBulkAction(id: "merge", title: "Merge", isDisabled: true) {},
                    ],
                    busyActionID: "archive",
                    onClear: {}
                )
                MonarchBulkActionBar(count: 2, actions: sampleActions, isBusy: true, onClear: {})
            }
        },
        Fixture("accessories and custom labels") {
            MonarchBulkActionBar(
                count: 4,
                actions: [MonarchBulkAction(id: "assign", title: "Assign to someone else in the team") {}],
                labels: MonarchBulkActionBarLabels(
                    selected: { count, _ in "\(count) conversations chosen for the next step" },
                    clear: "Deselect every conversation"
                ),
                onClear: {},
                accessories: {
                    Picker("Status", selection: .constant("open")) {
                        Text("Open").tag("open")
                        Text("Closed").tag("closed")
                    }
                }
            )
        },
        Fixture("nothing selected takes no room") {
            MonarchBulkActionBar(count: 0, actions: sampleActions, onClear: {})
        },
    ]
}

@Suite("MonarchBulkActionBar")
@MainActor
struct MonarchBulkActionBarTests {
    private let labels = MonarchBulkActionBarLabels()

    @Test("The count reads with and without a total")
    func count() {
        #expect(MonarchBulkActionBarTokens.countText(count: 3, total: nil, labels: labels) == "3 selected")
        #expect(MonarchBulkActionBarTokens.countText(count: 3, total: 120, labels: labels) == "3 of 120 selected")
        let custom = MonarchBulkActionBarLabels(selected: { "\($0)/\($1 ?? 0)" })
        #expect(MonarchBulkActionBarTokens.countText(count: 3, total: 9, labels: custom) == "3/9")
    }

    @Test("The bar is open only while something is selected")
    func open() {
        #expect(!MonarchBulkActionBarTokens.isOpen(count: 0))
        #expect(MonarchBulkActionBarTokens.isOpen(count: 1))
    }

    @Test("Actions after the limit go into the More menu")
    func split() {
        let three = MonarchBulkActionBarTokens.split(sampleActions, maxVisible: 3)
        #expect(three.visible.map(\.id) == ["archive", "tag", "export"])
        #expect(three.overflow.map(\.id) == ["delete"])
        let all = MonarchBulkActionBarTokens.split(sampleActions, maxVisible: 10)
        #expect(all.visible.count == 4)
        #expect(all.overflow.isEmpty)
        let none = MonarchBulkActionBarTokens.split(sampleActions, maxVisible: -1)
        #expect(none.visible.isEmpty)
        #expect(none.overflow.count == 4)
    }

    @Test("A running action blocks the others but stays enabled itself")
    func busy() {
        let archive = sampleActions[0]
        let tag = sampleActions[1]
        #expect(!MonarchBulkActionBarTokens.isDisabled(archive, isBusy: false, busyActionID: nil))
        #expect(!MonarchBulkActionBarTokens.isDisabled(archive, isBusy: false, busyActionID: "archive"))
        #expect(MonarchBulkActionBarTokens.isDisabled(tag, isBusy: false, busyActionID: "archive"))
        #expect(MonarchBulkActionBarTokens.isDisabled(tag, isBusy: true, busyActionID: nil))
        let disabled = MonarchBulkAction(id: "x", title: "X", isDisabled: true) {}
        #expect(MonarchBulkActionBarTokens.isDisabled(disabled, isBusy: false, busyActionID: nil))
        #expect(MonarchBulkActionBarTokens.isBlocked(isBusy: false, busyActionID: "a"))
        #expect(!MonarchBulkActionBarTokens.isBlocked(isBusy: false, busyActionID: nil))
    }

    @Test("Destructive actions use the danger variant")
    func variants() {
        #expect(MonarchBulkActionBarTokens.variant(sampleActions[0]) == .secondary)
        #expect(MonarchBulkActionBarTokens.variant(sampleActions[3]) == .danger)
    }

    @Test("Count changes are announced, and clearing says so")
    func announcements() {
        #expect(MonarchBulkActionBarTokens.announcement(from: 0, to: 2, total: 4, labels: labels) == "2 of 4 selected")
        #expect(MonarchBulkActionBarTokens.announcement(from: 2, to: 3, total: nil, labels: labels) == "3 selected")
        #expect(
            MonarchBulkActionBarTokens.announcement(from: 3, to: 0, total: 4, labels: labels) == "Selection cleared"
        )
        #expect(MonarchBulkActionBarTokens.announcement(from: 3, to: 3, total: 4, labels: labels) == nil)
        let custom = MonarchBulkActionBarLabels(cleared: "Nothing selected")
        #expect(
            MonarchBulkActionBarTokens.announcement(from: 1, to: 0, total: nil, labels: custom) == "Nothing selected"
        )
    }

    @Test("Colours")
    func colours() {
        #expect(MonarchBulkActionBarTokens.background == \.bgSurface)
        #expect(MonarchBulkActionBarTokens.border == \.borderSubtle)
        #expect(MonarchBulkActionBarTokens.countColor == \.textDefault)
    }

    @Test("Nothing selected takes no room")
    func emptyLayout() {
        let closed = Host.layout(MonarchBulkActionBar(count: 0, actions: sampleActions, onClear: {}))
        let open = Host.layout(MonarchBulkActionBar(count: 2, actions: sampleActions, onClear: {}))
        #expect(closed.height == 0)
        #expect(open.height > 0)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchBulkActionBarStrings.selected(2, total: nil) == "2 selected")
        #expect(MonarchBulkActionBarStrings.selected(2, total: 5) == "2 of 5 selected")
        #expect(MonarchBulkActionBarStrings.cleared == "Selection cleared")
        #expect(MonarchBulkActionBarStrings.clear == "Clear selection")
        #expect(MonarchBulkActionBarStrings.more == "More")
        #expect(MonarchBulkActionBarStrings.bar == "Bulk actions")
    }
}
