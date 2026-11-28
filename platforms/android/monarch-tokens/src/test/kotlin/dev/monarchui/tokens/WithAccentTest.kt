package dev.monarchui.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class WithAccentTest {
    private val themes = listOf(MonarchTokens.light, MonarchTokens.dark, MonarchTokens.hcLight, MonarchTokens.hcDark)
    private val accent = MonarchTokens.Accents.atlas

    @Test
    fun replacesTheTenAccentRoles() {
        themes.forEach { theme ->
            val scale = accent.scale(theme.id)
            val colors = theme.withAccent(accent).colors
            assertEquals(scale.solid, colors.accentSolid, theme.id)
            assertEquals(scale.solidHover, colors.accentSolidHover, theme.id)
            assertEquals(scale.solidActive, colors.accentSolidActive, theme.id)
            assertEquals(scale.onSolid, colors.accentOnSolid, theme.id)
            assertEquals(scale.subtle, colors.accentSubtle, theme.id)
            assertEquals(scale.subtleHover, colors.accentSubtleHover, theme.id)
            assertEquals(scale.strong, colors.accentStrong, theme.id)
            assertEquals(scale.onStrong, colors.accentOnStrong, theme.id)
            assertEquals(scale.border, colors.accentBorder, theme.id)
            assertEquals(scale.strongInverse, colors.accentStrongInverse, theme.id)
        }
    }

    @Test
    fun keepsThemeIdentityAndShadows() {
        themes.forEach { theme ->
            val next = theme.withAccent(accent)
            assertEquals(theme.id, next.id)
            assertEquals(theme.isDark, next.isDark)
            assertEquals(theme.isHighContrast, next.isHighContrast)
            assertEquals(theme.focusRingWidth, next.focusRingWidth)
            assertSame(theme.shadows, next.shadows)
        }
    }

    @Test
    fun leavesEveryRoleOutsideTheAccentFamilyAlone() {
        themes.forEach { theme ->
            val before = colourRoles(theme.colors)
            val after = colourRoles(theme.withAccent(accent).colors)
            val followers = setOf("textLink", "focusRing")
            before.keys.filterNot { it.startsWith("accent") || it in followers }.forEach { role ->
                assertEquals(before[role], after[role], "${theme.id}.$role changed")
            }
        }
    }

    @Test
    fun focusRingFollowsTheAccentInStandardThemesOnly() {
        listOf(MonarchTokens.light, MonarchTokens.dark).forEach { theme ->
            val next = theme.withAccent(accent)
            assertDiffers(theme.colors.focusRing, next.colors.focusRing, "${theme.id} focus ring did not follow")
        }
        listOf(MonarchTokens.hcLight, MonarchTokens.hcDark).forEach { theme ->
            val next = theme.withAccent(accent)
            assertEquals(theme.colors.focusRing, next.colors.focusRing, "${theme.id} focus ring must not change")
            assertEquals(theme.colors.textDefault, next.colors.focusRing, "${theme.id} focus ring is the text colour")
        }
    }

    @Test
    fun linksFollowTheAccentInEveryTheme() {
        themes.forEach { theme ->
            val next = theme.withAccent(accent)
            assertDiffers(theme.colors.textLink, next.colors.textLink, "${theme.id} link colour did not follow")
        }
    }

    @Test
    fun applyingTheDefaultAccentIsANoOp() {
        themes.forEach { theme ->
            assertEquals(
                colourRoles(theme.colors),
                colourRoles(theme.withAccent(MonarchTokens.Accents.rook).colors),
                theme.id,
            )
        }
    }
}
