package dev.monarchui.theme

import android.app.UiModeManager
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.tokens.MonarchBrandTheme
import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Plain Material 3 components inside the theme: what an app gets without any Monarch component. */
internal object MonarchThemeFixtures : ComponentFixtures {
    override val id = "theme"
    override val fixtures = listOf(
        fixture("Material 3 components take the Monarch scheme, type and shapes") {
            Column {
                Text("Body text")
                Button(onClick = {}) { Text("Material button") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
                FilterChip(selected = true, onClick = {}, label = { Text("Chip") })
            }
        },
        fixture("another accent, pinned inside the theme") {
            MonarchTheme(tokens = MonarchTokens.dark.withAccent(MonarchTokens.Accents.atlas)) {
                Surface(color = MonarchTheme.colors.bgCanvas) {
                    Button(onClick = {}) { Text("Atlas on dark") }
                }
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchThemeTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private class Observed {
        var colors: MonarchColors? = null
        var isDark = false
        var isHighContrast = false
        var focusRingWidth = Dp.Unspecified
        var reducedMotion = false
        var materialPrimary = Color.Unspecified
    }

    private fun observe(theme: @Composable (@Composable () -> Unit) -> Unit): Observed {
        val observed = Observed()
        rule.setContent {
            theme {
                observed.colors = MonarchTheme.colors
                observed.isDark = MonarchTheme.isDark
                observed.isHighContrast = MonarchTheme.isHighContrast
                observed.focusRingWidth = MonarchTheme.focusRingWidth
                observed.reducedMotion = MonarchTheme.motion.reducedMotion
                observed.materialPrimary = MaterialTheme.colorScheme.primary
            }
        }
        rule.waitForIdle()
        return observed
    }

    private fun assertTheme(expected: MonarchThemeTokens, observed: Observed) {
        assertEquals(Color(expected.colors.bgCanvas), observed.colors?.bgCanvas)
        assertEquals(Color(expected.colors.accentSolid), observed.colors?.accentSolid)
        assertEquals(expected.isDark, observed.isDark)
        assertEquals(expected.isHighContrast, observed.isHighContrast)
        assertEquals(expected.focusRingWidth.dp, observed.focusRingWidth)
    }

    @Test
    fun defaultsToLightOnALightStandardSystem() {
        assertTheme(MonarchTokens.light, observe { MonarchTheme(content = it) })
    }

    @Test
    @Config(qualifiers = "night")
    fun followsTheSystemDarkTheme() {
        assertTheme(MonarchTokens.dark, observe { MonarchTheme(content = it) })
    }

    @Test
    @Config(qualifiers = "night")
    fun anExplicitSchemeOverridesTheSystem() {
        assertTheme(MonarchTokens.light, observe { MonarchTheme(scheme = MonarchScheme.Light, content = it) })
    }

    @Test
    fun followsTheSystemContrastSetting() {
        shadowOf(context.getSystemService(UiModeManager::class.java)).setContrast(1f)
        assertTheme(MonarchTokens.hcLight, observe { MonarchTheme(content = it) })
    }

    @Test
    fun anExplicitContrastOverridesTheSystem() {
        assertTheme(
            MonarchTokens.hcDark,
            observe { MonarchTheme(scheme = MonarchScheme.Dark, contrast = MonarchContrast.More, content = it) },
        )
    }

    @Test
    fun appliesTheAccentToMonarchAndMaterial() {
        val accent = MonarchTokens.Accents.atlas
        val observed = observe { MonarchTheme(accent = accent, content = it) }
        assertEquals(Color(accent.light.solid), observed.colors?.accentSolid)
        assertEquals(Color(accent.light.strong), observed.materialPrimary)
    }

    @Test
    fun dynamicColourIsOffByDefaultAndLeavesMonarchColoursAloneWhenOn() {
        val off = observe { MonarchTheme(content = it) }
        assertEquals(Color(MonarchTokens.light.colors.accentStrong), off.materialPrimary)
    }

    @Test
    fun dynamicColourOptInChangesOnlyTheMaterialScheme() {
        val on = observe { MonarchTheme(dynamicColor = true, content = it) }
        assertEquals(Color(MonarchTokens.light.colors.accentSolid), on.colors?.accentSolid)
        assertTrue(on.materialPrimary != Color.Unspecified)
    }

    @Test
    fun reportsReducedMotionWhenAnimationsAreOff() {
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        assertTrue(observe { MonarchTheme(content = it) }.reducedMotion)
    }

    @Test
    fun reportsFullMotionByDefault() {
        assertFalse(observe { MonarchTheme(content = it) }.reducedMotion)
    }

    @Test
    fun providesContentColourTextStyleAndTouchTarget() {
        var contentColor = Color.Unspecified
        var textStyle = MonarchTypography.Default.caption
        var touchTarget = Dp.Unspecified
        rule.setContent {
            MonarchTheme(tokens = MonarchTokens.dark) {
                contentColor = LocalContentColor.current
                textStyle = LocalTextStyle.current
                touchTarget = LocalMinimumInteractiveComponentSize.current
            }
        }
        rule.waitForIdle()
        assertEquals(Color(MonarchTokens.dark.colors.textDefault), contentColor)
        assertEquals(MonarchTypography.Default.body, textStyle)
        assertEquals(48.dp, touchTarget)
    }

    @Test
    fun componentsOutsideTheThemeFallBackToLight() {
        var canvas = Color.Unspecified
        rule.setContent { canvas = MonarchTheme.colors.bgCanvas }
        rule.waitForIdle()
        assertEquals(Color(MonarchTokens.light.colors.bgCanvas), canvas)
    }

    @Test
    fun theMotionSettingCanBeOverriddenForASubtree() {
        var reduced: Boolean? = null
        var animated: Boolean? = null
        rule.setContent {
            MonarchTheme(tokens = MonarchTokens.light, reducedMotion = true) {
                reduced = MonarchTheme.motion.reducedMotion
            }
            MonarchTheme(reducedMotion = false) { animated = MonarchTheme.motion.reducedMotion }
        }
        rule.waitForIdle()
        assertEquals(true, reduced)
        assertEquals(false, animated)
    }

    @Test
    fun theDefaultAccentIsAvailableWithoutTheTokenModule() {
        assertEquals(MonarchTokens.Accents.rook, MonarchThemeDefaults.Accent)
    }

    @Test
    fun anAiColourIsAppliedAfterTheAccent() {
        val ai = assertNotNull(MonarchBrandTheme.derive("#0a7cff", ai = "#9333ea").aiAccent)
        var themed: MonarchColors? = null
        var plain: MonarchColors? = null
        rule.setContent {
            MonarchTheme(scheme = MonarchScheme.Light, contrast = MonarchContrast.Standard, ai = ai) {
                themed = MonarchTheme.colors
            }
            MonarchTheme(scheme = MonarchScheme.Light, contrast = MonarchContrast.Standard) {
                plain = MonarchTheme.colors
            }
        }
        rule.waitForIdle()
        val expected = MonarchColors(MonarchTokens.light.withAccent(MonarchThemeDefaults.Accent).withAi(ai).colors)
        assertEquals(expected.aiSolid, themed?.aiSolid)
        assertEquals(expected.aiSubtle, themed?.aiSubtle)
        assertEquals(plain?.accentSolid, themed?.accentSolid, "The accent is not touched")
        assertNotEquals(plain?.aiSolid, themed?.aiSolid)
    }

    @Test
    fun theBrandOverloadPassesTheAiColourOn() {
        val ai = assertNotNull(MonarchBrandTheme.derive("#0a7cff", ai = "#9333ea").aiAccent)
        var themed: MonarchColors? = null
        rule.setContent {
            MonarchTheme(
                brand = "#0a7cff",
                scheme = MonarchScheme.Light,
                contrast = MonarchContrast.Standard,
                ai = ai,
            ) {
                themed = MonarchTheme.colors
            }
        }
        rule.waitForIdle()
        val brand = MonarchBrandTheme.derive("#0a7cff").accent
        assertEquals(MonarchColors(MonarchTokens.light.withAccent(brand).withAi(ai).colors).aiSolid, themed?.aiSolid)
    }
}
