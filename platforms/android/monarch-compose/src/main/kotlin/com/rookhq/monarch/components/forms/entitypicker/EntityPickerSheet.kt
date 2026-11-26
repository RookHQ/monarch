package com.rookhq.monarch.components.forms.entitypicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.forms.checkbox.MonarchCheckbox
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.MonarchGlyphs
import com.rookhq.monarch.internal.MonarchSearchListSheet
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The bottom sheet of an entity picker: title, search, the list, and Cancel or Done. */
@Composable
internal fun EntityPickerSheet(
    config: EntityPickerConfig,
    onChoose: (MonarchEntityPickerEntity) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    val tokens = MonarchEntityPickerTokens
    val labels = config.labels
    var query by remember { mutableStateOf("") }
    val results = rememberEntityPickerResults(config, query)
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }
    val rows = tokens.rows(
        shown = results.shown,
        query = query,
        phase = results.phase,
        loading = config.loading,
        clearOptionLabel = config.clearOptionLabel,
        canCreate = config.onCreate != null,
        messages = entityPickerMessages(labels, config.minSearchLength),
        footerLabel = config.footerLabel.takeIf { config.onFooter != null },
    )
    val create: (String) -> Unit = { text ->
        val onCreate = config.onCreate
        if (onCreate != null && !creating) {
            creating = true
            scope.launch {
                val created = onCreate(text)
                creating = false
                if (created != null && (!config.multiple || created.id !in config.selected)) onChoose(created)
                query = ""
            }
        }
    }
    MonarchSearchListSheet(
        title = config.label,
        query = query,
        onQueryChange = { query = it },
        onDismiss = onClose,
        searchPlaceholder = monarchString(labels.search, R.string.monarch_entitypicker_search),
        searching = results.phase == EntityPickerPhase.Searching,
        containerColor = tokens.sheetColor(MonarchTheme.colors),
        gap = tokens.SheetGap,
        buttons = {
            MonarchButton(
                text = if (config.multiple) {
                    monarchString(labels.done, R.string.monarch_entitypicker_done)
                } else {
                    monarchString(labels.cancel, R.string.monarch_entitypicker_cancel)
                },
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = tokens.SheetPadding),
                variant = if (config.multiple) MonarchButtonVariant.Primary else MonarchButtonVariant.Ghost,
            )
        },
    ) {
        items(rows) { row ->
            EntityPickerListRow(
                row = row,
                config = config,
                creating = creating,
                actions = EntityPickerRowActions(onChoose, onClear, create, results.retry) {
                    onClose()
                    config.onFooter?.invoke()
                },
            )
        }
    }
}

/** The entities the list shows for the text, the phase of the search, and how to run a failed search again. */
internal class EntityPickerResults(
    val shown: List<MonarchEntityPickerEntity>,
    val phase: EntityPickerPhase,
    val retry: () -> Unit,
)

/**
 * Filters [EntityPickerConfig.entities] in place, or runs the app's search after the debounce. A newer text
 * restarts the effect, which cancels the wait and the older search.
 */
@Composable
private fun rememberEntityPickerResults(config: EntityPickerConfig, query: String): EntityPickerResults {
    val tokens = MonarchEntityPickerTokens
    val trimmed = query.trim()
    val search = config.search
    var found by remember { mutableStateOf<List<MonarchEntityPickerEntity>?>(null) }
    var phase by remember { mutableStateOf(EntityPickerPhase.Ready) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(trimmed, attempt, search) {
        phase = tokens.phase(trimmed, config.minSearchLength)
        if (search == null || trimmed.isEmpty() || phase == EntityPickerPhase.TooShort) {
            found = null
            return@LaunchedEffect
        }
        phase = EntityPickerPhase.Searching
        delay(config.debounceMillis)
        try {
            found = search(trimmed)
            phase = EntityPickerPhase.Ready
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Exception) {
            // The list shows a fixed failure message with Try again; the app logs its own errors in search.
            found = null
            phase = EntityPickerPhase.Failed
        }
    }
    val shown = when {
        search != null -> found ?: config.entities
        trimmed.isEmpty() -> config.entities
        else -> config.entities.filter { tokens.matches(it, trimmed) }
    }
    return EntityPickerResults(shown, if (search == null) tokens.phase(trimmed, 0) else phase) { attempt++ }
}

@Composable
private fun entityPickerMessages(labels: MonarchEntityPickerLabels, minSearchLength: Int): EntityPickerMessages {
    val locale = LocalConfiguration.current.locales[0]
    val create = stringResource(R.string.monarch_entitypicker_create)
    val tooShort = labels.tooShort?.invoke(minSearchLength)
        ?: pluralStringResource(R.plurals.monarch_entitypicker_too_short, minSearchLength, minSearchLength)
    return EntityPickerMessages(
        loading = monarchString(labels.loading, R.string.monarch_entitypicker_loading),
        empty = monarchString(labels.empty, R.string.monarch_entitypicker_empty),
        tooShort = tooShort,
        failed = monarchString(labels.failed, R.string.monarch_entitypicker_failed),
        create = labels.create ?: { String.format(locale, create, it) },
    )
}

/** What the rows of the list do. */
private class EntityPickerRowActions(
    val choose: (MonarchEntityPickerEntity) -> Unit,
    val clear: () -> Unit,
    val create: (String) -> Unit,
    val retry: () -> Unit,
    val footer: () -> Unit,
)

@Composable
private fun EntityPickerListRow(
    row: EntityPickerRow,
    config: EntityPickerConfig,
    creating: Boolean,
    actions: EntityPickerRowActions,
) {
    val tokens = MonarchEntityPickerTokens
    val colors = MonarchTheme.colors
    when (row) {
        is EntityPickerRow.Clear -> EntityPickerChoiceRow(
            selected = config.selected.isEmpty(),
            enabled = true,
            multiple = false,
            onClick = actions.clear,
        ) {
            Text(row.label, style = MonarchTheme.typography.body, color = colors.textMuted)
        }
        is EntityPickerRow.Entity -> {
            val entity = row.entity
            EntityPickerChoiceRow(
                selected = entity.id in config.selected,
                enabled = entity.enabled,
                multiple = config.multiple,
                onClick = { actions.choose(entity) },
            ) {
                EntityPickerMark(entity)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entity.name,
                        style = MonarchTheme.typography.body,
                        color = tokens.labelColor(entity.enabled, colors),
                    )
                    entity.description?.let {
                        Text(
                            it,
                            style = MonarchTheme.typography.caption,
                            color = tokens.secondaryColor(entity.enabled, colors),
                        )
                    }
                }
                entity.hint?.let {
                    Text(
                        it,
                        style = MonarchTheme.typography.caption,
                        color = tokens.secondaryColor(entity.enabled, colors),
                    )
                }
            }
        }
        is EntityPickerRow.Group -> Text(
            text = row.label,
            modifier = Modifier.padding(top = tokens.RowVerticalPadding),
            style = MonarchTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
            color = colors.textMuted,
        )
        is EntityPickerRow.Footer -> Text(
            text = row.label,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.MinHeight)
                .clickable(role = Role.Button) { actions.footer() }
                .padding(vertical = tokens.RowVerticalPadding)
                .wrapContentHeight(Alignment.CenterVertically),
            style = MonarchTheme.typography.label,
            color = colors.textDefault,
            textAlign = TextAlign.Center,
        )
        is EntityPickerRow.Create -> Text(
            text = row.label,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.MinHeight)
                .clickable(enabled = !creating, role = Role.Button) { actions.create(row.text) }
                .padding(vertical = tokens.RowVerticalPadding),
            style = MonarchTheme.typography.body,
            color = tokens.labelColor(!creating, colors),
        )
        is EntityPickerRow.Message -> Text(
            text = row.text,
            modifier = Modifier
                .padding(vertical = tokens.RowVerticalPadding)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = tokens.secondaryColor(true, colors),
        )
        is EntityPickerRow.Failed -> MonarchInlineErrorRetry(
            message = row.text,
            onRetry = actions.retry,
            retryLabel = config.labels.retry,
        )
    }
}

/** A row that chooses: one target with the radio or checkbox mark, read as selected or checked. */
@Composable
private fun EntityPickerChoiceRow(
    selected: Boolean,
    enabled: Boolean,
    multiple: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = MonarchEntityPickerTokens
    val interaction = if (multiple) {
        Modifier.toggleable(value = selected, enabled = enabled, role = Role.Checkbox) { onClick() }
    } else {
        Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.MinHeight)
            .then(interaction)
            .padding(vertical = tokens.RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (multiple) {
            MonarchCheckbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.clearAndSetSemantics { },
                enabled = enabled,
            )
        }
        content()
        // A single choice shows its selection as the web does: a check at the end of the row.
        if (!multiple && selected) {
            MonarchIcon(
                imageVector = MonarchGlyphs.Check,
                contentDescription = null,
                modifier = Modifier.clearAndSetSemantics { },
                tint = MonarchTheme.colors.textLink,
                size = MonarchIconSize.Sm,
            )
        }
    }
}
