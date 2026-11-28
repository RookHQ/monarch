import MonarchUI
import SwiftUI

struct MonarchInlineCodeStyleSection: CataloguePage {
    static let title = "Inline code"

    @State private var isPlain = false

    @Environment(\.monarchTheme) private var theme

    private var style: MonarchInlineCodeStyle { isPlain ? .plain : .subtle }

    var body: some View {
        Toggle("Plain", isOn: $isPlain)

        CatalogueCaption("In running text, from Markdown")
        Text(style.markdown("Run `swift build` in `platforms/ios` before you hand over.", in: theme))
            .monarchTextStyle(.body)

        CatalogueCaption("In other type roles")
        Text(style.markdown("The `monarchTheme` modifier", in: theme, textStyle: .heading))
            .monarchTextStyle(.heading)
        Text(style.markdown("Set `isLoading` to show progress.", in: theme, textStyle: .caption))
            .monarchTextStyle(.caption)

        CatalogueCaption("A fragment appended to text")
        Text(AttributedString("Exit code ") + style.attributedString("127", in: theme))
            .monarchTextStyle(.body)

        CatalogueCaption("A long token")
        Text(
            style.markdown(
                "Checksum `9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08` matches.",
                in: theme
            )
        )
        .monarchTextStyle(.body)
    }
}
