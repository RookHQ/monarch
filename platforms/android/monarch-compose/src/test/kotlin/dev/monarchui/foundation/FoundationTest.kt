package dev.monarchui.foundation

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.monarchui.testing.AllThemes
import dev.monarchui.tokens.MonarchTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FoundationTest {
    @Test
    fun everyTypeRoleUsesSpSizesAndEmLineHeightAndTracking() {
        val typography = MonarchTypography.Default
        MonarchTextStyle.entries.forEach { role ->
            val style = typography.style(role)
            assertEquals(TextUnitType.Sp, style.fontSize.type, role.name)
            assertEquals(TextUnitType.Em, style.lineHeight.type, role.name)
            assertEquals(TextUnitType.Em, style.letterSpacing.type, role.name)
        }
    }

    @Test
    fun typeRolesTakeTheirValuesFromTheTokens() {
        val typography = MonarchTypography.Default
        assertEquals(MonarchTokens.FontSize.xl4.sp, typography.display.fontSize)
        assertEquals(FontWeight.Bold, typography.display.fontWeight)
        assertEquals(MonarchTokens.FontSize.base.sp, typography.body.fontSize)
        assertEquals(FontWeight.Normal, typography.body.fontWeight)
        assertEquals(MonarchTokens.LineHeight.normal.em, typography.body.lineHeight)
        assertEquals(FontWeight.SemiBold, typography.bodyStrong.fontWeight)
        assertEquals(MonarchTokens.FontSize.sm.sp, typography.label.fontSize)
        assertEquals(FontWeight.Medium, typography.label.fontWeight)
        assertEquals(MonarchTokens.FontSize.xs.sp, typography.caption.fontSize)
        assertEquals(FontFamily.Monospace, typography.code.fontFamily)
    }

    /** The table shared with iOS: size, weight, line height and tracking of every role. */
    @Test
    fun typeRolesMatchTheSharedTable() {
        val size = MonarchTokens.FontSize
        val leading = MonarchTokens.LineHeight
        val tracking = MonarchTokens.LetterSpacing
        val expected = mapOf(
            MonarchTextStyle.Display to listOf(size.xl4, 700f, leading.tight, tracking.tight),
            MonarchTextStyle.Title to listOf(size.xl2, 600f, leading.tight, tracking.snug),
            MonarchTextStyle.Heading to listOf(size.xl, 600f, leading.snug, tracking.snug),
            MonarchTextStyle.Subheading to listOf(size.lg, 600f, leading.snug, tracking.normal),
            MonarchTextStyle.Body to listOf(size.base, 400f, leading.normal, tracking.normal),
            MonarchTextStyle.BodyStrong to listOf(size.base, 600f, leading.normal, tracking.normal),
            MonarchTextStyle.Label to listOf(size.sm, 500f, leading.snug, tracking.normal),
            MonarchTextStyle.Caption to listOf(size.xs, 400f, leading.snug, tracking.normal),
            MonarchTextStyle.Code to listOf(size.sm, 400f, leading.normal, tracking.normal),
            MonarchTextStyle.CodeSmall to listOf(size.xs, 400f, leading.normal, tracking.normal),
        )
        assertEquals(MonarchTextStyle.entries.toSet(), expected.keys)
        expected.forEach { (role, values) ->
            val style = MonarchTypography.Default.style(role)
            val actual = listOf(
                style.fontSize.value,
                style.fontWeight?.weight?.toFloat(),
                style.lineHeight.value,
                style.letterSpacing.value,
            )
            assertEquals(values, actual, role.name)
        }
    }

    @Test
    fun aProductTypefaceReplacesEveryRoleExceptCode() {
        val typography = monarchTypography(fontFamily = FontFamily.Serif)
        val code = setOf(MonarchTextStyle.Code, MonarchTextStyle.CodeSmall)
        MonarchTextStyle.entries.filterNot { it in code }.forEach { role ->
            assertEquals(FontFamily.Serif, typography.style(role).fontFamily, role.name)
        }
        assertEquals(FontFamily.Monospace, typography.code.fontFamily)
        assertEquals(FontFamily.Monospace, typography.codeSmall.fontFamily)
    }

    @Test
    fun headingRolesAreHeadingsByDefault() {
        val headings = MonarchTextStyle.entries.filter { it.isHeading }
        assertEquals(
            listOf(
                MonarchTextStyle.Display,
                MonarchTextStyle.Title,
                MonarchTextStyle.Heading,
                MonarchTextStyle.Subheading,
            ),
            headings,
        )
    }

    /** Fails when the generated spacing scale gains a step that MonarchSpacing does not mirror. */
    @Test
    fun spacingMirrorsEveryGeneratedStep() {
        val generated = MonarchTokens.Space::class.java.declaredFields
            .filter { it.type == Float::class.javaPrimitiveType }
            .associate { it.name to it.getFloat(null) }
        val mirrored = MonarchSpacing::class.java.methods
            // A `Dp` getter compiles to `getSpace1-D9Ej5fM()` returning a float.
            .filter { it.name.startsWith("get") && it.name.contains('-') && it.parameterCount == 0 }
            .associate {
                val name = it.name.removePrefix("get").substringBefore('-').replaceFirstChar(Char::lowercase)
                name to it.invoke(MonarchSpacing.Default) as Float
            }
        assertTrue(generated.size >= MINIMUM_SPACING_STEPS, "Only ${generated.size} spacing tokens were found")
        assertEquals(generated, mirrored)
    }

    @Test
    fun layoutWidthsComeFromTheWidthAndLayoutTokens() {
        assertEquals(672.dp, MonarchLayout.readingMax)
        assertEquals(MonarchTokens.Width.narrow.dp, MonarchLayout.narrowWidth)
        assertEquals(MonarchTokens.Width.content.dp, MonarchLayout.contentWidth)
        assertEquals(MonarchTokens.Width.wide.dp, MonarchLayout.wideWidth)
        assertEquals(MonarchTokens.Layout.contentMax.dp, MonarchLayout.contentMax)
        assertEquals(2.dp, MonarchLayout.selectedBorderWidth)
        assertEquals(1.dp, MonarchLayout.borderWidth)
    }

    @Test
    fun spacingIsInDp() {
        assertEquals(16.dp, MonarchSpacing.Default.space4)
        assertEquals(2.dp, MonarchSpacing.Default.space0_5)
    }

    @Test
    fun shapesTakeTheirRadiiFromTheTokens() {
        val shapes = MonarchShapes.Default
        assertEquals(RoundedCornerShape(MonarchTokens.Radius.md.dp), shapes.md)
        assertEquals(RoundedCornerShape(MonarchTokens.Radius.lg.dp), shapes.lg)
        assertEquals(RoundedCornerShape(percent = 50), shapes.full)
    }

    @Test
    fun elevationHasTheShadowLayersOfTheTheme() {
        AllThemes.forEach { theme ->
            val elevation = MonarchElevation.from(theme.shadows)
            assertEquals(theme.shadows.xs.size, elevation.xs.layers.size, theme.id)
            assertEquals(theme.shadows.sm.size, elevation.sm.layers.size, theme.id)
            assertEquals(theme.shadows.md.size, elevation.md.layers.size, theme.id)
            assertEquals(theme.shadows.lg.size, elevation.lg.layers.size, theme.id)
            assertEquals(theme.shadows.xl.size, elevation.xl.layers.size, theme.id)
            val first = theme.shadows.md.first()
            assertEquals(first.blur.dp, elevation.md.layers.first().radius, theme.id)
            assertEquals(first.offsetY.dp, elevation.md.layers.first().offset.y, theme.id)
        }
    }

    @Test
    fun presentedSurfacesUseOneSoftLowShadow() {
        AllThemes.forEach { theme ->
            val elevation = MonarchElevation.from(theme.shadows)
            assertSame(elevation.md, elevation.presented, theme.id)
        }
        // Soft and low in the light theme: at most a 4dp drop and at most 10 percent black per layer.
        MonarchElevation.from(MonarchTokens.light.shadows).presented.layers.forEach { layer ->
            assertTrue(layer.offset.y <= 4.dp, "offset ${layer.offset.y}")
            assertTrue(layer.color.alpha <= 0.1f, "alpha ${layer.color.alpha}")
        }
    }

    @Test
    fun motionUsesTheTokenDurations() {
        val motion = MonarchMotion(reducedMotion = false)
        assertEquals(MonarchTokens.Duration.fast, motion.fast)
        assertEquals(MonarchTokens.Duration.base, motion.base)
        assertEquals(MonarchTokens.Duration.slow, motion.slow)
        assertEquals(MonarchTokens.Duration.slower, motion.slower)
        val spec = motion.spec<Float>(durationMillis = motion.slow, easing = motion.enter)
        assertIs<TweenSpec<Float>>(spec)
        assertEquals(MonarchTokens.Duration.slow, spec.durationMillis)
        assertEquals(motion.enter, spec.easing)
    }

    @Test
    fun reducedMotionTurnsASpecIntoAnImmediateChange() {
        val motion = MonarchMotion(reducedMotion = true)
        assertIs<SnapSpec<Float>>(motion.spec<Float>())
        assertEquals(MonarchMotion(reducedMotion = true), motion)
    }

    private companion object {
        const val MINIMUM_SPACING_STEPS = 17
    }
}
