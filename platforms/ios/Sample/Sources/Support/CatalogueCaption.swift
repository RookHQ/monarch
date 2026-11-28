import MonarchUI
import SwiftUI

/// A small heading above a group of examples on a page.
struct CatalogueCaption: View {
    let text: String

    @Environment(\.monarchTheme) private var theme

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityAddTraits(.isHeader)
    }
}
