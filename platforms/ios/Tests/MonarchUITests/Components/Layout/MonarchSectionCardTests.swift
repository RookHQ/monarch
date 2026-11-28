import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchSectionCardFixtures: ComponentFixtures {
    static let id = "section-card"
    static let fixtures = [
        Fixture("ready, with actions and footer") {
            MonarchSectionCard("Invoices", description: "Sent in the last 30 days", meta: "12 invoices") {
                Text("Invoice 1042 · Due in 3 days").monarchTextStyle(.body)
                Text("Invoice 1043 · Paid").monarchTextStyle(.body)
            } actions: {
                MonarchButton("New", size: .sm) {}
            } footer: {
                Button("View all") {}.buttonStyle(.monarchLink(.standalone))
            }
        },
        Fixture("loading, busy, flush") {
            VStack {
                MonarchSectionCard("Team", state: .loading) { EmptyView() }
                MonarchSectionCard("Members", systemImage: "person.2", isBusy: true, isFlush: true) {
                    Text("Ana Flores").monarchTextStyle(.body)
                }
            }
        },
        Fixture("empty and error") {
            VStack {
                MonarchSectionCard("Webhooks", state: .empty, emptyMessage: "Add a webhook to get events.") {
                    EmptyView()
                }
                MonarchSectionCard(
                    "Usage",
                    state: .error,
                    errorMessage: "Check your connection and try again.",
                    onRetry: {},
                    content: { EmptyView() }
                )
                MonarchSectionCard("Usage", state: .error, isRetrying: true, onRetry: {}, content: { EmptyView() })
            }
        },
        Fixture("every tone, collapsible open and closed") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchSectionCard("Zone", tone: tone, isCollapsible: true) {
                        Text("Content").monarchTextStyle(.body)
                    }
                }
                MonarchSectionCard("Closed section", isCollapsible: true, isInitiallyExpanded: false) {
                    Text("Hidden").monarchTextStyle(.body)
                }
            }
        },
        Fixture("long title with actions") {
            MonarchSectionCard(
                "Recent activity across every workspace you belong to, including archived ones",
                meta: "Updated 5 min ago",
                labels: MonarchSectionCardLabels(loading: "Refreshing")
            ) {
                Text("Nothing new.").monarchTextStyle(.body)
            } actions: {
                MonarchButton("Refresh", variant: .ghost, size: .sm) {}
            }
        },
    ]
}

@Suite("MonarchSectionCard")
struct MonarchSectionCardTests {
    typealias Tokens = MonarchSectionCardTokens

    @Test("The neutral tone uses the card's border and default text; other tones take their roles")
    func tones() {
        #expect(Tokens.border(.neutral) == MonarchCardTokens.border)
        #expect(Tokens.border(.danger) == \.dangerBorder)
        #expect(Tokens.border(.warning) == \.warningBorder)
        #expect(Tokens.titleColor(.neutral) == \.textDefault)
        #expect(Tokens.titleColor(.danger) == \.dangerStrong)
    }

    @Test("A tone other than neutral has an icon, so it does not depend on colour")
    func symbols() {
        #expect(Tokens.symbol(.neutral, override: nil) == nil)
        #expect(Tokens.symbol(.neutral, override: "person") == "person")
        // Like the web card, a tone shows in the border and the title colour, with no icon of its own.
        for tone in MonarchTone.allCases {
            #expect(Tokens.symbol(tone, override: nil) == nil)
        }
        #expect(Tokens.symbol(.danger, override: "trash") == "trash")
    }

    @Test("A closed collapsible card hides its body and footer")
    func body() {
        #expect(Tokens.showsBody(isCollapsible: false, isExpanded: false))
        #expect(Tokens.showsBody(isCollapsible: true, isExpanded: true))
        #expect(!Tokens.showsBody(isCollapsible: true, isExpanded: false))
        #expect(Tokens.bodyPadding(isFlush: true) == 0)
        #expect(Tokens.bodyPadding(isFlush: false) == MonarchCardTokens.padding)
    }

    @Test("The title's value says whether it is open and whether it is loading")
    func titleValue() {
        func value(collapsible: Bool, expanded: Bool, busy: Bool) -> String {
            Tokens.titleValue(
                isCollapsible: collapsible,
                isExpanded: expanded,
                isBusy: busy,
                expanded: MonarchSectionCardStrings.expanded,
                collapsed: MonarchSectionCardStrings.collapsed,
                loading: MonarchSectionCardStrings.loading
            )
        }
        #expect(value(collapsible: false, expanded: true, busy: false) == "")
        #expect(value(collapsible: false, expanded: true, busy: true) == "Loading")
        #expect(value(collapsible: true, expanded: true, busy: false) == "Expanded")
        #expect(value(collapsible: true, expanded: false, busy: true) == "Collapsed, Loading")
        #expect(Tokens.isBusy(false, state: .loading))
        #expect(Tokens.isBusy(true, state: .ready))
        #expect(!Tokens.isBusy(false, state: .error))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchSectionCardStrings.loading == "Loading")
        #expect(MonarchSectionCardStrings.emptyTitle == "Nothing here yet")
        #expect(MonarchSectionCardStrings.errorTitle == "This section didn’t load")
        #expect(MonarchSectionCardStrings.expanded == "Expanded")
        #expect(MonarchSectionCardStrings.collapsed == "Collapsed")
    }
}
