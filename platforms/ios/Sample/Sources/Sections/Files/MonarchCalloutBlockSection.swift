import MonarchUI
import SwiftUI

struct MonarchCalloutBlockSection: CataloguePage {
    static let title = "CalloutBlock"

    @State private var tone = MonarchTone.info

    var body: some View {
        CatalogueCaption("Tone")
        Picker("Tone", selection: $tone) {
            ForEach(MonarchTone.allCases, id: \.self) { tone in
                Text(String(describing: tone)).tag(tone)
            }
        }
        .pickerStyle(.segmented)
        MonarchCalloutBlock("Back up the workspace before you migrate. The migration cannot be undone.", tone: tone)
        CatalogueCaption("Custom icon and mixed content")
        MonarchCalloutBlock(systemImage: "lightbulb", tone: .info, toneLabel: "Tip") {
            Text("Before you migrate").monarchTextStyle(.bodyStrong)
            Text("Callouts hold any content: paragraphs, lists, a button.")
            Button("Read the guide") {}
                .buttonStyle(.monarch(.secondary, size: .sm))
        }
    }
}
