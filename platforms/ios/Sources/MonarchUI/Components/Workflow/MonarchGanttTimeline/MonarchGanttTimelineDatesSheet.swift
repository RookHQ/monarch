import SwiftUI

/// Change dates: a sheet with two system date pickers in a form, the
/// first day and the last. Save reports the dates; Cancel closes it.
struct MonarchGanttTimelineDatesSheet: View {
    let model: MonarchGanttTimelineModel
    let item: MonarchGanttTimelineItem

    @Environment(\.dismiss) private var dismiss
    @State private var start: Date
    @State private var end: Date

    init(model: MonarchGanttTimelineModel, item: MonarchGanttTimelineItem) {
        self.model = model
        self.item = item
        let initial = MonarchGanttTimelineDatesSheet.initialDates(model: model, item: item)
        _start = State(initialValue: initial.start)
        _end = State(initialValue: initial.end)
    }

    /// The days the pickers start on: the item's span, or today (or the
    /// first visible day) for an item without dates.
    static func initialDates(model: MonarchGanttTimelineModel, item: MonarchGanttTimelineItem) -> (
        start: Date, end: Date
    ) {
        guard let span = model.span(item.dates) else {
            let day = model.days.date(model.today ?? model.anchor)
            return (day, day)
        }
        return (model.days.date(span.start), model.days.date(span.end))
    }

    var body: some View {
        NavigationStack {
            Form {
                Group {
                    DatePicker(model.text.startDate, selection: $start, displayedComponents: .date)
                    DatePicker(model.text.endDate, selection: $end, in: start..., displayedComponents: .date)

                }
                .monarchRowsOnSurface()
            }
            .monarchCanvas()
            .navigationTitle(item.label)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(model.text.cancel) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(model.text.save) {
                        model.commit(item, dates: MonarchGanttTimelineDates(start: start, end: max(start, end)))
                        dismiss()
                    }
                }
            }
        }
        .environment(\.calendar, model.days.calendar)
        .environment(\.timeZone, model.days.calendar.timeZone)
        .presentationDetents([.medium, .large])
    }
}
