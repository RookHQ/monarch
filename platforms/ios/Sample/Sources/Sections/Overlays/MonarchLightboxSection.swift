import MonarchUI
import SwiftUI

struct MonarchLightboxSection: CataloguePage {
    static let title = "Lightbox"

    @Environment(\.monarchTheme) private var theme
    @State private var isShown = false
    @State private var index = 0
    @State private var isLooping = false

    private let symbols = ["photo", "chart.bar", "map", "building.2"]

    private var items: [MonarchLightboxItem] {
        [
            MonarchLightboxItem(
                image: Image(systemName: "photo"),
                alt: "A placeholder photograph",
                caption: "Pinch or double-tap to zoom, drag to pan"
            ),
            MonarchLightboxItem(
                image: Image(systemName: "chart.bar"),
                alt: "Bar chart of weekly sign-ups",
                caption: "Sign-ups by week",
                shareURL: URL(string: "https://example.com/sign-ups.png")
            ),
            MonarchLightboxItem(image: Image(systemName: "map"), alt: "Map of the office"),
            MonarchLightboxItem(image: nil, alt: "A picture that was deleted", caption: "A missing file says so"),
        ]
    }

    var body: some View {
        CatalogueCaption("Tap a thumbnail to open the viewer at that item")
        HStack(spacing: theme.spacing.space2) {
            ForEach(symbols.indices, id: \.self) { position in
                Button {
                    index = position
                    isShown = true
                } label: {
                    MonarchIcon(systemName: symbols[position], size: .lg)
                }
                .buttonStyle(.monarch(.secondary, size: .lg))
                .accessibilityLabel(items[position].alt)
            }
        }
        Toggle("Loop from the last item to the first", isOn: $isLooping)
            .monarchTextStyle(.body)
        Text("Last viewed: \(index + 1) of \(items.count)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .monarchLightbox(isPresented: $isShown, items: items, selection: $index, isLooping: isLooping)
    }
}
