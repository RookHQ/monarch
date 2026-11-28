package dev.monarchui.components.identity

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.consenthistory.MonarchConsentMethod
import dev.monarchui.components.identity.consenthistory.MonarchConsentRecord
import dev.monarchui.components.identity.consenthistory.MonarchConsentRegime
import dev.monarchui.components.identity.consentpreferences.MonarchConsentPreferences
import dev.monarchui.components.identity.consentpreferences.MonarchConsentPreferencesCategory
import dev.monarchui.components.identity.consentpreferences.MonarchConsentPreferencesLabels
import dev.monarchui.components.identity.consentpreferences.MonarchConsentPreferencesMode
import dev.monarchui.components.identity.consentpreferences.MonarchConsentPreferencesTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Day = 86_400_000L
private const val Now = 1_800_000_000_000L

private val Necessary =
    MonarchConsentPreferencesCategory("necessary", "Necessary", "Keeps you signed in.", required = true)
private val Analytics = MonarchConsentPreferencesCategory("analytics", "Analytics", "Counts visits.")
private val Ads = MonarchConsentPreferencesCategory("ads", "Ads", "Shares data.", saleOrSharing = true)
private val All = listOf(Necessary, Analytics, Ads)
private val Short = MonarchConsentPreferencesLabels(description = "Choose.")

private fun saved(method: MonarchConsentMethod, at: Long = Now - 2 * Day) = MonarchConsentRecord(
    id = "r",
    dateMillis = at,
    method = method,
    policyVersion = "3",
    regime = MonarchConsentRegime.OptIn,
    categories = mapOf("analytics" to true),
)

internal object MonarchConsentPreferencesFixtures : ComponentFixtures {
    override val id = "consent-preferences"
    override val fixtures = listOf(
        fixture("first time, one optional category") {
            MonarchConsentPreferences(categories = listOf(Analytics), onDecide = { _, _ -> }, labels = Short)
        },
        fixture("required") {
            MonarchConsentPreferences(categories = listOf(Necessary), onDecide = { _, _ -> }, labels = Short)
        },
        fixture("privacy signal") {
            MonarchConsentPreferences(
                categories = listOf(Ads),
                onDecide = { _, _ -> },
                globalPrivacyControl = true,
                labels = Short,
            )
        },
        fixture("choice mode with a record") {
            MonarchConsentPreferences(
                categories = listOf(Analytics),
                onDecide = { _, _ -> },
                choices = mapOf("analytics" to true),
                mode = MonarchConsentPreferencesMode.Choice,
                record = saved(MonarchConsentMethod.AcceptAll),
                now = Now,
                labels = Short,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchConsentPreferencesTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchConsentPreferencesTokens

    @Test
    fun theFirstTimeEveryOptionalSwitchIsOffAndRequiredOnesAreLocked() {
        rule.setMonarchContent { MonarchConsentPreferences(categories = All, onDecide = { _, _ -> }) }
        rule.onNodeWithText("Privacy choices").assert(isHeading())
        rule.onNode(hasText("Analytics")).assertIsOff().assertTouchTargetAtLeast48dp(rule.density)
        rule.onNode(hasText("Necessary")).assertIsOn().assertIsNotEnabled()
        rule.onNode(hasText("Keeps you signed in.") and hasText("Always on")).assertExists()
    }

    @Test
    fun saveReportsTheSwitchesWithTheLocksApplied() {
        val decisions = mutableListOf<Pair<MonarchConsentMethod, Map<String, Boolean>>>()
        rule.setMonarchContent {
            MonarchConsentPreferences(categories = All, onDecide = { m, c -> decisions += m to c })
        }
        rule.onNode(hasText("Analytics")).performClick().assertIsOn()
        rule.onNodeWithText("Save choices").performClick()
        assertEquals(
            MonarchConsentMethod.Custom to mapOf("necessary" to true, "analytics" to true, "ads" to false),
            decisions.single(),
        )
    }

    @Test
    fun acceptAllKeepsSignalLockedCategoriesOffAndSaysWhy() {
        val decisions = mutableListOf<Map<String, Boolean>>()
        rule.setMonarchContent {
            MonarchConsentPreferences(
                categories = All,
                onDecide = { _, c -> decisions += c },
                globalPrivacyControl = true,
            )
        }
        rule.onNode(hasText("Ads")).assertIsOff().assertIsNotEnabled()
        rule.onNode(hasText("Shares data.\nOff because a Global Privacy Control signal is on.")).assertExists()
        rule.onNodeWithText("Accept all").performClick()
        rule.onNodeWithText("Reject all").performClick()
        assertEquals(
            listOf(
                mapOf("necessary" to true, "analytics" to true, "ads" to false),
                mapOf("necessary" to true, "analytics" to false, "ads" to false),
            ),
            decisions,
        )
    }

    @Test
    fun aSavedDecisionShowsItsRecordAndCanBeWithdrawn() {
        val methods = mutableListOf<MonarchConsentMethod>()
        rule.setMonarchContent {
            MonarchConsentPreferences(
                categories = All,
                onDecide = { m, _ -> methods += m },
                choices = mapOf("analytics" to true),
                record = saved(MonarchConsentMethod.Custom),
                now = Now,
            )
        }
        rule.onNode(hasText("Analytics")).assertIsOn()
        val time = tokens.savedTime(Now - 2 * Day, Now, Locale.US)
        rule.onNodeWithText("Policy version 3 · Saved $time").assertExists()
        rule.onNodeWithText("Withdraw consent").performClick()
        assertEquals(listOf(MonarchConsentMethod.Withdrawn), methods)
    }

    @Test
    fun choiceModeShowsStatusWordsAndOnlyTheTwoButtons() {
        rule.setMonarchContent {
            MonarchConsentPreferences(
                categories = All,
                onDecide = { _, _ -> },
                choices = mapOf("analytics" to true, "ads" to true),
                mode = MonarchConsentPreferencesMode.Choice,
                globalPrivacyControl = true,
                record = saved(MonarchConsentMethod.AcceptAll),
                now = Now,
            )
        }
        assertEquals("On", rule.onNodeWithContentDescription("Analytics, Counts visits.").stateDescription())
        assertEquals("Always on", rule.onNodeWithContentDescription("Necessary", substring = true).stateDescription())
        assertEquals("Off", rule.onNodeWithContentDescription("Ads", substring = true).stateDescription())
        rule.onNodeWithText("Save choices").assertDoesNotExist()
        rule.onNodeWithText("Withdraw consent").assertDoesNotExist()
        rule.onNodeWithText("Accept all").assertExists()
    }

    @Test
    fun thePolicyLinkAndTheStringsCanBeReplaced() {
        var opened = 0
        rule.setMonarchContent {
            MonarchConsentPreferences(
                categories = listOf(Necessary),
                onDecide = { _, _ -> },
                onPolicyClick = { opened++ },
                labels = MonarchConsentPreferencesLabels(
                    title = "Privacidad",
                    policy = "Política",
                    alwaysOn = "Siempre activo",
                    save = "Guardar",
                ),
            )
        }
        rule.onNodeWithText("Privacidad").assertExists()
        rule.onNodeWithText("Guardar").assertExists()
        rule.onNode(hasText("Keeps you signed in.") and hasText("Siempre activo")).assertExists()
        rule.onNodeWithText("Política").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun rejectAllComesBeforeAcceptAllRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchConsentPreferences(categories = listOf(Analytics), onDecide = { _, _ -> })
        }
        rule.onNodeWithText("Reject all").assertIsBefore(rule.onNodeWithText("Accept all"), LayoutDirection.Rtl)
    }

    @Test
    fun theDescriptionWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchConsentPreferences(categories = listOf(Analytics), onDecide = { _, _ -> })
        }
        rule.onNodeWithText("Choose which optional uses", substring = true).assertTextFullyVisible()
    }

    @Test
    fun decisionsApplyTheLocksAndReportEveryCategory() {
        val draft = mapOf("ads" to true)
        assertEquals(
            mapOf("necessary" to true, "analytics" to false, "ads" to true),
            tokens.decision(MonarchConsentMethod.Custom, All, draft, globalPrivacyControl = false),
        )
        assertEquals(
            mapOf("necessary" to true, "analytics" to false, "ads" to false),
            tokens.decision(MonarchConsentMethod.Withdrawn, All, draft, globalPrivacyControl = false),
        )
        assertTrue(tokens.locked(Necessary, globalPrivacyControl = false))
        assertTrue(tokens.locked(Ads, globalPrivacyControl = true))
        assertFalse(tokens.locked(Ads, globalPrivacyControl = false))
        val categories = MonarchConsentPreferencesMode.Categories
        assertTrue(tokens.offersWithdraw(categories, true, saved(MonarchConsentMethod.Custom)))
        assertFalse(tokens.offersWithdraw(categories, true, saved(MonarchConsentMethod.Withdrawn)))
        assertFalse(tokens.offersWithdraw(categories, false, saved(MonarchConsentMethod.Custom)))
        val choice = MonarchConsentPreferencesMode.Choice
        assertFalse(tokens.offersWithdraw(choice, true, saved(MonarchConsentMethod.Custom)))
        assertFalse(tokens.offersWithdraw(categories, true, null))
    }

    @Test
    fun anOldDecisionShowsItsDate() {
        val old = tokens.savedTime(Now - 30 * Day, Now, Locale.US)
        val recent = tokens.savedTime(Now - 2 * Day, Now, Locale.US)
        assertFalse(old.contains("ago"))
        assertTrue(recent != old)
        assertEquals("Policy version 3 · Saved x", tokens.recordLine("Policy version", "3", "Saved", "x"))
    }

    @Test
    fun theStatusColourComesFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.successStrong, tokens.statusColor(on = true, c), theme.id)
            assertEquals(c.textMuted, tokens.statusColor(on = false, c), theme.id)
        }
    }
}
