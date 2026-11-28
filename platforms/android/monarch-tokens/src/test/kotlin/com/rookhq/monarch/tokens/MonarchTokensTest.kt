package com.rookhq.monarch.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MonarchTokensTest {
    private val themes = listOf(MonarchTokens.light, MonarchTokens.dark, MonarchTokens.hcLight, MonarchTokens.hcDark)

    @Test
    fun themesHaveDistinctIds() {
        assertEquals(listOf("light", "dark", "hc-light", "hc-dark"), themes.map { it.id })
    }

    @Test
    fun themeFlagsMatchTheirIds() {
        assertFalse(MonarchTokens.light.isDark)
        assertFalse(MonarchTokens.light.isHighContrast)
        assertTrue(MonarchTokens.dark.isDark)
        assertFalse(MonarchTokens.dark.isHighContrast)
        assertFalse(MonarchTokens.hcLight.isDark)
        assertTrue(MonarchTokens.hcLight.isHighContrast)
        assertTrue(MonarchTokens.hcDark.isDark)
        assertTrue(MonarchTokens.hcDark.isHighContrast)
    }

    @Test
    fun everyThemeColourIsOpaqueOrAScrim() {
        themes.forEach { theme ->
            colourRoles(theme.colors).forEach { (role, argb) ->
                val alpha = (argb ushr ALPHA_SHIFT) and BYTE_MASK
                if (role != "overlayScrim") {
                    assertTrue(alpha > 0, "${theme.id}.$role is fully transparent")
                }
                assertTrue(argb in 0..MAX_ARGB, "${theme.id}.$role is not a 32-bit ARGB value")
            }
        }
    }

    @Test
    fun everyThemeHasNinetySixColourRoles() {
        themes.forEach { assertEquals(EXPECTED_ROLE_COUNT, colourRoles(it.colors).size, it.id) }
    }

    @Test
    fun focusRingIsWiderInHighContrastThemes() {
        assertTrue(MonarchTokens.hcLight.focusRingWidth > MonarchTokens.light.focusRingWidth)
        assertTrue(MonarchTokens.hcDark.focusRingWidth > MonarchTokens.dark.focusRingWidth)
    }

    @Test
    fun everyThemeHasFiveShadowLevels() {
        themes.forEach { theme ->
            val levels = with(theme.shadows) { listOf(xs, sm, md, lg, xl) }
            levels.forEach { layers -> assertTrue(layers.isNotEmpty(), "${theme.id} has an empty shadow level") }
        }
    }

    @Test
    fun spacingFollowsTheFourUnitGrid() {
        assertEquals(4f, MonarchTokens.Space.space1)
        assertEquals(16f, MonarchTokens.Space.space4)
        assertEquals(96f, MonarchTokens.Space.space24)
    }

    @Test
    fun accentsAreListedOnceWithUniqueIds() {
        val all = MonarchTokens.Accents.all
        assertEquals(EXPECTED_ACCENT_COUNT, all.size)
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertSame(MonarchTokens.Accents.rook, all.first())
    }

    @Test
    fun accentScaleIsSelectedByThemeId() {
        val accent = MonarchTokens.Accents.atlas
        assertSame(accent.light, accent.scale("light"))
        assertSame(accent.dark, accent.scale("dark"))
        assertSame(accent.hcLight, accent.scale("hc-light"))
        assertSame(accent.hcDark, accent.scale("hc-dark"))
    }

    private companion object {
        const val EXPECTED_ROLE_COUNT = 96
        const val EXPECTED_ACCENT_COUNT = 21
        const val ALPHA_SHIFT = 24
        const val BYTE_MASK = 0xFFL
        const val MAX_ARGB = 0xFFFFFFFFL
    }
}

/** Every colour role of a theme by name, read through the generated getters. */
internal fun colourRoles(colors: MonarchColorTokens): Map<String, Long> =
    MonarchColorTokens::class.java.methods
        .filter { it.name.startsWith("get") && it.parameterCount == 0 }
        .filter { it.returnType == Long::class.javaPrimitiveType }
        .associate { it.name.removePrefix("get").replaceFirstChar(Char::lowercase) to it.invoke(colors) as Long }

/** Fails the surrounding test when two values that must differ are equal. */
internal fun assertDiffers(illegal: Long, actual: Long, message: String) = assertNotEquals(illegal, actual, message)
