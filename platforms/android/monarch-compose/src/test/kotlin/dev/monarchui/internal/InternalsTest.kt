package dev.monarchui.internal

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.foundation.MonarchShapes
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The shared internals that component authors build on. */
@RunWith(RobolectricTestRunner::class)
class InternalsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyToneHasAPaletteFromOneRoleFamilyInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val danger = MonarchTonePalette.of(MonarchTone.Danger, colors)
            assertEquals(
                MonarchTonePalette(
                    subtle = colors.dangerSubtle,
                    strong = colors.dangerStrong,
                    solid = colors.dangerSolid,
                    onSolid = colors.dangerOnSolid,
                    border = colors.dangerBorder,
                ),
                danger,
                theme.id,
            )
            val neutral = MonarchTonePalette.of(MonarchTone.Neutral, colors)
            assertEquals(colors.bgMuted, neutral.subtle, theme.id)
            assertEquals(colors.textMuted, neutral.strong, theme.id)
            assertEquals(colors.bgInverse, neutral.solid, theme.id)
            assertEquals(colors.textInverse, neutral.onSolid, theme.id)
            // The five status tones differ from each other. The accent and AI
            // tones are not compared with them: the accent is the brand's and
            // may share a hue with a status family (the Rook accent and info).
            // MonarchToneHelpersTest checks that they use their own roles.
            val status = listOf(
                MonarchTone.Neutral,
                MonarchTone.Info,
                MonarchTone.Success,
                MonarchTone.Warning,
                MonarchTone.Danger,
            )
            val pairs = status.map { MonarchTonePalette.of(it, colors).let { p -> p.subtle to p.strong } }
            assertEquals(status.size, pairs.toSet().size, theme.id)
        }
    }

    @Test
    fun theThreeEmphasesPairTheRolesThatBelongTogether() {
        val palette = MonarchTonePalette.of(MonarchTone.Info, MonarchColors(MonarchTokens.light.colors))
        assertEquals(
            MonarchSurfaceColors(palette.subtle, palette.strong, null),
            palette.surface(MonarchToneEmphasis.Subtle),
        )
        assertEquals(
            MonarchSurfaceColors(palette.solid, palette.onSolid, null),
            palette.surface(MonarchToneEmphasis.Solid),
        )
        assertEquals(
            MonarchSurfaceColors(Color.Transparent, palette.strong, palette.border),
            palette.surface(MonarchToneEmphasis.Outline),
        )
    }

    @Test
    fun theSurfaceModifierDrawsTheFillAndTheBorder() {
        val fill = Color(MonarchTokens.light.colors.infoSubtle)
        val border = Color(MonarchTokens.light.colors.infoStrong)
        rule.setMonarchContent {
            Box(
                Modifier
                    .testTag("surface")
                    .size(40.dp)
                    .monarchSurface(MonarchSurfaceColors(fill, border, border), MonarchShapes.Default.none),
            )
        }
        val bitmap = rule.onNodeWithTag("surface").captureToImage().asAndroidBitmap()
        assertEquals(border.toArgb(), bitmap.getPixel(0, bitmap.height / 2))
        assertEquals(fill.toArgb(), bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
    }

    @Test
    fun aDisabledControlNeverShowsAPress() {
        val resting = MonarchControlState()
        assertEquals("resting", resting.pick(resting = "resting", disabled = "disabled", pressed = "pressed"))
        assertEquals("pressed", resting.copy(pressed = true).pick("resting", "disabled", "pressed"))
        assertEquals("hovered", resting.copy(hovered = true).pick("resting", "disabled", "pressed", "hovered"))
        val pressedAndHovered = resting.copy(pressed = true, hovered = true)
        assertEquals("pressed", pressedAndHovered.pick("resting", "disabled", "pressed", "hovered"))
        assertEquals("disabled", MonarchControlState(enabled = false, pressed = true).pick("resting", "disabled", "p"))
        assertEquals("resting", resting.copy(pressed = true).pick(resting = "resting", disabled = "disabled"))
    }

    @Test
    fun theControlStateFollowsTheInteractionSource() {
        val source = MutableInteractionSource()
        var state = MonarchControlState()
        rule.setContent { state = rememberMonarchControlState(source, enabled = true) }
        rule.waitForIdle()
        assertFalse(state.pressed)
        val press = PressInteraction.Press(Offset.Zero)
        runBlocking { source.emit(press) }
        rule.waitForIdle()
        assertTrue(state.pressed)
        runBlocking { source.emit(PressInteraction.Release(press)) }
        rule.waitForIdle()
        assertFalse(state.pressed)
        assertTrue(state.enabled)
    }

    @Test
    fun sharedShapesAreTheTokenShapes() {
        assertEquals(MonarchShapes.Default.md, MonarchShapeDefaults.Control)
        assertEquals(MonarchShapes.Default.lg, MonarchShapeDefaults.Container)
        assertEquals(MonarchShapes.Default.full, MonarchShapeDefaults.Mark)
        assertEquals(1.dp, MonarchShapeDefaults.BorderWidth)
    }

    @Test
    fun aBuiltInStringIsUsedUnlessTheCallerReplacesIt() {
        var builtIn = ""
        var replaced = ""
        rule.setContent {
            builtIn = monarchString(null, R.string.monarch_button_loading)
            replaced = monarchString("Saving", R.string.monarch_button_loading)
        }
        rule.waitForIdle()
        assertEquals("Loading", builtIn)
        assertEquals("Saving", replaced)
    }

    @Test
    fun anImageShowsThePlaceholderUntilAPainterIsGiven() {
        var loaded by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchImage(
                painter = if (loaded) ColorPainter(Color(MonarchTokens.light.colors.accentSolid)) else null,
                contentDescription = "Ada Lovelace",
                modifier = Modifier.size(40.dp),
                shape = MonarchShapeDefaults.Mark,
            ) { Text("AL") }
        }
        // The placeholder is decoration: drawn, but not in the tree that TalkBack reads.
        rule.onNodeWithText("AL").assertDoesNotExist()
        rule.onNodeWithText("AL", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Ada Lovelace").assertDoesNotExist()
        loaded = true
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Ada Lovelace").assertExists()
    }

    @Test
    fun aLabelledRowIsAtLeast48dpHighAndShowsItsDescription() {
        rule.setMonarchContent {
            LabelledControlRow(
                label = "Label",
                description = "Description",
                enabled = true,
                controlAtEnd = false,
                modifier = Modifier.testTag("row"),
            ) { Checkbox(checked = true, onCheckedChange = null) }
        }
        rule.onNodeWithTag("row").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithText("Description").assertExists()
        assertNull(MonarchSurfaceColors(Color.Transparent, Color.Transparent, null).border)
    }
}
