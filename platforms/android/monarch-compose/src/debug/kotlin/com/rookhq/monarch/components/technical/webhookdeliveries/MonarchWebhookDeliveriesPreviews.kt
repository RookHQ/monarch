@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.technical.webhookdeliveries

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale
import java.util.TimeZone

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewMinute = 60_000L
private const val PreviewHeightSteps = 6

private val PreviewDeliveries = listOf(
    MonarchWebhookDelivery(
        id = "1",
        status = MonarchWebhookDeliveryStatus.Failed,
        eventType = "invoice.paid",
        eventId = "evt_1Q2w3E",
        attempts = 3,
        atMillis = PreviewNow - 5 * PreviewMinute,
        responseCode = 500,
        durationMillis = 1_203,
        error = "Internal server error",
    ),
    MonarchWebhookDelivery(
        id = "2",
        status = MonarchWebhookDeliveryStatus.Retrying,
        eventType = "customer.updated",
        attempts = 1,
        atMillis = PreviewNow - 2 * PreviewMinute,
        nextAttemptMillis = PreviewNow + 10 * PreviewMinute,
        error = "No response within 10 seconds",
    ),
    MonarchWebhookDelivery(
        id = "3",
        status = MonarchWebhookDeliveryStatus.Succeeded,
        eventType = "invoice.created",
        attempts = 1,
        atMillis = PreviewNow - 60 * PreviewMinute,
        responseCode = 200,
        durationMillis = 87,
    ),
)

@MonarchPreviews
@Composable
internal fun MonarchWebhookDeliveriesPreview() {
    PreviewFrame {
        MonarchWebhookDeliveries(
            deliveries = PreviewDeliveries,
            modifier = Modifier.height(MonarchTheme.spacing.space24 * PreviewHeightSteps),
            counts = mapOf(MonarchWebhookDeliveryStatus.Succeeded to 12, MonarchWebhookDeliveryStatus.Failed to 1),
            now = PreviewNow,
            locale = Locale.US,
            timeZone = "UTC",
            onRedeliver = {},
            onLoadMore = {},
            hasMore = true,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchWebhookDeliveriesStatesPreview() {
    PreviewFrame {
        MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false)
        MonarchWebhookDeliveries(deliveries = emptyList(), filterable = false, failed = true, onRetry = {})
    }
}
