import MonarchUI
import SwiftUI

struct MonarchBadgeSection: CataloguePage {
    static let title = "Badge"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ForEach(MonarchBadgeVariant.allCases, id: \.self) { variant in
            CatalogueCaption(String(describing: variant))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: theme.spacing.space2) {
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchBadge(String(describing: tone), tone: tone, variant: variant)
                    }
                }
            }
        }
        CatalogueCaption("With an icon")
        MonarchBadge("Synced", tone: .success, systemImage: "checkmark")
    }
}
