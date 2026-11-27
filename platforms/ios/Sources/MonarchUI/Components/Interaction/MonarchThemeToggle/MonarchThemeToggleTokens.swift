import SwiftUI

/// Component tokens and pure logic for ``MonarchThemeToggle``.
enum MonarchThemeToggleTokens {
    /// The choices in the order they are shown, as on the web.
    static let order: [MonarchSchemePreference] = [.light, .dark, .system]

    /// The SF Symbol of a choice.
    static func systemImage(_ preference: MonarchSchemePreference) -> String {
        switch preference {
        case .light: "sun.max"
        case .dark: "moon"
        case .system: "desktopcomputer"
        }
    }

    /// The shown and spoken name of a choice, with the overrides applied.
    static func name(_ preference: MonarchSchemePreference, labels: MonarchThemeToggleLabels) -> String {
        switch preference {
        case .light: labels.light ?? MonarchThemeToggleStrings.light
        case .dark: labels.dark ?? MonarchThemeToggleStrings.dark
        case .system: labels.system ?? MonarchThemeToggleStrings.system
        }
    }

    /// The name of the control, with the override applied.
    static func title(labels: MonarchThemeToggleLabels) -> String {
        labels.title ?? MonarchThemeToggleStrings.title
    }

    /// The control size of the segmented picker. `md` asks for the large
    /// system control, `sm` for the regular one; `.small` is not used, so the
    /// segments stay a comfortable touch size.
    static func controlSize(_ size: MonarchThemeToggleSize) -> ControlSize {
        switch size {
        case .sm: .regular
        case .md: .large
        }
    }

    /// The button size of the menu variant.
    static func buttonSize(_ size: MonarchThemeToggleSize) -> MonarchButtonSize {
        switch size {
        case .sm: .sm
        case .md: .md
        }
    }

    /// The stored text of a choice: `light`, `dark` or `system`, the values
    /// the web toggle writes to its cookie.
    static func storageValue(_ preference: MonarchSchemePreference) -> String {
        switch preference {
        case .light: "light"
        case .dark: "dark"
        case .system: "system"
        }
    }

    /// The choice for a stored text, or `nil` if the text is not one of the three values.
    static func preference(storageValue: String?) -> MonarchSchemePreference? {
        order.first { storageValue == Self.storageValue($0) }
    }
}
