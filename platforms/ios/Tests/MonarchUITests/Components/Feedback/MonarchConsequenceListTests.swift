import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchConsequenceListFixtures: ComponentFixtures {
    static let id = "consequence-list"
    private static let items = [
        MonarchConsequence(.kept, "Invoices stay in Billing"),
        MonarchConsequence(.removed, "tasks will be deleted", detail: "Including 3 that are overdue", count: 1200),
        MonarchConsequence(.changed, "Members lose access immediately"),
        MonarchConsequence(.warning, "This cannot be undone, and the workspace name becomes available to others"),
        MonarchConsequence(.removed, "files will be deleted", count: 0),
    ]
    static let fixtures = [
        Fixture("ungrouped with heading") {
            MonarchConsequenceList(items, heading: "What happens")
        },
        Fixture("grouped") {
            MonarchConsequenceList(items, isGrouped: true)
        },
        Fixture("grouped, custom strings") {
            MonarchConsequenceList(
                items,
                heading: "Deleting the workspace",
                isGrouped: true,
                kindLabel: { _ in "Effect" },
                groupTitle: { kind in "Group \(kind)" }
            )
        },
        Fixture("loading") {
            MonarchConsequenceList(
                items,
                heading: "What happens",
                isLoading: true,
                loadingRowCount: 2,
                loadingLabel: "Working"
            )
        },
        Fixture("empty with heading") {
            MonarchConsequenceList([], heading: "Nothing changes")
        },
    ]
}

@Suite("MonarchConsequenceList")
struct MonarchConsequenceListTests {
    private let english = Locale(identifier: "en_US")
    private let german = Locale(identifier: "de_DE")

    @Test("Every kind has its own icon and its own colour")
    func kinds() {
        let symbols = MonarchConsequenceKind.allCases.map(MonarchConsequenceListTokens.symbol)
        #expect(Set(symbols).count == MonarchConsequenceKind.allCases.count)
        #expect(MonarchConsequenceListTokens.iconRole(.removed) == \MonarchColorTokens.dangerStrong)
        #expect(MonarchConsequenceListTokens.iconRole(.changed) == \MonarchColorTokens.textMuted)
        #expect(MonarchConsequenceListTokens.iconRole(.warning) == \MonarchColorTokens.warningStrong)
        #expect(MonarchConsequenceListTokens.iconRole(.kept) == \MonarchColorTokens.successStrong)
    }

    @Test("Groups are in the fixed order, keep item order and leave out empty kinds")
    func groups() {
        let items = [
            MonarchConsequence(.kept, "a"),
            MonarchConsequence(.removed, "b"),
            MonarchConsequence(.kept, "c"),
            MonarchConsequence(.removed, "d"),
        ]
        let groups = MonarchConsequenceListTokens.groups(items)
        #expect(groups.map(\.kind) == [.removed, .kept])
        #expect(groups[0].items.map(\.label) == ["b", "d"])
        #expect(groups[1].items.map(\.label) == ["a", "c"])
        #expect(MonarchConsequenceListTokens.groups([]).isEmpty)
        #expect(MonarchConsequenceListTokens.groupOrder.count == MonarchConsequenceKind.allCases.count)
    }

    @Test("The count is formatted for the locale and comes before the label")
    func title() {
        let item = MonarchConsequence(.removed, "tasks will be deleted", count: 1200)
        #expect(MonarchConsequenceListTokens.title(item, locale: english) == "1,200 tasks will be deleted")
        #expect(MonarchConsequenceListTokens.title(item, locale: german) == "1.200 tasks will be deleted")
        let plain = MonarchConsequence(.kept, "Invoices stay")
        #expect(MonarchConsequenceListTokens.title(plain, locale: english) == "Invoices stay")
        #expect(
            MonarchConsequenceListTokens.title(MonarchConsequence(.removed, "files", count: 0), locale: english)
                == "0 files"
        )
    }

    @Test("VoiceOver reads the kind first unless a group heading says it, then the detail")
    func accessibilityLabel() {
        let item = MonarchConsequence(.removed, "tasks will be deleted", detail: "Including 3 overdue", count: 12)
        #expect(
            MonarchConsequenceListTokens.accessibilityLabel(item, kindName: "Removed", locale: english)
                == "Removed: 12 tasks will be deleted, Including 3 overdue"
        )
        #expect(
            MonarchConsequenceListTokens.accessibilityLabel(item, kindName: nil, locale: english)
                == "12 tasks will be deleted, Including 3 overdue"
        )
        #expect(
            MonarchConsequenceListTokens.accessibilityLabel(
                MonarchConsequence(.kept, "Invoices stay", detail: ""),
                kindName: "Kept",
                locale: english
            ) == "Kept: Invoices stay"
        )
    }

    @Test("The built-in strings resolve for every kind", arguments: MonarchConsequenceKind.allCases)
    func strings(kind: MonarchConsequenceKind) {
        #expect(!MonarchConsequenceListStrings.group(kind).hasPrefix("monarch."))
        #expect(!MonarchConsequenceListStrings.kind(kind).hasPrefix("monarch."))
        #expect(MonarchConsequenceListStrings.group(kind) != MonarchConsequenceListStrings.kind(kind))
    }

    @Test("The loading text and the group headings are the documented ones")
    func defaultStrings() {
        #expect(MonarchConsequenceListStrings.loading == "Checking what will change")
        #expect(MonarchConsequenceListStrings.group(.removed) == "Will be deleted")
        #expect(MonarchConsequenceListStrings.kind(.removed) == "Removed")
    }

    @Test("At least one placeholder row is shown while loading")
    func loadingRows() {
        #expect(MonarchConsequenceListTokens.loadingRows(3) == 3)
        #expect(MonarchConsequenceListTokens.loadingRows(0) == 1)
        #expect(MonarchConsequenceListTokens.loadingRows(-2) == 1)
    }

    @Test("The default id is made of the kind and the label, and an explicit id wins")
    func identifiers() {
        #expect(MonarchConsequence(.kept, "a").id != MonarchConsequence(.removed, "a").id)
        #expect(MonarchConsequence(.kept, "a").id == MonarchConsequence(.kept, "a", count: 2).id)
        #expect(MonarchConsequence(.kept, "a", id: "row-1").id == "row-1")
    }
}
