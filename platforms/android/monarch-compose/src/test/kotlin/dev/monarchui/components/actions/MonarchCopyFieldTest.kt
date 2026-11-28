package dev.monarchui.components.actions

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.actions.copyfield.MonarchCopyField
import dev.monarchui.components.actions.copyfield.MonarchCopyFieldTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Hook = "https://example.com/hooks/1042"

internal object MonarchCopyFieldFixtures : ComponentFixtures {
    override val id = "copy-field"
    override val fixtures = listOf(
        fixture("text button") { MonarchCopyField(value = Hook, label = "Webhook URL") },
        fixture("custom icon, sensitive") {
            MonarchCopyField(
                value = "sk_live_example_1234",
                label = "API key",
                sensitive = true,
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("multiline, body text") {
            MonarchCopyField(
                value = "<iframe src=\"https://example.com/embed/7\" width=\"600\" height=\"400\"></iframe>",
                label = "Embed code",
                multiline = true,
                monospace = false,
            )
        },
        fixture("with helper text") {
            MonarchCopyField(value = "example.com", label = "Domain", helperText = "Add it as a CNAME record.")
        },
        fixture("disabled") { MonarchCopyField(value = Hook, label = "Webhook URL", enabled = false) },
        fixture("disabled, icon") {
            MonarchCopyField(
                value = Hook,
                label = "Webhook URL",
                enabled = false,
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("long label in a narrow width") {
            MonarchCopyField(
                value = Hook,
                label = "Address of the calendar subscription feed",
                modifier = Modifier.width(280.dp),
                copyLabel = "Copy the link",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCopyFieldTest {
    @get:Rule
    val rule = createComposeRule()

    private val isField = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

    private fun clipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()
    }

    @Test
    fun theButtonCopiesTheWholeValueAndIsNamedWithTheLabel() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent { MonarchCopyField(value = Hook, label = "Webhook URL", onCopy = { copied += it }) }
        val button = rule.onNodeWithContentDescription("Copy Webhook URL")
        assertNull(button.stateDescription())
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(Hook, clipboardText())
        assertEquals(listOf(Hook), copied)
        assertEquals("Copied to clipboard", button.stateDescription())
    }

    @Test
    fun theValueIsAReadOnlyFieldWithTheLabel() {
        rule.setMonarchContent { MonarchCopyField(value = Hook, label = "Webhook URL") }
        val field = rule.onNode(isField)
        field.assert(hasText(Hook)).assertContentDescriptionEquals("Webhook URL")
        // Read-only: the field offers no way to replace its text.
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    @Test
    fun theButtonShowsTheCopyIconAndTheWordCopyBesideTheFullValue() {
        rule.setMonarchContent { MonarchCopyField(value = Hook, label = "API key") }
        val button = rule.onNodeWithContentDescription("Copy API key").assertTouchTargetAtLeast48dp(rule.density)
        val field = rule.onNode(isField).getUnclippedBoundsInRoot()
        val beside = button.getUnclippedBoundsInRoot()
        assertTrue(beside.left >= field.right, "$beside is beside $field, not inside it")
        // The value is not truncated: the field node holds all of it and shows no ellipsis.
        assertEquals(Hook, rule.onNode(isField).fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        button.performClick()
        rule.waitForIdle()
        assertEquals(Hook, clipboardText())
    }

    @Test
    fun aSensitiveValueIsMarkedSensitiveOnTheClip() {
        rule.setMonarchContent { MonarchCopyField(value = "sk_live_1", label = "API key", sensitive = true) }
        rule.onNodeWithContentDescription("Copy API key").performClick()
        rule.waitForIdle()
        assertEquals("sk_live_1", clipboardText())
    }

    @Test
    fun aDisabledFieldDoesNotCopy() {
        var copies = 0
        rule.setMonarchContent {
            MonarchCopyField(value = Hook, label = "Webhook URL", enabled = false, onCopy = { copies++ })
        }
        rule.onNodeWithContentDescription("Copy Webhook URL").assertIsNotEnabled().performClick()
        rule.waitForIdle()
        assertEquals(0, copies)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCopyField(
                value = Hook,
                label = "Webhook",
                showConfirmation = true,
                copyLabel = "Copier",
                copiedLabel = "Copié",
                copiedAnnouncement = "Copié dans le presse-papiers",
            )
        }
        rule.onNodeWithContentDescription("Copier Webhook").performClick()
        rule.waitForIdle()
        assertEquals(
            "Copié dans le presse-papiers",
            rule.onNodeWithContentDescription("Copié Webhook").stateDescription(),
        )
    }

    @Test
    fun theButtonFollowsTheFieldInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchCopyField(value = Hook, label = "Webhook URL") }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNode(isField)
                .assertIsBefore(rule.onNodeWithContentDescription("Copy Webhook URL"), direction)
        }
    }

    @Test
    fun theButtonGrowsWithItsLabelAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCopyField(
                value = Hook,
                label = "Webhook URL",
                modifier = Modifier.width(300.dp),
                copyLabel = "Copy the link",
            )
        }
        val bounds = rule.onNodeWithContentDescription("Copy the link Webhook URL").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top > 48.dp, "The label wraps onto a second line instead of being cut")
    }

    @Test
    fun theValueUsesTheCodeStyleUnlessToldOtherwise() {
        rule.setMonarchContent {
            val typography = MonarchTheme.typography
            assertEquals(typography.code, MonarchCopyFieldTokens.textStyle(monospace = true, typography))
            assertEquals(typography.body, MonarchCopyFieldTokens.textStyle(monospace = false, typography))
        }
    }
}
