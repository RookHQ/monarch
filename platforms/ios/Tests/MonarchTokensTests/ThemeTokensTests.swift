import MonarchTokens
import Testing

@Suite("Theme tokens")
struct ThemeTokensTests {
    @Test("The four themes have the documented ids and flags")
    func themeIdentity() {
        #expect(MonarchTokens.themes.map(\.id) == ["light", "dark", "hc-light", "hc-dark"])
        #expect(MonarchTokens.themes.map(\.isDark) == [false, true, false, true])
        #expect(MonarchTokens.themes.map(\.isHighContrast) == [false, false, true, true])
    }

    @Test("High-contrast themes draw a wider focus ring")
    func focusRingWidth() {
        #expect(MonarchTokens.light.focusRingWidth == 2)
        #expect(MonarchTokens.dark.focusRingWidth == 2)
        #expect(MonarchTokens.hcLight.focusRingWidth == 3)
        #expect(MonarchTokens.hcDark.focusRingWidth == 3)
    }

    @Test("Every theme has shadows for all five levels")
    func shadows() {
        for theme in MonarchTokens.themes {
            let levels = [theme.shadows.xs, theme.shadows.sm, theme.shadows.md, theme.shadows.lg, theme.shadows.xl]
            #expect(levels.allSatisfy { !$0.isEmpty }, "theme \(theme.id)")
        }
    }

    @Test("A hex colour maps to sRGB components")
    func hexColour() {
        let colour = MonarchRGBA(0xFF8000, alpha: 0.5)
        #expect(colour.red == 1)
        #expect(abs(colour.green - 128.0 / 255.0) < 0.000_001)
        #expect(colour.blue == 0)
        #expect(colour.alpha == 0.5)
    }

    @Test("Primitives are in points, with 1rem equal to 16")
    func primitives() {
        #expect(MonarchTokens.Space.space4 == 16)
        #expect(MonarchTokens.FontSize.base == 16)
        #expect(MonarchTokens.Radius.md == 8)
        #expect(MonarchTokens.Control.md == 36)
        #expect(MonarchTokens.Duration.fast == 120)
    }
}
