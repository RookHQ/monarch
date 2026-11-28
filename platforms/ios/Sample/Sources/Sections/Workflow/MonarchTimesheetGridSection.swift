@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchTimesheetGridSection: CataloguePage {
    static let title = "Timesheet grid"

    private struct ClosedDay: LocalizedError {
        var errorDescription: String? { "Sunday is closed for entry in this demo." }
    }

    private static let monday: Date = {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: .now)
        let weekday = calendar.component(.weekday, from: today)
        let offset = (weekday - calendar.firstWeekday + 7) % 7
        return calendar.date(byAdding: .day, value: -offset, to: today) ?? today
    }()

    private static let dates: [Date] = (0..<7).map {
        Calendar.current.date(byAdding: .day, value: $0, to: monday) ?? monday
    }

    @State private var layout: MonarchTimesheetGridLayout = .automatic
    @State private var isSubmitted = false
    @State private var cells: [String: [Date: Int]] = [
        "atlas": [MonarchTimesheetGridSection.dates[0]: 240, MonarchTimesheetGridSection.dates[1]: 90],
        "pulse": [MonarchTimesheetGridSection.dates[1]: 300],
        "support": [MonarchTimesheetGridSection.dates[2]: 45],
    ]
    @State private var opened: String?

    private var days: [MonarchTimesheetGridDay] {
        Self.dates.enumerated().map { index, date in
            MonarchTimesheetGridDay(
                date,
                isToday: Calendar.current.isDateInToday(date),
                isNonWorking: index >= 5,
                lockReason: index == 0 ? "Approved" : nil,
                expectedMinutes: index < 5 ? 480 : nil
            )
        }
    }

    private var rows: [MonarchTimesheetGridRow] {
        [
            MonarchTimesheetGridRow(
                id: "atlas",
                label: "Atlas",
                description: "Design · Billable",
                cells: cells["atlas"] ?? [:]
            ),
            MonarchTimesheetGridRow(
                id: "pulse",
                label: "Pulse",
                description: "Engineering",
                cells: cells["pulse"] ?? [:]
            ),
            MonarchTimesheetGridRow(
                id: "support",
                label: "Support",
                description: "From the timer",
                cells: cells["support"] ?? [:],
                isReadOnly: true
            ),
        ]
    }

    var body: some View {
        Picker("Layout", selection: $layout) {
            Text("Automatic").tag(MonarchTimesheetGridLayout.automatic)
            Text("Grid").tag(MonarchTimesheetGridLayout.grid)
            Text("Day").tag(MonarchTimesheetGridLayout.day)
        }
        .pickerStyle(.segmented)
        Toggle("Submitted", isOn: $isSubmitted)
            .monarchTextStyle(.body)
        MonarchTimesheetGrid(
            days: days,
            rows: rows,
            summaryRows: [
                MonarchTimesheetGridSummaryRow(
                    id: "attendance",
                    label: "Attendance",
                    cells: [Self.dates[0]: "9:00–17:30", Self.dates[1]: "8:45–17:00"]
                )
            ],
            layout: layout,
            isReadOnly: isSubmitted,
            rowActions: { row in
                [
                    MonarchDataTableRowAction("Clear row", systemImage: "trash", isDestructive: row.isReadOnly == false)
                    {
                        cells[row.id] = [:]
                    }
                ]
            },
            onCellActivate: { rowID, _ in opened = rowID },
            onCellChange: { rowID, date, minutes in
                try await Task.sleep(for: .milliseconds(300))
                if date == Self.dates[6] { throw ClosedDay() }
                cells[rowID, default: [:]][date] = minutes == 0 ? nil : minutes
            }
        )
        if let opened {
            CatalogueCaption("Opened the entries of \(opened)")
        }
    }
}
