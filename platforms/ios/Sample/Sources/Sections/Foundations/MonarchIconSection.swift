import MonarchUI
import SwiftUI

struct MonarchIconSection: CataloguePage {
    static let title = "Icon"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        HStack(spacing: theme.spacing.space3) {
            ForEach(MonarchIconSize.allCases, id: \.self) { size in
                MonarchIcon(systemName: "tray.full", label: "Inbox, \(String(describing: size))", size: size)
            }
        }
        CatalogueCaption("In the accent colour")
        MonarchIcon(systemName: "star.fill", label: "Favourite", size: .lg)
            .foregroundStyle(theme.colors.accentSolid)
    }
}
