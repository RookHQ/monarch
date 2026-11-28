import MonarchUI
import SwiftUI

struct MonarchAiImageGenerationSection: CataloguePage {
    static let title = "AI image generation"

    @State private var status: MonarchAiImageGenerationStatus = .done
    @State private var images = [
        MonarchAiGeneratedImage(id: "1", image: Image(systemName: "photo"), alt: "A lighthouse at dusk"),
        MonarchAiGeneratedImage(id: "2", image: Image(systemName: "photo.artframe")),
        MonarchAiGeneratedImage(id: "3", image: Image(systemName: "mountain.2")),
        MonarchAiGeneratedImage(id: "4", url: nil),
    ]
    @State private var chosen: String?
    @State private var lastAction = ""

    var body: some View {
        CatalogueCaption("Choose a stage")
        Picker("Stage", selection: $status) {
            Text("Pending").tag(MonarchAiImageGenerationStatus.pending)
            Text("Refining").tag(MonarchAiImageGenerationStatus.progressive)
            Text("Done").tag(MonarchAiImageGenerationStatus.done)
            Text("Error").tag(MonarchAiImageGenerationStatus.error)
        }
        .pickerStyle(.segmented)

        MonarchAiImageGeneration(
            images: $images,
            status: status,
            progress: status == .pending ? 0.35 : 0.8,
            selection: $chosen,
            onVariations: { id in lastAction = "More like image \(id)" },
            onRetry: { status = .pending }
        )
        if !lastAction.isEmpty {
            Text(lastAction).monarchTextStyle(.caption)
        }
    }
}
