import MonarchUI
import SwiftUI

/// Recipe: workspace AI settings as `Form` sections kept as a draft, with
/// Save and Discard in the toolbar and MonarchSecretField for the provider
/// key; the read-only form on a scrolling page is a MonarchSectionCard.
struct AiSettingsSectionSection: CataloguePage {
    static let title = "AI settings section (Form recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var saved = Settings()
    @State private var draft = Settings()
    @State private var apiKey = ""

    private struct Settings: Equatable {
        var apps: Set<String> = ["crm"]
        var sharing = false
        var retention = "90d"
        var provider = ""
    }

    private let apps = [("crm", "CRM"), ("desk", "Help desk")]

    private var isDirty: Bool { draft != saved || !apiKey.isEmpty }

    var body: some View {
        CatalogueCaption("A settings screen, opened in a sheet")
        Button("AI settings") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) { screen }

        CatalogueCaption("Read only, on a scrolling page")
        MonarchSectionCard("AI", description: "Only workspace admins can change these settings.") {
            Toggle("CRM", isOn: .constant(true)).monarchTextStyle(.body)
            Toggle("Share data to improve AI", isOn: .constant(false)).monarchTextStyle(.body)
        }
        .disabled(true)
    }

    private func binding(for app: String) -> Binding<Bool> {
        Binding(
            get: { draft.apps.contains(app) },
            set: { isOn in
                if isOn { draft.apps.insert(app) } else { draft.apps.remove(app) }
            }
        )
    }

    private var screen: some View {
        NavigationStack {
            Form {
                Section {
                    ForEach(apps, id: \.0) { app in
                        Toggle(app.1, isOn: binding(for: app.0))
                    }
                } header: {
                    Text("Use AI in")
                }
                Section {
                    Toggle("Share data to improve AI", isOn: $draft.sharing)
                    Picker("Conversation history", selection: $draft.retention) {
                        Text("30 days").tag("30d")
                        Text("90 days").tag("90d")
                        Text("Until deleted").tag("forever")
                    }
                } header: {
                    Text("Data")
                } footer: {
                    Text("Off keeps prompts and responses private to this workspace.")
                }
                Section {
                    Picker("AI provider", selection: $draft.provider) {
                        Text("Built-in").tag("")
                        Text("Example AI").tag("example")
                    }
                    if !draft.provider.isEmpty {
                        MonarchSecretField(
                            "Example AI API key",
                            text: $apiKey,
                            helper: "Stored encrypted. Leave blank to keep the saved key.",
                            isSet: draft.provider == saved.provider
                        )
                    }
                } header: {
                    Text("Provider")
                }
            }
            .onChange(of: draft.provider) { apiKey = "" }
            .tint(theme.colors.accentSolid)
            .navigationTitle("AI")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Discard") {
                        draft = saved
                        apiKey = ""
                    }
                    .disabled(!isDirty)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saved = draft
                        apiKey = ""
                        isOpen = false
                    }
                    .disabled(!isDirty)
                }
            }
        }
    }
}
