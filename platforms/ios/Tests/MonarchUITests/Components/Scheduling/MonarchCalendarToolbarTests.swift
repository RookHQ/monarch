import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCalendarToolbarFixtures: ComponentFixtures {
    static let id = "calendar-toolbar"
    static let fixtures = [
        Fixture("every view") {
            VStack {
                ForEach(MonarchCalendarToolbarView.allCases, id: \.self) { view in
                    MonarchCalendarToolbar(
                        "November 2026",
                        view: .constant(view),
                        onToday: {},
                        onPrevious: {},
                        onNext: {}
                    )
                }
            }
        },
        Fixture("busy, today and arrows disabled, with an action") {
            MonarchCalendarToolbar(
                "2 – 8 November 2026",
                view: .constant(.week),
                isBusy: true,
                isTodayDisabled: true,
                isPreviousDisabled: true,
                isNextDisabled: true,
                onToday: {},
                onPrevious: {},
                onNext: {},
                actions: { MonarchButton("New event", systemImage: "plus", variant: .primary, size: .sm) {} }
            )
        },
        Fixture("one view, no switcher, custom labels") {
            MonarchCalendarToolbar(
                "Wednesday 4 November",
                view: .constant(.day),
                views: [.day],
                labels: MonarchCalendarToolbarLabels(today: "Now", previous: { "Back one \($0)" }),
                onToday: {},
                onPrevious: {},
                onNext: {}
            )
        },
        Fixture("disabled") {
            MonarchCalendarToolbar("November 2026", view: .constant(.month), onToday: {}, onPrevious: {}, onNext: {})
                .disabled(true)
        },
    ]
}

@Suite("MonarchCalendarToolbar")
struct MonarchCalendarToolbarTests {
    private let builtIn = MonarchCalendarToolbarLabels()

    @Test("The arrows are named for the view's unit")
    func arrowLabels() {
        #expect(MonarchCalendarToolbarTokens.previousLabel(.day, labels: builtIn) == "Previous day")
        #expect(MonarchCalendarToolbarTokens.previousLabel(.week, labels: builtIn) == "Previous week")
        #expect(MonarchCalendarToolbarTokens.nextLabel(.month, labels: builtIn) == "Next month")
        #expect(MonarchCalendarToolbarTokens.nextLabel(.agenda, labels: builtIn) == "Next period")
    }

    @Test("Views have built-in names")
    func viewNames() {
        let names = MonarchCalendarToolbarView.allCases.map {
            MonarchCalendarToolbarTokens.viewName($0, labels: builtIn)
        }
        #expect(names == ["Day", "Week", "Month", "Agenda"])
    }

    @Test("Every string can be replaced")
    func replaced() {
        let labels = MonarchCalendarToolbarLabels(
            today: "Now",
            previous: { "Back one \($0)" },
            next: { "On one \($0)" },
            view: "Layout",
            viewName: { $0 == .week ? "7 days" : "Other" },
            unit: { _ in "step" },
            loading: "Fetching"
        )
        #expect(MonarchCalendarToolbarTokens.today(labels) == "Now")
        #expect(MonarchCalendarToolbarTokens.previousLabel(.week, labels: labels) == "Back one step")
        #expect(MonarchCalendarToolbarTokens.nextLabel(.week, labels: labels) == "On one step")
        #expect(MonarchCalendarToolbarTokens.switcherLabel(labels) == "Layout")
        #expect(MonarchCalendarToolbarTokens.viewName(.week, labels: labels) == "7 days")
        #expect(MonarchCalendarToolbarTokens.loading(labels) == "Fetching")
    }

    @Test("Built-in strings come from the table")
    func strings() {
        #expect(MonarchCalendarToolbarTokens.today(builtIn) == "Today")
        #expect(MonarchCalendarToolbarTokens.switcherLabel(builtIn) == "Calendar view")
        #expect(MonarchCalendarToolbarTokens.loading(builtIn) == "Loading")
    }

    @Test("The switcher is shown only when there is a choice")
    func switcher() {
        #expect(MonarchCalendarToolbarTokens.showsSwitcher(MonarchCalendarToolbarView.allCases))
        #expect(MonarchCalendarToolbarTokens.showsSwitcher([.week, .month]))
        #expect(!MonarchCalendarToolbarTokens.showsSwitcher([.week]))
        #expect(!MonarchCalendarToolbarTokens.showsSwitcher([]))
    }

    @Test("The arrows use directional symbols that follow the reading direction")
    func symbols() {
        #expect(MonarchCalendarToolbarTokens.previousSymbol == "chevron.backward")
        #expect(MonarchCalendarToolbarTokens.nextSymbol == "chevron.forward")
        #expect(MonarchCalendarToolbarTokens.titleColor == \MonarchColorTokens.textDefault)
    }

    #if os(iOS)
        @Test("The toolbar keeps 44-point touch targets")
        @MainActor
        func hitTarget() {
            let toolbar = MonarchCalendarToolbar(
                "Nov",
                view: .constant(.month),
                onToday: {},
                onPrevious: {},
                onNext: {}
            )
            #expect(Host.layout(toolbar).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
