import MonarchUI
import SwiftUI

/// The Monarch select: the field box around the system menu.
struct SelectSection: CataloguePage {
    static let title = "Select"

    @State private var status = "open"
    @State private var priority: String?

    private let statuses = [
        MonarchSelectOption("Open", value: "open"),
        MonarchSelectOption("In progress", value: "progress"),
        MonarchSelectOption("Closed", value: "closed"),
    ]

    var body: some View {
        MonarchSelectField("Status", selection: $status, options: statuses, helper: "Shown to the customer.")

        MonarchSelectField(
            "Priority",
            selection: $priority,
            options: [
                MonarchSelectOption("Low", value: String?.some("low")),
                MonarchSelectOption("Medium", value: String?.some("medium")),
                MonarchSelectOption("High", value: String?.some("high")),
            ],
            prompt: "Choose a priority",
            error: priority == nil ? "Choose a priority." : nil,
            isRequired: true
        )

        CatalogueCaption("Small and large")
        MonarchSelectField("Status", selection: $status, options: statuses, labelVisibility: .hidden)
            .monarchFieldSize(.small)
        MonarchSelectField("Status", selection: $status, options: statuses, labelVisibility: .hidden)
            .monarchFieldSize(.large)

        CatalogueCaption("Disabled")
        MonarchSelectField("Status", selection: .constant("open"), options: statuses)
            .disabled(true)
    }
}
