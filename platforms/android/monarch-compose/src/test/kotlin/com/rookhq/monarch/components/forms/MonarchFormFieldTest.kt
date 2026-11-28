package com.rookhq.monarch.components.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Switch
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.forms.formfield.MonarchFormFieldLayout
import com.rookhq.monarch.components.forms.formfield.MonarchFormFieldTokens
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.field.MonarchFieldState
import com.rookhq.monarch.internal.field.rememberMonarchFieldState
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal object MonarchFormFieldFixtures : ComponentFixtures {
    override val id = "form-field"
    override val fixtures = listOf(
        fixture("stack, required, with help") {
            MonarchFormField(label = "Email", required = true, helperText = "We send receipts here.") {
                MonarchTextField(value = "", onValueChange = {}, modifier = it)
            }
        },
        fixture("stack, optional, with an error and an icon") {
            MonarchFormField(
                label = "Company",
                optionalText = "Optional",
                errorText = "Enter at most 80 characters.",
                errorIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            ) { MonarchTextField(value = "Rook", onValueChange = {}, modifier = it) }
        },
        fixture("row, with a switch") {
            MonarchFormField(
                label = "Weekly digest",
                layout = MonarchFormFieldLayout.Row,
                helperText = "Sent on Monday mornings.",
            ) { Switch(checked = true, onCheckedChange = {}, modifier = it) }
        },
        fixture("row, with an error") {
            MonarchFormField(label = "Alerts", layout = MonarchFormFieldLayout.Row, errorText = "Could not turn on.") {
                Switch(checked = false, onCheckedChange = {}, modifier = it)
            }
        },
        fixture("disabled") {
            MonarchFormField(label = "Plan", enabled = false, required = true, helperText = "Set by your admin.") {
                MonarchTextField(value = "Team", onValueChange = {}, modifier = it, enabled = false)
            }
        },
        fixture("long label and help") {
            MonarchFormField(
                label = "The address we send the yearly tax statement to",
                helperText = "Use the address registered with your tax office, including the postal code.",
            ) { MonarchTextField(value = "", onValueChange = {}, modifier = it) }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFormFieldTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aFieldBoxInsideAFieldWithAnErrorIsDrawnInTheErrorState() {
        // PREMIUM rule 10: an error is a red border plus the icon and the message, also when the error is given
        // to the form field and not to the control.
        var withError: MonarchFieldState? = null
        var withoutError: MonarchFieldState? = null
        rule.setMonarchContent {
            Column {
                MonarchFormField(label = "Project name", errorText = "Enter a name for the project.") {
                    withError = rememberMonarchFieldState(remember { MutableInteractionSource() }, enabled = true)
                }
                MonarchFormField(label = "Code", helperText = "Optional.") {
                    withoutError = rememberMonarchFieldState(
                        remember { MutableInteractionSource() },
                        enabled = true,
                        success = true,
                    )
                }
            }
        }
        rule.waitForIdle()
        assertEquals(true, withError?.invalid)
        assertEquals(false, withoutError?.invalid)
        assertEquals(true, withoutError?.success)
    }

    @Test
    fun theControlIsNamedByTheLabelAndTheVisibleLabelIsNotReadTwice() {
        rule.setMonarchContent {
            MonarchFormField(label = "Email", helperText = "We send receipts here.") {
                MonarchTextField(value = "", onValueChange = {}, modifier = it.testTag("control"))
            }
        }
        rule.onNodeWithTag("control").assert(hasContentDescription("Email"))
        rule.onNodeWithText("Email").assertDoesNotExist()
        rule.onNodeWithText("We send receipts here.").assertExists()
    }

    @Test
    fun aRequiredFieldAddsTheSpokenWordWhichCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchFormField(label = "Email", required = true) {
                    MonarchTextField(value = "", onValueChange = {}, modifier = it.testTag("first"))
                }
                MonarchFormField(label = "Courriel", required = true, requiredLabel = "obligatoire") {
                    MonarchTextField(value = "", onValueChange = {}, modifier = it.testTag("second"))
                }
            }
        }
        rule.onNodeWithTag("first").assert(hasContentDescription("Email, Required"))
        rule.onNodeWithTag("second").assert(hasContentDescription("Courriel, obligatoire"))
        rule.onNodeWithText("*").assertDoesNotExist()
    }

    @Test
    fun theErrorIsSetOnTheControlReplacesTheHelpAndIsAnnounced() {
        rule.setMonarchContent {
            MonarchFormField(
                label = "Email",
                modifier = Modifier.testTag("field"),
                helperText = "We send receipts here.",
                errorText = "Enter an email address.",
            ) { MonarchTextField(value = "x", onValueChange = {}, modifier = it.testTag("control")) }
        }
        rule.onNodeWithTag("control")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter an email address."))
        rule.onNodeWithText("We send receipts here.").assertExists()
        val live = rule.onNodeWithTag("field").fetchSemanticsNode().children.single {
            it.config.getOrNull(SemanticsProperties.LiveRegion) != null
        }
        assertEquals(LiveRegionMode.Polite, live.config[SemanticsProperties.LiveRegion])
        assertEquals(listOf("Enter an email address."), live.config[SemanticsProperties.ContentDescription])
    }

    @Test
    fun aBlankErrorIsNoError() {
        assertNull(MonarchFormFieldTokens.error("  "))
        assertNull(MonarchFormFieldTokens.error(null))
        rule.setMonarchContent {
            MonarchFormField(label = "Email", helperText = "Help", errorText = "") {
                MonarchTextField(value = "", onValueChange = {}, modifier = it.testTag("control"))
            }
        }
        rule.onNodeWithTag("control").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        rule.onNodeWithText("Help").assertExists()
    }

    @Test
    fun inTheRowLayoutTheLabelIsBeforeTheControlInBothDirectionsAndTheHelpStays() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchFormField(
                label = "Weekly digest",
                layout = MonarchFormFieldLayout.Row,
                helperText = "Sent on Mondays.",
                errorText = "Could not save.",
            ) { Switch(checked = true, onCheckedChange = {}, modifier = it.testTag("control")) }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Sent on Mondays.").assertIsBefore(rule.onNodeWithTag("control"), direction)
        }
    }

    @Test
    fun helpTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFormField(label = "Address", helperText = "Use the address registered with your tax office.") {
                MonarchTextField(value = "", onValueChange = {}, modifier = it)
            }
        }
        rule.onNodeWithText("Use the address registered with your tax office.").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheTextAndDangerRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchFormFieldTokens
            assertEquals(colors.textDefault, tokens.labelColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.labelColor(false, colors), theme.id)
            assertEquals(colors.textMuted, tokens.helperColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.helperColor(false, colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.requiredColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, tokens.requiredColor(false, colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.errorColor(colors), theme.id)
        }
    }
}
