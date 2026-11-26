import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let tabsFixtureItems = [
    MonarchTabsItem("Open", value: "open", count: "12"),
    MonarchTabsItem("Waiting", value: "waiting", count: "3 new", countTone: .warning),
    MonarchTabsItem("Closed", value: "closed"),
    MonarchTabsItem("Archived", value: "archived", isDisabled: true),
]

enum MonarchTabsFixtures: ComponentFixtures {
    static let id = "tabs"
    static let fixtures = [
        Fixture("line") {
            MonarchTabs("Tickets", selection: .constant("open"), items: tabsFixtureItems)
        },
        Fixture("pill") {
            MonarchTabs("Tickets", selection: .constant("waiting"), items: tabsFixtureItems, variant: .pill)
        },
        Fixture("too many tabs for the width, both variants") {
            VStack {
                ForEach(MonarchTabsVariant.allCases, id: \.self) { variant in
                    MonarchTabs(
                        "Months",
                        selection: .constant(3),
                        items: (1...12).map { MonarchTabsItem("Month \($0)", value: $0) },
                        variant: variant
                    )
                }
            }
        },
        Fixture("count tones") {
            MonarchTabs(
                "Tones",
                selection: .constant(MonarchTone.info),
                items: MonarchTone.allCases.map { tone in
                    MonarchTabsItem(String(describing: tone), value: tone, count: "4", countTone: tone)
                }
            )
        },
        Fixture("disabled strip, no selection match, one tab, none") {
            VStack {
                MonarchTabs("Tickets", selection: .constant("open"), items: tabsFixtureItems).disabled(true)
                MonarchTabs("Tickets", selection: .constant("missing"), items: tabsFixtureItems, variant: .pill)
                MonarchTabs("One", selection: .constant(1), items: [MonarchTabsItem("Only tab", value: 1, count: "")])
                MonarchTabs("None", selection: .constant(1), items: [])
                    .frame(minHeight: MonarchTokens.Space.space2)
            }
        },
        Fixture("long labels") {
            MonarchTabs(
                "Sections",
                selection: .constant(2),
                items: [
                    MonarchTabsItem("Overview of the organisation", value: 1),
                    MonarchTabsItem("Members and their permissions", value: 2, count: "128"),
                    MonarchTabsItem("Billing", value: 3),
                ]
            )
        },
    ]
}

@Suite("MonarchTabs")
struct MonarchTabsTests {
    @Test("The selected tab has the default text colour, the others the muted one")
    func labelColor() {
        #expect(MonarchTabsTokens.labelColor(isSelected: true, isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchTabsTokens.labelColor(isSelected: false, isEnabled: true) == \MonarchColorTokens.textMuted)
    }

    @Test("A disabled tab uses the disabled text role, selected or not", arguments: [true, false])
    func disabledColor(isSelected: Bool) {
        #expect(
            MonarchTabsTokens.labelColor(isSelected: isSelected, isEnabled: false) == \MonarchColorTokens.textDisabled
        )
    }

    @Test("The selection is marked by a shape as well as by colour")
    func selectionMarks() {
        #expect(MonarchTabsTokens.indicatorColor == \MonarchColorTokens.accentStrong)
        #expect(MonarchTabsTokens.indicatorHeight > MonarchTabsTokens.borderWidth)
        #expect(MonarchTabsTokens.pillAppearance.background == \MonarchColorTokens.bgSurface)
        #expect(MonarchTabsTokens.pillAppearance.border != nil)
        #expect(MonarchTabsTokens.trackColor != MonarchTabsTokens.pillAppearance.background)
    }

    @Test("A count is shown unless it is missing or empty")
    func showsCount() {
        #expect(MonarchTabsTokens.showsCount("12"))
        #expect(MonarchTabsTokens.showsCount("0"))
        #expect(!MonarchTabsTokens.showsCount(""))
        #expect(!MonarchTabsTokens.showsCount(nil))
    }

    @Test("The count is read after the label")
    func accessibilityLabel() {
        #expect(MonarchTabsTokens.accessibilityLabel(title: "Open", count: "12") == "Open, 12")
        #expect(MonarchTabsTokens.accessibilityLabel(title: "Open", count: "12 new") == "Open, 12 new")
        #expect(MonarchTabsTokens.accessibilityLabel(title: "Closed", count: nil) == "Closed")
        #expect(MonarchTabsTokens.accessibilityLabel(title: "Closed", count: "") == "Closed")
    }

    @Test("A pill is lower than a line tab, and both come from the control tokens")
    func sizes() {
        #expect(MonarchTabsTokens.minHeight(.line) == 36)
        #expect(MonarchTabsTokens.minHeight(.pill) == 30)
        #expect(MonarchTabsTokens.spacing(.pill) < MonarchTabsTokens.spacing(.line))
    }

    @Test("An item is identified by its value and keeps what it is given")
    func item() {
        let item = MonarchTabsItem("Open", value: 7, count: "2", countTone: .danger, isDisabled: true)
        #expect(item.id == 7)
        #expect(item.title == "Open")
        #expect(item.count == "2")
        #expect(item.countTone == .danger)
        #expect(item.isDisabled)

        let plain = MonarchTabsItem("Closed", value: 8)
        #expect(plain.count == nil)
        #expect(plain.countTone == .neutral)
        #expect(!plain.isDisabled)
    }

    #if os(iOS)
        @Test("Both variants keep a 44-point touch target", arguments: MonarchTabsVariant.allCases)
        @MainActor
        func hitTarget(variant: MonarchTabsVariant) {
            let tabs = MonarchTabs("Tickets", selection: .constant("open"), items: tabsFixtureItems, variant: variant)
            #expect(Host.layout(tabs).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
