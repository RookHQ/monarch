import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchAppearancePanel``.
enum MonarchAppearancePanelTokens {
    /// Space above and below each row.
    static let rowPadding: CGFloat = MonarchTokens.Space.space4
    /// Space between a row's label and its description.
    static let textSpacing: CGFloat = MonarchTokens.Space.space1
    /// Space between a row's text and its control.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space3
    /// Space under the panel's title.
    static let titleSpacing: CGFloat = MonarchTokens.Space.space2

    /// Contrast choices in the web's order.
    static let contrastOrder: [MonarchContrastPreference] = [.standard, .more, .system]
    /// Motion choices in the web's order.
    static let motionOrder: [MonarchAppearanceMotion] = [.system, .reduce]
    /// Density choices in the web's order.
    static let densityOrder: [MonarchAppearanceDensity] = [.comfortable, .compact]

    /// The rows to draw: the requested ones in order, each once.
    static func visibleRows(_ rows: [MonarchAppearancePanelRow]) -> [MonarchAppearancePanelRow] {
        var seen = Set<MonarchAppearancePanelRow>()
        return rows.filter { seen.insert($0).inserted }
    }

    /// The label of a row.
    static func title(_ section: MonarchAppearancePanelRow, labels: MonarchAppearancePanelLabels) -> String {
        switch section {
        case .theme: labels.theme ?? MonarchAppearancePanelStrings.theme
        case .contrast: labels.contrast ?? MonarchAppearancePanelStrings.contrast
        case .accent: labels.accent ?? MonarchAppearancePanelStrings.accent
        case .motion: labels.motion ?? MonarchAppearancePanelStrings.motion
        case .density: labels.density ?? MonarchAppearancePanelStrings.density
        }
    }

    /// The one-line description of a row.
    static func description(_ section: MonarchAppearancePanelRow, labels: MonarchAppearancePanelLabels) -> String {
        switch section {
        case .theme: labels.themeDescription ?? MonarchAppearancePanelStrings.themeDescription
        case .contrast: labels.contrastDescription ?? MonarchAppearancePanelStrings.contrastDescription
        case .accent: labels.accentDescription ?? MonarchAppearancePanelStrings.accentDescription
        case .motion: labels.motionDescription ?? MonarchAppearancePanelStrings.motionDescription
        case .density: labels.densityDescription ?? MonarchAppearancePanelStrings.densityDescription
        }
    }

    /// The name of a contrast choice.
    static func name(_ contrast: MonarchContrastPreference, labels: MonarchAppearancePanelLabels) -> String {
        switch contrast {
        case .standard: labels.contrastStandard ?? MonarchAppearancePanelStrings.contrastStandard
        case .more: labels.contrastMore ?? MonarchAppearancePanelStrings.contrastMore
        case .system: labels.contrastSystem ?? MonarchAppearancePanelStrings.contrastSystem
        }
    }

    /// The name of a motion choice.
    static func name(_ motion: MonarchAppearanceMotion, labels: MonarchAppearancePanelLabels) -> String {
        switch motion {
        case .system: labels.motionSystem ?? MonarchAppearancePanelStrings.motionSystem
        case .reduce: labels.motionReduce ?? MonarchAppearancePanelStrings.motionReduce
        }
    }

    /// The name of a density choice.
    static func name(_ density: MonarchAppearanceDensity, labels: MonarchAppearancePanelLabels) -> String {
        switch density {
        case .comfortable: labels.densityComfortable ?? MonarchAppearancePanelStrings.densityComfortable
        case .compact: labels.densityCompact ?? MonarchAppearancePanelStrings.densityCompact
        }
    }

    /// The labels handed to the theme row's ``MonarchThemeToggle``: the row
    /// label names the toggle, and unset theme names keep the toggle's own.
    static func themeToggleLabels(_ labels: MonarchAppearancePanelLabels) -> MonarchThemeToggleLabels {
        MonarchThemeToggleLabels(
            title: title(.theme, labels: labels),
            light: labels.light,
            dark: labels.dark,
            system: labels.system
        )
    }

    /// The accents offered: the given ones, each id once, with the current
    /// accent added at the end when it is not among them, so the menu always
    /// shows the current choice.
    static func accentChoices(_ accents: [MonarchAccent], current: MonarchAccent) -> [MonarchAccent] {
        var seen = Set<String>()
        var choices = accents.filter { seen.insert($0.id).inserted }
        if !seen.contains(current.id) { choices.append(current) }
        return choices
    }

    /// The accent with an id among the choices, or `nil`.
    static func accent(id: String, in choices: [MonarchAccent]) -> MonarchAccent? {
        choices.first { $0.id == id }
    }
}
