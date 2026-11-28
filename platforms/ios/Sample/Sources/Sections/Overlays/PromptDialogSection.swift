import MonarchUI
import SwiftUI

/// Native adoption: the system alert with a text field, and a sheet when the value must be checked.
struct PromptDialogSection: CataloguePage {
    static let title = "Prompt dialog (alert with a field)"

    @Environment(\.monarchTheme) private var theme
    @State private var isRenaming = false
    @State private var name = ""
    @State private var saved = "Untitled view"
    @State private var isAddingLink = false
    @State private var link = ""
    @State private var linkError: String?
    @State private var links: [String] = []

    var body: some View {
        CatalogueCaption("One short value")
        MonarchButton("Rename \(saved)") {
            name = saved
            isRenaming = true
        }
        .alert("Rename view", isPresented: $isRenaming) {
            TextField("Name", text: $name)
            Button("Save") {
                let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty { saved = trimmed }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Everyone in the workspace sees this name.")
        }

        CatalogueCaption("A value that is checked before closing belongs in a sheet")
        MonarchButton("Add a link") {
            link = ""
            linkError = nil
            isAddingLink = true
        }
        ForEach(links, id: \.self) { address in
            Text(address)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textLink)
        }
        theme.colors.bgCanvas
            .frame(height: 0)
            .accessibilityHidden(true)
            .sheet(isPresented: $isAddingLink) { linkSheet }
    }

    private var linkSheet: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Add a link")
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            MonarchTextField("Address", text: $link, prompt: "https://", error: linkError)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            MonarchButton("Add", variant: .primary, fullWidth: true) {
                if URL(string: link)?.scheme == "https" {
                    links.append(link)
                    isAddingLink = false
                } else {
                    linkError = "Enter a full web address, starting with https://."
                }
            }
            MonarchButton("Cancel", variant: .ghost, fullWidth: true) { isAddingLink = false }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .frame(maxHeight: .infinity, alignment: .top)
        .presentationDetents([.medium])
    }
}
