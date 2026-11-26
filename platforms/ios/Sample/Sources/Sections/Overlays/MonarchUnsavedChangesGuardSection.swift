import MonarchUI
import SwiftUI

struct MonarchUnsavedChangesGuardSection: CataloguePage {
    static let title = "Unsaved changes guard"

    @State private var showsSheet = false
    @State private var result = "Nothing edited yet"

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)

        CatalogueCaption("A sheet: it cannot be swiped away while there are changes, and Cancel asks first")
        MonarchButton("Edit contact", variant: .primary) { showsSheet = true }
            .sheet(isPresented: $showsSheet) {
                NavigationStack {
                    GuardedContactForm(isSheet: true, failsToSave: false) { reason in
                        result = reason == .save ? "Saved and closed" : "Changes discarded"
                    }
                }
            }

        CatalogueCaption("A pushed screen: the back button asks first. Saving fails here, to show the error.")
        NavigationLink("Open a contact") {
            GuardedContactForm(isSheet: false, failsToSave: true) { reason in
                result = reason == .save ? "Saved" : "Changes discarded"
            }
        }
        .buttonStyle(.monarch())
    }
}

private struct GuardedContactForm: View {
    let isSheet: Bool
    let failsToSave: Bool
    let onLeave: @MainActor (MonarchUnsavedChangesGuardLeaveReason) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @State private var name = "Ada Lovelace"
    @State private var isAsking = false

    private var hasChanges: Bool { name != "Ada Lovelace" }

    var body: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchTextField("Name", text: $name)
            Text(hasChanges ? "Unsaved changes" : "No changes").monarchTextStyle(.caption)
        }
        .padding(theme.spacing.space4)
        .frame(maxHeight: .infinity, alignment: .top)
        .navigationTitle("Contact")
        .toolbar {
            if isSheet {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        if hasChanges { isAsking = true } else { dismiss() }
                    }
                }
            }
        }
        .monarchUnsavedChangesGuard(
            isPresented: $isAsking,
            hasChanges: hasChanges,
            onSave: {
                try await Task.sleep(for: .seconds(1))
                if failsToSave { throw URLError(.notConnectedToInternet) }
            },
            onLeave: { reason in
                onLeave(reason)
                dismiss()
            }
        )
    }
}
