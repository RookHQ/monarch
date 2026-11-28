import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEmptyStateFixtures: ComponentFixtures {
    static let id = "empty-state"
    static let fixtures = [
        Fixture("first run with actions and tip") {
            MonarchEmptyState(
                "No invoices yet",
                message: "Invoices you send appear here, with their status and the date they were paid.",
                systemImage: "doc.text",
                tip: "You can also import a CSV file.",
                actionTitle: "New invoice",
                secondaryActionTitle: "Import",
                action: {},
                secondaryAction: {}
            )
        },
        Fixture("filtered with clear filters") {
            MonarchEmptyState(
                "No results",
                message: "No tickets match the filters.",
                kind: .filtered,
                clearFiltersLabel: "Reset filters",
                onClearFilters: {}
            )
        },
        Fixture("success, small, title only") {
            MonarchEmptyState("All caught up", kind: .success, size: .sm)
        },
        Fixture("every size") {
            VStack {
                ForEach(MonarchEmptyStateSize.allCases, id: \.self) { size in
                    MonarchEmptyState(
                        "Nothing here",
                        message: "A message.",
                        size: size,
                        actionTitle: "Create",
                        action: {}
                    )
                }
            }
        },
    ]
}

@Suite("MonarchEmptyState")
struct MonarchEmptyStateTests {
    @Test("Each kind has its own default icon, and the app's icon wins")
    func symbol() {
        let symbols = MonarchEmptyStateKind.allCases.map { MonarchEmptyStateTokens.symbol(kind: $0, override: nil) }
        #expect(Set(symbols).count == MonarchEmptyStateKind.allCases.count)
        #expect(MonarchEmptyStateTokens.symbol(kind: .firstRun, override: "doc.text") == "doc.text")
        #expect(MonarchEmptyStateTokens.symbol(kind: .firstRun, override: "") == "tray")
    }

    @Test("The icon is the accent, and green only for the success kind")
    func iconRole() {
        #expect(MonarchEmptyStateTokens.iconRole(kind: .firstRun) == \MonarchColorTokens.accentStrong)
        #expect(MonarchEmptyStateTokens.iconRole(kind: .filtered) == \MonarchColorTokens.accentStrong)
        #expect(MonarchEmptyStateTokens.iconRole(kind: .success) == \MonarchColorTokens.successStrong)
    }

    @Test("Clear filters shows only for a filtered view with a handler")
    func clearFilters() {
        #expect(MonarchEmptyStateTokens.showsClearFilters(kind: .filtered, hasHandler: true))
        #expect(!MonarchEmptyStateTokens.showsClearFilters(kind: .filtered, hasHandler: false))
        #expect(!MonarchEmptyStateTokens.showsClearFilters(kind: .firstRun, hasHandler: true))
        #expect(!MonarchEmptyStateTokens.showsClearFilters(kind: .success, hasHandler: true))
        #expect(MonarchEmptyStateStrings.clearFilters == "Clear filters")
    }

    @Test("A button needs both a title and an action")
    func showsAction() {
        #expect(MonarchEmptyStateTokens.showsAction(title: "Create", hasHandler: true))
        #expect(!MonarchEmptyStateTokens.showsAction(title: "Create", hasHandler: false))
        #expect(!MonarchEmptyStateTokens.showsAction(title: nil, hasHandler: true))
    }

    @Test("Sizes grow from sm to lg, and only lg fills the height")
    func sizes() {
        #expect(MonarchEmptyStateTokens.iconSize(.sm) < MonarchEmptyStateTokens.iconSize(.md))
        #expect(MonarchEmptyStateTokens.iconSize(.md) < MonarchEmptyStateTokens.iconSize(.lg))
        #expect(MonarchEmptyStateTokens.titleStyle(.sm) == .bodyStrong)
        // The default size follows the web: a 16 point semibold title over a 14 point message, a 20 point
        // glyph on a 40 point tile.
        #expect(MonarchEmptyStateTokens.titleStyle(.md) == .bodyStrong)
        #expect(MonarchEmptyStateTokens.messageStyle(.md) == .label)
        #expect(MonarchEmptyStateTokens.iconSize(.md) == 20)
        #expect(MonarchEmptyStateTokens.tileSize(.md) == 40)
        #expect(MonarchEmptyStateTokens.tileRole(kind: .firstRun) == \MonarchColorTokens.accentSubtle)
        #expect(MonarchEmptyStateTokens.tileRole(kind: .success) == \MonarchColorTokens.successSubtle)
        #expect(MonarchEmptyStateTokens.titleStyle(.lg) == .heading)
        #expect(MonarchEmptyStateTokens.buttonSize(.sm) == .sm)
        #expect(MonarchEmptyStateTokens.buttonSize(.lg) == .lg)
        #expect(!MonarchEmptyStateTokens.fillsHeight(.sm))
        #expect(!MonarchEmptyStateTokens.fillsHeight(.md))
        #expect(MonarchEmptyStateTokens.fillsHeight(.lg))
    }

    @Test("An action button makes the state taller")
    @MainActor
    func actionsAddHeight() {
        let plain = Host.layout(MonarchEmptyState("Nothing here"))
        let withAction = Host.layout(MonarchEmptyState("Nothing here", actionTitle: "Create", action: {}))
        let titleOnly = Host.layout(MonarchEmptyState("Nothing here", actionTitle: "Create"))
        #expect(withAction.height > plain.height)
        #expect(titleOnly.height == plain.height)
    }
}
