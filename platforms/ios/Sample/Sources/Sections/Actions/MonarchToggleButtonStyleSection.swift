import MonarchUI
import SwiftUI

struct MonarchToggleButtonStyleSection: CataloguePage {
    static let title = "ToggleButton"

    @Environment(\.monarchTheme) private var theme
    @State private var isBold = true
    @State private var isItalic = false
    @State private var showsArchived = false
    @State private var usesWeb = true

    var body: some View {
        CatalogueCaption("Ghost, icons only")
        HStack(spacing: theme.spacing.space1) {
            Toggle("Bold", systemImage: "bold", isOn: $isBold)
            Toggle("Italic", systemImage: "italic", isOn: $isItalic)
        }
        .labelStyle(.iconOnly)
        .toggleStyle(.monarchButton())

        CatalogueCaption("Outline, every size")
        ForEach(MonarchButtonSize.allCases, id: \.self) { size in
            Toggle("Show archived", isOn: $showsArchived)
                .toggleStyle(.monarchButton(.outline, size: size))
        }

        CatalogueCaption("Disabled")
        HStack(spacing: theme.spacing.space2) {
            Toggle("Off", isOn: .constant(false))
            Toggle("On", isOn: .constant(true))
        }
        .toggleStyle(.monarchButton(.outline))
        .disabled(true)

        CatalogueCaption("The AI tone, for an AI control")
        Toggle("Web search", systemImage: "globe", isOn: $usesWeb)
            .toggleStyle(.monarchButton(.outline, tone: .ai))

        CatalogueCaption("Bold: \(isBold ? "on" : "off"), italic: \(isItalic ? "on" : "off")")
    }
}
