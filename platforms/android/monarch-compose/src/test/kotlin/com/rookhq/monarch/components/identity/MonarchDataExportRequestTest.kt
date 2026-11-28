@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity

import android.icu.util.TimeZone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportFormatOption
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequest
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequestDetails
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequestLabels
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequestRules
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportRequestTokens
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportScopeOption
import com.rookhq.monarch.components.identity.dataexportrequest.MonarchDataExportStatus
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Day = 1_800_000_000_000L
private val Scope = listOf(
    MonarchDataExportScopeOption("profile", "Profile"),
    MonarchDataExportScopeOption("messages", "Messages", "Every conversation"),
)
private val Formats = listOf(
    MonarchDataExportFormatOption("json", "JSON"),
    MonarchDataExportFormatOption("csv", "CSV"),
)
private val FixedDates = MonarchDataExportRequestLabels(date = { "day $it" })

internal object MonarchDataExportRequestFixtures : ComponentFixtures {
    override val id = "data-export-request"
    override val fixtures = listOf(
        fixture("idle with scope") {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Idle, scope = Scope, onRequest = {})
        },
        fixture("idle with formats and a note") {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Idle,
                formats = Formats,
                preparationNote = "It can take up to 48 hours.",
                onRequest = {},
            )
        },
        fixture("requested") {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Requested, requestedAt = Day, labels = FixedDates)
        },
        fixture("ready with download", tall = true) {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Ready,
                readyAt = Day,
                expiresAt = Day,
                onDownload = {},
                labels = FixedDates,
            )
        },
        fixture("expired, request again") {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Expired, onRequest = {})
        },
        fixture("failed with a reason, pending") {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Failed,
                failureReason = "The archive was larger than the limit for one download.",
                pending = true,
                onRequest = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDataExportRequestTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun theRequestSendsTheChosenScopeAndFormat() {
        var sent: MonarchDataExportRequestDetails? = null
        rule.setMonarchContent {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Idle,
                scope = Scope,
                formats = Formats,
                defaultFormat = "csv",
                onRequest = { sent = it },
            )
        }
        rule.onNodeWithText("Download your data").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Not requested").assertExists()
        rule.onNode(hasText("Profile")).performClick().assertIsOff()
        rule.onNodeWithText("Request export").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(MonarchDataExportRequestDetails(listOf("messages"), "csv"), sent)
    }

    @Test
    fun noScopeTickedStopsTheRequestWithAnError() {
        var calls = 0
        rule.setMonarchContent {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Idle, scope = Scope, onRequest = { calls++ })
        }
        rule.onNode(hasText("Profile")).performClick()
        rule.onNode(hasText("Messages")).performClick()
        rule.onNodeWithText("Request export").performClick()
        rule.waitForIdle()
        assertEquals(0, calls)
        rule.onNodeWithText("Choose at least one kind of data.", useUnmergedTree = true).assertExists()
        rule.onNode(hasText("Profile")).performClick()
        rule.onNodeWithText("Choose at least one kind of data.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun aFailedRequestShowsTheMessageOrTheBuiltInSentence() {
        var message: String? = "Too many requests today."
        rule.setMonarchContent {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Expired,
                onRequest = { throw IOException(message) },
            )
        }
        rule.onNodeWithText("Request a new export").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Too many requests today.", useUnmergedTree = true).assertExists()
        message = null
        rule.onNodeWithText("Request a new export").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The request did not go through. Try again.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStatusLineIsAPoliteLiveRegionThatFollowsTheStatus() {
        var status by mutableStateOf(MonarchDataExportStatus.Requested)
        rule.setMonarchContent {
            MonarchDataExportRequest(
                status = status,
                requestedAt = Day,
                readyAt = Day,
                expiresAt = Day + 1,
                failureReason = "Too large.",
                labels = FixedDates,
            )
        }
        rule.onNode(polite).assertContentDescriptionEquals("We received your request on day $Day.")
        status = MonarchDataExportStatus.Ready
        rule.waitForIdle()
        rule.onNode(polite).assertContentDescriptionEquals(
            "Your export is ready. Prepared on day $Day. You can download it until day ${Day + 1}.",
        )
        status = MonarchDataExportStatus.Failed
        rule.waitForIdle()
        rule.onNode(polite).assertContentDescriptionEquals(
            "Your export could not be prepared. Request it again. Too large.",
        )
        status = MonarchDataExportStatus.Expired
        rule.waitForIdle()
        rule.onNode(polite).assertContentDescriptionEquals(
            "The download expired on day ${Day + 1}. Request a new export to get your data.",
        )
    }

    @Test
    fun preparingShowsANamedProgressBar() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Preparing,
                requestedAt = Day,
                labels = FixedDates,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNode(polite).assertContentDescriptionEquals("Your export is being prepared. Requested on day $Day.")
        rule.onNodeWithContentDescription("Preparing your export").assertExists()
    }

    @Test
    fun downloadIsOfferedOnlyWhenReady() {
        var downloads = 0
        var status by mutableStateOf(MonarchDataExportStatus.Ready)
        rule.setMonarchContent {
            MonarchDataExportRequest(status = status, onDownload = { downloads++ })
        }
        rule.onNodeWithText("Download").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, downloads)
        rule.onNodeWithText("Request export").assertDoesNotExist()
        status = MonarchDataExportStatus.Expired
        rule.waitForIdle()
        rule.onNodeWithText("Download").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDataExportRequest(
                status = MonarchDataExportStatus.Idle,
                onRequest = {},
                labels = MonarchDataExportRequestLabels(
                    title = "Deine Daten",
                    request = "Anfordern",
                    status = { "Status ${it.name}" },
                ),
            )
        }
        rule.onNodeWithText("Deine Daten").assertExists()
        rule.onNodeWithText("Anfordern").assertExists()
        rule.onNodeWithText("Status Idle").assertExists()
    }

    @Test
    fun theBadgeFollowsTheTitleInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Idle)
        }
        rule.onNodeWithText("Download your data")
            .assertIsBefore(rule.onNodeWithText("Not requested"), LayoutDirection.Rtl)
    }

    @Test
    fun theTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDataExportRequest(status = MonarchDataExportStatus.Idle)
        }
        rule.onNodeWithText("Get a copy of the data in your account.").assertTextFullyVisible()
    }

    @Test
    fun theRulesDecideToneFormAndNote() {
        val rules = MonarchDataExportRequestRules
        assertEquals(MonarchTone.Success, rules.tone(MonarchDataExportStatus.Ready))
        assertEquals(MonarchTone.Danger, rules.tone(MonarchDataExportStatus.Failed))
        assertEquals(MonarchTone.Info, rules.tone(MonarchDataExportStatus.Preparing))
        assertEquals(MonarchTone.Neutral, rules.tone(MonarchDataExportStatus.Expired))
        assertTrue(rules.canRequest(MonarchDataExportStatus.Failed, hasOnRequest = true))
        assertFalse(rules.canRequest(MonarchDataExportStatus.Preparing, hasOnRequest = true))
        assertFalse(rules.canRequest(MonarchDataExportStatus.Idle, hasOnRequest = false))
        assertTrue(rules.showsNote(MonarchDataExportStatus.Requested))
        assertFalse(rules.showsNote(MonarchDataExportStatus.Ready))
        assertEquals(listOf("profile", "messages"), rules.chosenScope(Scope, listOf("messages", "gone", "profile")))
        assertTrue(rules.scopeMissing(Scope, listOf("gone")))
        assertEquals("json", rules.initialFormat(Formats, "xml"))
        assertNull(rules.initialFormat(emptyList(), null))
    }

    @Test
    fun datesUseTheMediumDateAndShortTimeOfTheLocale() {
        val text = MonarchDataExportRequestRules.formatDate(Day, Locale.US, TimeZone.getTimeZone("UTC"))
        assertTrue(text.startsWith("Jan 15, 2027"), text)
        assertTrue("8:00" in text, text)
    }

    @Test
    fun theColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.textMuted, MonarchDataExportRequestTokens.noteColor(colors))
            assertEquals(colors.textDefault, MonarchDataExportRequestTokens.statusColor(colors))
        }
    }
}
