import MonarchUI
import SwiftUI

/// Native adoption: a maximum width and gutters by size class.
struct ContainerSection: CataloguePage {
    static let title = "Container (frame and size class)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.horizontalSizeClass) private var sizeClass

    /// The web's narrow width, 44rem, for reading and forms.
    private let narrowWidth: CGFloat = 704

    var body: some View {
        CatalogueCaption("Centred at a reading width, with 16-point gutters (24 in a regular width)")
        Text(
            "On a phone this text fills the width inside the gutters. On an iPad it stops growing at 704 points and stays centred."
        )
        .monarchTextStyle(.body)
        .padding(.horizontal, sizeClass == .regular ? theme.spacing.space6 : theme.spacing.space4)
        .frame(maxWidth: narrowWidth)
        .frame(maxWidth: .infinity)
        .background(theme.colors.bgMuted)

        CatalogueCaption("A part of the container's width: containerRelativeFrame")
        ScrollView(.horizontal) {
            HStack(spacing: theme.spacing.space4) {
                ForEach(1..<6, id: \.self) { index in
                    Text("Card \(index)")
                        .monarchTextStyle(.label)
                        .frame(maxWidth: .infinity, minHeight: theme.spacing.space16)
                        .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.md))
                        .containerRelativeFrame(.horizontal, count: 2, spacing: theme.spacing.space4)
                }
            }
        }
    }
}
