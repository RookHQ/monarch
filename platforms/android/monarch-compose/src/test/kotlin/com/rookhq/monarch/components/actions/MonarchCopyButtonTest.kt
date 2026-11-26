package com.rookhq.monarch.components.actions

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButtonTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchCopyButtonFixtures : ComponentFixtures {
    override val id = "copy-button"
    override val fixtures = MonarchButtonVariant.entries.map { variant ->
        fixture("${variant.name}, every size") {
            Column {
                MonarchButtonSize.entries.forEach { size ->
                    MonarchCopyButton(value = "https://example.com", variant = variant, size = size)
                }
            }
        }
    } + listOf(
        fixture("with icons and a target") {
            MonarchCopyButton(
                value = "https://example.com/hooks/1042",
                target = "webhook URL",
                leadingIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                copiedIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("disabled") { MonarchCopyButton(value = "x", enabled = false) },
        fixture("icon only") {
            MonarchCopyButton(
                value = "sk_live_1234",
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                target = "API key",
            )
        },
        fixture("icon only, disabled") {
            MonarchCopyButton(
                value = "sk_live_1234",
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                enabled = false,
            )
        },
        fixture("long labels in a narrow width") {
            MonarchCopyButton(
                value = "x",
                modifier = Modifier.width(140.dp),
                label = "Copy the invitation link",
                copiedLabel = "Link copied",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCopyButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun clipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    }

    @Test
    fun aTapCopiesTheValueAndAnnouncesIt() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchCopyButton(
                value = "https://example.com/hooks/1042",
                modifier = Modifier.testTag("copy"),
                target = "webhook URL",
                onCopy = { copied += it },
            )
        }
        val button = rule.onNodeWithContentDescription("Copy webhook URL")
        button.assert(isButton).assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertNull(button.stateDescription())
        button.performClick()
        rule.waitForIdle()
        assertEquals("https://example.com/hooks/1042", clipboardText())
        assertEquals(listOf("https://example.com/hooks/1042"), copied)
        assertEquals("Copied to clipboard", rule.onNodeWithTag("copy").stateDescription())
    }

    @Test
    fun withTheConfirmationTheLabelChangesAndThenResets() {
        rule.setMonarchContent {
            MonarchCopyButton(value = "x", modifier = Modifier.testTag("copy"), showConfirmation = true)
        }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("copy").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Copied").assertExists()
        rule.mainClock.advanceTimeBy(MonarchCopyButtonTokens.DEFAULT_RESET_MILLIS + 100)
        rule.onNodeWithContentDescription("Copy").assertExists()
        assertNull(rule.onNodeWithTag("copy").stateDescription())
    }

    @Test
    fun withoutTheConfirmationTheLabelStaysButTheStateIsStillSpoken() {
        rule.setMonarchContent {
            MonarchCopyButton(value = "x", modifier = Modifier.testTag("copy"), showConfirmation = false)
        }
        rule.onNodeWithTag("copy").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Copy").assertExists()
        assertEquals("Copied to clipboard", rule.onNodeWithTag("copy").stateDescription())
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCopyButton(
                value = "x",
                modifier = Modifier.testTag("copy"),
                showConfirmation = true,
                label = "Copier",
                copiedLabel = "Copié",
                copiedAnnouncement = "Copié dans le presse-papiers",
            )
        }
        rule.onNodeWithContentDescription("Copier").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Copié").assertExists()
        assertEquals("Copié dans le presse-papiers", rule.onNodeWithTag("copy").stateDescription())
    }

    @Test
    fun aDisabledButtonDoesNotCopy() {
        var copies = 0
        rule.setMonarchContent { MonarchCopyButton(value = "secret", enabled = false, onCopy = { copies++ }) }
        rule.onNodeWithContentDescription("Copy").assertIsNotEnabled().performClick()
        rule.waitForIdle()
        assertEquals(0, copies)
    }

    @Test
    fun theIconOnlyButtonIsNamedAndCopies() {
        rule.setMonarchContent {
            MonarchCopyButton(
                value = "sk_live_1234",
                icon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                target = "API key",
                sensitive = true,
            )
        }
        val button = rule.onNodeWithContentDescription("Copy API key").assert(isButton)
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("sk_live_1234", clipboardText())
        assertEquals("Copied to clipboard", button.stateDescription())
    }

    @Test
    fun aSensitiveClipIsMarkedForTheSystemPreview() {
        val clip = MonarchCopyButtonTokens.clip("API key", "sk_live_1234", sensitive = true)
        assertTrue(clip.description.extras.getBoolean(MonarchCopyButtonTokens.EXTRA_IS_SENSITIVE))
        val plain = MonarchCopyButtonTokens.clip("Link", "https://example.com", sensitive = false)
        assertNull(plain.description.extras)
        assertEquals("text/plain", plain.description.getMimeType(0))
    }

    @Test
    fun theCopiedStateIsShownOnlyWithTheConfirmation() {
        assertTrue(MonarchCopyButtonTokens.showsCopied(copied = true, showConfirmation = true))
        assertTrue(!MonarchCopyButtonTokens.showsCopied(copied = true, showConfirmation = false))
        assertTrue(!MonarchCopyButtonTokens.showsCopied(copied = false, showConfirmation = true))
    }

    @Test
    fun everyFormHasATouchTargetOfAtLeast48dp() {
        rule.setMonarchContent {
            Column {
                MonarchButtonSize.entries.forEach { size ->
                    MonarchCopyButton(value = "x", modifier = Modifier.testTag(size.name), size = size)
                }
            }
        }
        MonarchButtonSize.entries.forEach { rule.onNodeWithTag(it.name).assertTouchTargetAtLeast48dp(rule.density) }
    }

    @Test
    fun theButtonKeepsItsWidthWhenTheLabelChanges() {
        rule.setMonarchContent {
            Box {
                MonarchCopyButton(
                    value = "x",
                    modifier = Modifier.testTag("copy"),
                    showConfirmation = true,
                    label = "Copy",
                    copiedLabel = "Copied to your clipboard",
                )
            }
        }
        val before = rule.onNodeWithTag("copy").getUnclippedBoundsInRoot()
        rule.onNodeWithTag("copy").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("copy").assertWidthIsEqualTo(before.right - before.left)
    }

    @Test
    fun theIconIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCopyButton(
                value = "x",
                modifier = Modifier.testTag("copy"),
                leadingIcon = { Box(Modifier.testTag("icon").width(16.dp)) },
            )
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val button = rule.onNodeWithTag("copy").getUnclippedBoundsInRoot()
            val icon = rule.onNodeWithTag("icon", useUnmergedTree = true)
            val centre = (button.left + button.right) / 2
            val iconBounds = icon.getUnclippedBoundsInRoot()
            if (direction == LayoutDirection.Ltr) {
                assertTrue(iconBounds.right <= centre, "LTR: $iconBounds in $button ")
            } else {
                assertTrue(iconBounds.left >= centre, "RTL: $iconBounds in $button")
            }
        }
    }

    @Test
    fun theLabelGrowsTheButtonAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCopyButton(value = "x", modifier = Modifier.testTag("copy").width(120.dp), label = "Copy the link")
        }
        val bounds = rule.onNodeWithTag("copy").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top > 48.dp, "The label wraps onto a second line instead of being cut")
    }

    @Test
    fun theIconOnlyColoursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchCopyButtonTokens.iconColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchCopyButtonTokens.iconColor(false, colors), theme.id)
        }
    }
}
