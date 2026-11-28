package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingChannel
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingConsent
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingConsentCheckbox
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingConsentCheckboxLabels
import dev.monarchui.components.forms.marketingconsentcheckbox.MonarchMarketingConsentCheckboxTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal object MonarchMarketingConsentCheckboxFixtures : ComponentFixtures {
    override val id = "marketing-consent-checkbox"
    override val fixtures = listOf(
        fixture("one channel, unticked") {
            MonarchMarketingConsentCheckbox(selection = emptySet(), onConsentChange = {})
        },
        fixture("one channel with notes and policy") {
            MonarchMarketingConsentCheckbox(
                selection = setOf(MonarchMarketingChannel.Email),
                onConsentChange = {},
                description = "Product updates and offers, about twice a month.",
                policyTitle = "Privacy policy",
                onPolicyClick = {},
            )
        },
        fixture("several channels") {
            MonarchMarketingConsentCheckbox(
                selection = setOf(MonarchMarketingChannel.Sms),
                onConsentChange = {},
                channels = listOf(MonarchMarketingChannel.Email, MonarchMarketingChannel.Sms),
                showUnsubscribeNote = false,
            )
        },
        fixture("disabled") {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = {},
                enabled = false,
                policyTitle = "Privacy policy",
                onPolicyClick = {},
            )
        },
        fixture("phone, long question") {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = {},
                channels = listOf(MonarchMarketingChannel.Phone),
                label = "Call me about new plans, events and offers from the company and its partners",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMarketingConsentCheckboxTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun startsUntickedAndReportsTheWordingShown() {
        var selection by mutableStateOf(emptySet<MonarchMarketingChannel>())
        var last: MonarchMarketingConsent? = null
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = selection,
                onConsentChange = {
                    last = it
                    selection = it.granted
                },
                description = "Product updates,   about twice a month.",
            )
        }
        val box = rule.onNodeWithText("Send me product news by email", substring = true)
        box.assertIsOff().performClick()
        box.assertIsOn()
        val consent = assertNotNull(last)
        assertEquals(mapOf(MonarchMarketingChannel.Email to true), consent.channels)
        assertEquals(
            "Send me product news by email Product updates, about twice a month. You can unsubscribe at any time.",
            consent.text,
        )
        assertTrue(consent.at > 0)
        assertEquals(setOf(MonarchMarketingChannel.Email), consent.granted)
    }

    @Test
    fun severalChannelsAreSeparateCheckboxesUnderAHeading() {
        var selection by mutableStateOf(emptySet<MonarchMarketingChannel>())
        var last: MonarchMarketingConsent? = null
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = selection,
                onConsentChange = {
                    last = it
                    selection = it.granted
                },
                channels = listOf(MonarchMarketingChannel.Email, MonarchMarketingChannel.Sms),
            )
        }
        val heading = rule.onNodeWithText("Send me product news").fetchSemanticsNode()
        assertTrue(heading.config.contains(SemanticsProperties.Heading))
        rule.onNodeWithText("Text message (SMS)").assertIsOff().performClick()
        rule.onNodeWithText("Text message (SMS)").assertIsOn()
        rule.onNodeWithText("Email").assertIsOff()
        val consent = assertNotNull(last)
        assertEquals(
            mapOf(MonarchMarketingChannel.Email to false, MonarchMarketingChannel.Sms to true),
            consent.channels,
        )
        assertEquals(
            "Send me product news Email Text message (SMS) You can unsubscribe at any time.",
            consent.text,
        )
    }

    @Test
    fun theQuestionUsesTheWordOfASingleChannel() {
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = {},
                channels = listOf(MonarchMarketingChannel.Post),
            )
        }
        rule.onNodeWithText("Send me product news by post", substring = true).assertExists()
    }

    @Test
    fun thePolicyLinkCallsBackAndHasA48dpTarget() {
        var opened = 0
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = {},
                policyTitle = "Privacy policy",
                onPolicyClick = { opened++ },
            )
        }
        rule.onNodeWithText("Privacy policy").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun disabledChangesNothing() {
        var changes = 0
        var opened = 0
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = { changes++ },
                enabled = false,
                policyTitle = "Privacy policy",
                onPolicyClick = { opened++ },
            )
        }
        rule.onNodeWithText("Send me product news by email", substring = true).assertIsNotEnabled().performClick()
        rule.onNodeWithText("Privacy policy").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        assertEquals(0, opened)
    }

    @Test
    fun builtInStringsCanBeReplacedAndHidden() {
        var last: MonarchMarketingConsent? = null
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(
                selection = emptySet(),
                onConsentChange = { last = it },
                channels = listOf(MonarchMarketingChannel.Email, MonarchMarketingChannel.Phone),
                label = "Keep me posted",
                labels = MonarchMarketingConsentCheckboxLabels(
                    unsubscribeNote = "Stop whenever you like.",
                    channelLabels = mapOf(MonarchMarketingChannel.Phone to "Calls"),
                ),
            )
        }
        rule.onNodeWithText("Calls").performClick()
        assertEquals("Keep me posted Email Calls Stop whenever you like.", last?.text)
    }

    @Test
    fun theNoteCanBeHidden() {
        rule.setMonarchContent {
            MonarchMarketingConsentCheckbox(selection = emptySet(), onConsentChange = {}, showUnsubscribeNote = false)
        }
        rule.onNodeWithText("You can unsubscribe at any time.", substring = true, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun theQuestionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMarketingConsentCheckbox(selection = emptySet(), onConsentChange = {})
        }
        rule.onNodeWithText("Send me product news by email", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchMarketingConsentCheckboxTokens
            assertEquals(colors.textDefault, t.question(true, colors), theme.id)
            assertEquals(colors.textDisabled, t.question(false, colors), theme.id)
            assertEquals(colors.textMuted, t.note(true, colors), theme.id)
            assertEquals(colors.textDisabled, t.note(false, colors), theme.id)
            assertEquals(colors.textLink, t.link(true, colors), theme.id)
            assertEquals(colors.textDisabled, t.link(false, colors), theme.id)
        }
    }

    @Test
    fun theWordingCollapsesWhitespaceAndSkipsEmptyParts() {
        assertEquals("a b c", MonarchMarketingConsentCheckboxTokens.wording(listOf(" a ", null, "", "b\n  c")))
    }
}
