import MonarchUI
import SwiftUI

struct MonarchCheckboxGroupSection: CataloguePage {
    static let title = "Checkbox group"

    private static let topics = [
        MonarchCheckboxGroupOption(id: "comments", label: "Comments"),
        MonarchCheckboxGroupOption(id: "mentions", label: "Mentions", description: "When someone tags you."),
        MonarchCheckboxGroupOption(id: "reviews", label: "Review requests"),
        MonarchCheckboxGroupOption(
            id: "billing",
            label: "Billing",
            description: "Managed by your admin.",
            isDisabled: true
        ),
    ]

    @State private var topics: Set<String> = ["comments", "billing"]
    @State private var days: Set<String> = []
    @State private var isLocked = false

    var body: some View {
        CatalogueCaption("Select all: checked, mixed or clear")
        MonarchCheckboxGroup(
            "Notify me about",
            selection: $topics,
            options: Self.topics,
            helper: "Sent by email.",
            showsSelectAll: true
        )
        Text("Selected: \(topics.sorted().joined(separator: ", "))").monarchTextStyle(.caption)

        CatalogueCaption("Wrapping row, required, with validation")
        MonarchCheckboxGroup(
            "Working days",
            selection: $days,
            options: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map {
                MonarchCheckboxGroupOption(id: $0, label: $0)
            },
            error: days.isEmpty ? "Choose at least one day." : nil,
            layout: .horizontal,
            isRequired: true
        )

        CatalogueCaption("Disabled")
        Toggle("Lock the group", isOn: $isLocked)
        MonarchCheckboxGroup("Notify me about", selection: $topics, options: Self.topics)
            .disabled(isLocked)
    }
}
