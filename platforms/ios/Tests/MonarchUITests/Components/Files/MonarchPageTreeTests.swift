import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchPageTreeFixtures: ComponentFixtures {
    static let id = "page-tree"

    static let groups = [
        MonarchPageTreeGroup(
            id: "basics",
            label: "Basics",
            pages: [
                MonarchPageTreePage(id: "welcome", title: "Welcome"),
                MonarchPageTreePage(id: "setup", title: "Set up"),
                MonarchPageTreePage(id: "blank", title: "  "),
            ]
        ),
        MonarchPageTreeGroup(
            id: "guides",
            label: "Guides",
            pages: [MonarchPageTreePage(id: "billing", title: "Billing")]
        ),
        MonarchPageTreeGroup(id: "empty", label: "Reference", pages: []),
    ]

    static let fixtures = [
        Fixture("navigation, numbered, one open") {
            MonarchPageTree(groups, selection: .constant("setup"))
                .frame(minHeight: MonarchAccessibility.minimumHitTarget * 10)
        },
        Fixture("organiser with every handler") {
            MonarchPageTree(
                groups,
                selection: .constant(nil),
                onReorder: { _, _ in },
                onAddPage: { _ in },
                onAddGroup: { _ in },
                onRenameGroup: { _, _ in },
                onDeleteGroup: { _ in },
                onDeletePage: { _ in }
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 12)
        },
        Fixture("not numbered, custom labels") {
            MonarchPageTree(
                groups,
                selection: .constant("welcome"),
                isNumbered: false,
                labels: MonarchPageTreeLabels(untitled: "No title", noPages: "Nothing here")
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 10)
        },
        Fixture("long titles") {
            MonarchPageTree(
                [
                    MonarchPageTreeGroup(
                        id: "g",
                        label: "A group with a name long enough to need a second line on a phone",
                        pages: [
                            MonarchPageTreePage(
                                id: "p",
                                title: "A page whose title is long enough to wrap onto a second line in the list"
                            )
                        ]
                    )
                ],
                selection: .constant("p"),
                onAddPage: { _ in }
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 6)
        },
        Fixture("disabled") {
            MonarchPageTree(groups, selection: .constant("welcome"), onReorder: { _, _ in })
                .frame(minHeight: MonarchAccessibility.minimumHitTarget * 10)
                .disabled(true)
        },
    ]
}

@Suite("MonarchPageTree")
struct MonarchPageTreeTests {
    private typealias Tokens = MonarchPageTreeTokens
    private let groups = MonarchPageTreeFixtures.groups
    private let labels = MonarchPageTreeLabels()

    private func ids(_ groups: [MonarchPageTreeGroup]) -> [[String]] {
        groups.map { $0.pages.map(\.id) }
    }

    @Test("Page numbers run across groups and are padded to two digits")
    func numbers() {
        let numbers = Tokens.pageNumbers(groups)
        #expect(numbers["welcome"] == 1)
        #expect(numbers["billing"] == 4)
        #expect(Tokens.numberText(3, locale: Locale(identifier: "en_US")) == "03")
        #expect(Tokens.numberText(12, locale: Locale(identifier: "en_US")) == "12")
        #expect(Tokens.numberText(1_234, locale: Locale(identifier: "en_US")) == "1234")
    }

    @Test("A blank title is shown as Untitled page")
    func titles() {
        #expect(Tokens.title(groups[0].pages[2], labels: labels) == "Untitled page")
        #expect(Tokens.title(groups[0].pages[2], labels: MonarchPageTreeLabels(untitled: "No title")) == "No title")
        #expect(Tokens.title(groups[0].pages[0], labels: labels) == "Welcome")
    }

    @Test("The last page and the last group cannot be deleted")
    func deletion() {
        #expect(Tokens.canDeletePage(groups))
        #expect(!Tokens.canDeletePage([groups[1]]))
        #expect(Tokens.canDeleteGroup(groups))
        #expect(!Tokens.canDeleteGroup([groups[0]]))
        #expect(Tokens.moveTargets(groups, from: "basics").map(\.id) == ["guides", "empty"])
    }

    @Test("The group menu lists only what applies")
    func groupActions() {
        #expect(
            Tokens.groupActions(index: 0, groupCount: 3, canRename: true, canReorder: true, canDelete: true)
                == [.rename, .moveDown, .delete]
        )
        #expect(
            Tokens.groupActions(index: 1, groupCount: 3, canRename: false, canReorder: true, canDelete: false)
                == [.moveUp, .moveDown]
        )
        #expect(
            Tokens.groupActions(index: 0, groupCount: 1, canRename: false, canReorder: true, canDelete: true).isEmpty
        )
    }

    @Test("Dragging a page reorders its group and reports the move")
    func reordering() throws {
        let result = try #require(Tokens.reordering(groups, groupID: "basics", from: [0], to: 2))
        #expect(ids(result.groups)[0] == ["setup", "welcome", "blank"])
        #expect(
            result.move
                == MonarchPageTreeMove(
                    kind: .page,
                    id: "welcome",
                    fromGroupID: "basics",
                    toGroupID: "basics",
                    toIndex: 1
                )
        )
        #expect(Tokens.reordering(groups, groupID: "basics", from: [0], to: 0) == nil)
        #expect(Tokens.reordering(groups, groupID: "missing", from: [0], to: 1) == nil)
    }

    @Test("Moving a page one place up or down stops at the ends")
    func movingByOne() throws {
        let down = try #require(Tokens.movingPage(groups, pageID: "welcome", by: 1))
        #expect(ids(down.groups)[0] == ["setup", "welcome", "blank"])
        #expect(down.move.toIndex == 1)
        let up = try #require(Tokens.movingPage(groups, pageID: "blank", by: -1))
        #expect(ids(up.groups)[0] == ["welcome", "blank", "setup"])
        #expect(Tokens.movingPage(groups, pageID: "welcome", by: -1) == nil)
        #expect(Tokens.movingPage(groups, pageID: "billing", by: 1) == nil)
    }

    @Test("Moving a page to another group puts it at the end")
    func movingToGroup() throws {
        let result = try #require(Tokens.movingPage(groups, pageID: "setup", toGroup: "guides"))
        #expect(ids(result.groups) == [["welcome", "blank"], ["billing", "setup"], []])
        #expect(
            result.move
                == MonarchPageTreeMove(kind: .page, id: "setup", fromGroupID: "basics", toGroupID: "guides", toIndex: 1)
        )
        #expect(Tokens.movingPage(groups, pageID: "setup", toGroup: "basics") == nil)
    }

    @Test("Groups move up and down")
    func movingGroups() throws {
        let result = try #require(Tokens.movingGroup(groups, groupID: "guides", by: -1))
        #expect(result.groups.map(\.id) == ["guides", "basics", "empty"])
        #expect(result.move == MonarchPageTreeMove(kind: .group, id: "guides", toIndex: 0))
        #expect(Tokens.movingGroup(groups, groupID: "empty", by: 1) == nil)
    }

    @Test("Every move is announced with its new place")
    func announcements() throws {
        let group = try #require(Tokens.movingGroup(groups, groupID: "guides", by: 1))
        #expect(
            Tokens.announcement(for: group.move, in: group.groups, labels: labels) == "Guides moved to position 3 of 3."
        )
        let page = try #require(Tokens.movingPage(groups, pageID: "welcome", by: 1))
        #expect(
            Tokens.announcement(for: page.move, in: page.groups, labels: labels) == "Welcome moved to position 2 of 3."
        )
        let across = try #require(Tokens.movingPage(groups, pageID: "blank", toGroup: "empty"))
        #expect(
            Tokens.announcement(for: across.move, in: across.groups, labels: labels)
                == "Untitled page moved to Reference."
        )
    }

    @Test("Names are trimmed and blank names refused")
    func names() {
        #expect(Tokens.cleanedName("  Guides ") == "Guides")
        #expect(Tokens.cleanedName("   ") == nil)
    }

    @Test("A group with pages asks for its name before it is deleted")
    func deleteConfirmation() {
        #expect(Tokens.needsPhrase(groups[0]))
        #expect(!Tokens.canDelete(groups[0], typed: "basics"))
        #expect(Tokens.canDelete(groups[0], typed: " Basics "))
        #expect(Tokens.canDelete(groups[2], typed: ""))
        #expect(
            Tokens.deleteGroupMessage(groups[0], labels: labels)
                == "This cannot be undone. Its 3 pages are deleted too. Type “Basics” to confirm."
        )
        #expect(Tokens.deleteGroupMessage(groups[2], labels: labels) == "This cannot be undone.")
        #expect(MonarchPageTreeDialog.deleteGroup(groups[0]).hasTextField)
        #expect(!MonarchPageTreeDialog.deleteGroup(groups[2]).hasTextField)
        #expect(MonarchPageTreeDialog.addGroup.hasTextField)
        #expect(!MonarchPageTreeDialog.deletePage(groups[0].pages[0]).hasTextField)
    }

    @Test("Colours dim when disabled")
    func colors() {
        #expect(Tokens.numberColor(isEnabled: true) == \MonarchColorTokens.textSubtle)
        #expect(Tokens.titleColor(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.titleColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.mutedColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchPageTreeStrings.label == "Pages")
        #expect(MonarchPageTreeStrings.addPage("Basics") == "Add page to Basics")
        #expect(MonarchPageTreeStrings.groupActions("Basics") == "Actions for Basics")
        #expect(MonarchPageTreeStrings.deleteGroupConsequence(1) == "Its page is deleted too.")
        #expect(MonarchPageTreeStrings.deletePageTitle("Welcome") == "Delete “Welcome”?")
        #expect(MonarchPageTreeStrings.movedTo("Welcome", "Guides") == "Welcome moved to Guides.")
        for value in [
            MonarchPageTreeStrings.untitled, MonarchPageTreeStrings.noPages, MonarchPageTreeStrings.addGroup,
            MonarchPageTreeStrings.renameGroup, MonarchPageTreeStrings.moveGroupUp,
            MonarchPageTreeStrings.moveGroupDown,
            MonarchPageTreeStrings.deleteGroup, MonarchPageTreeStrings.moveTo, MonarchPageTreeStrings.movePageUp,
            MonarchPageTreeStrings.movePageDown, MonarchPageTreeStrings.deletePage,
            MonarchPageTreeStrings.addGroupTitle,
            MonarchPageTreeStrings.groupName, MonarchPageTreeStrings.newGroupName, MonarchPageTreeStrings.renameSubmit,
            MonarchPageTreeStrings.cancel, MonarchPageTreeStrings.cannotUndo,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
