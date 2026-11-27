import MonarchUI
import SwiftUI

struct MonarchFormFieldSection: CataloguePage {
    static let title = "Form field"

    @State private var zone = "UTC"
    @State private var seats = 0
    @State private var digest = true
    @State private var isLocked = false
    @State private var start = Date()

    var body: some View {
        CatalogueCaption("Stack, required, with help text")
        MonarchFormField("Time zone", helper: "Used for reminders.", isRequired: true) {
            Picker("Time zone", selection: $zone) {
                ForEach(["UTC", "London", "Kolkata", "New York"], id: \.self) { Text($0).tag($0) }
            }
        }

        CatalogueCaption("An error that clears when the value is valid")
        MonarchFormField(
            "Seats",
            error: seats == 0 ? "Choose at least 1 seat." : nil,
            optionalText: "Optional"
        ) {
            Stepper("Seats: \(seats)", value: $seats, in: 0...10)
        }

        CatalogueCaption("Row layout")
        MonarchFormField("Weekly digest", helper: "A summary of activity, sent on Monday mornings.", layout: .row) {
            Toggle("Weekly digest", isOn: $digest).labelsHidden()
        }

        CatalogueCaption("Disabled")
        Toggle("Lock the field", isOn: $isLocked)
        MonarchFormField("Plan", helper: "Managed by your administrator.", layout: .row) {
            Toggle("Plan", isOn: $digest).labelsHidden()
        }
        .disabled(isLocked)

        CatalogueCaption("A group of controls")
        MonarchFormField("Starts", helper: "In your time zone.", isRequired: true, mode: .group) {
            HStack {
                DatePicker("Date", selection: $start, displayedComponents: .date).labelsHidden()
                DatePicker("Time", selection: $start, displayedComponents: .hourAndMinute).labelsHidden()
            }
        }
    }
}
