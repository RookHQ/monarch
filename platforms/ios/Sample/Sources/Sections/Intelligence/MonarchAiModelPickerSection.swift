import MonarchUI
import SwiftUI

struct MonarchAiModelPickerSection: CataloguePage {
    static let title = "AI model picker"

    @State private var modelID: String? = "rook-large"
    @State private var emptyID: String?
    @State private var keyRequest = "No key requested"

    private let models = [
        MonarchAiModelOption(
            id: "rook-large",
            provider: "Rook",
            name: "Rook Large",
            description: "Best for long documents",
            capabilities: [.vision, .tools, .reasoning],
            contextTokens: 200_000,
            speed: .balanced,
            costTier: 3
        ),
        MonarchAiModelOption(
            id: "rook-fast",
            provider: "Rook",
            name: "Rook Fast",
            capabilities: [.tools],
            contextTokens: 32_000,
            speed: .fast,
            costTier: 1
        ),
        MonarchAiModelOption(id: "rook-legacy", provider: "Rook", name: "Rook Legacy", isDisabled: true),
        MonarchAiModelOption(
            id: "hosted",
            provider: "Workspace provider",
            name: "Your hosted model",
            capabilities: [.web],
            requiresKey: true,
            isKeyConfigured: true
        ),
        MonarchAiModelOption(
            id: "hosted-2",
            provider: "Workspace provider",
            name: "Second hosted model",
            requiresKey: true
        ),
    ]

    var body: some View {
        CatalogueCaption("Selected")
        MonarchAiModelPicker(selection: $modelID, models: models) { model in
            keyRequest = "Add a key for \(model.name)"
        }
        Text(keyRequest).monarchTextStyle(.caption)

        CatalogueCaption("Nothing selected, small")
        MonarchAiModelPicker(selection: $emptyID, models: models, size: .sm)
    }
}
