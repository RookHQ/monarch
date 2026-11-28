package com.rookhq.monarch.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsList
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsListLabels
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsListTokens
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsRow
import com.rookhq.monarch.components.forms.switchsettingslist.MonarchSwitchSettingsStatus
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchSwitchSettingsListFixtures : ComponentFixtures {
    override val id = "switch-settings-list"
    override val fixtures = listOf(
        fixture("a list with a title") {
            MonarchSwitchSettingsList(title = "Email notifications", description = "Each setting saves on its own.") {
                MonarchSwitchSettingsRow(checked = true, onCheckedChange = {}, onSave = {}, label = "Weekly summary")
                MonarchSwitchSettingsRow(checked = false, onCheckedChange = {}, onSave = {}, label = "Product news")
            }
        },
        fixture("a row with a description") {
            MonarchSwitchSettingsList {
                MonarchSwitchSettingsRow(
                    checked = true,
                    onCheckedChange = {},
                    onSave = {},
                    label = "Weekly summary",
                    description = "Sent on Mondays.",
                )
            }
        },
        fixture("disabled") {
            MonarchSwitchSettingsList {
                MonarchSwitchSettingsRow(
                    checked = true,
                    onCheckedChange = {},
                    onSave = {},
                    label = "Security alerts",
                    enabled = false,
                )
            }
        },
        fixture("long text, outside a list") {
            MonarchSwitchSettingsRow(
                checked = false,
                onCheckedChange = {},
                onSave = {},
                label = "Send me a summary of the activity in every workspace I belong to",
                description = "Includes comments, mentions and changes to documents you follow.",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSwitchSettingsListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aToggleShowsTheNewValueWhileSavingThenSavedThenIdle() {
        val gate = CompletableDeferred<Unit>()
        val statuses = mutableListOf<MonarchSwitchSettingsStatus>()
        var saved = false
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            var checked by remember { mutableStateOf(false) }
            MonarchSwitchSettingsList(savedDurationMillis = SAVED_MILLIS) {
                MonarchSwitchSettingsRow(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        saved = it
                    },
                    onSave = { gate.await() },
                    label = "Weekly summary",
                    onStatusChange = { statuses += it },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val row = rule.onNodeWithText("Weekly summary")
        row.assertIsOff().performClick()
        rule.mainClock.advanceTimeByFrame()
        row.assertIsOn()
        rule.onNodeWithContentDescription("Saving").assertExists()
        assertEquals(false, saved)
        // Toggles while saving are ignored.
        row.performClick()
        rule.mainClock.advanceTimeByFrame()
        row.assertIsOn()

        gate.complete(Unit)
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertEquals(true, saved)
        row.assertIsOn()
        rule.onNodeWithText("Saved").assertDoesNotExist() // the visible text is hidden; the live region says it
        rule.onNodeWithContentDescription("Saved").assertExists()

        rule.mainClock.advanceTimeBy(SAVED_MILLIS + FRAME)
        rule.onNodeWithContentDescription("Saved").assertDoesNotExist()
        assertEquals(
            listOf(
                MonarchSwitchSettingsStatus.Saving,
                MonarchSwitchSettingsStatus.Saved,
                MonarchSwitchSettingsStatus.Idle,
            ),
            statuses,
        )
    }

    @Test
    fun aFailedSaveRollsBackShowsTheReasonAndRetriesTheSameValue() {
        val attempts = mutableListOf<Boolean>()
        var fail = true
        rule.setMonarchContent {
            var checked by remember { mutableStateOf(false) }
            MonarchSwitchSettingsRow(
                checked = checked,
                onCheckedChange = { checked = it },
                onSave = { value ->
                    attempts += value
                    if (fail) error("No connection.")
                },
                label = "Weekly summary",
            )
        }
        val row = rule.onNodeWithText("Weekly summary")
        row.performClick()
        rule.waitForIdle()
        row.assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Couldn’t save. No connection."))
        rule.onNodeWithContentDescription("Couldn’t save. No connection.").assertExists()
        val retry = rule.onNodeWithContentDescription("Retry Weekly summary")
        retry.assertTouchTargetAtLeast48dp(rule.density)
        fail = false
        retry.performClick()
        rule.waitForIdle()
        row.assertIsOn().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        assertEquals(listOf(true, true), attempts)
    }

    @Test
    fun aDisabledRowDoesNotSave() {
        var saves = 0
        rule.setMonarchContent {
            MonarchSwitchSettingsRow(
                checked = false,
                onCheckedChange = {},
                onSave = { saves++ },
                label = "Weekly summary",
                enabled = false,
            )
        }
        rule.onNodeWithText("Weekly summary").performClick()
        rule.waitForIdle()
        assertEquals(0, saves)
    }

    @Test
    fun theListLabelsApplyToEveryRowAndARowCanReplaceThem() {
        rule.setMonarchContent {
            MonarchSwitchSettingsList(
                labels = MonarchSwitchSettingsListLabels(
                    error = "Échec.",
                    retry = "Réessayer",
                    errorMessage = { null },
                ),
            ) {
                MonarchSwitchSettingsRow(
                    checked = false,
                    onCheckedChange = {},
                    onSave = { error("hidden") },
                    label = "Résumé",
                    labels = MonarchSwitchSettingsListLabels(retryLabel = { "Réessayer : $it" }),
                )
            }
        }
        rule.onNodeWithText("Résumé").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Échec.").assertExists()
        rule.onNodeWithContentDescription("Réessayer : Résumé").assertExists()
        rule.onNodeWithText("Réessayer", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theTitleIsAHeadingAndTheRowIsATouchTarget() {
        rule.setMonarchContent {
            MonarchSwitchSettingsList(title = "Email notifications") {
                MonarchSwitchSettingsRow(checked = true, onCheckedChange = {}, onSave = {}, label = "Weekly")
            }
        }
        rule.onNodeWithText("Email notifications").assert(isHeading())
        rule.onNodeWithText("Weekly").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theRowWrapsAtTheLargestFontScaleAndTheLabelIsAtTheStartInRightToLeft() {
        val label = "Send me a summary of the activity in every workspace"
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchSwitchSettingsRow(checked = true, onCheckedChange = {}, onSave = {}, label = label)
        }
        val text = rule.onNodeWithText(label, useUnmergedTree = true).assertTextFullyVisible()
        val row = rule.onNodeWithText(label).getUnclippedBoundsInRoot()
        val bounds = text.getUnclippedBoundsInRoot()
        assertTrue(bounds.right > (row.left + row.right) / 2, "the label is on the right in right to left")
    }

    @Test
    fun statusColoursAndTheErrorDetailComeFromRolesAndTheException() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchSwitchSettingsListTokens
            assertEquals(colors.textMuted, tokens.statusColor(MonarchSwitchSettingsStatus.Saving, colors), theme.id)
            assertEquals(colors.successStrong, tokens.statusColor(MonarchSwitchSettingsStatus.Saved, colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.statusColor(MonarchSwitchSettingsStatus.Error, colors), theme.id)
            assertEquals(colors.textMuted, tokens.descriptionColor(colors), theme.id)
        }
        val tokens = MonarchSwitchSettingsListTokens
        assertEquals("Offline.", tokens.errorDetail(IllegalStateException(" Offline. "), null))
        assertNull(tokens.errorDetail(IllegalStateException(), null))
        assertEquals("Try later.", tokens.errorDetail(IllegalStateException("x"), { "Try later." }))
        assertNull(tokens.errorDetail(IllegalStateException("x"), { " " }))
        assertEquals(true, tokens.shownValue(checked = false, pending = true))
        assertEquals(false, tokens.shownValue(checked = false, pending = null))
    }

    private companion object {
        const val SAVED_MILLIS = 2_000L
        const val FRAME = 32L
    }
}
