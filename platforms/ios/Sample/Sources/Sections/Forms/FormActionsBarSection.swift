import MonarchUI
import SwiftUI

/// Native adoption: Save and Discard are toolbar items of the navigation bar.
struct FormActionsBarSection: CataloguePage {
    static let title = "Form actions bar (toolbar items)"

    @State private var name = "Acme"
    @State private var savedName = "Acme"
    @State private var isSaving = false
    @State private var failsNextSave = false
    @State private var failure: String?
    @State private var isConfirmingDiscard = false

    private var isDirty: Bool { name != savedName }

    var body: some View {
        CatalogueCaption("Change the name: Save and Discard in the navigation bar become available.")
        MonarchTextField("Company name", text: $name, helper: isDirty ? "You have unsaved changes." : nil)
            .disabled(isSaving)
        Toggle("Fail the next save", isOn: $failsNextSave)
            .monarchTextStyle(.body)
        if let failure {
            MonarchBanner(message: failure, tone: .danger)
        }
        Color.clear
            .frame(height: 0)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Discard") { isConfirmingDiscard = true }
                        .disabled(!isDirty || isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView().accessibilityLabel("Saving")
                    } else {
                        Button("Save") { save() }
                            .disabled(!isDirty)
                    }
                }
            }
            .confirmationDialog(
                "Discard unsaved changes?",
                isPresented: $isConfirmingDiscard,
                titleVisibility: .visible
            ) {
                Button("Discard changes", role: .destructive) {
                    name = savedName
                    failure = nil
                }
                Button("Keep editing", role: .cancel) {}
            } message: {
                Text("Your changes will be lost. This cannot be undone.")
            }
    }

    private func save() {
        isSaving = true
        failure = nil
        Task {
            try? await Task.sleep(for: .seconds(1))
            if failsNextSave {
                failsNextSave = false
                failure = "Your changes could not be saved. Try again."
            } else {
                savedName = name
            }
            isSaving = false
        }
    }
}
