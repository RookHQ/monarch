import MonarchUI
import SwiftUI

struct FormBuilderSection: CataloguePage {
    static let title = "Form builder"

    @Environment(\.monarchTheme) private var theme
    @State private var fields = [
        MonarchFormFieldDefinition(id: "name", type: .shortText, label: "Full name", isRequired: true),
        MonarchFormFieldDefinition(id: "email", type: .email, label: "Work email"),
    ]
    @State private var tab = "build"
    @State private var editing: MonarchFormFieldDefinition?
    @State private var added = 0
    @State private var answers: [String: MonarchFormFieldValue] = [:]
    private let palette: [MonarchFormFieldType] = [.shortText, .longText, .email, .date, .yesNo]

    private func typeName(_ type: MonarchFormFieldType) -> String {
        switch type {
        case .shortText: "Short text"
        case .longText: "Long text"
        case .email: "Email"
        case .date: "Date"
        case .yesNo: "Yes or no"
        default: type.rawValue
        }
    }

    private func move(_ field: MonarchFormFieldDefinition, by step: Int) {
        guard let index = fields.firstIndex(of: field), fields.indices.contains(index + step) else { return }
        fields.swapAt(index, index + step)
        AccessibilityNotification.Announcement(
            "\(field.label) moved to position \(index + step + 1) of \(fields.count)"
        ).post()
    }

    private func settings(_ id: String) -> Binding<MonarchFormFieldDefinition>? {
        guard let current = fields.first(where: { $0.id == id }) else { return nil }
        return Binding(
            get: { fields.first { $0.id == id } ?? current },
            set: { edited in
                if let index = fields.firstIndex(where: { $0.id == id }) { fields[index] = edited }
            }
        )
    }

    var body: some View {
        CatalogueCaption("Build and preview a form on a phone")
        VStack(spacing: theme.spacing.space3) {
            MonarchTabs(
                "Form builder",
                selection: $tab,
                items: [MonarchTabsItem("Build", value: "build"), MonarchTabsItem("Preview", value: "preview")]
            )
            if tab == "build" {
                List {
                    Menu {
                        ForEach(palette, id: \.self) { type in
                            Button(typeName(type)) {
                                added += 1
                                fields.append(
                                    MonarchFormFieldDefinition(id: "field\(added)", type: type, label: typeName(type))
                                )
                            }
                        }
                    } label: {
                        Label("Add a field", systemImage: "plus")
                    }
                    if fields.isEmpty {
                        MonarchEmptyState("No fields yet", message: "Add a field to start the form.", size: .sm)
                    }
                    ForEach(fields) { field in
                        HStack(spacing: theme.spacing.space2) {
                            VStack(alignment: .leading, spacing: theme.spacing.space0_5) {
                                Text(field.label)
                                    .monarchTextStyle(.body)
                                Text(field.isRequired ? "\(typeName(field.type)), required" : typeName(field.type))
                                    .monarchTextStyle(.caption)
                                    .foregroundStyle(theme.colors.textMuted)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .accessibilityElement(children: .combine)
                            .accessibilityActions {
                                Button("Move up") { move(field, by: -1) }
                                Button("Move down") { move(field, by: 1) }
                            }
                            Button("Edit") { editing = field }
                                .buttonStyle(.monarch(.ghost, size: .sm))
                                .accessibilityLabel("Edit \(field.label)")
                        }
                    }
                    .onMove { fields.move(fromOffsets: $0, toOffset: $1) }
                }
                #if os(iOS)
                    .environment(\.editMode, .constant(.active))
                #endif
            } else {
                Form {
                    ForEach(fields.filter { $0.isVisible(in: answers) }) { field in
                        MonarchFieldRenderer(field, value: $answers[field.id])
                    }
                }
            }
        }
        .sheet(item: $editing) { snapshot in
            if let field = settings(snapshot.id) {
                NavigationStack {
                    Form {
                        MonarchTextField(
                            "Question",
                            text: field.label,
                            error: field.wrappedValue.label.trimmingCharacters(in: .whitespaces).isEmpty
                                ? "Write the question." : nil
                        )
                        Picker("Type", selection: field.type) {
                            ForEach(palette, id: \.self) { Text(typeName($0)).tag($0) }
                        }
                        .pickerStyle(.menu)
                        Toggle("Required", isOn: field.isRequired)
                        Button("Remove field", role: .destructive) {
                            fields.removeAll { $0.id == snapshot.id }
                            editing = nil
                        }
                    }
                    .navigationTitle("Field settings")
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { editing = nil }
                        }
                    }
                }
                .presentationDetents([.medium, .large])
            }
        }
        .frame(height: theme.spacing.space24 * 6)
    }
}
