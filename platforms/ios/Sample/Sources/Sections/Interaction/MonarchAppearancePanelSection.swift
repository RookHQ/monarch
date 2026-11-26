import MonarchUI
import SwiftUI

struct MonarchAppearancePanelSection: CataloguePage {
    static let title = "Appearance panel"

    @State private var preferences = MonarchAppearancePreferences()
    @Environment(\.accessibilityReduceMotion) private var systemReducesMotion

    var body: some View {
        CatalogueCaption("Every row, applied to the preview below")
        MonarchAppearancePanel(
            "Appearance",
            selection: $preferences,
            rows: [.theme, .contrast, .accent, .motion, .density]
        )
        AppearancePanelDemoCard(
            reducesMotion: preferences.reducesMotion(systemReducesMotion: systemReducesMotion),
            isCompact: preferences.density == .compact
        )
        .monarchTheme(preferences.appearance)
        CatalogueCaption("The web's four rows")
        MonarchAppearancePanel(selection: $preferences)
    }
}

/// A card that reads the theme installed around it and shows the motion and density choices.
private struct AppearancePanelDemoCard: View {
    let reducesMotion: Bool
    let isCompact: Bool

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: isCompact ? theme.spacing.space1 : theme.spacing.space3) {
            Text("This card uses the chosen theme.")
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            Text(reducesMotion ? "Motion: reduced" : "Motion: standard")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            MonarchButton("Accent button", variant: .primary) {}
        }
        .padding(isCompact ? theme.spacing.space2 : theme.spacing.space4)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
        .overlay(RoundedRectangle(cornerRadius: theme.radius.lg).stroke(theme.colors.borderDefault))
    }
}
