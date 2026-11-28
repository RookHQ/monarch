import MonarchUI
import SwiftUI

/// Native adoption: the system edit menu over a text selection, with
/// selection actions above the keyboard.
struct BubbleMenuSection: CataloguePage {
    static let title = "Bubble menu (system edit menu)"

    @State private var notes = "Select a few words in this text to see the system edit menu."

    var body: some View {
        CatalogueCaption("Plain text: the system menu (Cut, Copy, Paste, Look Up, Translate, Share) needs no code")
        TextEditor(text: $notes)
            .monarchTextStyle(.body)
            .frame(minHeight: 120)

        CatalogueCaption("Rich text: the menu adds Format; a Comment action waits above the keyboard")
        if #available(iOS 26.0, *) {
            BubbleMenuSelectionEditor()
        } else {
            Text("Rich text selection needs iOS 26.")
                .monarchTextStyle(.caption)
        }
    }
}

@available(iOS 26.0, *)
private struct BubbleMenuSelectionEditor: View {
    @State private var text = AttributedString("Select any words in this paragraph, then comment on them.")
    @State private var selection = AttributedTextSelection()
    @State private var quoted = ""

    private var selectedText: String {
        guard case .ranges(let ranges) = selection.indices(in: text) else { return "" }
        return String(text[ranges].characters)
    }

    var body: some View {
        VStack(alignment: .leading) {
            TextEditor(text: $text, selection: $selection)
                .monarchTextStyle(.body)
                .frame(minHeight: 120)
                .toolbar {
                    ToolbarItemGroup(placement: .keyboard) {
                        Button("Comment", systemImage: "text.bubble") { quoted = selectedText }
                            .buttonStyle(.monarch(.ghost, size: .sm))
                            .disabled(selectedText.isEmpty)
                    }
                }
            Text(quoted.isEmpty ? "No comment yet." : "Commenting on “\(quoted)”")
                .monarchTextStyle(.caption)
        }
    }
}
