@_spi(Experimental) import MonarchUI
import SwiftUI

/// A stand-in for the app's own failure.
private struct RenameError: Error {}

struct MonarchAiConversationHeaderSection: CataloguePage {
    static let title = "AI conversation header"

    private static let assistant = MonarchAiAgentRef(id: "research", name: "Research agent", codename: "Ra")

    @State private var title = "Quarterly planning notes"
    @State private var model = "Fast model"
    @State private var renameFails = true
    @State private var isNaming = true

    var body: some View {
        CatalogueCaption("Tap the title: rename (the first attempt fails), model and actions")
        MonarchAiConversationHeader(
            title,
            agent: Self.assistant,
            model: model,
            onRename: { newTitle in
                try await Task.sleep(for: .milliseconds(600))
                if renameFails {
                    renameFails = false
                    throw RenameError()
                }
                title = newTitle
            },
            actions: {
                Picker("Model", selection: $model) {
                    Text("Fast model").tag("Fast model")
                    Text("Reasoning model").tag("Reasoning model")
                }
                Button("Delete", systemImage: "trash", role: .destructive) {}
            }
        )

        CatalogueCaption("Naming, then untitled")
        MonarchAiConversationHeader("", agent: Self.assistant, isNaming: isNaming, onRename: { _ in })
        Toggle("Naming", isOn: $isNaming)

        CatalogueCaption("Plain title, no menu")
        MonarchAiConversationHeader("Trip ideas", model: "Fast model")
    }
}
