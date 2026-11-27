import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

@MainActor
private func tableToolbarViewPicker() -> some View {
    Picker("View", selection: .constant("table")) {
        Text("Table").tag("table")
        Text("Board").tag("board")
    }
    .pickerStyle(.segmented)
    .fixedSize()
}

@MainActor
private func tableToolbarColumnMenu() -> some View {
    Menu {
        Toggle("Email", isOn: .constant(true))
        Toggle("Plan", isOn: .constant(false))
    } label: {
        Label("Columns", systemImage: "tablecells")
    }
    .menuStyle(.button)
    .buttonStyle(.monarch(.secondary, size: .sm))
}

enum MonarchTableToolbarFixtures: ComponentFixtures {
    static let id = "table-toolbar"
    static let fixtures = [
        Fixture("filters, count, view switcher and columns") {
            MonarchTableToolbar(
                resultCount: 1204,
                filters: {
                    MonarchTag("Status: Open")
                    MonarchTag("Owner: Me")
                },
                controls: {
                    tableToolbarViewPicker()
                    tableToolbarColumnMenu()
                }
            )
        },
        Fixture("rows selected: bulk bar replaces the filters") {
            MonarchTableToolbar(
                resultCount: 12,
                selectedCount: 3,
                totalCount: 12,
                bulkActions: [
                    MonarchBulkAction(id: "archive", title: "Archive") {},
                    MonarchBulkAction(id: "delete", title: "Delete", isDestructive: true) {},
                ],
                onClearSelection: {},
                filters: { MonarchTag("Status: Open") },
                controls: { tableToolbarColumnMenu() }
            )
        },
        Fixture("count only, one result") {
            MonarchTableToolbar(resultCount: 1) { EmptyView() }
        },
        Fixture("actions only, custom labels") {
            MonarchTableToolbar(
                resultCount: 0,
                labels: MonarchTableToolbarLabels(toolbar: "Invoice controls", results: { "\($0) invoices found" })
            ) {
                MonarchButton("New invoice", systemImage: "plus", variant: .primary, size: .sm) {}
            }
        },
        Fixture("selection without a clear action keeps the filters") {
            MonarchTableToolbar(
                selectedCount: 2,
                filters: { MonarchTag("A filter with a long name that keeps going") },
                controls: { tableToolbarViewPicker() }
            )
        },
    ]
}

@Suite("MonarchTableToolbar")
@MainActor
struct MonarchTableToolbarTests {
    private let labels = MonarchTableToolbarLabels()

    @Test("The result count is singular for one and formatted")
    func results() {
        #expect(MonarchTableToolbarTokens.resultsText(1, labels: labels) == "1 result")
        #expect(MonarchTableToolbarTokens.resultsText(0, labels: labels) == "0 results")
        #expect(MonarchTableToolbarTokens.resultsText(1204, labels: labels) == "\(1204.formatted()) results")
        let custom = MonarchTableToolbarLabels(results: { "\($0) found" })
        #expect(MonarchTableToolbarTokens.resultsText(3, labels: custom) == "3 found")
    }

    @Test("The bulk bar shows only with a selection and a clear action")
    func selecting() {
        #expect(MonarchTableToolbarTokens.isSelecting(selectedCount: 2, canClear: true))
        #expect(!MonarchTableToolbarTokens.isSelecting(selectedCount: 0, canClear: true))
        #expect(!MonarchTableToolbarTokens.isSelecting(selectedCount: 2, canClear: false))
    }

    @Test("A changed result count is announced")
    func announcement() {
        #expect(MonarchTableToolbarTokens.announcement(from: 10, to: 3, labels: labels) == "3 results")
        #expect(MonarchTableToolbarTokens.announcement(from: nil, to: 1, labels: labels) == "1 result")
        #expect(MonarchTableToolbarTokens.announcement(from: 3, to: 3, labels: labels) == nil)
        #expect(MonarchTableToolbarTokens.announcement(from: 3, to: nil, labels: labels) == nil)
    }

    @Test("The count uses the muted text colour")
    func colours() {
        #expect(MonarchTableToolbarTokens.countColor == \MonarchColorTokens.textMuted)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchTableToolbarStrings.toolbar == "Table controls")
        #expect(!MonarchTableToolbarStrings.results(2).hasPrefix("monarch."))
    }
}
