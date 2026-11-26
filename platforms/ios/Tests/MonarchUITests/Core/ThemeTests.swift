import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchThemeFixtures: ComponentFixtures {
    static let id = "theme"
    static let fixtures = [
        Fixture("every accent on a themed surface") {
            VStack(alignment: .leading) {
                ForEach(MonarchTokens.Accents.all, id: \.id) { accent in
                    AccentSwatch(label: accent.label).monarchTheme(accent: accent)
                }
            }
        }
    ]
}

private struct AccentSwatch: View {
    let label: String
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text(label)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors.accentOnSolid)
            .padding(theme.spacing.space2)
            .background(theme.colors.accentSolid)
    }
}

@Suite("Theme")
@MainActor
struct ThemeTests {
    @Test(
        "Appearance resolves to one of the four themes",
        arguments: [
            (MonarchSchemePreference.system, MonarchContrastPreference.system, false, false, "light"),
            (.system, .system, true, false, "dark"),
            (.system, .system, false, true, "hc-light"),
            (.system, .system, true, true, "hc-dark"),
            (.light, .system, true, false, "light"),
            (.dark, .system, false, false, "dark"),
            (.dark, .system, false, true, "hc-dark"),
            (.system, .standard, true, true, "dark"),
            (.system, .more, false, false, "hc-light"),
            (.light, .standard, true, true, "light"),
            (.dark, .more, false, false, "hc-dark"),
        ]
    )
    func resolution(
        scheme: MonarchSchemePreference,
        contrast: MonarchContrastPreference,
        systemIsDark: Bool,
        systemIncreasesContrast: Bool,
        expected: String
    ) {
        let appearance = MonarchAppearance(scheme: scheme, contrast: contrast)
        let tokens = appearance.resolve(
            systemIsDark: systemIsDark,
            systemIncreasesContrast: systemIncreasesContrast
        )
        #expect(tokens.id == expected)
    }

    @Test("Resolution from SwiftUI values matches the pure rule")
    func resolutionFromEnvironmentValues() {
        let appearance = MonarchAppearance()
        #expect(appearance.resolve(colorScheme: .light, colorSchemeContrast: .standard).id == "light")
        #expect(appearance.resolve(colorScheme: .dark, colorSchemeContrast: .standard).id == "dark")
        #expect(appearance.resolve(colorScheme: .light, colorSchemeContrast: .increased).id == "hc-light")
        #expect(appearance.resolve(colorScheme: .dark, colorSchemeContrast: .increased).id == "hc-dark")
    }

    @Test("The accent is applied to the resolved theme")
    func accent() {
        let accent = MonarchTokens.Accents.stride
        let tokens = MonarchAppearance(scheme: .dark, accent: accent)
            .resolve(systemIsDark: false, systemIncreasesContrast: false)
        #expect(tokens.colors.accentSolid == accent.dark.solid)
        #expect(tokens == MonarchTokens.dark.withAccent(accent))
    }

    @Test("A custom AI colour is applied after the accent")
    func aiColour() {
        let accent = MonarchTokens.Accents.stride
        let ai = MonarchTokens.Accents.signal
        let tokens = MonarchAppearance(scheme: .dark, accent: accent, ai: ai)
            .resolve(systemIsDark: false, systemIncreasesContrast: false)
        #expect(tokens == MonarchTokens.dark.withAccent(accent).withAI(ai))
        #expect(tokens.colors.accentSolid == accent.dark.solid, "the AI colour leaves the accent alone")
        #expect(tokens.colors.aiSolid != MonarchTokens.dark.colors.aiSolid)
        let plain = MonarchAppearance(scheme: .dark, accent: accent)
            .resolve(systemIsDark: false, systemIncreasesContrast: false)
        #expect(plain.colors.aiSolid == MonarchTokens.dark.colors.aiSolid, "no AI colour keeps the theme's AI roles")
    }

    @Test("The modifiers install the AI colour, also with a brand colour")
    func modifierInstallsAI() {
        let probe = ThemeProbe()
        let ai = MonarchTokens.Accents.signal
        _ = Host.layout(probe.view.monarchTheme(scheme: .light, contrast: .standard, ai: ai))
        #expect(probe.theme?.tokens == MonarchTokens.light.withAccent(MonarchTokens.Accents.rook).withAI(ai))
        _ = Host.layout(probe.view.monarchTheme(brand: "#0E7C66", scheme: .dark, contrast: .standard, ai: ai))
        #expect(probe.theme?.tokens == MonarchTokens.dark.withAccent(MonarchAccent(brand: "#0E7C66")).withAI(ai))
    }

    @Test("A colour role maps to the same sRGB components")
    func colourMapping() {
        let rgba = MonarchRGBA(red: 0.25, green: 0.5, blue: 0.75, alpha: 0.5)
        let resolved = Color(monarch: rgba).resolve(in: EnvironmentValues())
        #expect(abs(Double(resolved.red) - rgba.red) < 0.001)
        #expect(abs(Double(resolved.green) - rgba.green) < 0.001)
        #expect(abs(Double(resolved.blue) - rgba.blue) < 0.001)
        #expect(abs(Double(resolved.opacity) - rgba.alpha) < 0.001)
    }

    @Test("Colours are looked up by role name and by key path")
    func colourLookup() {
        let theme = MonarchTheme.dark
        #expect(theme.colors.bgCanvas == Color(monarch: MonarchTokens.dark.colors.bgCanvas))
        #expect(theme.colors[role: \.accentSolid] == theme.colors.accentSolid)
        #expect(theme.colors.textDefault != theme.colors.bgCanvas)
    }

    @Test("The four built-in themes carry the token values")
    func builtInThemes() {
        #expect(MonarchTheme.light.id == "light")
        #expect(MonarchTheme.dark.isDark)
        #expect(MonarchTheme.highContrastLight.isHighContrast)
        #expect(MonarchTheme.highContrastDark.focusRingWidth == 3)
        #expect(MonarchTheme.light.spacing.space4 == 16)
        #expect(MonarchTheme.light.radius.lg == 12)
        #expect(MonarchTheme.light.typography == .system)
    }

    @Test("The default environment value is the light theme")
    func defaultEnvironment() {
        #expect(EnvironmentValues().monarchTheme == .light)
    }

    @Test("The modifier installs the resolved theme in the environment")
    func modifierInstallsTheme() {
        let probe = ThemeProbe()
        let accent = MonarchTokens.Accents.signal

        _ = Host.layout(probe.view.monarchTheme(scheme: .dark, contrast: .more, accent: accent))
        #expect(probe.theme?.id == "hc-dark")
        #expect(probe.theme?.tokens == MonarchTokens.hcDark.withAccent(accent))
        #expect(probe.colorScheme == .dark)

        _ = Host.layout(probe.view.monarchTheme(contrast: .standard).environment(\.colorScheme, .dark))
        #expect(probe.theme?.id == "dark")

        _ = Host.layout(probe.view.monarchTheme(scheme: .light, contrast: .standard).environment(\.colorScheme, .dark))
        #expect(probe.theme?.id == "light")
        #expect(probe.colorScheme == .light)
    }

    @Test("Custom font families are carried by the theme")
    func typography() {
        let probe = ThemeProbe()
        let typography = MonarchTypography(fontFamily: "Poppins", codeFontFamily: "Menlo")
        _ = Host.layout(
            VStack {
                probe.view
                Text("Custom").monarchTextStyle(.body)
                Text("Code").monarchTextStyle(.code)
            }
            .monarchTheme(MonarchAppearance(scheme: .light, contrast: .standard), typography: typography)
        )
        #expect(probe.theme?.typography == typography)
    }
}

/// Records the theme and colour scheme a hosted view sees.
@MainActor
final class ThemeProbe {
    var theme: MonarchTheme?
    var colorScheme: ColorScheme?

    var view: some View { ProbeView(probe: self) }

    private struct ProbeView: View {
        let probe: ThemeProbe
        @Environment(\.monarchTheme) private var theme
        @Environment(\.colorScheme) private var colorScheme

        var body: some View {
            Text("probe").onAppear {
                probe.theme = theme
                probe.colorScheme = colorScheme
            }
        }
    }
}
