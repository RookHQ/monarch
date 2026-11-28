package dev.monarchui.components.technical.webhookdeliveries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

private typealias RowTokens = MonarchWebhookDeliveriesTokens
private typealias RowFormat = MonarchWebhookDeliveriesFormat

/** What a row of MonarchWebhookDeliveries shows and does, gathered so the row takes few parameters. */
internal class MonarchWebhookDeliveriesRowModel(
    val registry: MonarchStatusRegistry,
    val now: Long,
    val locale: Locale,
    val showDestination: Boolean,
    val text: MonarchWebhookDeliveriesText,
    val actions: (@Composable RowScope.(MonarchWebhookDelivery) -> Unit)?,
)

/**
 * One delivery: status and time, the event, the response and error, the
 * attempts, and the actions. Tapping the row opens its details; Redeliver and
 * the app's actions stay separate TalkBack elements.
 */
@Composable
internal fun MonarchWebhookDeliveriesRow(
    delivery: MonarchWebhookDelivery,
    model: MonarchWebhookDeliveriesRowModel,
    busy: Boolean,
    onRedeliver: (() -> Unit)?,
    onOpen: () -> Unit,
) {
    val text = model.text
    val colors = MonarchTheme.colors
    val outcome = RowTokens.outcome(delivery, text, model.locale)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = text.viewDetails(delivery), role = Role.Button, onClick = onOpen)
            .padding(horizontal = RowTokens.RowPaddingHorizontal, vertical = RowTokens.RowPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(RowTokens.LineGap),
    ) {
        // The badge is a TalkBack element of its own; here its name joins the row's one reading.
        val status = RowTokens.statusName(delivery.status, model.registry)
        Fact(text.status) {
            Box(Modifier.clearAndSetSemantics { contentDescription = status }) {
                MonarchStatusBadge(
                    status = delivery.status,
                    registry = model.registry,
                    size = MonarchStatusBadgeSize.Sm,
                    showIcon = true,
                    icon = RowTokens.statusIcon(delivery.status),
                )
            }
        }
        Fact(text.eventTerm) {
            Column {
                MonarchText(text = delivery.eventType, style = MonarchTextStyle.Code)
                delivery.eventId?.let { id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonarchText(text = id, style = MonarchTextStyle.Code, color = RowTokens.metaColor(colors))
                        MonarchCopyButton(
                            value = id,
                            icon = MonarchDefaultIcons.Copy,
                            target = text.eventId,
                            size = MonarchButtonSize.Sm,
                        )
                    }
                }
            }
        }
        if (model.showDestination) {
            delivery.destination?.let { Fact(text.destination) { MonarchText(text = it) } }
        }
        Fact(text.response) {
            Column {
                outcome?.let { MonarchText(text = it, color = RowTokens.metaColor(colors)) }
                delivery.error?.let { MonarchText(text = it, color = RowTokens.errorColor(colors)) }
            }
        }
        Fact(text.attemptsTerm) {
            MonarchText(text = RowFormat.number(delivery.attempts.toLong(), model.locale))
        }
        Fact(text.whenTerm) {
            MonarchText(text = RowFormat.whenText(delivery, model.now, text), color = RowTokens.metaColor(colors))
        }
        if (onRedeliver != null || model.actions != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(RowTokens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onRedeliver != null) {
                    MonarchButton(
                        text = text.redeliver,
                        onClick = onRedeliver,
                        modifier = Modifier.semantics { contentDescription = text.redeliverItem(delivery) },
                        size = MonarchButtonSize.Sm,
                        variant = MonarchButtonVariant.Secondary,
                        loading = busy,
                        leadingIcon = MonarchDefaultIcons.Retry,
                    )
                }
                model.actions?.invoke(this, delivery)
            }
        }
    }
}

/** One labelled fact of a row: the term in the muted style at the start, its value after it. */
@Composable
private fun Fact(term: String, value: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(RowTokens.PartGap), verticalAlignment = Alignment.Top) {
        MonarchText(
            text = term,
            modifier = Modifier.width(FactTermWidth),
            style = MonarchTextStyle.Label,
            color = RowTokens.metaColor(MonarchTheme.colors),
        )
        Box(Modifier.weight(1f)) { value() }
    }
}

private val FactTermWidth = (MonarchTokens.Space.space20 + MonarchTokens.Space.space2).dp
