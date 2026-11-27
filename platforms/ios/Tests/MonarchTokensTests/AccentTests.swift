import MonarchTokens
import Testing

@Suite("Accents")
struct AccentTests {
    @Test("There are 21 accents with unique ids, and the default is first")
    func inventory() {
        let ids = MonarchTokens.Accents.all.map(\.id)
        #expect(ids.count == 21)
        #expect(Set(ids).count == ids.count)
        #expect(ids.first == "rook")
    }

    @Test("The default accent is the one built into every theme")
    func defaultAccentIsIdentity() {
        for theme in MonarchTokens.themes {
            #expect(theme.withAccent(MonarchTokens.Accents.rook) == theme, "theme \(theme.id)")
        }
    }

    @Test("withAccent replaces every accent role", arguments: MonarchTokens.themes)
    func replacesAccentRoles(theme: MonarchThemeTokens) {
        let accent = MonarchTokens.Accents.signal
        let scale = accent.scale(themeID: theme.id)
        let colors = theme.withAccent(accent).colors

        #expect(colors.accentSolid == scale.solid)
        #expect(colors.accentSolidHover == scale.solidHover)
        #expect(colors.accentSolidActive == scale.solidActive)
        #expect(colors.accentOnSolid == scale.onSolid)
        #expect(colors.accentSubtle == scale.subtle)
        #expect(colors.accentSubtleHover == scale.subtleHover)
        #expect(colors.accentStrong == scale.strong)
        #expect(colors.accentOnStrong == scale.onStrong)
        #expect(colors.accentBorder == scale.border)
        #expect(colors.accentStrongInverse == scale.strongInverse)
        #expect(colors.textLink == scale.strong)
    }

    @Test("withAccent keeps everything that does not follow the accent", arguments: MonarchTokens.themes)
    func keepsOtherRoles(theme: MonarchThemeTokens) {
        let accented = theme.withAccent(MonarchTokens.Accents.signal)

        #expect(accented.id == theme.id)
        #expect(accented.isDark == theme.isDark)
        #expect(accented.isHighContrast == theme.isHighContrast)
        #expect(accented.focusRingWidth == theme.focusRingWidth)
        #expect(accented.shadows == theme.shadows)
        #expect(accented.colors.bgCanvas == theme.colors.bgCanvas)
        #expect(accented.colors.textDefault == theme.colors.textDefault)
        #expect(accented.colors.borderInput == theme.colors.borderInput)
        #expect(accented.colors.dangerSolid == theme.colors.dangerSolid)
        #expect(accented.colors.successStrong == theme.colors.successStrong)
    }

    @Test("The focus ring follows the accent in the standard themes only")
    func focusRing() {
        let accent = MonarchTokens.Accents.signal

        #expect(MonarchTokens.light.withAccent(accent).colors.focusRing == accent.light.strong)
        #expect(MonarchTokens.dark.withAccent(accent).colors.focusRing == accent.dark.strong)
        #expect(MonarchTokens.hcLight.withAccent(accent).colors.focusRing == MonarchTokens.hcLight.colors.focusRing)
        #expect(MonarchTokens.hcDark.withAccent(accent).colors.focusRing == MonarchTokens.hcDark.colors.focusRing)
    }

    @Test("An unknown theme id falls back to the light scale")
    func scaleLookup() {
        let accent = MonarchTokens.Accents.neutral
        #expect(accent.scale(themeID: "light") == accent.light)
        #expect(accent.scale(themeID: "dark") == accent.dark)
        #expect(accent.scale(themeID: "hc-light") == accent.hcLight)
        #expect(accent.scale(themeID: "hc-dark") == accent.hcDark)
        #expect(accent.scale(themeID: "unknown") == accent.light)
    }
}
