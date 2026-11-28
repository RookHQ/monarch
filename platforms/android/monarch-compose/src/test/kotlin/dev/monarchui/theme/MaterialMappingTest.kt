package dev.monarchui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MaterialMappingTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun keyRolesMapToMonarchRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val scheme = colors.toMaterialColorScheme(theme.isDark)
            assertEquals(colors.accentStrong, scheme.primary, theme.id)
            assertEquals(colors.accentOnStrong, scheme.onPrimary, theme.id)
            assertEquals(colors.bgCanvas, scheme.background, theme.id)
            assertEquals(colors.bgSurface, scheme.surface, theme.id)
            assertEquals(colors.textDefault, scheme.onSurface, theme.id)
            assertEquals(colors.textMuted, scheme.onSurfaceVariant, theme.id)
            assertEquals(colors.dangerStrong, scheme.error, theme.id)
            assertEquals(colors.dangerOnStrong, scheme.onError, theme.id)
            assertEquals(colors.borderInput, scheme.outline, theme.id)
            assertEquals(colors.overlayScrim, scheme.scrim, theme.id)
        }
    }

    /**
     * Every colour of the mapped scheme is a Monarch token of that theme. A
     * role that Material adds in a later version, and that the mapping does
     * not set, keeps its Material baseline colour and fails here.
     */
    @Test
    fun noMaterialBaselineColourIsLeftInTheScheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val monarch = colourGetters(MonarchColors::class.java, colors).values.toSet()
            val scheme = colourGetters(ColorScheme::class.java, colors.toMaterialColorScheme(theme.isDark))
            assertTrue(scheme.size >= MINIMUM_SCHEME_ROLES, "Only ${scheme.size} scheme roles were found")
            val unmapped = scheme.filterValues { it !in monarch }.keys
            assertEquals(emptySet(), unmapped, "${theme.id}: scheme roles that are not Monarch colors")
        }
    }

    @Test
    fun typographyMapsEveryMaterialStyleToAMonarchRole() {
        val monarch = MonarchTypography.Default
        val material: Typography = monarch.toMaterialTypography()
        assertSame(monarch.display, material.displayLarge)
        assertSame(monarch.title, material.headlineLarge)
        assertSame(monarch.heading, material.titleLarge)
        assertSame(monarch.subheading, material.titleMedium)
        assertSame(monarch.body, material.bodyLarge)
        assertSame(monarch.body, material.bodyMedium)
        assertSame(monarch.caption, material.bodySmall)
        assertSame(monarch.label, material.labelLarge)
    }

    @Test
    fun shapesMapToMonarchRadii() {
        val monarch = MonarchShapes.Default
        val material: Shapes = monarch.toMaterialShapes()
        assertSame(monarch.xs, material.extraSmall)
        assertSame(monarch.md, material.small)
        assertSame(monarch.lg, material.medium)
        assertSame(monarch.xl, material.large)
        assertSame(monarch.xl2, material.extraLarge)
    }

    @Test
    fun materialThemeInsideMonarchThemeHasTheMappedValues() {
        var primary = Color.Unspecified
        var bodyLarge = MonarchTypography.Default.caption
        val theme = MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas)
        rule.setMonarchContent(tokens = theme) {
            primary = MaterialTheme.colorScheme.primary
            bodyLarge = MaterialTheme.typography.bodyLarge
        }
        rule.waitForIdle()
        assertEquals(Color(MonarchTokens.Accents.atlas.dark.strong), primary)
        assertEquals(MonarchTypography.Default.body, bodyLarge)
    }

    /** Getters of Compose `Color` properties: mangled names that return the packed value as a long. */
    private fun colourGetters(type: Class<*>, instance: Any): Map<String, Long> =
        type.methods
            .filter { it.name.startsWith("get") && it.name.contains('-') && it.parameterCount == 0 }
            .filter { it.returnType == Long::class.javaPrimitiveType }
            .associate { it.name.substringBefore('-') to it.invoke(instance) as Long }

    private companion object {
        const val MINIMUM_SCHEME_ROLES = 40
    }
}
