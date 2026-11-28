import MonarchUI
import SwiftUI

/// Native adoption: the standard `DatePicker` for dates, in a Monarch form
/// field, with a menu of presets.
struct DatePickerSection: CataloguePage {
    static let title = "Date picker (standard DatePicker)"

    @Environment(\.monarchTheme) private var theme
    @State private var due = Date.now
    @State private var renewal: Date?
    @State private var birthday = Date.now

    private var today: Date { Calendar.current.startOfDay(for: .now) }

    var body: some View {
        CatalogueCaption("Compact, with presets and a minimum")
        MonarchFormField("Due date", helper: "Invoices are due on or after today.") {
            HStack {
                DatePicker("Due date", selection: $due, in: today..., displayedComponents: .date)
                    .labelsHidden()
                Menu("Quick picks") {
                    Button("Today") { due = today }
                    Button("Tomorrow") { due = Calendar.current.date(byAdding: .day, value: 1, to: today) ?? today }
                    Button("In a week") { due = Calendar.current.date(byAdding: .day, value: 7, to: today) ?? today }
                }
                .monarchTextStyle(.body)
                .monarchMinimumHitTarget()
            }
        }
        CatalogueCaption("Optional, with Clear")
        MonarchFormField("Renewal date", helper: renewal == nil ? "Not set" : nil) {
            HStack {
                if let renewal {
                    DatePicker(
                        "Renewal date",
                        selection: Binding(get: { renewal }, set: { self.renewal = $0 }),
                        displayedComponents: .date
                    )
                    .labelsHidden()
                    MonarchButton("Clear date", variant: .ghost, size: .sm) { self.renewal = nil }
                } else {
                    MonarchButton("Set date", systemImage: "calendar", size: .sm) { self.renewal = today }
                }
            }
        }
        CatalogueCaption("Graphical, inline calendar")
        DatePicker("Birthday", selection: $birthday, in: ...today, displayedComponents: .date)
            .datePickerStyle(.graphical)
            .monarchTextStyle(.body)
        CatalogueCaption("Disabled")
        DatePicker("Start date", selection: .constant(today), displayedComponents: .date)
            .monarchTextStyle(.body)
            .disabled(true)
    }
}
