import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchWidgetFixtures: ComponentFixtures {
    static let id = "widget"
    static let fixtures = [
        Fixture("ready with every header part, both variants") {
            VStack {
                ForEach(MonarchWidgetVariant.allCases, id: \.self) { variant in
                    MonarchWidget(
                        "Team load",
                        description: "Open tickets per agent",
                        period: "Last 30 days",
                        systemImage: "person.3",
                        variant: variant,
                        updatedAt: Date(timeIntervalSince1970: 1_800_000_000).addingTimeInterval(-300),
                        footer: "Excludes archived tickets",
                        onViewAll: {},
                        actions: { MonarchButton("Export", size: .sm) {} },
                        menu: {
                            Button("Refresh", systemImage: "arrow.clockwise") {}
                            Divider()
                            Button("Remove from dashboard", systemImage: "trash", role: .destructive) {}
                        },
                        content: { MonarchMetric("Open tickets", value: 128) }
                    )
                }
            }
        },
        Fixture("loading") {
            MonarchWidget(
                "Approvals",
                period: "This week",
                state: .loading,
                updatedAt: Date(timeIntervalSince1970: 1_800_000_000)
            ) {
                Text("Hidden while loading")
            }
        },
        Fixture("empty, built-in and custom") {
            VStack {
                MonarchWidget("Mentions", state: .empty()) { EmptyView() }
                MonarchWidget("Mentions", state: .empty("No mentions this week")) { EmptyView() }
            }
        },
        Fixture("failed with and without retry, retrying, a menu-only header") {
            VStack {
                MonarchWidget("Revenue", state: .failed(), onRetry: {}) { EmptyView() }
                MonarchWidget("Revenue", state: .failed("The billing service is down."), isRetrying: true, onRetry: {})
                {
                    EmptyView()
                }
                MonarchWidget("Revenue", menu: { Button("Refresh") {} }, content: { EmptyView() })
                MonarchWidget("Revenue", state: .failed()) { EmptyView() }
            }
        },
        Fixture("long title and labels") {
            MonarchWidget(
                "Median first response time across every support queue in the workspace",
                period: "Since the start of the financial year",
                labels: MonarchWidgetLabels(viewAll: "Open the full report"),
                onViewAll: {},
                menu: { Button("Refresh") {} },
                content: { Text("Content").monarchTextStyle(.body) }
            )
        },
    ]
}

@Suite("MonarchWidget")
struct MonarchWidgetTests {
    private let labels = MonarchWidgetLabels()

    @Test("A failure uses its message, then the label, then the built-in text")
    func errorMessage() {
        #expect(MonarchWidgetTokens.errorMessage(.failed(), labels: labels) == "This section could not be loaded.")
        #expect(MonarchWidgetTokens.errorMessage(.failed("Down"), labels: labels) == "Down")
        #expect(MonarchWidgetTokens.errorMessage(.failed(), labels: MonarchWidgetLabels(error: "Oops")) == "Oops")
        #expect(MonarchWidgetTokens.errorMessage(.ready, labels: labels) == nil)
        #expect(MonarchWidgetTokens.errorMessage(.loading, labels: labels) == nil)
    }

    @Test("Empty uses its title, then the label, then the built-in text")
    func emptyTitle() {
        #expect(MonarchWidgetTokens.emptyTitle(.empty(), labels: labels) == "Nothing to show yet")
        #expect(MonarchWidgetTokens.emptyTitle(.empty("None"), labels: labels) == "None")
        #expect(MonarchWidgetTokens.emptyTitle(.empty(), labels: MonarchWidgetLabels(empty: "Rien")) == "Rien")
        #expect(MonarchWidgetTokens.emptyTitle(.failed(), labels: labels) == nil)
    }

    @Test("Spoken names include the title")
    func spokenNames() {
        #expect(MonarchWidgetTokens.menuLabel(title: "Team load", labels: labels) == "More actions for Team load")
        #expect(MonarchWidgetTokens.viewAllLabel(title: "Team load", labels: labels) == "View all: Team load")
        #expect(MonarchWidgetTokens.viewAllTitle(labels: labels) == "View all")
        #expect(MonarchWidgetTokens.loadingLabel(title: "Team load", labels: labels) == "Loading Team load")
        let custom = MonarchWidgetLabels(menu: { "Menu \($0)" }, loading: { "Chargement \($0)" })
        #expect(MonarchWidgetTokens.menuLabel(title: "A", labels: custom) == "Menu A")
        #expect(MonarchWidgetTokens.loadingLabel(title: "A", labels: custom) == "Chargement A")
    }

    @Test("The updated line is relative to now")
    func updated() {
        let now = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let english = Locale(identifier: "en_US")
        let text = MonarchWidgetTokens.updatedText(
            now.addingTimeInterval(-300),
            now: now,
            locale: english,
            labels: labels
        )
        #expect(text == "Updated 5 minutes ago")
        let custom = MonarchWidgetLabels(updated: { "Refreshed \($0)" })
        #expect(
            MonarchWidgetTokens.updatedText(now.addingTimeInterval(-300), now: now, locale: english, labels: custom)
                == "Refreshed 5 minutes ago"
        )
    }

    @Test("The footer is hidden while loading and when empty")
    func footer() {
        let updated = Date(timeIntervalSinceReferenceDate: 800_000_000)
        #expect(MonarchWidgetTokens.showsFooter(state: .ready, footer: "Note", updatedAt: nil))
        #expect(MonarchWidgetTokens.showsFooter(state: .failed(), footer: nil, updatedAt: updated))
        #expect(!MonarchWidgetTokens.showsFooter(state: .loading, footer: "Note", updatedAt: updated))
        #expect(!MonarchWidgetTokens.showsFooter(state: .ready, footer: nil, updatedAt: nil))
    }

    @Test("A new failure is announced once")
    func announcement() {
        #expect(
            MonarchWidgetTokens.announcement(from: .loading, to: .failed(), labels: labels)
                == "This section could not be loaded."
        )
        #expect(MonarchWidgetTokens.announcement(from: .failed(), to: .failed("Other"), labels: labels) == nil)
        #expect(MonarchWidgetTokens.announcement(from: .loading, to: .ready, labels: labels) == nil)
        #expect(MonarchWidgetTokens.announcement(from: .ready, to: .ready, labels: labels) == nil)
    }

    @Test("Colours and the placeholder lines")
    func tokens() {
        #expect(MonarchWidgetTokens.titleColor == \.textDefault)
        #expect(MonarchWidgetTokens.muted == \.textMuted)
        #expect(MonarchWidgetTokens.skeletonFill == \.bgMuted)
        #expect(MonarchWidgetTokens.skeletonWidths.count == 3)
        #expect(MonarchWidgetTokens.skeletonWidths.allSatisfy { $0 > 0 && $0 <= 1 })
        #expect(MonarchWidgetTokens.stacksActions(at: .accessibility1))
        #expect(!MonarchWidgetTokens.stacksActions(at: .large))
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchWidgetStrings.viewAll == "View all")
        #expect(MonarchWidgetStrings.error == "This section could not be loaded.")
        #expect(MonarchWidgetStrings.empty == "Nothing to show yet")
        #expect(MonarchWidgetStrings.updated("now") == "Updated now")
        #expect(MonarchWidgetStrings.menu("X") == "More actions for X")
        #expect(MonarchWidgetStrings.viewAllLabel("X") == "View all: X")
        #expect(MonarchWidgetStrings.loading("X") == "Loading X")
    }
}
