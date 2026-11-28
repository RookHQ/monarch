package com.rookhq.monarch.theme

import androidx.compose.ui.graphics.Color
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.tokens.MonarchColorTokens
import com.rookhq.monarch.tokens.MonarchTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Guards the hand-written [MonarchColors] view against drift from the
 * generated [MonarchColorTokens]: a role added to or removed from the
 * generator output fails here until the view is updated.
 */
class MonarchColorsTest {
    /** Role name to ARGB, read from the generated class by reflection. */
    private fun tokenRoles(tokens: MonarchColorTokens): Map<String, Long> =
        MonarchColorTokens::class.java.methods
            .filter { it.name.startsWith("get") && it.parameterCount == 0 }
            .filter { it.returnType == Long::class.javaPrimitiveType }
            .associate { roleName(it.name) to it.invoke(tokens) as Long }

    /** Role name to packed Compose colour, read from the view by reflection. */
    private fun viewRoles(colors: MonarchColors): Map<String, Long> =
        MonarchColors::class.java.methods
            // A `Color` getter compiles to `getRole-0d7_KjU()` returning the packed value as a long.
            .filter { it.name.startsWith("get") && it.name.contains('-') && it.parameterCount == 0 }
            .associate { roleName(it.name) to it.invoke(colors) as Long }

    private fun roleName(getter: String): String =
        getter.removePrefix("get").substringBefore('-').replaceFirstChar(Char::lowercase)

    @Test
    fun theViewHasExactlyTheGeneratedRoles() {
        val generated = tokenRoles(MonarchTokens.light.colors).keys
        val view = viewRoles(MonarchColors(MonarchTokens.light.colors)).keys
        assertEquals(emptySet(), generated - view, "Roles missing from MonarchColors")
        assertEquals(emptySet(), view - generated, "Roles in MonarchColors that the generator does not emit")
        assertEquals(EXPECTED_ROLE_COUNT, view.size)
    }

    @Test
    fun everyRoleReadsTheTokenOfTheSameNameInEveryTheme() {
        AllThemes.forEach { theme ->
            val generated = tokenRoles(theme.colors)
            val view = viewRoles(MonarchColors(theme.colors))
            generated.forEach { (role, argb) ->
                assertEquals(Color(argb).value.toLong(), view[role], "${theme.id}.$role")
            }
        }
    }

    @Test
    fun argbLongsConvertToTheSameComposeColour() {
        val colors = MonarchColors(MonarchTokens.light.colors)
        assertEquals(Color(0xFFFCFCFD), colors.bgCanvas)
        assertEquals(1f, colors.textDefault.alpha)
    }

    @Test
    fun equalityFollowsTheTokenObject() {
        val light = MonarchTokens.light.colors
        assertEquals(MonarchColors(light), MonarchColors(light))
        assertEquals(MonarchColors(light).hashCode(), MonarchColors(light).hashCode())
        assertNotEquals(MonarchColors(light), MonarchColors(MonarchTokens.dark.colors))
    }

    private companion object {
        const val EXPECTED_ROLE_COUNT = 96
    }
}
