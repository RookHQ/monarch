import MonarchUI
import SwiftUI

struct MonarchInlineEditTextSection: CataloguePage {
    static let title = "Inline edit text"

    @State private var title = "Quarterly plan"
    @State private var summary = ""
    @State private var failingName = "Try to rename me"

    private struct NameTaken: LocalizedError {
        var errorDescription: String? { "That name is already in use." }
    }

    var body: some View {
        CatalogueCaption("Touch the text to edit. Saving takes a second. An empty title is refused.")
        MonarchInlineEditText(
            "Title",
            text: $title,
            validate: { $0.isEmpty ? "Enter a title." : nil },
            onSave: { _ in try await Task.sleep(for: .seconds(1)) }
        )

        CatalogueCaption("Several lines, empty with a prompt")
        MonarchInlineEditText("Summary", text: $summary, prompt: "Add a summary", isMultiline: true) { _ in
            try await Task.sleep(for: .seconds(1))
        }

        CatalogueCaption("A save that always fails keeps the editor open")
        MonarchInlineEditText("Name", text: $failingName) { _ in
            try await Task.sleep(for: .seconds(1))
            throw NameTaken()
        }

        CatalogueCaption("Disabled")
        MonarchInlineEditText("Owner", text: .constant("Read only")) { _ in }
            .disabled(true)
    }
}
