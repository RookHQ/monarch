import MonarchUI
import SwiftUI

/// Recipe: titled groups of settings as `Form` sections, with Save and
/// Discard in the toolbar while there are changes, a danger zone, and a
/// loading state through `redacted`. The card form for scrolling pages is
/// MonarchSectionCard.
struct SettingsSectionSection: CataloguePage {
    static let title = "Settings section (Form recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var saved = Settings()
    @State private var draft = Settings()
    @State private var isLoading = false
    @State private var isConfirming = false

    private struct Settings: Equatable {
        var digest = true
        var language = "en"
    }

    var body: some View {
        CatalogueCaption("A settings screen, opened in a sheet")
        Button("Workspace settings") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) { screen }
        Toggle("Show the loading state", isOn: $isLoading)
            .monarchTextStyle(.body)

        CatalogueCaption("On a scrolling page: MonarchSectionCard")
        MonarchSectionCard("Workspace", description: "Changes apply to everyone in the workspace.") {
            Toggle("Weekly digest", isOn: $draft.digest)
                .monarchTextStyle(.body)
        } actions: {
            EmptyView()
        } footer: {
            if draft != saved {
                Text("You have unsaved changes.")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                MonarchButton("Discard", variant: .ghost, size: .sm) { draft = saved }
                MonarchButton("Save changes", variant: .primary, size: .sm) { saved = draft }
            }
        }
    }

    private var screen: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle("Weekly digest", isOn: $draft.digest)
                    Picker("Language", selection: $draft.language) {
                        Text("English").tag("en")
                        Text("Português").tag("pt")
                    }
                } header: {
                    Text("Workspace")
                } footer: {
                    Text("Changes apply to everyone in the workspace.")
                }
                Section {
                    Button("Delete workspace", role: .destructive) { isConfirming = true }
                } header: {
                    Text("Danger zone")
                } footer: {
                    Text("Deleting removes every project. This can’t be undone.")
                }
            }
            .redacted(reason: isLoading ? .placeholder : [])
            .disabled(isLoading)
            .tint(theme.colors.accentSolid)
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Discard") { draft = saved }
                        .disabled(draft == saved)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saved = draft
                        isOpen = false
                    }
                    .disabled(draft == saved)
                }
            }
            .confirmationDialog("Delete this workspace?", isPresented: $isConfirming, titleVisibility: .visible) {
                Button("Delete workspace", role: .destructive) { isOpen = false }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Every project and file in it is deleted. This can’t be undone.")
            }
        }
    }
}
