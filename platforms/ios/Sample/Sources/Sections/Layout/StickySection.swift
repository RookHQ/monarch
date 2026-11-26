import MonarchUI
import SwiftUI

/// Native adoption: pinned section headers and safe-area insets.
struct StickySection: CataloguePage {
    static let title = "Sticky (pinned views and safeAreaInset)"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Section headers pin to the top; the action bar stays at the bottom")
        ScrollView {
            LazyVStack(alignment: .leading, spacing: theme.spacing.space2, pinnedViews: [.sectionHeaders]) {
                ForEach(["January", "February", "March"], id: \.self) { month in
                    Section {
                        ForEach(1..<9, id: \.self) { day in
                            Text("\(month) \(day)")
                                .monarchTextStyle(.body)
                                .padding(.horizontal, theme.spacing.space3)
                        }
                    } header: {
                        Text(month)
                            .monarchTextStyle(.label)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(theme.spacing.space3)
                            .background(theme.colors.bgSurface)
                            .accessibilityAddTraits(.isHeader)
                    }
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            MonarchButton("Add an entry", variant: .primary, fullWidth: true) {}
                .padding(theme.spacing.space3)
                .background(.bar)
        }
        .frame(height: theme.spacing.space24 * 4)
        .clipShape(RoundedRectangle(cornerRadius: theme.radius.md))
        .overlay {
            RoundedRectangle(cornerRadius: theme.radius.md).strokeBorder(theme.colors.borderDefault)
        }
    }
}
