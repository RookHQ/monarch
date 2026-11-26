import MonarchTokens
import MonarchUI
import Observation

/// The appearance the catalogue is shown in.
@MainActor
@Observable
final class CatalogueSettings {
    var scheme: MonarchSchemePreference = .system
    var contrast: MonarchContrastPreference = .system
    var accent: MonarchAccent = MonarchTokens.Accents.rook
}
