import MonarchUI
import SwiftUI

struct MonarchKbdSection: CataloguePage {
    static let title = "Kbd"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Always visible")
        HStack(spacing: theme.spacing.space2) {
            MonarchKbd("K", isAlwaysVisible: true)
            MonarchKbd("⌘K", isAlwaysVisible: true)
            MonarchKbd("⇧⌘P", isAlwaysVisible: true)
            MonarchKbd("Esc", isAlwaysVisible: true)
            MonarchKbd("↩", isAlwaysVisible: true)
        }

        CatalogueCaption("Shown only while a hardware keyboard is attached")
        HStack {
            Text("Search").monarchTextStyle(.body)
            Spacer()
            MonarchKbd("⌘F")
        }
        HStack {
            Text("New invoice").monarchTextStyle(.body)
            Spacer()
            MonarchKbd("⌘N")
        }

        CatalogueCaption("A list of shortcuts")
        ForEach(["⌘S": "Save", "⌘Z": "Undo", "⇧⌘Z": "Redo"].sorted(by: { $0.value < $1.value }), id: \.key) { item in
            LabeledContent(item.value) {
                MonarchKbd(item.key, isAlwaysVisible: true)
            }
            .monarchTextStyle(.body)
        }
    }
}
