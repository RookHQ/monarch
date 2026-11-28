import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let savedViewsFixture = [
    MonarchSavedView("All deals", id: "all", isReadOnly: true, group: "Built-in"),
    MonarchSavedView("Open deals", id: "open", count: 12, isDefault: true),
    MonarchSavedView("Team pipeline", id: "team", count: 1_200, isShared: true, group: "Shared with me"),
    MonarchSavedView("Closed this month", id: "closed", count: 0),
]

enum MonarchSavedViewsFixtures: ComponentFixtures {
    static let id = "saved-views"
    static let fixtures = [
        Fixture("a view selected, every action") {
            MonarchSavedViews(
                savedViewsFixture,
                selection: .constant("open"),
                onSaveNew: { _ in },
                onSaveChanges: { _ in },
                onDiscardChanges: {},
                onRename: { _, _ in },
                onDelete: { _ in },
                onSetDefault: { _ in }
            )
        },
        Fixture("edited, every size") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MonarchSavedViews(savedViewsFixture, selection: .constant("team"), isModified: true, size: size)
                }
            }
        },
        Fixture("no view selected, no views") {
            VStack(alignment: .leading) {
                MonarchSavedViews(savedViewsFixture, selection: .constant(nil))
                MonarchSavedViews([], selection: .constant(nil), labels: MonarchSavedViewsLabels(noView: "Everything"))
            }
        },
        Fixture("long name, disabled") {
            VStack(alignment: .leading) {
                MonarchSavedViews(
                    [MonarchSavedView("Deals over fifty thousand in the northern region this quarter", id: "long")],
                    selection: .constant("long"),
                    isModified: true
                )
                MonarchSavedViews(savedViewsFixture, selection: .constant("open")).disabled(true)
            }
        },
    ]
}

@Suite("MonarchSavedViews")
struct MonarchSavedViewsTests {
    private let text = MonarchSavedViewsText(labels: MonarchSavedViewsLabels())
    private let every = Set(MonarchSavedViewsAction.allCases)
    private let editable = MonarchSavedView("Open deals", id: "open")
    private let readOnly = MonarchSavedView("All deals", id: "all", isReadOnly: true)

    @Test("A modified, editable view offers every action, in order")
    func allActions() {
        #expect(
            MonarchSavedViewsTokens.actions(selected: editable, isModified: true, available: every)
                == [.saveChanges, .saveAsNew, .discardChanges, .rename, .setDefault, .delete]
        )
    }

    @Test("Saving changes and discarding need unsaved changes")
    func unmodified() {
        #expect(
            MonarchSavedViewsTokens.actions(selected: editable, isModified: false, available: every)
                == [.saveAsNew, .rename, .setDefault, .delete]
        )
    }

    @Test("A read-only view is never saved over, renamed or deleted")
    func readOnlyView() {
        #expect(
            MonarchSavedViewsTokens.actions(selected: readOnly, isModified: true, available: every)
                == [.saveAsNew, .discardChanges, .setDefault]
        )
    }

    @Test("The default view is not offered as default again; nothing selected leaves save as new")
    func defaultsAndNone() {
        let defaultView = MonarchSavedView("Open", id: "o", isDefault: true)
        #expect(
            !MonarchSavedViewsTokens.actions(selected: defaultView, isModified: false, available: every).contains(
                .setDefault
            )
        )
        #expect(
            MonarchSavedViewsTokens.actions(selected: nil, isModified: true, available: every) == [
                .saveAsNew, .discardChanges,
            ]
        )
    }

    @Test("Only actions with a callback are offered")
    func available() {
        #expect(
            MonarchSavedViewsTokens.actions(selected: editable, isModified: true, available: [.rename]) == [.rename]
        )
        #expect(MonarchSavedViewsTokens.actions(selected: editable, isModified: true, available: []).isEmpty)
    }

    @Test("Views are grouped in order of first appearance and keep their order")
    func groups() {
        let groups = MonarchSavedViewsTokens.groups(savedViewsFixture)
        #expect(groups.map(\.title) == ["Built-in", nil, "Shared with me"])
        #expect(groups[1].views.map(\.id) == ["open", "closed"])
        #expect(MonarchSavedViewsTokens.groups([]).isEmpty)
    }

    @Test("A view's second line has its count, Shared and Default view")
    func subtitle() {
        let english = Locale(identifier: "en_US")
        #expect(
            MonarchSavedViewsTokens.subtitle(savedViewsFixture[1], text: text, locale: english) == "12 · Default view"
        )
        #expect(MonarchSavedViewsTokens.subtitle(savedViewsFixture[2], text: text, locale: english) == "1,200 · Shared")
        #expect(MonarchSavedViewsTokens.subtitle(savedViewsFixture[3], text: text, locale: english) == "0")
        #expect(MonarchSavedViewsTokens.subtitle(savedViewsFixture[0], text: text, locale: english) == nil)
    }

    @Test("A name must not be empty or taken, ignoring case and spaces")
    func problem() {
        let views = savedViewsFixture
        #expect(
            MonarchSavedViewsTokens.problem(name: "  ", views: views, excluding: nil, text: text) == "Enter a name."
        )
        #expect(
            MonarchSavedViewsTokens.problem(name: " open DEALS ", views: views, excluding: nil, text: text)
                == "A view with this name already exists."
        )
        #expect(MonarchSavedViewsTokens.problem(name: "Open deals", views: views, excluding: "open", text: text) == nil)
        #expect(MonarchSavedViewsTokens.problem(name: "Won deals", views: views, excluding: nil, text: text) == nil)
    }

    @Test("Names are cut to the longest length allowed")
    func limit() {
        #expect(MonarchSavedViewsTokens.limit("abcdef", to: 3) == "abc")
        #expect(MonarchSavedViewsTokens.limit("abc", to: 80) == "abc")
        #expect(MonarchSavedViewsTokens.limit("abc", to: 0).isEmpty)
    }

    @Test("The button reads the view's name, then Edited")
    func accessibility() {
        #expect(
            MonarchSavedViewsTokens.accessibilityValue(selected: editable, isModified: true, text: text)
                == "Open deals, Edited"
        )
        #expect(
            MonarchSavedViewsTokens.accessibilityValue(selected: editable, isModified: false, text: text)
                == "Open deals"
        )
        #expect(MonarchSavedViewsTokens.accessibilityValue(selected: nil, isModified: false, text: text) == "All items")
        #expect(MonarchSavedViewsTokens.modifiedTone == .warning)
    }

    @Test("Built-in strings come from the table")
    func builtIn() {
        #expect(text.view == "View")
        #expect(text.saveAsNew == "Save as new view…")
        #expect(text.saveChanges("Open deals") == "Save changes to “Open deals”")
        #expect(text.deleteTitle("Open deals") == "Delete “Open deals”?")
        #expect(text.deleteConfirm == "Delete view")
        let keys = [
            text.noView, text.modified, text.shared, text.isDefault, text.views, text.discardChanges, text.rename,
            text.setDefault, text.delete, text.saveTitle, text.renameTitle, text.name, text.save, text.cancel,
            text.nameRequired, text.nameTaken, text.deleteMessage,
        ]
        #expect(keys.allSatisfy { !$0.hasPrefix("monarch.") })
    }

    @Test("Every string can be replaced")
    func replaced() {
        let custom = MonarchSavedViewsText(
            labels: MonarchSavedViewsLabels(
                view: "Saved filter",
                saveChanges: { "Update \($0)" },
                deleteTitle: { "Remove \($0)?" },
                deleteConfirm: "Remove"
            )
        )
        #expect(custom.view == "Saved filter")
        #expect(custom.saveChanges("A") == "Update A")
        #expect(custom.deleteTitle("A") == "Remove A?")
        #expect(custom.deleteConfirm == "Remove")
    }

    #if os(iOS)
        @Test("The menu button keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let menu = MonarchSavedViews(savedViewsFixture, selection: .constant("open"), size: .sm).fixedSize()
            #expect(Host.layout(menu).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
