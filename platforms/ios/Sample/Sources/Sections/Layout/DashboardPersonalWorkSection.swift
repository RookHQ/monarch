import MonarchUI
import SwiftUI

/// Recipe: a person's own home ("My work"): key figures, insights and
/// widgets for today, assigned work, progress and recent activity.
struct DashboardPersonalWorkSection: CataloguePage {
    static let title = "Dashboard: personal work (recipe)"

    /// One section of the home screen.
    struct Tile: Identifiable {
        let id: String
        let title: String
        var isWide = false
    }

    /// A task due today.
    struct TodayTask: Identifiable {
        let id: String
        let title: String
        var isOverdue = false
        var isDone = false
    }

    @Environment(\.monarchTheme) private var theme
    @State private var period = "week"
    @State private var layout: [String] = []
    @State private var isEditing = false
    @State private var dismissed: Set<String> = []
    @State private var opened = "Nothing opened"
    @State private var tasks = [
        TodayTask(id: "1", title: "Send the Q3 report", isOverdue: true),
        TodayTask(id: "2", title: "Review 4 new applicants"),
    ]

    private let tiles = [
        Tile(id: "today", title: "Today"),
        Tile(id: "assigned", title: "Assigned to you"),
        Tile(id: "progress", title: "Progress"),
        Tile(id: "activity", title: "Recent activity", isWide: true),
    ]

    var body: some View {
        CatalogueCaption("Header: title, period and arrange")
        HStack {
            Text("My work")
                .monarchTextStyle(.title)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)
            Picker("Period", selection: $period) {
                Text("This week").tag("week")
                Text("This month").tag("month")
            }
            .pickerStyle(.menu)
            Toggle("Arrange", isOn: $isEditing)
                .toggleStyle(.monarchButton(.ghost, size: .sm))
        }

        CatalogueCaption("Key figures")
        ScrollView(.horizontal) {
            HStack(spacing: theme.spacing.space3) {
                MonarchStatCard("Open tasks", value: 12, hint: "3 due today", action: { opened = "Open tasks" })
                MonarchStatCard(
                    "Hours logged",
                    value: 31.5,
                    unit: "h",
                    change: MonarchMetricChange(0.08, comparison: "vs last week")
                )
                MonarchStatCard("Approvals waiting", value: 2, tone: .warning)
            }
        }
        .scrollIndicators(.hidden)

        MonarchInsightStrip(
            "Insights",
            insights: [
                MonarchInsight(
                    id: "focus",
                    title: "Two meetings overlap on Thursday",
                    why: "Design review and the vendor call both start at 14:00.",
                    tone: .warning,
                    origin: .rule,
                    actionTitle: "Open calendar",
                    action: { opened = "Calendar" }
                )
            ]
            .filter { !dismissed.contains($0.id) },
            onDismiss: { dismissed.insert($0) }
        )

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
        case "today":
            MonarchWidget(
                "Today",
                systemImage: "sun.max",
                onViewAll: { opened = "Today" },
                content: { todayContent }
            )
        case "assigned":
            MonarchWidget("Assigned to you", systemImage: "person.crop.circle.badge.checkmark") {
                MonarchBarList(
                    items: [
                        MonarchBarListItem("In progress", value: 5),
                        MonarchBarListItem("To do", value: 4),
                        MonarchBarListItem("In review", value: 2),
                    ],
                    onSelect: { opened = $0.label }
                )
            }
        case "progress":
            MonarchWidget("Progress", systemImage: "target") {
                VStack(alignment: .leading, spacing: theme.spacing.space3) {
                    ProgressView("Q4 goals", value: 0.6).progressViewStyle(.monarchLinear)
                    ProgressView("Training", value: 0.25).progressViewStyle(.monarchLinear)
                }
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

    private var todayContent: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            MonarchEventChip("Stand-up", time: "9:30 – 9:45", variant: .bar, action: { opened = "Stand-up" })
            ForEach($tasks) { $task in
                HStack {
                    Toggle(task.title, isOn: $task.isDone).toggleStyle(.monarchCheckbox)
                    if task.isOverdue && !task.isDone {
                        MonarchBadge("Overdue", tone: .danger, systemImage: "clock.badge.exclamationmark")
                    }
                }
            }
        }
    }

    private var activityContent: some View {
        MonarchActivityTimeline(
            [
                MonarchActivityTimelineEvent(
                    id: "1",
                    actor: MonarchActivityTimelineActor("You"),
                    verb: "closed",
                    object: "Request 1042",
                    time: Date().addingTimeInterval(-1_200)
                ),
                MonarchActivityTimelineEvent(
                    id: "2",
                    actor: MonarchActivityTimelineActor("Ada Moss"),
                    verb: "assigned you",
                    object: "Contract 88",
                    time: Date().addingTimeInterval(-7_200)
                ),
            ],
            isGroupedByDay: false,
            density: .compact
        )
    }
}
