import MonarchTokens
import MonarchUI
import SwiftUI

/// Pickers for the scheme, the contrast and the accent. These are standard
/// `Picker`s; inside the theme they take the accent from the tint.
struct AppearanceControls: View {
    @Environment(CatalogueSettings.self) private var settings
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        @Bindable var settings = settings
        Picker("Scheme", selection: $settings.scheme) {
            Text("System").tag(MonarchSchemePreference.system)
            Text("Light").tag(MonarchSchemePreference.light)
            Text("Dark").tag(MonarchSchemePreference.dark)
        }
        Picker("Contrast", selection: $settings.contrast) {
            Text("System").tag(MonarchContrastPreference.system)
            Text("Standard").tag(MonarchContrastPreference.standard)
            Text("More").tag(MonarchContrastPreference.more)
        }
        Picker("Accent", selection: $settings.accent) {
            ForEach(MonarchTokens.Accents.all, id: \.id) { accent in
                Text(accent.label).tag(accent)
            }
        }
        LabeledContent("Resolved theme", value: theme.id)
    }
}
