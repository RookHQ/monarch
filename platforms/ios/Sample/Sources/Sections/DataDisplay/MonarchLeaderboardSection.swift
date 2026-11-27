import MonarchUI
import SwiftUI

struct MonarchLeaderboardSection: CataloguePage {
    static let title = "Leaderboard"

    private enum Shown: String, CaseIterable {
        case ready, loading, empty, failed
    }

    @State private var shown: Shown = .ready
    @State private var highlightsTop = true
    @State private var showsAvatars = true
    @State private var opened = "None"

    private let agents = [
        MonarchLeaderboardEntry("Ana Lima", value: 42, change: 0.12, badge: "3 breached", badgeTone: .danger),
        MonarchLeaderboardEntry("Sam Ortiz", value: 42, secondary: "Tier 2 support", change: 0, isCurrent: true),
        MonarchLeaderboardEntry("Kai Chen", value: 31, change: -0.08),
        MonarchLeaderboardEntry("Noor Haddad", value: 17),
        MonarchLeaderboardEntry("Lee Park", value: 9),
    ]

    var body: some View {
        Picker("State", selection: $shown) {
            ForEach(Shown.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        Toggle("Highlight the top three", isOn: $highlightsTop).monarchTextStyle(.body)
        Toggle("Show avatars", isOn: $showsAvatars).monarchTextStyle(.body)
        CatalogueCaption("Tickets solved this month")
        MonarchLeaderboard(
            shown == .empty ? [] : agents,
            label: "Tickets solved this month",
            valueLabel: "tickets solved",
            sort: .descending,
            changeComparison: "vs last month",
            showsAvatars: showsAvatars,
            highlightsTop: highlightsTop,
            limit: 4,
            total: 12,
            isLoading: shown == .loading,
            errorMessage: shown == .failed ? "Couldn’t load the ranking." : nil,
            onShowMore: { opened = "Full report" },
            onSelect: { opened = $0.name },
            onRetry: { shown = .ready }
        )
        Text("Opened: \(opened)").monarchTextStyle(.caption)
        CatalogueCaption("Reply rate by campaign step, without avatars")
        MonarchLeaderboard(
            [
                MonarchLeaderboardEntry("Intro email", value: 0.12, secondary: "Spring launch"),
                MonarchLeaderboardEntry("Follow-up call", value: 0.31, secondary: "Spring launch"),
                MonarchLeaderboardEntry("Case study", value: 0.22, secondary: "Retail outreach"),
            ],
            label: "Reply rate by step",
            sort: .descending,
            format: .percent,
            showsAvatars: false
        )
    }
}
