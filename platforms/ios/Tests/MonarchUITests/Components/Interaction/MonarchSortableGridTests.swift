import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct GridFixtureItem: Identifiable {
    let id: Int
    let title: String
    var isLocked = false
}

private let gridFixtureItems = (1...7).map { GridFixtureItem(id: $0, title: "Widget \($0)", isLocked: $0 == 3) }

/// A plain tile for the fixtures.
private struct GridFixtureTile: View {
    let title: String
    let state: MonarchSortableGridItemState

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(state.isDisabled ? "\(title) (locked)" : title)
            .monarchTextStyle(.body)
            .padding(theme.spacing.space4)
            .frame(maxWidth: .infinity, minHeight: theme.spacing.space20, alignment: .topLeading)
            .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
    }
}

enum MonarchSortableGridFixtures: ComponentFixtures {
    static let id = "sortable-grid"
    static let fixtures = [
        Fixture("adaptive columns, one locked item") {
            MonarchSortableGrid(
                .constant(gridFixtureItems),
                itemLabel: \.title,
                isItemDisabled: { $0.isLocked },
                content: { item, state in GridFixtureTile(title: item.title, state: state) }
            )
        },
        Fixture("three fixed columns") {
            MonarchSortableGrid(.constant(gridFixtureItems), itemLabel: \.title, columns: 3) { item, state in
                GridFixtureTile(title: item.title, state: state)
            }
        },
        Fixture("one column, long names") {
            MonarchSortableGrid(
                .constant([
                    GridFixtureItem(id: 1, title: "A widget with a name long enough to wrap onto several lines"),
                    GridFixtureItem(id: 2, title: "Short"),
                ]),
                itemLabel: \.title,
                columns: 1
            ) { item, state in
                GridFixtureTile(title: item.title, state: state)
            }
        },
        Fixture("disabled and empty") {
            VStack {
                MonarchSortableGrid(.constant(gridFixtureItems), itemLabel: \.title) { item, state in
                    GridFixtureTile(title: item.title, state: state)
                }
                .disabled(true)
                MonarchSortableGrid(.constant([GridFixtureItem]()), itemLabel: \.title) { item, state in
                    GridFixtureTile(title: item.title, state: state)
                }
            }
        },
    ]
}

@Suite("MonarchSortableGrid")
struct MonarchSortableGridTests {
    typealias Tokens = MonarchSortableGridTokens

    @Test("A fixed column count wins and is at least one")
    func fixedColumns() {
        #expect(Tokens.columnCount(width: 390, columns: 3, minimumItemWidth: 160, spacing: 12) == 3)
        #expect(Tokens.columnCount(width: 390, columns: 0, minimumItemWidth: 160, spacing: 12) == 1)
    }

    @Test("Adaptive columns fit as many minimum-width tiles as the width allows")
    func adaptiveColumns() {
        #expect(Tokens.columnCount(width: 366, columns: nil, minimumItemWidth: 160, spacing: 12) == 2)
        #expect(Tokens.columnCount(width: 331, columns: nil, minimumItemWidth: 160, spacing: 12) == 1)
        #expect(Tokens.columnCount(width: 332, columns: nil, minimumItemWidth: 160, spacing: 12) == 2)
        #expect(Tokens.columnCount(width: 1000, columns: nil, minimumItemWidth: 160, spacing: 12) == 5)
        // Larger text scales the minimum width up, so fewer columns fit.
        #expect(Tokens.columnCount(width: 366, columns: nil, minimumItemWidth: 320, spacing: 12) == 1)
        #expect(Tokens.columnCount(width: 0, columns: nil, minimumItemWidth: 160, spacing: 12) == 1)
    }

    @Test("A move removes at the old index and inserts at the new one")
    func move() {
        let list = ["a", "b", "c", "d"]
        #expect(Tokens.move(list, from: 0, to: 2) == ["b", "c", "a", "d"])
        #expect(Tokens.move(list, from: 3, to: 0) == ["d", "a", "b", "c"])
        #expect(Tokens.move(list, from: 1, to: 1) == list)
        #expect(Tokens.move(list, from: 9, to: 0) == list)
        #expect(Tokens.move(list, from: 0, to: -1) == list)
    }

    @Test("Actions move one place in reading order or one row, and stop at the edges")
    func targets() {
        // 7 items in 3 columns: rows 0-2, 3-5, 6.
        #expect(Tokens.target(from: 4, direction: .backward, columns: 3, count: 7) == 3)
        #expect(Tokens.target(from: 4, direction: .forward, columns: 3, count: 7) == 5)
        #expect(Tokens.target(from: 4, direction: .up, columns: 3, count: 7) == 1)
        #expect(Tokens.target(from: 4, direction: .down, columns: 3, count: 7) == nil)
        #expect(Tokens.target(from: 3, direction: .down, columns: 3, count: 7) == 6)
        #expect(Tokens.target(from: 0, direction: .backward, columns: 3, count: 7) == nil)
        #expect(Tokens.target(from: 6, direction: .forward, columns: 3, count: 7) == nil)
        #expect(Tokens.target(from: 1, direction: .up, columns: 3, count: 7) == nil)
    }

    @Test("One column offers up and down; more columns offer all four directions")
    func axis() {
        #expect(Tokens.axis(columns: 1) == .vertical)
        #expect(Tokens.axis(columns: 2) == .both)
        #expect(Tokens.target(from: 1, direction: .up, columns: 1, count: 3) == 0)
    }

    @Test("Payloads round-trip only within the same grid and range")
    func payloads() {
        let payload = Tokens.payload(namespace: "grid-a", index: 4)
        #expect(Tokens.index(payload: payload, namespace: "grid-a", count: 7) == 4)
        #expect(Tokens.index(payload: payload, namespace: "grid-b", count: 7) == nil)
        #expect(Tokens.index(payload: payload, namespace: "grid-a", count: 4) == nil)
        #expect(Tokens.index(payload: "grid-a#x", namespace: "grid-a", count: 7) == nil)
        #expect(Tokens.index(payload: "Some dragged text", namespace: "grid-a", count: 7) == nil)
    }

    @Test("Built-in labels and announcements resolve, counting positions from 1")
    func builtInStrings() {
        let labels = MonarchSortableGridLabels()
        #expect(Tokens.handleLabel("Sales", labels: labels) == "Reorder Sales")
        #expect(Tokens.movedAnnouncement("Sales", to: 2, count: 8, labels: labels) == "Sales moved to position 3 of 8.")
        #expect(
            Tokens.droppedAnnouncement("Sales", to: 0, count: 8, labels: labels) == "Sales dropped at position 1 of 8."
        )
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let labels = MonarchSortableGridLabels(
            handle: { "Verschieben: \($0)" },
            moved: { "\($0) → \($1)/\($2)" },
            dropped: { "\($0) ↓ \($1)/\($2)" }
        )
        #expect(Tokens.handleLabel("Sales", labels: labels) == "Verschieben: Sales")
        #expect(Tokens.movedAnnouncement("Sales", to: 2, count: 8, labels: labels) == "Sales → 3/8")
        #expect(Tokens.droppedAnnouncement("Sales", to: 2, count: 8, labels: labels) == "Sales ↓ 3/8")
    }

    @Test("The drop target outline uses the strong accent, thicker than a hairline")
    func dropTarget() {
        #expect(Tokens.dropTargetRole == \MonarchColorTokens.accentStrong)
        #expect(Tokens.dropTargetWidth > MonarchShapes.borderWidth)
    }
}
