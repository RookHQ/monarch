import MonarchUI
import SwiftUI

/// Recipe: the home of a portfolio product (assets, contracts, intellectual
/// property): composition, what needs attention, what comes up, a breakdown,
/// the largest items and recent activity.
struct DashboardPortfolioSection: CataloguePage {
    static let title = "Dashboard: portfolio (recipe)"

    /// One section of the home screen.
    struct Tile: Identifiable {
        let id: String
        let title: String
        var isWide = false
    }

    @Environment(\.monarchTheme) private var theme
    @State private var layout: [String] = []
    @State private var isEditing = false
    @State private var opened = "Nothing opened"

    private let tiles = [
        Tile(id: "composition", title: "Composition"),
        Tile(id: "attention", title: "Needs attention"),
        Tile(id: "upcoming", title: "Coming up"),
        Tile(id: "breakdown", title: "Breakdown"),
        Tile(id: "ranking", title: "Top items"),
        Tile(id: "activity", title: "Recent activity", isWide: true),
    ]

    var body: some View {
        CatalogueCaption("Header and key figures")
        HStack {
            Text("Portfolio")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)
            Toggle("Arrange", isOn: $isEditing)
                .toggleStyle(.monarchButton(.ghost, size: .sm))
        }
        ScrollView(.horizontal) {
            HStack(spacing: theme.spacing.space3) {
                MonarchStatCard(
                    "Active assets",
                    value: 865,
                    change: MonarchMetricChange(0.04, comparison: "vs last quarter")
                )
                MonarchStatCard("Book value", value: 1_240_000, format: .currency(code: "EUR"), isCompact: true)
                MonarchStatCard("Renewals in 90 days", value: 14, tone: .warning)
            }
        }
        .scrollIndicators(.hidden)

        CatalogueCaption("Sections")
        MonarchDashboardGrid(
            tiles,
            layout: $layout,
            itemLabel: \.title,
            span: { $0.isWide ? .two : .one },
            isEditing: isEditing,
            content: { tile in section(tile) }
        )
        Text(opened)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    @ViewBuilder private func section(_ tile: Tile) -> some View {
        switch tile.id {
        case "composition":
            MonarchWidget("Composition", variant: .plain) {
                MonarchDonutChart(
                    "Assets by type",
                    segments: [
                        MonarchDonutChartSegment("laptop", label: "Laptops", value: 412),
                        MonarchDonutChartSegment("phone", label: "Phones", value: 268),
                        MonarchDonutChartSegment("monitor", label: "Monitors", value: 131),
                        MonarchDonutChartSegment("tablet", label: "Tablets", value: 54),
                    ],
                    centerLabel: "assets"
                )
            }
        case "attention":
            MonarchWidget(
                "Needs attention",
                systemImage: "exclamationmark.triangle",
                onViewAll: { opened = "Attention" },
                content: { attentionContent }
            )
        case "upcoming":
            MonarchWidget("Coming up", systemImage: "calendar") {
                VStack(alignment: .leading, spacing: theme.spacing.space2) {
                    row("Adobe licence", detail: "Renews 2 Dec", tone: .info, badge: "Renewal")
                    row("Office lease", detail: "Notice by 15 Jan", tone: .warning, badge: "Deadline")
                }
            }
        case "breakdown":
            MonarchWidget("Breakdown", systemImage: "chart.bar") {
                MonarchBarList(
                    "By office",
                    items: [
                        MonarchBarListItem("Lisbon", value: 402),
                        MonarchBarListItem("Berlin", value: 288),
                        MonarchBarListItem("Remote", value: 175),
                    ],
                    onSelect: { opened = $0.label }
                )
            }
        case "ranking":
            MonarchWidget("Top items", systemImage: "list.number") {
                MonarchLeaderboard(
                    [
                        MonarchLeaderboardEntry("Data centre lease", value: 420_000),
                        MonarchLeaderboardEntry("Fleet contract", value: 180_000),
                        MonarchLeaderboardEntry("ERP licence", value: 96_000),
                    ],
                    label: "Largest contracts",
                    valueLabel: "yearly value",
                    format: .currency(code: "EUR"),
                    isCompact: true,
                    showsAvatars: false
                )
            }
        default:
            MonarchWidget(
                "Recent activity",
                systemImage: "clock.arrow.circlepath",
                onViewAll: { opened = "Activity" },
                content: { activityContent }
            )
        }
    }

    private var attentionContent: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            row("Laptop LT-0412", detail: "No owner", tone: .warning, badge: "Missing owner")
            row("Contract 88", detail: "Expired 3 days ago", tone: .danger, badge: "Expired")
        }
    }

    private var activityContent: some View {
        MonarchActivityTimeline(
            [
                MonarchActivityTimelineEvent(
                    id: "1",
                    actor: MonarchActivityTimelineActor("Renewal rule", kind: .automation),
                    verb: "flagged",
                    object: "Contract 88",
                    time: Date().addingTimeInterval(-6_900),
                    tone: .warning
                ),
                MonarchActivityTimelineEvent(
                    id: "2",
                    actor: MonarchActivityTimelineActor("Bea Lund"),
                    verb: "added",
                    object: "Laptop LT-0412",
                    time: Date().addingTimeInterval(-13_500)
                ),
            ],
            isGroupedByDay: false,
            density: .compact
        )
    }

    private func row(_ title: String, detail: String, tone: MonarchTone, badge: String) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space2) {
            VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                Text(title).monarchTextStyle(.bodyStrong)
                Text(detail)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            MonarchBadge(badge, tone: tone)
        }
        .accessibilityElement(children: .combine)
    }
}
