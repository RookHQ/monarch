package com.rookhq.monarch.components.technical.webhookdeliveries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionItem
import com.rookhq.monarch.components.datadisplay.descriptionlist.MonarchDescriptionList
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchTimeZone
import java.util.TimeZone

private typealias SheetTokens = MonarchWebhookDeliveriesTokens
private typealias SheetFormat = MonarchWebhookDeliveriesFormat

/**
 * The details of one delivery in a `MonarchBottomSheet`: the status
 * and event as a heading, every known fact as a `MonarchDescriptionList` with
 * a copyable event id, Redeliver, then the app's own [details].
 */
@Composable
internal fun MonarchWebhookDeliveriesSheet(
    delivery: MonarchWebhookDelivery,
    model: MonarchWebhookDeliveriesRowModel,
    timeZone: String?,
    busy: Boolean,
    onRedeliver: (() -> Unit)?,
    onDismiss: () -> Unit,
    details: (@Composable ColumnScope.(MonarchWebhookDelivery) -> Unit)?,
) {
    val text = model.text
    MonarchBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(SheetTokens.SectionGap),
        ) {
            MonarchStatusBadge(status = delivery.status, registry = model.registry, showIcon = false)
            MonarchText(text = delivery.eventType, style = MonarchTextStyle.Heading, heading = true)
            MonarchDescriptionList(items = sheetItems(delivery, model, monarchTimeZone(timeZone)))
            if (onRedeliver != null) {
                MonarchButton(
                    text = text.redeliver,
                    onClick = onRedeliver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = text.redeliverItem(delivery) },
                    loading = busy,
                )
            }
            details?.invoke(this, delivery)
        }
    }
}

/** The facts of a delivery that are known, as description items. */
private fun sheetItems(
    delivery: MonarchWebhookDelivery,
    model: MonarchWebhookDeliveriesRowModel,
    timeZone: TimeZone,
): List<MonarchDescriptionItem> {
    val text = model.text
    val locale = model.locale
    val response = delivery.responseCode?.let(text::httpStatus) ?: text.notAttempted.takeIf { delivery.attempts == 0 }
    val next = SheetTokens.nextAttempt(delivery)
    fun time(millis: Long) =
        SheetFormat.absoluteTime(millis, locale, timeZone) + SheetTokens.SEPARATOR +
            SheetFormat.relativeTime(millis, model.now)
    return listOfNotNull(
        delivery.eventId?.let { MonarchDescriptionItem(term = text.eventId, value = it, copyable = true) },
        delivery.destination?.let { MonarchDescriptionItem(term = text.destination, value = it) },
        response?.let { MonarchDescriptionItem(term = text.response, value = it) },
        delivery.durationMillis?.let {
            MonarchDescriptionItem(term = text.durationTerm, value = text.duration(SheetFormat.number(it, locale)))
        },
        delivery.error?.let { MonarchDescriptionItem(term = text.error, value = it) },
        MonarchDescriptionItem(
            term = text.attemptsTerm,
            value = SheetFormat.number(delivery.attempts.toLong(), locale),
        ),
        MonarchDescriptionItem(term = text.lastAttempt, value = time(delivery.atMillis)),
        next?.let { MonarchDescriptionItem(term = text.nextAttemptTerm, value = time(it)) },
    )
}
