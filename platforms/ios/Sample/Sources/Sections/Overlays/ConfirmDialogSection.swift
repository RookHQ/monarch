import MonarchUI
import SwiftUI

/// Native adoption: `alert` and `confirmationDialog` with a destructive button.
struct ConfirmDialogSection: CataloguePage {
    static let title = "Confirm dialog (alert, confirmationDialog)"

    @State private var isConfirmingAlert = false
    @State private var isConfirmingDialog = false
    @State private var isTyping = false
    @State private var phrase = ""
    @State private var result = "Nothing deleted yet"

    var body: some View {
        Text(result).monarchTextStyle(.bodyStrong)

        CatalogueCaption("An alert, for an action the person did not start from a list")
        MonarchButton("Delete project", variant: .danger) { isConfirmingAlert = true }
            .alert(
                "Delete the project?",
                isPresented: $isConfirmingAlert,
                actions: {
                    Button("Delete", role: .destructive) { result = "Project deleted" }
                    Button("Cancel", role: .cancel) {}
                },
                message: {
                    Text("Its 12 tickets are deleted with it. This cannot be undone.")
                }
            )

        CatalogueCaption("A confirmation dialog, anchored to the control that asked")
        MonarchButton("Remove member", variant: .danger) { isConfirmingDialog = true }
            .confirmationDialog(
                "Remove Ada from the team?",
                isPresented: $isConfirmingDialog,
                titleVisibility: .visible,
                actions: {
                    Button("Remove", role: .destructive) { result = "Member removed" }
                    Button("Cancel", role: .cancel) {}
                }
            )

        CatalogueCaption("Type to confirm: a text field in the alert")
        MonarchButton("Delete workspace", variant: .danger) {
            phrase = ""
            isTyping = true
        }
        .alert(
            "Type DELETE to confirm",
            isPresented: $isTyping,
            actions: {
                TextField("DELETE", text: $phrase)
                    .textInputAutocapitalization(.characters)
                Button("Delete", role: .destructive) { result = "Workspace deleted" }
                    .disabled(phrase != "DELETE")
                Button("Cancel", role: .cancel) {}
            }
        )
    }
}
