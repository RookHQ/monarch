import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct DashboardGridWidget: Identifiable {
    let id: String
    var span: MonarchDashboardGridSpan = .one
    var title: String { id }
}

private let dashboardWidgets = [
    DashboardGridWidget(id: "Team load", span: .two),
    DashboardGridWidget(id: "Response time"),
    DashboardGridWidget(id: "Backlog"),
    DashboardGridWidget(id: "Satisfaction", span: .full),
]

@MainActor private func dashboardTile(_ widget: DashboardGridWidget) -> some View {
    MonarchWidget(widget.title, period: "Last 30 days") {
        MonarchMetric("Open", value: 42)
    }
}

enum MonarchDashboardGridFixtures: ComponentFixtures {
    static let id = "dashboard-grid"
    static let fixtures = [
        Fixture("phone width, one column") {
            MonarchDashboardGrid(
                dashboardWidgets,
                layout: .constant([]),
                itemLabel: \.title,
                span: { $0.span },
                content: { dashboardTile($0) }
            )
        },
        Fixture("editing, stored layout with an unknown id") {
            MonarchDashboardGrid(
                dashboardWidgets,
                layout: .constant(["Backlog", "Removed widget", "Team load"]),
                itemLabel: \.title,
                isEditing: true
            ) {
                dashboardTile($0)
            }
        },
        Fixture("wide grid with spans, editing") {
            MonarchDashboardGrid(
                dashboardWidgets,
                layout: .constant([]),
                itemLabel: \.title,
                span: { $0.span },
                columns: 4,
                isEditing: true,
                content: { dashboardTile($0) }
            )
            .frame(width: MonarchTokens.Breakpoint.lg)
        },
        Fixture("no widgets") {
            MonarchDashboardGrid([DashboardGridWidget](), layout: .constant([]), itemLabel: \.title) {
                dashboardTile($0)
            }
        },
    ]
}

@Suite("MonarchDashboardGrid")
struct MonarchDashboardGridTests {
    private typealias Tokens = MonarchDashboardGridTokens

    @Test("A stored layout keeps known ids once, then appends new items")
    func resolve() {
        #expect(Tokens.resolve(ids: ["a", "b", "c"], layout: []) == ["a", "b", "c"])
        #expect(Tokens.resolve(ids: ["a", "b", "c"], layout: ["c", "x", "a", "c"]) == ["c", "a", "b"])
        #expect(Tokens.resolve(ids: [String](), layout: ["x"]).isEmpty)
    }

    @Test("Columns: one on phones and at accessibility sizes, two at medium widths, then the setting")
    func columnCount() {
        #expect(Tokens.columnCount(width: 390, columns: 3, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(width: 0, columns: 3, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(width: 700, columns: 3, isAccessibilitySize: false) == 2)
        #expect(Tokens.columnCount(width: 700, columns: 1, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(width: 1100, columns: 3, isAccessibilitySize: false) == 3)
        #expect(Tokens.columnCount(width: 1100, columns: 9, isAccessibilitySize: false) == 4)
        #expect(Tokens.columnCount(width: 1100, columns: 0, isAccessibilitySize: false) == 1)
        #expect(Tokens.columnCount(width: 1100, columns: 3, isAccessibilitySize: true) == 1)
    }

    @Test("Spans clamp to the columns available")
    func spans() {
        #expect(Tokens.span(.one, columns: 3) == 1)
        #expect(Tokens.span(.two, columns: 1) == 1)
        #expect(Tokens.span(.columns(5), columns: 3) == 3)
        #expect(Tokens.span(.columns(0), columns: 3) == 1)
        #expect(Tokens.span(.full, columns: 4) == 4)
    }

    @Test("Widgets pack into rows in order; one that does not fit starts the next row")
    func rows() {
        #expect(Tokens.rows(spans: [2, 1, 1, 3], columns: 3) == [[0, 1], [2], [3]])
        #expect(Tokens.rows(spans: [1, 1, 1], columns: 1) == [[0], [1], [2]])
        #expect(Tokens.rows(spans: [1, 1, 1, 1], columns: 2) == [[0, 1], [2, 3]])
        #expect(Tokens.rows(spans: [], columns: 3).isEmpty)
    }

    @Test("Cell widths share the grid width and include the gaps they span")
    func widths() {
        #expect(Tokens.width(span: 1, columns: 3, total: 320, spacing: 10) == 100)
        #expect(Tokens.width(span: 2, columns: 3, total: 320, spacing: 10) == 210)
        #expect(Tokens.width(span: 3, columns: 3, total: 320, spacing: 10) == 320)
        #expect(Tokens.width(span: 1, columns: 1, total: 0, spacing: 10) == 0)
    }

    @Test("Moves stop at the ends; the buttons point up and down in one column")
    func moves() {
        #expect(Tokens.target(from: 0, step: -1, count: 3) == nil)
        #expect(Tokens.target(from: 0, step: 1, count: 3) == 1)
        #expect(Tokens.target(from: 2, step: 1, count: 3) == nil)
        #expect(Tokens.moveSymbols(columns: 1).earlier == "chevron.up")
        #expect(Tokens.moveSymbols(columns: 1).later == "chevron.down")
        #expect(Tokens.moveSymbols(columns: 3).earlier == "chevron.backward")
        #expect(Tokens.moveSymbols(columns: 3).later == "chevron.forward")
    }

    @Test("Labels and announcements")
    func labels() {
        let labels = MonarchDashboardGridLabels()
        #expect(Tokens.moveEarlierLabel("Team load", labels: labels) == "Move Team load earlier")
        #expect(Tokens.moveLaterLabel("Team load", labels: labels) == "Move Team load later")
        #expect(
            Tokens.movedAnnouncement("Team load", to: 1, count: 6, labels: labels)
                == "Team load moved to position 2 of 6."
        )
        #expect(
            Tokens.droppedAnnouncement("Team load", to: 0, count: 6, labels: labels)
                == "Team load dropped at position 1 of 6."
        )
        let custom = MonarchDashboardGridLabels(moveEarlier: { "← \($0)" }, moved: { "\($0) \($1)/\($2)" })
        #expect(Tokens.moveEarlierLabel("A", labels: custom) == "← A")
        #expect(Tokens.movedAnnouncement("A", to: 0, count: 2, labels: custom) == "A 1/2")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchDashboardGridStrings.label == "Dashboard")
        #expect(MonarchDashboardGridStrings.dragHint == "Touch and hold, then drag to move.")
    }
}
