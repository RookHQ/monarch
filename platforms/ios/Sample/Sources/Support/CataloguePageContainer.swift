import MonarchUI
import SwiftUI

/// The frame around a page: a scrolling column on the theme's canvas.
struct CataloguePageContainer<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                content
            }
            .foregroundStyle(theme.colors.textDefault)
            .padding(theme.spacing.space4)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(theme.colors.bgCanvas)
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
    }
}
