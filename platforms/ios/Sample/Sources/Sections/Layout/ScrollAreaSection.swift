import MonarchUI
import SwiftUI

/// Native adoption: the standard `ScrollView`, with the system's indicators.
struct ScrollAreaSection: CataloguePage {
    static let title = "Scroll area (ScrollView)"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("A panel that scrolls inside the page")
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                ForEach(1..<21, id: \.self) { line in
                    Text("Log line \(line)").monarchTextStyle(.code)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(theme.spacing.space3)
        }
        .scrollIndicators(.visible)
        .scrollIndicatorsFlash(onAppear: true)
        .frame(height: theme.spacing.space24 * 2)
        .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))

        CatalogueCaption("Sideways, without indicators")
        ScrollView(.horizontal) {
            HStack(spacing: theme.spacing.space2) {
                ForEach(1..<13, id: \.self) { index in
                    MonarchBadge("Tag \(index)")
                }
            }
        }
        .scrollIndicators(.hidden)

        CatalogueCaption("Short content does not bounce: scrollBounceBehavior(.basedOnSize)")
        ScrollView {
            Text("Two lines of content\nthat fit the panel.")
                .monarchTextStyle(.body)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(theme.spacing.space3)
        }
        .scrollBounceBehavior(.basedOnSize)
        .frame(height: theme.spacing.space24)
        .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
    }
}
