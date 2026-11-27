import MonarchUI
import SwiftUI

/// Recipe: the home screen of an operational product. Key figures, insights
/// and a MonarchDashboardGrid of sections, one column on a phone, with the
/// period and arrange mode in the toolbar.
struct DashboardOperationalSection: CataloguePage {
    static let title = "Operational dashboard (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var period = "7d"
    @State private var layout: [String] = []
    @State private var isEditing = false
    @State private var isLoading = false
    @State private var showsNotice = true
    @State private var dismissed: Set<String> = []
    @State private var isRetrying = false

    private let now = Date(timeIntervalSince1970: 1_793_534_400)

    private enum HomeSection: String, CaseIterable, Identifiable {
        case status, attention, queue, trend, workload, upcoming, activity

        var id: String { rawValue }

        var title: String {
            switch self {
            case .status: "Status"
            case .attention: "Needs attention"
            case .queue: "My queue"
            case .trend: "Trend"
            case .workload: "Team load"
            case .upcoming: "Coming up"
            case .activity: "Recent activity"
            }
        }

        var span: MonarchDashboardGridSpan {
            switch self {
            case .status: .full
            case .attention, .trend: .two
            default: .one
            }
        }
    }

    private var periodTitle: String {
        switch period {
        case "30d": "Last 30 days"
        case "quarter": "This quarter"
        default: "Last 7 days"
        }
    }

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Show the loading states", isOn: $isLoading)
            .monarchTextStyle(.body)
        Toggle("Show the setup notice", isOn: $showsNotice)
            .monarchTextStyle(.body)
        Button("Reset the layout") { layout = [] }
            .buttonStyle(.monarch(.secondary))

        CatalogueCaption("Home screen")
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: theme.spacing.space4) {
                    Text("All teams, \(periodTitle.lowercased())")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                    if showsNotice {
                        MonarchBanner(
                            message: "Connect your status page to see live service health.",
                            tone: .info,
                            actionTitle: "Hide",
                            action: { showsNotice = false }
                        )
                    }
                    figures
                    MonarchInsightStrip(
                        "Insights",
                        insights: insights,
                        isLoading: isLoading,
                        onDismiss: { dismissed.insert($0) }
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
            .navigationTitle("Home")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Menu(
                        content: {
                            Picker("Period", selection: $period) {
                                Text("Last 7 days").tag("7d")
                                Text("Last 30 days").tag("30d")
                                Text("This quarter").tag("quarter")
                            }
                        },
                        label: { Label("Period", systemImage: "calendar") }
                    )
                }
                ToolbarItem(placement: .secondaryAction) {
                    Toggle("Arrange sections", systemImage: "square.grid.2x2", isOn: $isEditing)
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 9)
        Text("Stored layout: \(layout.isEmpty ? "default" : layout.joined(separator: ", "))")
            .monarchTextStyle(.caption)
    }

    private var insights: [MonarchInsight] {
        [
            MonarchInsight(
                id: "breach",
                title: "4 tickets breach their SLA within an hour",
                why: "All four are waiting on tier 2.",
                tone: .warning,
                origin: .rule
            )
        ]
        .filter { !dismissed.contains($0.id) }
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
                "Open tickets",
                value: 128,
                change: MonarchMetricChange(0.12, comparison: "vs last week", goodDirection: .down),
                isLoading: isLoading
            )
            MonarchStatCard(
                "First reply",
                text: "1h 12m",
                change: MonarchMetricChange(-0.08, goodDirection: .down),
                isLoading: isLoading
            )
            MonarchStatCard("Solved", value: 342, change: MonarchMetricChange(0.05), isLoading: isLoading)
            MonarchStatCard("Satisfaction", value: 0.94, format: .percent, isLoading: isLoading)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Key figures")
    }

    @ViewBuilder private func sectionView(_ section: HomeSection) -> some View {
        switch section {
        case .status:
            MonarchWidget(section.title, systemImage: "waveform.path.ecg", state: isLoading ? .loading : .ready) {
                MonarchFlowLayout(spacing: theme.spacing.space2) {
                    MonarchStatusBadge(MonarchStatusDefinition("API: operational", tone: .success))
                    MonarchStatusBadge(MonarchStatusDefinition("Email: degraded", tone: .warning))
                    MonarchStatusBadge(MonarchStatusDefinition("Chat: operational", tone: .success))
                }
            }
        case .attention:
            MonarchWidget(
                section.title,
                state: isLoading ? .loading : .ready,
                onViewAll: {},
                content: {
                    LabeledContent("Request 1042 · Billing") {
                        MonarchBadge("Breaches in 20 min", tone: .danger, systemImage: "clock.badge.exclamationmark")
                    }
                    LabeledContent("Incident 88 · Email") {
                        MonarchBadge("Open", tone: .warning)
                    }
                }
            )
        case .queue:
            MonarchWidget(section.title, state: .empty("Your queue is empty.")) {
                EmptyView()
            }
        case .trend:
            MonarchLineChart(
                "Tickets opened",
                categories: ["Mon", "Tue", "Wed", "Thu", "Fri"],
                series: [
                    MonarchLineChartSeries("opened", label: "Opened", values: [42, 51, 38, 47, 55]),
                    MonarchLineChartSeries("solved", label: "Solved", values: [40, 49, 44, 45, 50]),
                ],
                description: periodTitle,
                status: isLoading ? .loading : .ready
            )
        case .workload:
            MonarchWidget(
                section.title,
                state: .failed("Team load could not be loaded."),
                isRetrying: isRetrying,
                onRetry: { isRetrying.toggle() },
                content: {
                    MonarchLeaderboard(
                        [MonarchLeaderboardEntry("Ana Lima", value: 18)],
                        label: section.title,
                        valueLabel: "open tickets",
                        isCompact: true
                    )
                }
            )
        case .upcoming:
            MonarchWidget(section.title, state: isLoading ? .loading : .ready) {
                LabeledContent("Database maintenance", value: "Sat 02:00")
                LabeledContent("Certificate renewal", value: "In 12 days")
            }
        case .activity:
            MonarchWidget(
                section.title,
                systemImage: "clock.arrow.circlepath",
                onViewAll: {},
                content: {
                    MonarchActivityTimeline(
                        [
                            MonarchActivityTimelineEvent(
                                id: "1",
                                actor: MonarchActivityTimelineActor("Ada Moss"),
                                verb: "closed",
                                object: "Request 1042",
                                time: now.addingTimeInterval(-1_200)
                            ),
                            MonarchActivityTimelineEvent(
                                id: "2",
                                actor: MonarchActivityTimelineActor("SLA rule", kind: .automation),
                                verb: "escalated",
                                object: "Request 1039",
                                time: now.addingTimeInterval(-6_900),
                                tone: .warning
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
