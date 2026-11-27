import MonarchUI
import SwiftUI

struct MonarchCalendarHeatmapSection: CataloguePage {
    static let title = "CalendarHeatmap"

    @State private var layout = MonarchCalendarHeatmapLayout.year
    @State private var status = MonarchChartFrameStatus.ready
    @State private var color = MonarchChartColor.data1
    @State private var selected = "none"

    @Environment(\.calendar) private var calendar

    /// 1 January 2026 in the device calendar; fixed so the page looks the same every day.
    private var start: Date {
        calendar.date(from: DateComponents(year: 2026, month: 1, day: 1)) ?? Date(timeIntervalSinceReferenceDate: 0)
    }

    private func date(_ offset: Int) -> Date {
        calendar.date(byAdding: .day, value: offset, to: start) ?? start
    }

    private var halfYear: [MonarchCalendarHeatmapDay] {
        (0..<181).map { offset in
            let weekday = calendar.component(.weekday, from: date(offset))
            let weekend = weekday == 1 || weekday == 7
            return MonarchCalendarHeatmapDay(
                date(offset),
                value: weekend ? Double(offset % 5 == 0 ? 1 : 0) : Double((offset * 7 + weekday * 3) % 11),
                note: offset == 45 ? "Team offsite" : nil,
                isInactive: weekend,
                hasMarker: offset == 45
            )
        }
    }

    private var march: [MonarchCalendarHeatmapDay] {
        let hours: [Int: (Double?, String?)] = [
            60: (480, "8h"), 61: (450, "7h 30m"), 63: (510, "8h 30m"), 64: (240, "4h"),
            68: (465, "7h 45m"), 69: (420, "7h"), 70: (600, "10h"),
        ]
        var days = hours.map { offset, entry in
            MonarchCalendarHeatmapDay(date(offset), value: entry.0, valueLabel: entry.1)
        }
        days.append(MonarchCalendarHeatmapDay(date(62), value: 0, note: "Missing hours", hasMarker: true))
        days.append(MonarchCalendarHeatmapDay(date(65), value: nil, isInactive: true))
        days.append(MonarchCalendarHeatmapDay(date(66), value: nil, isInactive: true))
        days.append(MonarchCalendarHeatmapDay(date(67), value: nil, note: "Holiday", isInactive: true))
        return days
    }

    var body: some View {
        Picker("Layout", selection: $layout) {
            Text("Year").tag(MonarchCalendarHeatmapLayout.year)
            Text("Month").tag(MonarchCalendarHeatmapLayout.month)
        }
        .pickerStyle(.segmented)
        Picker("Status", selection: $status) {
            Text("Ready").tag(MonarchChartFrameStatus.ready)
            Text("Loading").tag(MonarchChartFrameStatus.loading)
            Text("Empty").tag(MonarchChartFrameStatus.empty)
            Text("Error").tag(MonarchChartFrameStatus.error)
        }
        .pickerStyle(.segmented)
        Picker("Colour", selection: $color) {
            Text("1").tag(MonarchChartColor.data1)
            Text("2").tag(MonarchChartColor.data2)
            Text("3").tag(MonarchChartColor.data3)
            Text("Neutral").tag(MonarchChartColor.neutral)
        }
        .pickerStyle(.segmented)

        CatalogueCaption("Tap a day. Selected: \(selected)")
        if layout == .year {
            MonarchCalendarHeatmap(
                "Contributions",
                days: halfYear,
                from: date(0),
                to: date(180),
                description: "January to June 2026",
                today: date(180),
                status: status,
                color: color,
                emptyMessage: "Contributions appear after the first commit.",
                onRetry: { status = .ready },
                onSelect: { selected = $0.value.map { $0.formatted() } ?? "no data" }
            )
        } else {
            MonarchCalendarHeatmap(
                "March 2026",
                days: march,
                from: date(59),
                to: date(89),
                description: "Hours logged per day",
                layout: .month,
                today: date(70),
                status: status,
                color: color,
                footer: "Source: timesheets.",
                onRetry: { status = .ready },
                onSelect: { selected = $0.valueLabel ?? $0.note ?? "no entry" }
            )
        }
    }
}
