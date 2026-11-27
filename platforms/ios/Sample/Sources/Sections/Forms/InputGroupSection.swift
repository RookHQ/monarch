import MonarchUI
import SwiftUI

/// Recipe: text and a button attached to a standard `TextField` in one bordered box.
struct InputGroupSection: CataloguePage {
    static let title = "Input group (recipe)"

    @State private var address = ""
    @State private var weight = ""
    @State private var copyCount = 0

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        CatalogueCaption("Text before the field and a button after it. Copied \(copyCount) times.")
        group(label: "Website") {
            Text("https://")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
            TextField("Website", text: $address, prompt: Text("example.com"))
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            MonarchButton("Copy", systemImage: "doc.on.doc", variant: .ghost, size: .sm) {
                copyCount += 1
            }
        }

        CatalogueCaption("A unit after the field")
        group(label: "Weight") {
            TextField("Weight", text: $weight, prompt: Text("0"))
                .textFieldStyle(.plain)
                .monarchTextStyle(.body)
                .keyboardType(.decimalPad)
            Text("kg")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func group(label: String, @ViewBuilder content: () -> some View) -> some View {
        let shape = RoundedRectangle(cornerRadius: theme.radius.md, style: .continuous)
        return VStack(alignment: .leading, spacing: theme.spacing.space1_5) {
            Text(label).monarchTextStyle(.label)
            HStack(spacing: theme.spacing.space2) {
                content()
            }
            .padding(.horizontal, theme.spacing.space3)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .background(shape.fill(theme.colors.bgSurface))
            .overlay(shape.strokeBorder(theme.colors.borderInput, lineWidth: theme.spacing.px))
        }
    }
}
