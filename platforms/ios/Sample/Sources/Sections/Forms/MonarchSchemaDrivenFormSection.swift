import MonarchUI
import SwiftUI

struct MonarchSchemaDrivenFormSection: CataloguePage {
    static let title = "Schema-driven form"

    private struct SubmitRefused: LocalizedError {
        var errorDescription: String? { "The server is busy. Nothing was sent; try again in a minute." }
    }

    private static let application = [
        MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
        MonarchFormFieldDefinition(id: "email", type: .email, label: "Work email", isRequired: true),
        MonarchFormFieldDefinition(id: "remote", type: .yesNo, label: "Do you want to work remotely?"),
        MonarchFormFieldDefinition(
            id: "setup",
            type: .section,
            label: "Your setup",
            description: "Only asked when you work remotely.",
            conditions: [MonarchCondition(id: "c1", field: "remote", operator: .isTrue)]
        ),
        MonarchFormFieldDefinition(id: "timezone", type: .shortText, label: "Time zone", isRequired: true),
        MonarchFormFieldDefinition(
            id: "equipment",
            type: .checkboxes,
            label: "Equipment you need",
            options: [
                MonarchFormFieldOption(value: "laptop", label: "Laptop"),
                MonarchFormFieldOption(value: "monitor", label: "Monitor"),
                MonarchFormFieldOption(value: "headset", label: "Headset"),
            ]
        ),
        MonarchFormFieldDefinition(id: "about", type: .section, label: "About the role"),
        MonarchFormFieldDefinition(id: "start", type: .date, label: "Earliest start date"),
        MonarchFormFieldDefinition(
            id: "consent",
            type: .consent,
            label: "Consent",
            isRequired: true,
            consentText: "I agree to be contacted about this application."
        ),
    ]

    private static let workspace = [
        MonarchFormFieldDefinition(id: "workspace", type: .shortText, label: "Workspace name", isRequired: true),
        MonarchFormFieldDefinition(id: "seats", type: .number, label: "Seats", min: 1, max: 500),
    ]

    @Environment(\.monarchTheme) private var theme

    @State private var answers: [String: MonarchFormFieldValue] = [:]
    @State private var validateOnSubmit = false
    @State private var failsNext = false
    @State private var isApplying = false
    @State private var sent: [String: MonarchFormFieldValue]?

    @State private var saved: [String: MonarchFormFieldValue] = ["workspace": .text("Rook"), "seats": .number(12)]
    @State private var edited: [String: MonarchFormFieldValue] = ["workspace": .text("Rook"), "seats": .number(12)]
    @State private var isEditing = false

    var body: some View {
        CatalogueCaption("An application form with a conditional section")
        Toggle("Show errors only on submit", isOn: $validateOnSubmit)
            .monarchTextStyle(.body)
        Toggle("Fail the next submit", isOn: $failsNext)
            .monarchTextStyle(.body)
        MonarchButton("Open the application form", systemImage: "doc.text", variant: .primary) {
            isApplying = true
        }
        .sheet(isPresented: $isApplying) {
            NavigationStack {
                MonarchSchemaDrivenForm(
                    Self.application,
                    values: $answers,
                    validateOn: validateOnSubmit ? .submit : .blur
                ) { values in
                    try await Task.sleep(for: .seconds(1))
                    if failsNext {
                        failsNext = false
                        throw SubmitRefused()
                    }
                    sent = values
                    isApplying = false
                }
                .navigationTitle("Apply")
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { isApplying = false }
                    }
                }
            }
        }
        if let sent {
            Text("Sent \(sent.count) answers.")
                .monarchTextStyle(.caption)
        }

        CatalogueCaption("An edit form that saves from the navigation bar")
        MonarchButton("Edit the workspace", systemImage: "pencil") {
            edited = saved
            isEditing = true
        }
        .sheet(isPresented: $isEditing) {
            NavigationStack {
                MonarchSchemaDrivenForm(
                    Self.workspace,
                    values: $edited,
                    isDirty: edited != saved,
                    onSubmit: { values in
                        try await Task.sleep(for: .milliseconds(600))
                        saved = values
                        isEditing = false
                    },
                    onDiscard: { edited = saved }
                )
                .navigationTitle("Workspace")
            }
        }

        CatalogueCaption("Read only")
        MonarchSchemaDrivenForm(Self.workspace, values: .constant(saved), isReadOnly: true)
            .frame(height: theme.spacing.space24 * 3)
    }
}
