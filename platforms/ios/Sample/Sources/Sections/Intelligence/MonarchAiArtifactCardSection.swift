import MonarchUI
import SwiftUI

struct MonarchAiArtifactCardSection: CataloguePage {
    static let title = "AI artifact card"

    @State private var shown: MonarchAiArtifactKind? = .chart

    var body: some View {
        CatalogueCaption("Tap a card to show it")
        ForEach(MonarchAiArtifactKind.allCases, id: \.self) { kind in
            MonarchAiArtifactCard(
                "Q3 summary",
                kind: kind,
                version: 3,
                description: kind == .chart ? "Updated just now" : nil,
                isActive: shown == kind
            ) {
                shown = kind
            }
        }

        CatalogueCaption("Disabled")
        MonarchAiArtifactCard("parser.swift", kind: .code, version: 1) {}
            .disabled(true)
    }
}
