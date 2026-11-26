import MonarchUI
import SwiftUI

struct MonarchTimesheetDiffSection: CataloguePage {
    static let title = "Timesheet diff"

    private static let monday = Calendar.current.startOfDay(for: .now)
    private static let tuesday = monday.addingTimeInterval(86400)

    private static let submitted = [
        MonarchTimesheetDiffEntry(
            id: "a",
            date: monday,
            minutes: 120,
            project: "Atlas",
            activity: "Design",
            task: "ENG-1"
        ),
        MonarchTimesheetDiffEntry(id: "b", date: monday, minutes: 45, project: "Pulse", isBillable: false),
        MonarchTimesheetDiffEntry(id: "c", date: tuesday, minutes: 30, project: "Atlas"),
    ]

    @State private var showsBillable = true
    @State private var draft = MonarchTimesheetDiffSection.submitted

    var body: some View {
        MonarchTimesheetDiff(
            before: Self.submitted,
            after: draft,
            subtitle: "since version 3",
            showsBillable: showsBillable
        )
        Toggle("Billing applies", isOn: $showsBillable)
            .monarchTextStyle(.body)
        MonarchButton("Add an hour to ENG-1", size: .sm) {
            draft = draft.map { entry in
                guard entry.id == "a" else { return entry }
                return MonarchTimesheetDiffEntry(
                    id: entry.id,
                    date: entry.date,
                    minutes: entry.minutes + 60,
                    project: entry.project,
                    activity: entry.activity,
                    task: entry.task
                )
            }
        }
        MonarchButton("Remove the Pulse entry", size: .sm) {
            draft.removeAll { $0.id == "b" }
        }
        MonarchButton("Log 30 minutes without a project", size: .sm) {
            draft.append(MonarchTimesheetDiffEntry(id: UUID().uuidString, date: Self.tuesday, minutes: 30))
        }
        MonarchButton("Reset", variant: .ghost, size: .sm) {
            draft = Self.submitted
        }
    }
}
