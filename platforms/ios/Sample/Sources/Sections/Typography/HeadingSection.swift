import MonarchUI
import SwiftUI

/// Style reuse: a heading is a `Text` with a heading role of
/// `monarchTextStyle(_:)` and the header trait.
struct HeadingSection: CataloguePage {
    static let title = "Heading (monarchTextStyle)"

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Heading roles, each marked as a header")
        Text("Display").monarchTextStyle(.display).accessibilityAddTraits(.isHeader)
        Text("Title").monarchTextStyle(.title).accessibilityAddTraits(.isHeader)
        Text("Heading").monarchTextStyle(.heading).accessibilityAddTraits(.isHeader)
        Text("Subheading").monarchTextStyle(.subheading).accessibilityAddTraits(.isHeader)
        Text("Body strong").monarchTextStyle(.bodyStrong).accessibilityAddTraits(.isHeader)
        Text("Label").monarchTextStyle(.label).accessibilityAddTraits(.isHeader)

        CatalogueCaption("With an outline level for the VoiceOver rotor")
        Text("Billing").monarchTextStyle(.title).accessibilityHeading(.h1)
        Text("Payment methods").monarchTextStyle(.heading).accessibilityHeading(.h2)

        CatalogueCaption("Muted")
        Text("Nothing here yet")
            .monarchTextStyle(.subheading)
            .foregroundStyle(theme.colors.textMuted)
            .accessibilityAddTraits(.isHeader)

        CatalogueCaption("Kept to one line")
        Text("A heading that is long enough to need more than one line on a phone")
            .monarchTextStyle(.heading)
            .lineLimit(1)
            .accessibilityAddTraits(.isHeader)
    }
}
