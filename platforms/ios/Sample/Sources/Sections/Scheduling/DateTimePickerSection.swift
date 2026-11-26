import MonarchUI
import SwiftUI

/// Native adoption: the standard `DatePicker` with a date and a time, in a
/// Monarch form field, with the zone the time belongs to.
struct DateTimePickerSection: CataloguePage {
    static let title = "Date and time picker (standard DatePicker)"

    @State private var start = Date.now
    @State private var send = Date.now
    @State private var zoneID: String? = TimeZone.current.identifier
    @State private var reminder: Date?

    private var zone: TimeZone { zoneID.flatMap(TimeZone.init(identifier:)) ?? .current }

    var body: some View {
        CatalogueCaption("A date and a time, from now on")
        MonarchFormField("Starts", helper: zone.localizedName(for: .standard, locale: .current)) {
            DatePicker("Starts", selection: $start, in: Date.now..., displayedComponents: [.date, .hourAndMinute])
                .labelsHidden()
                .environment(\.timeZone, zone)
        }

        CatalogueCaption("The zone chosen beside it; a preset keeps the time")
        MonarchFormField("Send at") {
            HStack {
                DatePicker("Send at", selection: $send, displayedComponents: [.date, .hourAndMinute])
                    .labelsHidden()
                    .environment(\.timeZone, zone)
                Menu("Quick picks") {
                    Button("Tomorrow, same time") {
                        send = Calendar.current.date(byAdding: .day, value: 1, to: send) ?? send
                    }
                    Button("Next week, same time") {
                        send = Calendar.current.date(byAdding: .day, value: 7, to: send) ?? send
                    }
                }
                .monarchTextStyle(.body)
                .monarchMinimumHitTarget()
            }
        }
        MonarchTimezoneSelect("Time zone", selection: $zoneID)

        CatalogueCaption("Optional, with Clear")
        MonarchFormField("Reminder", helper: reminder == nil ? "Not set" : nil) {
            HStack {
                if let reminder {
                    DatePicker(
                        "Reminder",
                        selection: Binding(get: { reminder }, set: { self.reminder = $0 }),
                        displayedComponents: [.date, .hourAndMinute]
                    )
                    .labelsHidden()
                    MonarchButton("Clear", variant: .ghost, size: .sm) { self.reminder = nil }
                } else {
                    MonarchButton("Set reminder", systemImage: "bell", size: .sm) { self.reminder = .now }
                }
            }
        }

        CatalogueCaption("Disabled")
        DatePicker("Created", selection: .constant(Date.now), displayedComponents: [.date, .hourAndMinute])
            .monarchTextStyle(.body)
            .disabled(true)
    }
}
