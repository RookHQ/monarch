import MonarchUI
import SwiftUI

/// Native adoption: `confirmationDialog` with one button per option.
struct ChoiceDialogSection: CataloguePage {
    static let title = "Choice dialog (confirmationDialog)"

    @State private var isChoosing = false
    @State private var choice = "Nothing chosen yet"

    var body: some View {
        Text(choice).monarchTextStyle(.bodyStrong)
        MonarchButton("Share the document", variant: .primary) { isChoosing = true }
            .confirmationDialog(
                "Who can open this document?",
                isPresented: $isChoosing,
                titleVisibility: .visible,
                actions: {
                    Button("Everyone in the workspace") { choice = "Shared with the workspace" }
                    Button("Only people I invite") { choice = "Private" }
                    Button("Cancel", role: .cancel) {}
                },
                message: {
                    Text("You can change this later in the document's settings.")
                }
            )
    }
}
