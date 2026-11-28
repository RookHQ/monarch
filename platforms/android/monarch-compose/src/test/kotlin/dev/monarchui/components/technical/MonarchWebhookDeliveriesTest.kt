@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.technical

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveries
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveriesFormat
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveriesLabels
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveriesText
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveriesTokens
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDelivery
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveryFilters
import dev.monarchui.components.technical.webhookdeliveries.MonarchWebhookDeliveryStatus
import dev.monarchui.foundation.MonarchTone
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
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_800_000_000_000L
private const val Minute = 60_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private val FailedDelivery = MonarchWebhookDelivery(
    id = "d1",
    status = MonarchWebhookDeliveryStatus.Failed,
    eventType = "invoice.paid",
    eventId = "evt_1",
    attempts = 3,
    atMillis = Now - 5 * Minute,
    responseCode = 500,
    durationMillis = 1_203,
    error = "Internal server error",
)
private val RetryingDelivery = MonarchWebhookDelivery(
    id = "d2",
    status = MonarchWebhookDeliveryStatus.Retrying,
    eventType = "customer.updated",
    eventId = "evt_2",
    attempts = 1,
    atMillis = Now - 2 * Minute,
    nextAttemptMillis = Now + 10 * Minute,
    destination = "https://example.com/hooks",
    error = "No response within 10 seconds",
)
private val PendingDelivery = MonarchWebhookDelivery(
    id = "d3",
    status = MonarchWebhookDeliveryStatus.Pending,
    eventType = "invoice.created",
    attempts = 0,
    atMillis = Now - Minute,
)
private val DeliveredDelivery = MonarchWebhookDelivery(
    id = "d4",
    status = MonarchWebhookDeliveryStatus.Succeeded,
    eventType = "invoice.created",
    eventId = "evt_4",
    attempts = 1,
    atMillis = Now - 60 * Minute,
    responseCode = 200,
    durationMillis = 87,
)

internal object MonarchWebhookDeliveriesFixtures : ComponentFixtures {
    override val id = "webhook-deliveries"
    override val fixtures = listOf(
        fixture("a failed delivery with redeliver") {
            MonarchWebhookDeliveries(
                deliveries = listOf(FailedDelivery),
                filterable = false,
                now = Now,
                locale = Locale.US,
                onRedeliver = {},
            )
        },
        fixture("retrying, with the destination and a next attempt") {
            MonarchWebhookDeliveries(
                deliveries = listOf(RetryingDelivery),
                filterable = false,
                showDestination = true,
                now = Now,
                locale = Locale.US,
                onRedeliver = {},
            )
        },
        fixture("pending and not attempted, read only") {
            MonarchWebhookDeliveries(deliveries = listOf(PendingDelivery), filterable = false, now = Now)
        },
        fixture("filters and counts") {
            MonarchWebhookDeliveries(
                deliveries = emptyList(),
                eventTypes = listOf("invoice.paid"),
                counts = mapOf(MonarchWebhookDeliveryStatus.Succeeded to 12),
                locale = Locale.US,
            )
        },
        fixture("filtered to nothing") {
            MonarchWebhookDeliveries(
                deliveries = listOf(DeliveredDelivery),
                filterable = false,
                filters = MonarchWebhookDeliveryFilters(status = MonarchWebhookDeliveryStatus.Failed),
            )
        },
        fixture("empty with an action") {
            MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false) {
                MonarchButton(text = "Send a test event", onClick = {})
            }
        },
        fixture("loading") { MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false, loading = true) },
        fixture("failed to load") {
            MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false, failed = true, onRetry = {})
        },
        fixture("with a load more footer") {
            MonarchWebhookDeliveries(
                deliveries = listOf(DeliveredDelivery),
                filterable = false,
                now = Now,
                hasMore = true,
                onLoadMore = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchWebhookDeliveriesTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchWebhookDeliveriesTokens
    private val format = MonarchWebhookDeliveriesFormat
    private val text = MonarchWebhookDeliveriesText(
        MonarchWebhookDeliveriesLabels(),
        RuntimeEnvironment.getApplication().resources,
    )

    private fun announced(message: String) = rule.onNodeWithContentDescription(message)
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))

    private fun row(eventType: String) = rule.onNode(hasClickAction() and hasText(eventType))

    private fun show(
        deliveries: List<MonarchWebhookDelivery>,
        onRedeliver: (suspend (MonarchWebhookDelivery) -> Unit)? = null,
        onSelect: ((MonarchWebhookDelivery) -> Unit)? = null,
    ) = rule.setMonarchContent {
        MonarchWebhookDeliveries(
            deliveries = deliveries,
            modifier = Modifier.height(460.dp),
            filterable = false,
            now = Now,
            locale = Locale.US,
            timeZone = Utc.id,
            onRedeliver = onRedeliver,
            onSelect = onSelect,
        )
    }

    @Test
    fun aRowIsOneElementWithAViewDetailsAction() {
        show(listOf(FailedDelivery))
        val node = row("invoice.paid").fetchSemanticsNode()
        assertEquals("View delivery of invoice.paid evt_1", node.config[SemanticsActions.OnClick].label)
        row("invoice.paid")
            .assert(hasContentDescription("Failed"))
            .assert(hasText(format.relativeTime(Now - 5 * Minute, Now)))
            .assert(hasText("evt_1"))
            .assert(hasText("HTTP 500 · 1,203 ms"))
            .assert(hasText("3"))
            .assert(hasText("Internal server error"))
        rule.onNodeWithContentDescription("Webhook deliveries").assertExists()
    }

    @Test
    fun aScheduledRetryShowsTheNextAttemptAndTheDestination() {
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(RetryingDelivery),
                filterable = false,
                showDestination = true,
                now = Now,
            )
        }
        val next = "Next attempt ${format.relativeTime(Now + 10 * Minute, Now)}"
        row("customer.updated").assert(hasText(next)).assert(hasText("https://example.com/hooks"))
    }

    @Test
    fun tappingARowOpensTheDetailsSheet() {
        show(listOf(FailedDelivery))
        row("invoice.paid").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Event ID").assertExists()
        rule.onNodeWithText("Duration").assertExists()
        rule.onNodeWithText("1,203 ms").assertExists()
        rule.onNodeWithText(format.absoluteTime(Now - 5 * Minute, Locale.US, Utc), substring = true).assertExists()
    }

    @Test
    fun onSelectReplacesTheSheet() {
        var selected: String? = null
        show(listOf(FailedDelivery), onSelect = { selected = it.id })
        row("invoice.paid").performClick()
        rule.waitForIdle()
        assertEquals("d1", selected)
        rule.onNodeWithText("Event ID").assertDoesNotExist()
    }

    @Test
    fun redeliverShowsProgressThenAnnouncesTheResult() {
        val gate = CompletableDeferred<Unit>()
        val sent = mutableListOf<String>()
        show(
            listOf(FailedDelivery),
            onRedeliver = {
                gate.await()
                sent += it.id
            },
        )
        val button = rule.onNodeWithContentDescription("Redeliver invoice.paid evt_1")
        button.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals("Loading", button.stateDescription())
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals(listOf("d1"), sent)
        announced("Redelivery queued.")
    }

    @Test
    fun aFailedRedeliveryIsAnnounced() {
        show(listOf(FailedDelivery), onRedeliver = { throw IOException() })
        rule.onNodeWithContentDescription("Redeliver invoice.paid evt_1").performClick()
        rule.waitForIdle()
        announced("Could not redeliver. Try again.")
    }

    @Test
    fun pendingAndSendingDeliveriesOfferNoRedeliverByDefault() {
        show(listOf(PendingDelivery), onRedeliver = {})
        rule.onNodeWithText("Redeliver").assertDoesNotExist()
        assertFalse(tokens.redeliverable(PendingDelivery))
        assertFalse(tokens.redeliverable(PendingDelivery.copy(status = MonarchWebhookDeliveryStatus.Sending)))
        assertTrue(tokens.redeliverable(FailedDelivery))
    }

    @Test
    fun canRedeliverDecidesWhichRowsOfferIt() {
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(FailedDelivery),
                filterable = false,
                onRedeliver = {},
                canRedeliver = { false },
            )
        }
        rule.onNodeWithText("Redeliver").assertDoesNotExist()
    }

    @Test
    fun theStatusMenuFiltersAndClearFiltersShowsEverything() {
        var reported: MonarchWebhookDeliveryFilters? = null
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(FailedDelivery, DeliveredDelivery),
                modifier = Modifier.height(460.dp),
                filterStatuses = listOf(MonarchWebhookDeliveryStatus.Failed, MonarchWebhookDeliveryStatus.Succeeded),
                now = Now,
                onFiltersChange = { reported = it },
            )
        }
        rule.onNodeWithText("Any status").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Delivered") and hasAnyAncestor(isPopup())).performClick()
        rule.waitForIdle()
        assertEquals(MonarchWebhookDeliveryStatus.Succeeded, reported?.status)
        row("invoice.paid").assertDoesNotExist()
        row("invoice.created").assertExists()
        rule.onNodeWithContentDescription("Status").performClick()
        rule.onNode(hasText("Delivered") and hasAnyAncestor(isPopup())).assertIsSelected()
        rule.onNode(hasText("Any status") and hasAnyAncestor(isPopup())).performClick()
        rule.waitForIdle()
        row("invoice.paid").assertExists()
    }

    @Test
    fun searchMatchesTheEventIdExactlyAndTheTypeInPart() {
        val filters = MonarchWebhookDeliveryFilters(query = "EVT_1")
        assertEquals(listOf(FailedDelivery), filters.filter(listOf(FailedDelivery, RetryingDelivery)))
        assertEquals(emptyList(), MonarchWebhookDeliveryFilters(query = "evt").filter(listOf(FailedDelivery)))
        assertEquals(
            listOf(FailedDelivery),
            MonarchWebhookDeliveryFilters(query = "paid").filter(listOf(FailedDelivery, RetryingDelivery)),
        )
        assertFalse(MonarchWebhookDeliveryFilters(query = "  ").active)
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(FailedDelivery, RetryingDelivery),
                modifier = Modifier.height(460.dp),
                now = Now,
            )
        }
        rule.onNode(
            hasContentDescription("Find by event ID") and SemanticsMatcher.keyIsDefined(SemanticsActions.SetText),
        ).performTextInput("evt_2")
        rule.waitForIdle()
        row("invoice.paid").assertDoesNotExist()
        row("customer.updated").assertExists()
    }

    @Test
    fun nothingMatchingShowsTheFilteredStateWithClearFilters() {
        var reported: MonarchWebhookDeliveryFilters? = null
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(DeliveredDelivery),
                filterable = false,
                filters = MonarchWebhookDeliveryFilters(status = MonarchWebhookDeliveryStatus.Failed),
                onFiltersChange = { reported = it },
            )
        }
        rule.onNodeWithText("No deliveries match").assertExists()
        rule.onNodeWithText("Clear filters").performClick()
        assertEquals(MonarchWebhookDeliveryFilters(), reported)
    }

    @Test
    fun manualFilteringShowsTheDeliveriesAsGiven() {
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(DeliveredDelivery),
                filterable = false,
                filters = MonarchWebhookDeliveryFilters(status = MonarchWebhookDeliveryStatus.Failed),
                manualFiltering = true,
            )
        }
        row("invoice.created").assertExists()
    }

    @Test
    fun emptyLoadingAndFailedStates() {
        var retried = 0
        var state by mutableStateOf(0)
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = emptyList(),
                filterable = false,
                loading = state == 1,
                failed = state == 2,
                onRetry = { retried++ },
            )
        }
        rule.onNodeWithText("No deliveries yet").assertExists()
        state = 1
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Loading deliveries")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        state = 2
        rule.waitForIdle()
        rule.onNodeWithText("Deliveries did not load.", substring = true).assertExists()
        rule.onNode(hasText("Try again", substring = true) and hasClickAction()).performClick()
        assertEquals(1, retried)
    }

    @Test
    fun theCountsLineUsesStatusNamesAndTheLocale() {
        val registry = tokens.registry(text::statusName, null)
        val counts = mapOf(MonarchWebhookDeliveryStatus.Succeeded to 1_200, MonarchWebhookDeliveryStatus.Retrying to 3)
        assertEquals("1,200 delivered · 3 retrying", format.countLine(counts, registry, text, Locale.US))
        assertEquals("1.200 delivered · 3 retrying", format.countLine(counts, registry, text, Locale.GERMANY))
        rule.setMonarchContent {
            MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false, counts = counts, locale = Locale.US)
        }
        rule.onNodeWithText("1,200 delivered · 3 retrying")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theAppsStatusesOverrideAndExtendTheBuiltInOnes() {
        val custom = MonarchStatusRegistry(
            mapOf(
                MonarchWebhookDeliveryStatus.Gone to MonarchStatusDefinition("Dead letter", MonarchTone.Danger),
                "paused" to MonarchStatusDefinition("Paused", MonarchTone.Warning),
            ),
        )
        val registry = tokens.registry(text::statusName, custom)
        assertEquals("Dead letter", tokens.statusName(MonarchWebhookDeliveryStatus.Gone, registry))
        assertEquals("Paused", tokens.statusName("paused", registry))
        assertEquals("Delivered", tokens.statusName(MonarchWebhookDeliveryStatus.Succeeded, registry))
        assertEquals("unknown", tokens.statusName("unknown", registry))
        assertEquals(MonarchTone.Warning, registry.resolve(MonarchWebhookDeliveryStatus.Retrying)?.tone)
    }

    @Test
    fun theResponseLineCoversEveryCase() {
        assertEquals("HTTP 500 · 1,203 ms", tokens.outcome(FailedDelivery, text, Locale.US))
        assertEquals("Not attempted yet", tokens.outcome(PendingDelivery, text, Locale.US))
        assertNull(tokens.outcome(RetryingDelivery, text, Locale.US))
        assertNull(tokens.nextAttempt(FailedDelivery.copy(nextAttemptMillis = Now)))
        assertEquals(Now, tokens.nextAttempt(PendingDelivery.copy(nextAttemptMillis = Now)))
    }

    @Test
    fun datesInTheSheetUseTheGivenLocaleAndTimeZone() {
        val utc = format.absoluteTime(Now, Locale.US, Utc)
        val tokyo = format.absoluteTime(Now, Locale.US, TimeZone.getTimeZone("Asia/Tokyo"))
        assertTrue(utc.contains("2027"), utc)
        assertTrue(utc != tokyo)
    }

    @Test
    fun coloursComeFromRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.dangerStrong, tokens.errorColor(colors))
            assertEquals(colors.textMuted, tokens.metaColor(colors))
        }
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(FailedDelivery),
                modifier = Modifier.height(460.dp),
                onRedeliver = {},
                labels = MonarchWebhookDeliveriesLabels(
                    status = "State",
                    anyStatus = "All",
                    redeliver = "Send again",
                    redeliverItem = { "Send ${it.eventType} again" },
                    attempts = { "$it tries" },
                ),
            )
        }
        rule.onNodeWithText("All").assertExists()
        rule.onNodeWithContentDescription("Send invoice.paid again").assertExists()
        rule.onNodeWithText("Send again", useUnmergedTree = true).assertExists()
        row("invoice.paid").assert(hasText("HTTP 500 · 1,203 ms"))
    }

    @Test
    fun theFooterLoadsMore() {
        var loads = 0
        rule.setMonarchContent {
            MonarchWebhookDeliveries(
                deliveries = listOf(DeliveredDelivery),
                filterable = false,
                hasMore = true,
                onLoadMore = { loads++ },
            )
        }
        rule.onNode(hasText("Load more", substring = true) and hasClickAction()).performClick()
        assertEquals(1, loads)
    }

    @Test
    fun rightToLeftPutsTheStatusAtTheStart() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchWebhookDeliveries(deliveries = listOf(FailedDelivery), filterable = false, now = Now)
        }
        rule.onNodeWithText("Status", useUnmergedTree = true).assertIsBefore(
            rule.onNodeWithContentDescription("Failed", useUnmergedTree = true),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun longTextIsNotCutAtTheLargestFontScale() {
        val long = FailedDelivery.copy(
            eventType = "subscription.payment_method.automatically_updated",
            error = "The endpoint closed the connection before sending a response",
        )
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchWebhookDeliveries(deliveries = listOf(long), filterable = false, now = Now)
        }
        rule.onNodeWithText(long.eventType, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText(long.error!!, useUnmergedTree = true).assertTextFullyVisible()
    }
}
