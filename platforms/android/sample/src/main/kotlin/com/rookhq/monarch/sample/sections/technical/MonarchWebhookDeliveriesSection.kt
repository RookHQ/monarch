@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.sample.sections.technical

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.technical.webhookdeliveries.MonarchWebhookDeliveries
import com.rookhq.monarch.components.technical.webhookdeliveries.MonarchWebhookDelivery
import com.rookhq.monarch.components.technical.webhookdeliveries.MonarchWebhookDeliveryStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of WebhookDeliveries: tap a row for its details; Redeliver takes a moment. */
object MonarchWebhookDeliveriesSection : CataloguePage {
    override val title = "WebhookDeliveries"

    private const val MINUTE = 60_000L
    private const val HOUR = 3_600_000L
    private const val WORK_MILLIS = 700L
    private const val LIST_HEIGHT_STEPS = 6
    private const val PAGE_SIZE = 3
    private const val MAX_PAGES = 3
    private const val HOURS_A_DAY = 24
    private const val FAILED_COUNT = 1
    private const val DELIVERED_COUNT = 128

    private fun deliveries(now: Long, pages: Int): List<MonarchWebhookDelivery> {
        val first = listOf(
            MonarchWebhookDelivery(
                id = "1",
                status = MonarchWebhookDeliveryStatus.Failed,
                eventType = "invoice.paid",
                eventId = "evt_1Q2w3E",
                attempts = 3,
                atMillis = now - 5 * MINUTE,
                responseCode = 500,
                durationMillis = 1_203,
                error = "Internal server error",
            ),
            MonarchWebhookDelivery(
                id = "2",
                status = MonarchWebhookDeliveryStatus.Retrying,
                eventType = "customer.updated",
                eventId = "evt_8Hk2pQ",
                attempts = 1,
                atMillis = now - 2 * MINUTE,
                nextAttemptMillis = now + 10 * MINUTE,
                error = "No response within 10 seconds",
            ),
            MonarchWebhookDelivery(
                id = "3",
                status = MonarchWebhookDeliveryStatus.Succeeded,
                eventType = "invoice.created",
                eventId = "evt_5Rt7yU",
                attempts = 1,
                atMillis = now - HOUR,
                responseCode = 200,
                durationMillis = 87,
            ),
        )
        val older = (1 until pages).flatMap { page ->
            (0 until PAGE_SIZE).map { index ->
                MonarchWebhookDelivery(
                    id = "p$page-$index",
                    status = MonarchWebhookDeliveryStatus.Succeeded,
                    eventType = "invoice.created",
                    eventId = "evt_old_${page}_$index",
                    attempts = 1,
                    atMillis = now - (page * HOURS_A_DAY + index) * HOUR,
                    responseCode = 200,
                    durationMillis = 120,
                )
            }
        }
        return first + older
    }

    @Composable
    override fun Content() {
        val now = remember { System.currentTimeMillis() }
        var pages by rememberSaveable { mutableIntStateOf(1) }
        CatalogueCaption("A delivery log with filters, details and Redeliver")
        MonarchWebhookDeliveries(
            deliveries = deliveries(now, pages),
            modifier = Modifier.height(MonarchTheme.spacing.space24 * LIST_HEIGHT_STEPS),
            eventTypes = listOf("invoice.paid", "invoice.created", "customer.updated"),
            counts = mapOf(
                MonarchWebhookDeliveryStatus.Succeeded to DELIVERED_COUNT,
                MonarchWebhookDeliveryStatus.Failed to FAILED_COUNT,
            ),
            hasMore = pages < MAX_PAGES,
            onLoadMore = { pages++ },
            onRedeliver = { delay(WORK_MILLIS) },
        )
        CatalogueCaption("No deliveries yet")
        MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false)
    }
}
