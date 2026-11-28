import Foundation
import MonarchUI
import SwiftUI

/// Recipe: today's agenda and the tasks due today, as two sections of one list.
struct TodayPanelSection: CataloguePage {
    static let title = "Today panel (recipe)"

    /// A task due today, as the app models it.
    struct TodayTask: Identifiable {
        let id: String
        let title: String
        var detail: String?
        var isOverdue = false
        var isDone = false
    }

    @Environment(\.monarchTheme) private var theme
    @State private var opened = "Nothing opened"
    @State private var tasks = [
        TodayTask(id: "1", title: "Send the Q3 report", detail: "Finance", isOverdue: true),
        TodayTask(id: "2", title: "Review 4 new applicants", detail: "Hiring"),
        TodayTask(id: "3", title: "Book the offsite venue", isDone: true),
    ]

    private let start = Calendar.current.startOfDay(for: .now)

    var body: some View {
        CatalogueCaption("Tap an event; tick a task")
        List {
            Section {
                MonarchEventChip(
                    "Stand-up",
                    time: time(9.5, 9.75),
                    variant: .bar,
                    action: { opened = "Opened Stand-up" }
                )
                MonarchEventChip(
                    "Design review",
                    time: time(14, 15),
                    detail: "Room 4",
                    variant: .bar,
                    action: { opened = "Opened Design review" }
                )
            } header: {
                Text("Schedule").monarchTextStyle(.label)
            }
            Section {
                ForEach($tasks) { $task in
                    HStack(alignment: .firstTextBaseline, spacing: theme.spacing.space2) {
                        Toggle(isOn: $task.isDone) {
                            VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                                Text(task.title).monarchTextStyle(.body)
                                if let detail = task.detail {
                                    Text(detail)
                                        .monarchTextStyle(.caption)
                                        .foregroundStyle(theme.colors.textMuted)
                                }
                            }
                        }
                        .toggleStyle(.monarchCheckbox)
                        if task.isOverdue && !task.isDone {
                            MonarchBadge("Overdue", tone: .danger, systemImage: "clock.badge.exclamationmark")
                        }
                    }
                }
            } header: {
                Text("Due today").monarchTextStyle(.label)
            }
        }
        .frame(minHeight: theme.spacing.space24 * 5)
        Text(opened).monarchTextStyle(.caption)

        CatalogueCaption("Nothing today")
        MonarchEmptyState(
            "Nothing scheduled and nothing due today.",
            systemImage: "sun.max",
            kind: .success,
            size: .sm
        )
    }

    private func time(_ from: Double, _ to: Double) -> String {
        let begin = start.addingTimeInterval(from * 3_600)
        let end = start.addingTimeInterval(to * 3_600)
        return
            "\(begin.formatted(date: .omitted, time: .shortened)) – \(end.formatted(date: .omitted, time: .shortened))"
    }
}
