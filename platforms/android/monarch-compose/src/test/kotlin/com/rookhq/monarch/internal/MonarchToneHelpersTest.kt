package com.rookhq.monarch.internal

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.R
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The accent and AI tones, the shared tone names and the inverse tone colours. */
@RunWith(RobolectricTestRunner::class)
class MonarchToneHelpersTest {
    @get:Rule
    val rule = createComposeRule()

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    @Test
    fun theAccentAndAiTonesUseTheirOwnRoleFamilyInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(
                MonarchTonePalette(
                    colors.accentSubtle,
                    colors.accentStrong,
                    colors.accentSolid,
                    colors.accentOnSolid,
                    colors.accentBorder,
                ),
                MonarchTonePalette.of(MonarchTone.Accent, colors),
                theme.id,
            )
            assertEquals(
                MonarchTonePalette(colors.aiSubtle, colors.aiStrong, colors.aiSolid, colors.aiOnSolid, colors.aiBorder),
                MonarchTonePalette.of(MonarchTone.Ai, colors),
                theme.id,
            )
        }
    }

    @Test
    fun theAccentAndAiPairsReachTextContrastInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            listOf(MonarchTone.Accent, MonarchTone.Ai).forEach { tone ->
                val palette = MonarchTonePalette.of(tone, colors)
                assertTrue(contrast(palette.strong, palette.subtle) >= 4.5, "${theme.id} $tone strong on subtle")
                assertTrue(contrast(palette.onSolid, palette.solid) >= 4.5, "${theme.id} $tone on solid")
            }
        }
    }

    @Test
    fun everyToneHasAnInverseStrongColour() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentStrongInverse, MonarchTonePalette.strongInverse(MonarchTone.Accent, colors))
            assertEquals(colors.aiStrongInverse, MonarchTonePalette.strongInverse(MonarchTone.Ai, colors))
            assertEquals(colors.textInverseMuted, MonarchTonePalette.strongInverse(MonarchTone.Neutral, colors))
            assertEquals(colors.dangerStrongInverse, MonarchTonePalette.strongInverse(MonarchTone.Danger, colors))
        }
    }

    @Test
    fun neutralAndAccentHaveNoSpokenNameAndTheOthersDo() {
        assertNull(MonarchToneStrings.resource(MonarchTone.Neutral))
        assertNull(MonarchToneStrings.resource(MonarchTone.Accent))
        assertEquals(R.string.monarch_internal_tone_ai, MonarchToneStrings.resource(MonarchTone.Ai))
        assertEquals(R.string.monarch_internal_tone_danger, MonarchToneStrings.resource(MonarchTone.Danger))
        val names = mutableMapOf<MonarchTone, String?>()
        rule.setMonarchContent {
            MonarchTone.entries.forEach { names[it] = monarchToneName(it) }
        }
        rule.waitForIdle()
        assertEquals("Information", names[MonarchTone.Info])
        assertEquals("Success", names[MonarchTone.Success])
        assertEquals("Warning", names[MonarchTone.Warning])
        assertEquals("Error", names[MonarchTone.Danger])
        assertEquals("AI", names[MonarchTone.Ai])
        assertNull(names[MonarchTone.Accent])
    }

    @Test
    fun aToneNameCanBeReplaced() {
        var name: String? = null
        rule.setMonarchContent { name = monarchToneName(MonarchTone.Warning, override = "Heads up") }
        rule.waitForIdle()
        assertEquals("Heads up", name)
    }

    @Test
    fun theDeprecatedComponentKeysDefaultToTheSharedNames() {
        val keys = listOf(
            R.string.monarch_calloutbanner_tone_info to R.string.monarch_internal_tone_info,
            R.string.monarch_calloutbanner_tone_danger to R.string.monarch_internal_tone_danger,
            R.string.monarch_inlinenotice_tone_success to R.string.monarch_internal_tone_success,
            R.string.monarch_inlinenotice_tone_warning to R.string.monarch_internal_tone_warning,
            R.string.monarch_inlineerrorretry_tone_info to R.string.monarch_internal_tone_info,
            R.string.monarch_inlineerrorretry_tone_danger to R.string.monarch_internal_tone_danger,
        )
        val context = ApplicationProvider.getApplicationContext<Context>()
        keys.forEach { (legacy, shared) ->
            val name = context.resources.getResourceEntryName(legacy)
            assertEquals(context.getString(shared), context.getString(legacy), name)
        }
    }

    @Test
    fun aDeprecatedComponentKeyStillWinsWhenAnAppRedefinesIt() {
        val names = mutableMapOf<String, String?>()
        rule.setMonarchContent {
            // The default alias: same text as the shared name, so the shared name is used.
            names["default"] = monarchToneName(MonarchTone.Danger, null) { R.string.monarch_calloutbanner_tone_danger }
            // A key whose text differs from the shared name stands in for an app's resource override.
            names["app"] = monarchToneName(MonarchTone.Danger, null) { R.string.monarch_insightstrip_tone_danger }
            names["parameter"] = monarchToneName(MonarchTone.Danger, "Blocked") {
                R.string.monarch_insightstrip_tone_danger
            }
            names["neutral"] = monarchToneName(MonarchTone.Neutral, null) { null }
            names["ai"] = monarchToneName(MonarchTone.Ai, null) { null }
        }
        rule.waitForIdle()
        assertEquals("Error", names["default"])
        assertEquals("Critical", names["app"])
        assertEquals("Blocked", names["parameter"])
        assertNull(names["neutral"])
        assertEquals("AI", names["ai"])
    }
}
