package dev.monarchui.components.technical.webhookdeliveries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusRegistry
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.textfield.MonarchTextField

/**
 * The filters of MonarchWebhookDeliveries on a phone, as the web lists them: a select for the status, one for the
 * event type, the event id search field, and Clear filters while any filter is set.
 */
@Composable
internal fun MonarchWebhookDeliveriesFilterBar(
    filters: MonarchWebhookDeliveryFilters,
    onFiltersChange: (MonarchWebhookDeliveryFilters) -> Unit,
    statusKeys: List<String>,
    eventTypes: List<String>,
    registry: MonarchStatusRegistry,
    text: MonarchWebhookDeliveriesText,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchWebhookDeliveriesTokens.FilterGap)) {
        MonarchWebhookDeliveriesFilterSelect(
            name = text.status,
            anyLabel = text.anyStatus,
            choices = statusKeys.map { it to MonarchWebhookDeliveriesTokens.statusName(it, registry) },
            selected = filters.status,
            onSelect = { onFiltersChange(filters.copy(status = it)) },
        )
        if (eventTypes.isNotEmpty()) {
            MonarchWebhookDeliveriesFilterSelect(
                name = text.eventType,
                anyLabel = text.anyEventType,
                choices = eventTypes.map { it to it },
                selected = filters.eventType,
                onSelect = { onFiltersChange(filters.copy(eventType = it)) },
            )
        }
        val search = text.search
        MonarchTextField(
            value = filters.query,
            onValueChange = { onFiltersChange(filters.copy(query = it)) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = search },
            placeholder = text.searchHint,
        )
        if (filters.active) {
            MonarchButton(
                text = text.clearFilters,
                onClick = { onFiltersChange(MonarchWebhookDeliveryFilters()) },
                size = MonarchButtonSize.Sm,
                variant = MonarchButtonVariant.Ghost,
            )
        }
    }
}

/**
 * A select that names a filter by its value ("Any status", "Failed") and opens a list of single-choice items,
 * the first of which is "any"; its name for TalkBack is the filter's name.
 */
@Composable
private fun MonarchWebhookDeliveriesFilterSelect(
    name: String,
    anyLabel: String,
    choices: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    MonarchSelect(
        options = (listOf("" to anyLabel) + choices).map { (key, label) -> MonarchSelectOption(key, label) },
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
        contentDescription = name,
    )
}
