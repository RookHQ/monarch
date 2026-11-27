import MonarchTokens
import Testing

@Suite("Custom AI colour")
struct WithAITests {
    static let brand = MonarchBrandTheme.derive(brand: "#0a7cff", ai: "#9333ea")

    @Test("The brand theme has an AI accent")
    func aiAccent() {
        #expect(Self.brand.aiAccent != nil)
    }

    @Test("withAI replaces the ten AI roles, thinking and citation", arguments: MonarchTokens.themes)
    func replacesAIRoles(theme: MonarchThemeTokens) throws {
        let ai = try #require(Self.brand.aiAccent)
        let scale = ai.scale(themeID: theme.id)
        let colors = theme.withAI(ai).colors

        #expect(colors.aiSolid == scale.solid)
        #expect(colors.aiSolidHover == scale.solidHover)
        #expect(colors.aiSolidActive == scale.solidActive)
        #expect(colors.aiOnSolid == scale.onSolid)
        #expect(colors.aiSubtle == scale.subtle)
        #expect(colors.aiSubtleHover == scale.subtleHover)
        #expect(colors.aiStrong == scale.strong)
        #expect(colors.aiOnStrong == scale.onStrong)
        #expect(colors.aiBorder == scale.border)
        #expect(colors.aiStrongInverse == scale.strongInverse)
        #expect(colors.aiThinking == scale.subtle)
        #expect(colors.aiCitation == scale.strong)
    }

    @Test("withAI keeps everything outside the AI family", arguments: MonarchTokens.themes)
    func keepsOtherRoles(theme: MonarchThemeTokens) throws {
        let ai = try #require(Self.brand.aiAccent)
        let next = theme.withAI(ai)

        #expect(next.id == theme.id)
        #expect(next.isDark == theme.isDark)
        #expect(next.isHighContrast == theme.isHighContrast)
        #expect(next.focusRingWidth == theme.focusRingWidth)
        #expect(next.shadows == theme.shadows)
        #expect(next.colors.accentSolid == theme.colors.accentSolid)
        #expect(next.colors.textLink == theme.colors.textLink)
        #expect(next.colors.focusRing == theme.colors.focusRing)
        #expect(next.colors.aiTool == theme.colors.aiTool)
        #expect(next.colors.aiDiffAdd == theme.colors.aiDiffAdd)
        #expect(next.colors.infoStrong == theme.colors.infoStrong)
    }

    @Test("withAI composes with withAccent", arguments: MonarchTokens.themes)
    func composes(theme: MonarchThemeTokens) throws {
        let ai = try #require(Self.brand.aiAccent)
        let both = theme.withAccent(Self.brand.accent).withAI(ai)
        #expect(both.colors.accentSolid == Self.brand.accent.scale(themeID: theme.id).solid)
        #expect(both.colors.aiSolid == ai.scale(themeID: theme.id).solid)
    }

    @Test("Applying the built-in AI roles is a no-op", arguments: MonarchTokens.themes)
    func identity(theme: MonarchThemeTokens) {
        let c = theme.colors
        let own = MonarchAccentScale(
            solid: c.aiSolid,
            solidHover: c.aiSolidHover,
            solidActive: c.aiSolidActive,
            onSolid: c.aiOnSolid,
            subtle: c.aiSubtle,
            subtleHover: c.aiSubtleHover,
            strong: c.aiStrong,
            onStrong: c.aiOnStrong,
            border: c.aiBorder,
            strongInverse: c.aiStrongInverse
        )
        let builtIn = MonarchAccent(id: "ai", label: "Monarch AI", light: own, dark: own, hcLight: own, hcDark: own)
        #expect(theme.withAI(builtIn) == theme)
    }
}

@Suite("Neutral roles (C-086)")
struct NeutralRolesTests {
    @Test("bgSubtle differs from bgSurface, borderDefault from bgMuted", arguments: MonarchTokens.themes)
    func distinct(theme: MonarchThemeTokens) {
        #expect(theme.colors.bgSubtle != theme.colors.bgSurface)
        #expect(theme.colors.borderDefault != theme.colors.bgMuted)
    }

    @Test("Width and layout tokens match the web (1rem = 16pt)")
    func layout() {
        #expect(MonarchTokens.Width.narrow == 704)
        #expect(MonarchTokens.Width.content == 1152)
        #expect(MonarchTokens.Width.wide == 1280)
        #expect(MonarchTokens.Layout.readingMax == 672)
        #expect(MonarchTokens.Layout.contentMax == 1280)
    }
}
