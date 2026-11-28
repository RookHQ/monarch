import MonarchUI
import SwiftUI

/// Native adoption: `VStack` with spacing from the Monarch space scale.
struct StackSection: CataloguePage {
    static let title = "Stack (VStack)"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("spacing: theme.spacing.space2")
        VStack(alignment: .leading, spacing: theme.spacing.space2) { rows }

        CatalogueCaption("spacing: theme.spacing.space6, centred")
        VStack(alignment: .center, spacing: theme.spacing.space6) { rows }
            .frame(maxWidth: .infinity)

        CatalogueCaption("With dividers between the children")
        VStack(alignment: .leading, spacing: theme.spacing.space3) {
            Text("Profile").monarchTextStyle(.body)
            MonarchDivider()
            Text("Billing").monarchTextStyle(.body)
            MonarchDivider()
            Text("Members").monarchTextStyle(.body)
        }
    }

    @ViewBuilder private var rows: some View {
        ForEach(["Profile", "Billing", "Members"], id: \.self) { row in
            Text(row)
                .monarchTextStyle(.label)
                .padding(theme.spacing.space2)
                .background(theme.colors.bgMuted, in: RoundedRectangle(cornerRadius: theme.radius.sm))
        }
    }
}
