import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchAppAccessPickerFixtures: ComponentFixtures {
    static let id = "app-access-picker"
    static let apps = [
        MonarchAppAccessOption(id: "mail", name: "Mail", description: "Email and calendars for the team."),
        MonarchAppAccessOption(id: "docs", name: "Docs", badge: "Beta"),
        MonarchAppAccessOption(
            id: "reports",
            name: "Quarterly finance and compliance reports",
            description: "Read-only dashboards for the finance team and the auditors who review them.",
            logoURL: URL(filePath: "/missing.png"),
            badge: "Business plan"
        ),
        MonarchAppAccessOption(id: "home", name: "Home", description: "Everyone has it.", isDisabled: true),
    ]

    static let fixtures = [
        Fixture("some selected, helper") {
            MonarchAppAccessPicker(apps, selection: .constant(["mail", "home"]), helper: "Applies at next sign-in.")
        },
        Fixture("all selected, custom label") {
            MonarchAppAccessPicker(apps, selection: .constant(["mail", "docs", "reports", "home"]), label: "Apps")
        },
        Fixture("no bulk actions, hidden label") {
            MonarchAppAccessPicker(apps, selection: .constant([]), isLabelHidden: true, showsBulkActions: false)
        },
        Fixture("loading") {
            MonarchAppAccessPicker(apps, selection: .constant([]), isLoading: true)
        },
        Fixture("empty") {
            MonarchAppAccessPicker([], selection: .constant([]))
        },
        Fixture("disabled while saving") {
            MonarchAppAccessPicker(apps, selection: .constant(["docs"])).disabled(true)
        },
        Fixture("summary with more") {
            MonarchAppAccessSummary(apps, granted: ["mail", "docs", "reports", "home"])
        },
        Fixture("summary with one name, and none") {
            VStack(alignment: .leading) {
                MonarchAppAccessSummary(apps, granted: ["mail"], maxVisible: 0)
                MonarchAppAccessSummary(apps, granted: [])
            }
        },
    ]
}

@Suite("MonarchAppAccessPicker")
struct MonarchAppAccessPickerTests {
    private let apps = MonarchAppAccessPickerFixtures.apps
    private let labels = MonarchAppAccessPickerLabels()

    @Test("Loading and the number of apps decide what is shown")
    func content() {
        #expect(MonarchAppAccessPickerTokens.content(isLoading: true, count: 3) == .loading)
        #expect(MonarchAppAccessPickerTokens.content(isLoading: false, count: 0) == .empty)
        #expect(MonarchAppAccessPickerTokens.content(isLoading: false, count: 3) == .rows)
    }

    @Test("A toggle adds or removes one app")
    func setting() {
        #expect(MonarchAppAccessPickerTokens.setting("docs", isOn: true, in: ["mail"]) == ["mail", "docs"])
        #expect(MonarchAppAccessPickerTokens.setting("mail", isOn: false, in: ["mail"]).isEmpty)
        #expect(MonarchAppAccessPickerTokens.setting("mail", isOn: true, in: ["mail"]) == ["mail"])
    }

    @Test("Select all and Clear all change only the apps that are not disabled")
    func bulk() {
        #expect(MonarchAppAccessPickerTokens.changeableIDs(apps) == ["mail", "docs", "reports"])
        #expect(!MonarchAppAccessPickerTokens.isAllSelected(apps, selection: ["mail", "home"]))
        let all = MonarchAppAccessPickerTokens.toggledAll(apps, selection: ["mail", "home"])
        #expect(all == ["mail", "docs", "reports", "home"])
        #expect(MonarchAppAccessPickerTokens.isAllSelected(apps, selection: all))
        #expect(MonarchAppAccessPickerTokens.toggledAll(apps, selection: all) == ["home"])
        #expect(MonarchAppAccessPickerTokens.toggledAll(apps, selection: ["mail", "docs", "reports"]) == [])
        let fixed = [MonarchAppAccessOption(id: "home", name: "Home", isDisabled: true)]
        #expect(!MonarchAppAccessPickerTokens.isAllSelected(fixed, selection: ["home"]))
    }

    @Test("The count covers listed apps only")
    func count() {
        #expect(MonarchAppAccessPickerTokens.selectedCount(apps, selection: ["mail", "home", "gone"]) == 2)
        #expect(MonarchAppAccessPickerTokens.countText(selected: 2, total: 4, labels: labels) == "2 of 4 selected")
        let custom = MonarchAppAccessPickerLabels(count: { "\($0)/\($1)" })
        #expect(MonarchAppAccessPickerTokens.countText(selected: 2, total: 4, labels: custom) == "2/4")
    }

    @Test("A selected row uses the accent colours; others have no border")
    func colours() {
        #expect(MonarchAppAccessPickerTokens.rowFill(isSelected: true) == \MonarchColorTokens.bgMuted)
        #expect(MonarchAppAccessPickerTokens.rowFill(isSelected: false) == \MonarchColorTokens.bgSubtle)
        #expect(MonarchAppAccessPickerTokens.rowBorder(isSelected: true) == \MonarchColorTokens.accentBorder)
        #expect(MonarchAppAccessPickerTokens.rowBorder(isSelected: false) == nil)
        #expect(MonarchAppAccessPickerTokens.descriptionColor(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchAppAccessPickerTokens.descriptionColor(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("The summary sorts names, shows at least one and keeps unknown ids")
    func summary() {
        let summary = MonarchAppAccessPickerTokens.summary(apps, granted: ["mail", "docs", "gone"], maxVisible: 2)
        #expect(summary.shown == ["Docs", "gone"])
        #expect(summary.hidden == ["Mail"])
        let one = MonarchAppAccessPickerTokens.summary(apps, granted: ["mail", "docs"], maxVisible: 0)
        #expect(one.shown == ["Docs"])
        #expect(one.hidden == ["Mail"])
        #expect(MonarchAppAccessPickerTokens.summary(apps, granted: [], maxVisible: 2).shown.isEmpty)
    }

    @Test("VoiceOver reads every name of the summary, or No app access")
    func summaryLabel() {
        #expect(
            MonarchAppAccessPickerTokens.summaryLabel(
                shown: ["Docs", "Mail"],
                hidden: ["Notes", "Sheets"],
                labels: labels
            )
                == "Docs, Mail, and 2 more: Notes, Sheets"
        )
        #expect(MonarchAppAccessPickerTokens.summaryLabel(shown: ["Docs"], hidden: [], labels: labels) == "Docs")
        #expect(MonarchAppAccessPickerTokens.summaryLabel(shown: [], hidden: [], labels: labels) == "No app access")
        let custom = MonarchAppAccessPickerLabels(none: "Nothing", more: { "+\($0): \($1)" })
        #expect(MonarchAppAccessPickerTokens.summaryLabel(shown: [], hidden: [], labels: custom) == "Nothing")
        #expect(MonarchAppAccessPickerTokens.summaryLabel(shown: ["A"], hidden: ["B"], labels: custom) == "A, +1: B")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let plain = [
            MonarchAppAccessPickerStrings.label, MonarchAppAccessPickerStrings.selectAll,
            MonarchAppAccessPickerStrings.clearAll, MonarchAppAccessPickerStrings.loading,
            MonarchAppAccessPickerStrings.empty, MonarchAppAccessPickerStrings.none,
        ]
        #expect(
            plain == [
                "App access", "Select all", "Clear all", "Loading apps", "No apps are available yet.", "No app access",
            ]
        )
        #expect(MonarchAppAccessPickerStrings.moreCount(3) == "+3")
        #expect(MonarchAppAccessPickerStrings.count(selected: 1, total: 2) == "1 of 2 selected")
    }
}
