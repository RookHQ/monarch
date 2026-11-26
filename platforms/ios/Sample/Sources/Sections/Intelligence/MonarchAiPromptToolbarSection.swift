import MonarchUI
import SwiftUI

struct MonarchAiPromptToolbarSection: CataloguePage {
    static let title = "AI prompt toolbar"

    @Environment(\.monarchTheme) private var theme
    @State private var searchesWeb = false
    @State private var deepResearch = true
    @State private var mode = "ask"
    @State private var lastAction = ""

    var body: some View {
        CatalogueCaption("Toggles and buttons under a prompt")
        MonarchAiPromptToolbar {
            Toggle("Search the web", systemImage: "globe", isOn: $searchesWeb)
            Toggle("Deep research", systemImage: "books.vertical", isOn: $deepResearch)
            Button("Templates", systemImage: "list.bullet") { lastAction = "Templates opened" }
            Button("Attach a file", systemImage: "paperclip") { lastAction = "Attach opened" }
                .labelStyle(.iconOnly)
        }
        if !lastAction.isEmpty {
            Text(lastAction)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }

        CatalogueCaption("With the mode switcher")
        MonarchAiPromptToolbar {
            MonarchAiModeSwitcher(selection: $mode, showsDescription: false)
            Toggle("Search the web", isOn: $searchesWeb)
        }

        CatalogueCaption("Disabled")
        MonarchAiPromptToolbar {
            Toggle("Search the web", isOn: .constant(true))
            Button("Templates") {}
        }
        .disabled(true)
    }
}
