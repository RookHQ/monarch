import MonarchFontsPoppins
import MonarchUI
import SwiftUI

/// The catalogue: every Monarch component, in any theme and accent.
@main
struct MonarchCatalogueApp: App {
    @State private var settings = CatalogueSettings()

    var body: some Scene {
        WindowGroup {
            // `-MonarchShowcase YES` opens the docs landing page's showcase screen instead of the catalogue.
            if UserDefaults.standard.bool(forKey: "MonarchShowcase") {
                ShowcaseApp()
                    .monarchTheme(scheme: .system, contrast: .standard, typography: .poppins)
            } else {
                CatalogueView()
                    .environment(settings)
                    .monarchTheme(scheme: settings.scheme, contrast: settings.contrast, accent: settings.accent)
            }
        }
    }
}
