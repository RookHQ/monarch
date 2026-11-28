import SwiftUI

/// The stage of a lightbox: one page per item, swiped between with the
/// system's paging `TabView`.
struct MonarchLightboxPager: View {
    let items: [MonarchLightboxItem]
    @Binding var selection: Int
    @Binding var zoom: Double
    let maximumZoom: Double
    let labels: MonarchLightboxLabels

    var body: some View {
        if items.isEmpty {
            MonarchLightboxNotice(text: labels.unavailable ?? MonarchLightboxStrings.unavailable)
        } else {
            #if os(iOS)
                TabView(selection: $selection) {
                    ForEach(items.indices, id: \.self) { index in
                        page(index).tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            #else
                // The paging style does not exist on macOS, which is only a
                // build convenience: the host build shows the current item.
                page(MonarchLightboxTokens.clampedIndex(selection, count: items.count))
            #endif
        }
    }

    @ViewBuilder
    private func page(_ index: Int) -> some View {
        let item = items[index]
        let isCurrent = index == selection
        switch item.kind {
        case .image:
            MonarchLightboxImagePage(
                item: item,
                // Zoom belongs to the item on screen; the others stay fitted.
                zoom: isCurrent ? $zoom : .constant(MonarchLightboxTokens.fitZoom),
                maximumZoom: maximumZoom,
                labels: labels
            )
        case .video:
            MonarchLightboxVideoPage(item: item, isCurrent: isCurrent, labels: labels)
        }
    }
}
