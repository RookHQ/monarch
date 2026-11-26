package com.rookhq.monarch.components.technical.webhookdeliveries

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and the pure rules of MonarchWebhookDeliveries. */
internal object MonarchWebhookDeliveriesTokens {
    /** The space between the filters, the counts line and the list. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The space between the filter controls. */
    val FilterGap: Dp = MonarchTokens.Space.space2.dp

    /** The space inside a row, at the start and end. */
    val RowPaddingHorizontal: Dp = MonarchTokens.Space.space3.dp

    /** The space inside a row, above and below. */
    val RowPaddingVertical: Dp = MonarchTokens.Space.space3.dp

    /** The space between the lines of a row. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the parts of one line of a row. */
    val PartGap: Dp = MonarchTokens.Space.space2.dp

    /** The height of one loading placeholder row. */
    val SkeletonHeight: Dp = MonarchTokens.Space.space10.dp

    /** The number of placeholder rows while the first page loads. */
    const val SKELETON_ROWS: Int = 4

    /** The separator of the parts of the response line and the counts line. */
    const val SEPARATOR: String = " · "

    /** The colour of an error on a row: the strong danger role, with the error also written as text. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** The colour of the secondary lines of a row. */
    fun metaColor(colors: MonarchColors): Color = colors.textMuted

    /** The built-in statuses with their [names] and tones, then the app's [custom] ones over them. */
    fun registry(names: (String) -> String?, custom: MonarchStatusRegistry?): MonarchStatusRegistry {
        val tones = listOf(
            MonarchWebhookDeliveryStatus.Pending to MonarchTone.Neutral,
            MonarchWebhookDeliveryStatus.Sending to MonarchTone.Info,
            MonarchWebhookDeliveryStatus.Succeeded to MonarchTone.Success,
            MonarchWebhookDeliveryStatus.Retrying to MonarchTone.Warning,
            MonarchWebhookDeliveryStatus.Failed to MonarchTone.Danger,
            MonarchWebhookDeliveryStatus.Gone to MonarchTone.Danger,
        )
        val builtIn = MonarchStatusRegistry(
            tones.associate { (key, tone) -> key to MonarchStatusDefinition(names(key) ?: key, tone) },
        )
        return if (custom == null) builtIn else builtIn.merging(custom)
    }

    /** The name of a status key: its label in [registry], or the key itself. */
    /** The glyph before a status name, as the web: a check, a retry mark, the error mark and a clock. */
    fun statusIcon(key: String): (@Composable () -> Unit)? = when (key) {
        MonarchWebhookDeliveryStatus.Succeeded -> MonarchDefaultIcons.CheckCircle
        MonarchWebhookDeliveryStatus.Retrying, MonarchWebhookDeliveryStatus.Sending -> MonarchDefaultIcons.Retry
        MonarchWebhookDeliveryStatus.Failed, MonarchWebhookDeliveryStatus.Gone -> MonarchDefaultIcons.Error
        MonarchWebhookDeliveryStatus.Pending -> MonarchDefaultIcons.Clock
        else -> null
    }

    fun statusName(key: String, registry: MonarchStatusRegistry): String = registry.resolve(key)?.label ?: key

    /** Whether a delivery can be redelivered by default: every status but pending and sending. */
    fun redeliverable(delivery: MonarchWebhookDelivery): Boolean = delivery.status !in setOf(
        MonarchWebhookDeliveryStatus.Pending,
        MonarchWebhookDeliveryStatus.Sending,
    )

    /**
     * The response line of a row: "HTTP 500 · 1,203 ms", or "Not attempted yet"
     * before the first attempt, or `null` when only the error is known.
     */
    fun outcome(delivery: MonarchWebhookDelivery, text: MonarchWebhookDeliveriesText, locale: Locale): String? {
        val parts = listOfNotNull(
            delivery.responseCode?.let(text::httpStatus),
            delivery.durationMillis?.let { text.duration(MonarchWebhookDeliveriesFormat.number(it, locale)) },
        )
        return when {
            parts.isNotEmpty() -> parts.joinToString(SEPARATOR)
            delivery.error != null -> null
            delivery.attempts == 0 -> text.notAttempted
            else -> null
        }
    }

    /** The time a pending or retrying delivery is tried again, or `null` when that does not apply. */
    fun nextAttempt(delivery: MonarchWebhookDelivery): Long? =
        delivery.nextAttemptMillis.takeIf {
            delivery.status == MonarchWebhookDeliveryStatus.Pending ||
                delivery.status == MonarchWebhookDeliveryStatus.Retrying
        }

    /** The status keys the status filter offers: [filterStatuses], or every key of [registry]. */
    fun statusChoices(filterStatuses: List<String>?, registry: MonarchStatusRegistry): List<String> =
        filterStatuses ?: registry.definitions.keys.toList()

    /** The deliveries the list shows: those that pass [filters], or all of them when the server filters. */
    fun shown(
        deliveries: List<MonarchWebhookDelivery>,
        filters: MonarchWebhookDeliveryFilters,
        manualFiltering: Boolean,
    ): List<MonarchWebhookDelivery> = if (manualFiltering) deliveries else filters.filter(deliveries)
}
