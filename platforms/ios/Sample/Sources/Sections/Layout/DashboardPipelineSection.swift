import MonarchUI
import SwiftUI

/// Recipe: the home screen of a pipeline product. Key figures, insights and a
/// MonarchDashboardGrid with the funnel, today's agenda, follow-ups,
/// performance, a ranking and recent activity.
struct DashboardPipelineSection: CataloguePage {
    static let title = "Pipeline dashboard (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var period = "month"
    @State private var layout: [String] = []
    @State private var isEditing = false
    @State private var isLoading = false
    @State private var stage: String?
    @State private var lastAction = ""

    private let now = Date(timeIntervalSince1970: 1_793_534_400)

    private enum HomeSection: String, CaseIterable, Identifiable {
        case pipeline, today, followUps, upcoming, performance, leaders, activity

        var id: String { rawValue }

        var title: String {
            switch self {
            case .pipeline: "Pipeline"
            case .today: "Today"
            case .followUps: "Waiting on you"
            case .upcoming: "Coming up"
            case .performance: "Performance"
            case .leaders: "Top performers"
            case .activity: "Recent activity"
            }
        }

        var span: MonarchDashboardGridSpan {
            switch self {
            case .pipeline, .performance: .two
            default: .one
            }
        }
    }

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Show the loading states", isOn: $isLoading)
            .monarchTextStyle(.body)

        CatalogueCaption("Home screen")
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text(period == "month" ? "Your deals this month" : "Your deals this quarter")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                    figures
                    MonarchInsightStrip(
                        "Insights",
                        insights: [
                            MonarchInsight(
                                id: "stalled",
                                title: "6 deals have had no activity for 14 days",
                                why: "All six are in Proposal.",
                                tone: .warning,
                                origin: .rule,
                                actionTitle: "Show deals",
                                action: { lastAction = "Show stalled deals" }
                            )
                        ],
                        isLoading: isLoading
                    )
                    MonarchDashboardGrid(
                        HomeSection.allCases,
                        layout: $layout,
                        itemLabel: \.title,
                        span: { $0.span },
                        isEditing: isEditing,
                        labels: MonarchDashboardGridLabels(label: "Dashboard sections"),
                        content: { section in sectionView(section) }
                    )
                    .labeledContentStyle(.monarch)
                }
                .padding(theme.spacing.space4)
            }
            .navigationTitle("Pipeline")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("New deal", systemImage: "plus") { lastAction = "New deal" }
                }
                ToolbarItem(placement: .secondaryAction) {
                    Picker("Period", selection: $period) {
                        Text("This month").tag("month")
                        Text("This quarter").tag("quarter")
                    }
                }
                ToolbarItem(placement: .secondaryAction) {
                    Toggle("Arrange sections", systemImage: "square.grid.2x2", isOn: $isEditing)
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 9)
        if let stage {
            Text("Chosen stage: \(stage)").monarchTextStyle(.caption)
        }
        if !lastAction.isEmpty {
            Text("Last action: \(lastAction)").monarchTextStyle(.caption)
        }
    }

    private var figures: some View {
        LazyVGrid(
            columns: Array(
                repeating: GridItem(.flexible(), spacing: theme.spacing.space3),
                count: dynamicTypeSize.isAccessibilitySize ? 1 : 2
            ),
            spacing: theme.spacing.space3
        ) {
            MonarchStatCard(
                "Pipeline value",
                value: 1_240_000,
                change: MonarchMetricChange(0.08, comparison: "vs last month"),
                format: .currency(code: "USD"),
                isCompact: true,
                isLoading: isLoading
            )
            MonarchStatCard("Open deals", value: 86, change: MonarchMetricChange(0.03), isLoading: isLoading)
            MonarchStatCard(
                "Win rate",
                value: 0.27,
                change: MonarchMetricChange(-0.02),
                format: .percent,
                isLoading: isLoading
            )
            MonarchStatCard("Closing this month", value: 14, isLoading: isLoading)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Key figures")
    }

    @ViewBuilder private func sectionView(_ section: HomeSection) -> some View {
        switch section {
        case .pipeline:
            MonarchFunnelChart(
                section.title,
                stages: [
                    MonarchFunnelChartStage("Lead", value: 240),
                    MonarchFunnelChartStage("Qualified", value: 120),
                    MonarchFunnelChartStage("Proposal", value: 48),
                    MonarchFunnelChartStage("Won", value: 14),
                ],
                description: "Deals by stage",
                status: isLoading ? .loading : .ready,
                onSelect: { stage = $0.label }
            )
        case .today:
            MonarchWidget(section.title, systemImage: "calendar", state: isLoading ? .loading : .ready) {
                MonarchEventChip(
                    "Acme renewal call",
                    time: "10:00 – 10:30",
                    variant: .bar,
                    action: { lastAction = "Acme renewal call" }
                )
                MonarchEventChip(
                    "Demo for Globex",
                    time: "14:00 – 15:00",
                    variant: .bar,
                    action: { lastAction = "Demo for Globex" }
                )
            }
        case .followUps:
            MonarchWidget(
                section.title,
                onViewAll: { lastAction = "View all follow-ups" },
                content: {
                    LabeledContent("Discount approval · Initech") {
                        MonarchBadge("Overdue", tone: .danger, systemImage: "clock.badge.exclamationmark")
                    }
                    LabeledContent("Reply to Umbrella", value: "Today")
                }
            )
        case .upcoming:
            MonarchWidget(section.title, state: .empty("No closes expected this week.")) {
                EmptyView()
            }
        case .performance:
            MonarchWidget(section.title, period: "This month") {
                MonarchBarList(
                    "Won deals by source",
                    items: [
                        MonarchBarListItem("Referral", value: 6),
                        MonarchBarListItem("Website", value: 5),
                        MonarchBarListItem("Outbound", value: 3),
                    ],
                    isLoading: isLoading
                )
            }
        case .leaders:
            MonarchWidget(section.title) {
                MonarchLeaderboard(
                    [
                        MonarchLeaderboardEntry("Ana Lima", value: 182_000, change: 0.12),
                        MonarchLeaderboardEntry("Sam Ortiz", value: 140_000, isCurrent: true),
                        MonarchLeaderboardEntry("Kai Chen", value: 96_000, change: -0.05),
                    ],
                    label: section.title,
                    valueLabel: "won",
                    sort: .descending,
                    format: .currency(code: "USD"),
                    isCompact: true,
                    changeComparison: "vs last month",
                    isLoading: isLoading
                )
            }
        case .activity:
            MonarchWidget(
                section.title,
                systemImage: "clock.arrow.circlepath",
                onViewAll: { lastAction = "View all activity" },
                content: {
                    MonarchActivityTimeline(
                        [
                            MonarchActivityTimelineEvent(
                                id: "1",
                                actor: MonarchActivityTimelineActor("Ana Lima"),
                                verb: "moved",
                                object: "Globex",
                                time: now.addingTimeInterval(-1_800),
                                body: "Proposal → Negotiation"
                            ),
                            MonarchActivityTimelineEvent(
                                id: "2",
                                actor: MonarchActivityTimelineActor("Kai Chen"),
                                verb: "won",
                                object: "Initech",
                                time: now.addingTimeInterval(-7_200),
                                tone: .success
                            ),
                        ],
                        isGroupedByDay: false,
                        density: .compact,
                        isLoading: isLoading,
                        now: now
                    )
                }
            )
        }
    }
}
