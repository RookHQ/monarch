package com.rookhq.monarch.components.identity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButton
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonSize
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonTokens
import com.rookhq.monarch.components.identity.signinwithrookbutton.MonarchSignInWithRookButtonVariant
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.testing.textLayout
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.theme.MonarchScheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchSignInWithRookButtonFixtures : ComponentFixtures {
    override val id = "sign-in-with-rook-button"
    override val fixtures = listOf(
        fixture("every variant") {
            Column {
                MonarchSignInWithRookButtonVariant.entries.forEach {
                    MonarchSignInWithRookButton(onClick = {}, variant = it, size = MonarchSignInWithRookButtonSize.Sm)
                }
            }
        },
        fixture("every size") {
            Column {
                MonarchSignInWithRookButtonSize.entries.forEach { MonarchSignInWithRookButton(onClick = {}, size = it) }
            }
        },
        fixture("loading") { MonarchSignInWithRookButton(onClick = {}, loading = true) },
        fixture("disabled") { MonarchSignInWithRookButton(onClick = {}, enabled = false) },
        fixture("full width") { MonarchSignInWithRookButton(onClick = {}, fullWidth = true) },
        fixture("long label") {
            MonarchSignInWithRookButton(onClick = {}, label = "Continue to the workspace with your Rook ID account")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSignInWithRookButtonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun itIsAButtonNamedByItsLabelThatCallsBack() {
        var taps = 0
        rule.setMonarchContent { MonarchSignInWithRookButton(onClick = { taps++ }) }
        rule.onNodeWithText("Sign in with Rook ID")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        assertEquals(1, taps)
    }

    @Test
    fun theLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchSignInWithRookButton(onClick = {}, label = "Continuer avec Rook ID") }
        rule.onNodeWithText("Continuer avec Rook ID").assertExists()
    }

    @Test
    fun whileLoadingItReadsLoadingAndIgnoresTaps() {
        var taps = 0
        rule.setMonarchContent {
            MonarchSignInWithRookButton(onClick = { taps++ }, loading = true, loadingLabel = "Starting")
        }
        val button = rule.onNodeWithText("Sign in with Rook ID")
        assertEquals("Starting", button.stateDescription())
        button.assertIsNotEnabled()
        button.performClick()
        assertEquals(0, taps)
    }

    @Test
    fun aDisabledButtonDoesNotCallBack() {
        var taps = 0
        rule.setMonarchContent { MonarchSignInWithRookButton(onClick = { taps++ }, enabled = false) }
        rule.onNodeWithText("Sign in with Rook ID").assertIsNotEnabled().performClick()
        assertEquals(0, taps)
    }

    @Test
    fun everySizeHasA48dpTouchTarget() {
        rule.setMonarchContent {
            Column {
                MonarchSignInWithRookButtonSize.entries.forEach {
                    MonarchSignInWithRookButton(onClick = {}, size = it, label = it.name)
                }
            }
        }
        MonarchSignInWithRookButtonSize.entries.forEach {
            rule.onNodeWithText(it.name).assertTouchTargetAtLeast48dp(rule.density)
        }
    }

    @Test
    fun fullWidthFillsTheContainer() {
        rule.setMonarchContent {
            Column(Modifier.width(300.dp)) {
                MonarchSignInWithRookButton(onClick = {}, modifier = Modifier.testTag("button"), fullWidth = true)
            }
        }
        rule.onNodeWithTag("button").assertWidthIsEqualTo(300.dp)
    }

    @Test
    fun theLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSignInWithRookButton(onClick = {}, size = MonarchSignInWithRookButtonSize.Lg)
        }
        rule.onNodeWithText("Sign in with Rook ID", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aWrappedLabelIsCentredLineByLineAndNotCut() {
        // C-035: the label lost its centring as a workaround for the old button label bug.
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Column(Modifier.width(220.dp)) {
                MonarchSignInWithRookButton(onClick = {}, fullWidth = true, label = "Continue with your Rook ID")
            }
        }
        val label = rule.onNodeWithText("Continue with your Rook ID", useUnmergedTree = true).assertTextFullyVisible()
        val layout = label.textLayout()
        assertTrue(layout.lineCount > 1, "the label should wrap at this width")
        (0 until layout.lineCount).forEach { line ->
            val left = layout.getLineLeft(line)
            val right = layout.size.width - layout.getLineRight(line)
            assertTrue(kotlin.math.abs(left - right) <= 1f, "line $line is not centered: $left and $right")
        }
    }

    @Test
    fun theFixedVariantsPinTheirSchemeAndAutoFollowsTheScreen() {
        fun scheme(variant: MonarchSignInWithRookButtonVariant) =
            MonarchSignInWithRookButtonTokens.pinnedScheme(variant)
        assertEquals(MonarchScheme.Light, scheme(MonarchSignInWithRookButtonVariant.Light))
        assertEquals(MonarchScheme.Light, scheme(MonarchSignInWithRookButtonVariant.Neutral))
        assertEquals(MonarchScheme.Dark, scheme(MonarchSignInWithRookButtonVariant.Dark))
        assertNull(scheme(MonarchSignInWithRookButtonVariant.Auto))
        assertEquals(MonarchContrast.More, MonarchSignInWithRookButtonTokens.pinnedContrast(highContrast = true))
        assertEquals(MonarchContrast.Standard, MonarchSignInWithRookButtonTokens.pinnedContrast(highContrast = false))
    }

    @Test
    fun theColoursComeFromRolesInEveryThemeAndState() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val light = MonarchSignInWithRookButtonVariant.Light
            val neutral = MonarchSignInWithRookButtonVariant.Neutral
            val resting = MonarchSignInWithRookButtonTokens.colors(light, enabled = true, pressed = false, colors = c)
            assertEquals(c.bgSurface, resting.container, theme.id)
            assertEquals(c.textDefault, resting.content, theme.id)
            assertEquals(c.borderStrong, resting.border, theme.id)
            val pressed = MonarchSignInWithRookButtonTokens.colors(light, enabled = true, pressed = true, colors = c)
            assertEquals(c.bgMuted, pressed.container, theme.id)
            val grey = MonarchSignInWithRookButtonTokens.colors(neutral, enabled = true, pressed = false, colors = c)
            assertEquals(c.bgMuted, grey.container, theme.id)
            assertNull(grey.border, theme.id)
            val greyPressed =
                MonarchSignInWithRookButtonTokens.colors(neutral, enabled = true, pressed = true, colors = c)
            assertEquals(c.bgEmphasis, greyPressed.container, theme.id)
            val disabled = MonarchSignInWithRookButtonTokens.colors(light, enabled = false, pressed = true, colors = c)
            assertEquals(c.textDisabled, disabled.content, theme.id)
            assertEquals(c.borderSubtle, disabled.border, theme.id)
        }
    }
}
