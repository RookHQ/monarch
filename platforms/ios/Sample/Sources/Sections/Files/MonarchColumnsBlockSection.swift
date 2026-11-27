import MonarchUI
import SwiftUI

struct MonarchColumnsBlockSection: CataloguePage {
    static let title = "Columns block"

    @Environment(\.monarchTheme) private var theme
    @State private var ratio = 0.4
    @State private var isThree = false

    var body: some View {
        Slider(value: $ratio, in: 0.2...0.8) { Text("First column share") }
        Toggle("Three columns", isOn: $isThree)
            .monarchTextStyle(.body)
        CatalogueCaption("Side by side from 480 points wide; stacked on a phone in portrait")
        MonarchColumnsBlock(ratio: ratio) {
            column("Before", "Sign-ups came from search and a few partner links.")
            column("After", "Sign-ups doubled after the launch, mostly from the new referral page.")
            if isThree {
                column("Next", "Add the referral link to every receipt.")
            }
        }
    }

    private func column(_ title: String, _ text: String) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space1) {
            Text(title).monarchTextStyle(.bodyStrong)
            Text(text).monarchTextStyle(.body)
        }
        .padding(theme.spacing.space3)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors.bgSubtle, in: RoundedRectangle(cornerRadius: theme.radius.md))
    }
}
