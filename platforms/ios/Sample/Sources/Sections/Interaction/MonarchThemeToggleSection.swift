import MonarchUI
import SwiftUI

struct MonarchThemeToggleSection: CataloguePage {
    static let title = "Theme toggle"

    @State private var scheme: MonarchSchemePreference = .system
    @AppStorage("catalogue.themeToggle.demo") private var stored = "system"

    var body: some View {
        CatalogueCaption("Segmented, applied to the preview below")
        MonarchThemeToggle(selection: $scheme)
        ThemeToggleDemoCard()
            .monarchTheme(scheme: scheme)
        CatalogueCaption("Menu, kept in UserDefaults")
        MonarchThemeToggle(
            selection: Binding(
                get: { MonarchThemeToggle.preference(storageValue: stored) ?? .system },
                set: { stored = MonarchThemeToggle.storageValue(for: $0) }
            ),
            variant: .menu
        )
        Text("Stored value: \(stored)")
            .monarchTextStyle(.caption)
        CatalogueCaption("Small, icon only")
        HStack {
            MonarchThemeToggle(selection: $scheme, size: .sm)
            MonarchThemeToggle(selection: $scheme, variant: .menu, size: .sm, showsLabel: false)
        }
    }
}

/// A card that reads the theme installed around it.
private struct ThemeToggleDemoCard: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text("This card uses the chosen scheme.")
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textDefault)
            .padding(theme.spacing.space4)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
            .overlay(RoundedRectangle(cornerRadius: theme.radius.lg).stroke(theme.colors.borderDefault))
    }
}
