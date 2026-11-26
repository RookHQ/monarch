import MonarchUI
import SwiftUI

struct MonarchProseStyleSection: CataloguePage {
    static let title = "Prose"

    @State private var size = MonarchProseSize.md
    @State private var limitsLineLength = true

    @Environment(\.monarchTheme) private var theme

    private var style: MonarchProseStyle {
        MonarchProseStyle(size: size, limitsLineLength: limitsLineLength)
    }

    var body: some View {
        Picker("Size", selection: $size) {
            Text("sm").tag(MonarchProseSize.sm)
            Text("md").tag(MonarchProseSize.md)
            Text("lg").tag(MonarchProseSize.lg)
        }
        .pickerStyle(.segmented)
        Toggle("Reading measure", isOn: $limitsLineLength)
            .monarchTextStyle(.body)

        CatalogueCaption("A rendered article")
        MonarchProse(style) {
            Text("Getting started").monarchProseBlock(.heading(2))
            Text(
                style.markdown(
                    "This guide covers the basics. Read the [overview](https://example.com) first, then run `npm install`.",
                    in: theme
                )
            )
            Text("Create a project").monarchProseBlock(.listItem(marker: "•"))
            Text("Invite your team").monarchProseBlock(.listItem(marker: "•"))
            Text("Set up notifications").monarchProseBlock(.listItem(marker: "•"))
            Text("Email").monarchProseBlock(.listItem(marker: "◦", depth: 1))
            Text("Mobile").monarchProseBlock(.listItem(marker: "◦", depth: 1))
            Text("Small steps every day add up.").monarchProseBlock(.quote)
            Text("const total = items.reduce((sum, i) => sum + i.price, 0);").monarchProseBlock(.code)
            Text("Options").monarchProseBlock(.heading(3))
            Text(
                style.markdown("`size` sets the base size of the content; `measure` limits the line length.", in: theme)
            )
            Divider().monarchProseBlock(.rule)
            Text(style.markdown("Press **Return** to save.", in: theme))
        }

        CatalogueCaption("Small size for comments")
        MonarchProse(MonarchProseStyle(size: .sm, limitsLineLength: false)) {
            Text(
                style.markdown(
                    "Looks good to me. One question about the **second step**: should it run before the import?",
                    in: theme
                )
            )
            Text("Check the file").monarchProseBlock(.listItem(marker: "1."))
            Text("Import it").monarchProseBlock(.listItem(marker: "2."))
        }
    }
}
