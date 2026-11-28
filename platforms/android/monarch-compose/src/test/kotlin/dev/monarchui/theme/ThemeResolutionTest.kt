package dev.monarchui.theme

import dev.monarchui.tokens.MonarchTokens
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeResolutionTest {
    private val rook = MonarchTokens.Accents.rook

    private fun resolve(
        scheme: MonarchScheme,
        contrast: MonarchContrast,
        systemDark: Boolean = false,
        systemHighContrast: Boolean = false,
    ) = resolveMonarchTheme(scheme, contrast, rook, systemDark, systemHighContrast).id

    @Test
    fun explicitInputsIgnoreTheSystem() {
        for (systemDark in listOf(false, true)) {
            for (systemHc in listOf(false, true)) {
                assertEquals("light", resolve(MonarchScheme.Light, MonarchContrast.Standard, systemDark, systemHc))
                assertEquals("dark", resolve(MonarchScheme.Dark, MonarchContrast.Standard, systemDark, systemHc))
                assertEquals("hc-light", resolve(MonarchScheme.Light, MonarchContrast.More, systemDark, systemHc))
                assertEquals("hc-dark", resolve(MonarchScheme.Dark, MonarchContrast.More, systemDark, systemHc))
            }
        }
    }

    @Test
    fun systemSchemeFollowsTheSystemDarkTheme() {
        assertEquals("light", resolve(MonarchScheme.System, MonarchContrast.Standard, systemDark = false))
        assertEquals("dark", resolve(MonarchScheme.System, MonarchContrast.Standard, systemDark = true))
        assertEquals("hc-dark", resolve(MonarchScheme.System, MonarchContrast.More, systemDark = true))
    }

    @Test
    fun systemContrastFollowsTheSystemContrastSetting() {
        assertEquals("light", resolve(MonarchScheme.Light, MonarchContrast.System, systemHighContrast = false))
        assertEquals("hc-light", resolve(MonarchScheme.Light, MonarchContrast.System, systemHighContrast = true))
        assertEquals("hc-dark", resolve(MonarchScheme.Dark, MonarchContrast.System, systemHighContrast = true))
    }

    @Test
    fun bothSystemInputsCoverAllFourThemes() {
        val ids = buildList {
            for (dark in listOf(false, true)) {
                for (hc in listOf(false, true)) {
                    add(resolve(MonarchScheme.System, MonarchContrast.System, dark, hc))
                }
            }
        }
        assertEquals(listOf("light", "hc-light", "dark", "hc-dark"), ids)
    }

    @Test
    fun theAccentIsAppliedToTheResolvedTheme() {
        val accent = MonarchTokens.Accents.atlas
        val theme = resolveMonarchTheme(
            scheme = MonarchScheme.Dark,
            contrast = MonarchContrast.Standard,
            accent = accent,
            systemInDarkTheme = false,
            systemInHighContrast = false,
        )
        assertEquals("dark", theme.id)
        assertEquals(accent.dark.solid, theme.colors.accentSolid)
    }

    @Test
    fun mediumAndHighSystemContrastSelectTheHighContrastThemes() {
        assertEquals(false, isHighContrastLevel(-1f))
        assertEquals(false, isHighContrastLevel(0f))
        assertEquals(true, isHighContrastLevel(0.5f))
        assertEquals(true, isHighContrastLevel(1f))
    }
}
