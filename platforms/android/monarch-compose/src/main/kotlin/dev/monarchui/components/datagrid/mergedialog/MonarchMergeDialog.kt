package dev.monarchui.components.datagrid.mergedialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.consequencelist.MonarchConsequence
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceList
import dev.monarchui.components.forms.radiorow.MonarchRadioRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The content of a full-screen dialog that merges two or more duplicate
 * records: pick the record to keep, then the value to keep for each field
 * where they differ. Fields that match are listed once.
 *
 * Built by Monarch from `MonarchRadioRow`, `MonarchConsequenceList`,
 * `MonarchBanner` and `MonarchButton`. Place it in a Material 3 `Dialog` with
 * `DialogProperties(usePlatformDefaultWidth = false)` for a full-screen
 * dialog on a phone, or in a `ModalBottomSheet`; it fills the height it is
 * given, scrolls its choices and keeps the buttons visible. The choices are
 * kept across configuration changes. The app runs the merge: it sets
 * [merging] while it runs, closes the dialog when it succeeds, and sets
 * [failed] when it does not; the choices stay as they were.
 *
 * @param records the records to merge, two or more.
 * @param fields the fields, in display order. Those that differ get a choice; those that match are listed once.
 * @param onMerge called with the choices when the merge button is pressed.
 * @param modifier the modifier of the content.
 * @param defaultPrimaryId the record kept by default; the first record when `null`.
 * @param consequences what else happens, shown with `MonarchConsequenceList` under "What happens".
 * @param blockers problems that stop the merge, shown in a warning `MonarchBanner`; the merge button is disabled.
 * @param merging the merge runs: the merge button shows progress and the choices and Cancel are disabled.
 * @param failed the last merge failed: a danger `MonarchBanner` shows [MonarchMergeDialogLabels.error] and is
 * announced.
 * @param onCancel shows a Cancel button that calls it. The app closes the dialog.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchMergeDialog(
    records: List<MonarchMergeRecord>,
    fields: List<MonarchMergeField>,
    onMerge: (MonarchMergeSelection) -> Unit,
    modifier: Modifier = Modifier,
    defaultPrimaryId: String? = null,
    consequences: List<MonarchConsequence> = emptyList(),
    blockers: List<String> = emptyList(),
    merging: Boolean = false,
    failed: Boolean = false,
    onCancel: (() -> Unit)? = null,
    labels: MonarchMergeDialogLabels = MonarchMergeDialogLabels(),
) {
    val tokens = MonarchMergeDialogTokens
    var primaryId by rememberSaveable { mutableStateOf(defaultPrimaryId) }
    var picked by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    val primary = tokens.primary(records, primaryId)
    val keptId = primary?.id.orEmpty()
    val split = tokens.split(fields, records)
    val state = MergeDialogState(records, keptId, picked, !merging, labels)
    Column(modifier = modifier.fillMaxWidth()) {
        MergeDialogHeader(labels)
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = tokens.Padding),
            verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
        ) {
            MergeRecordChoice(state, onSelect = { primaryId = it })
            if (split.differing.isNotEmpty()) {
                MergeValueChoices(split.differing, state) { field, record -> picked = picked + (field to record) }
            }
            if (split.same.isNotEmpty() && primary != null) MergeSameValues(split.same, primary, labels)
            if (consequences.isNotEmpty()) {
                MonarchConsequenceList(
                    items = consequences,
                    heading = monarchString(labels.consequences, R.string.monarch_mergedialog_consequences),
                )
            }
            if (blockers.isNotEmpty()) {
                MonarchBanner(message = blockers.joinToString("\n"), tone = MonarchTone.Warning)
            }
            if (failed) {
                MonarchBanner(
                    message = monarchString(labels.error, R.string.monarch_mergedialog_error),
                    tone = MonarchTone.Danger,
                    announcement = MonarchBannerAnnouncement.Polite,
                )
            }
        }
        MergeDialogFooter(
            recordCount = records.size,
            canMerge = tokens.canMerge(records.size, blockers.size, merging),
            merging = merging,
            onMerge = { onMerge(tokens.selection(records, fields, keptId, picked)) },
            onCancel = onCancel,
            labels = labels,
        )
    }
}

/** What the choices need to draw themselves. */
private class MergeDialogState(
    val records: List<MonarchMergeRecord>,
    val keptId: String,
    val picked: Map<String, String>,
    val enabled: Boolean,
    val labels: MonarchMergeDialogLabels,
)

@Composable
private fun MergeDialogHeader(labels: MonarchMergeDialogLabels) {
    val tokens = MonarchMergeDialogTokens
    Column(
        modifier = Modifier.fillMaxWidth().padding(tokens.Padding),
        verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
    ) {
        MonarchText(
            text = monarchString(labels.title, R.string.monarch_mergedialog_title),
            style = MonarchTextStyle.Heading,
            color = tokens.headingColor(MonarchTheme.colors),
        )
        MonarchText(
            text = monarchString(labels.description, R.string.monarch_mergedialog_description),
            color = tokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun MergeSectionHeading(text: String) {
    // One step under the 20sp title (16sp semibold), so a section never reads as large as the dialog's title.
    MonarchText(
        text = text,
        style = MonarchTextStyle.BodyStrong,
        color = MonarchTheme.colors.textDefault,
        heading = true,
    )
}

@Composable
private fun MergeRecordChoice(state: MergeDialogState, onSelect: (String) -> Unit) {
    val kept = monarchString(state.labels.kept, R.string.monarch_mergedialog_kept)
    Column(verticalArrangement = Arrangement.spacedBy(MonarchMergeDialogTokens.Gap)) {
        MergeSectionHeading(monarchString(state.labels.keep, R.string.monarch_mergedialog_keep))
        MergeChoiceGrid(state.records, selected = { it.id == state.keptId }) { record, selected, modifier ->
            MergeChoiceCard(selected, modifier) {
                Row(verticalAlignment = Alignment.Top) {
                    MonarchRadioRow(
                        selected = selected,
                        onClick = { onSelect(record.id) },
                        label = record.title,
                        modifier = Modifier.weight(1f),
                        enabled = state.enabled,
                        description = record.description,
                    )
                    // The selected state already says it; the badge is for sighted people.
                    if (selected) {
                        MonarchBadge(
                            text = kept,
                            modifier = Modifier.padding(MonarchMergeDialogTokens.Gap).clearAndSetSemantics { },
                            tone = MonarchTone.Accent,
                            variant = MonarchBadgeVariant.Solid,
                        )
                    }
                }
            }
        }
    }
}

/** The records two to a row, each in the card [content] draws, as the web lays out its choices. */
@Composable
private fun MergeChoiceGrid(
    records: List<MonarchMergeRecord>,
    selected: (MonarchMergeRecord) -> Boolean = { false },
    content: @Composable (MonarchMergeRecord, Boolean, Modifier) -> Unit,
) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(MonarchMergeDialogTokens.Gap)) {
        records.chunked(MonarchMergeDialogTokens.CHOICE_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchMergeDialogTokens.Gap)) {
                row.forEach { record -> content(record, selected(record), Modifier.weight(1f)) }
                repeat(MonarchMergeDialogTokens.CHOICE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The card of one choice: an outline, with the accent fill and border while chosen. */
@Composable
private fun MergeChoiceCard(selected: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accentSubtle else colors.bgSurface)
            .border(
                MonarchShapeDefaults.BorderWidth,
                if (selected) colors.accentBorder else colors.borderDefault,
                shape,
            ),
    ) { content() }
}

@Composable
private fun MergeValueChoices(
    fields: List<MonarchMergeField>,
    state: MergeDialogState,
    onPick: (field: String, record: String) -> Unit,
) {
    val tokens = MonarchMergeDialogTokens
    val empty = monarchString(state.labels.empty, R.string.monarch_mergedialog_empty)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MergeSectionHeading(monarchString(state.labels.values, R.string.monarch_mergedialog_values))
        fields.forEach { field ->
            val chosen = tokens.choice(field.id, state.records, state.keptId, state.picked)
            MonarchText(text = field.label, style = MonarchTextStyle.Label, heading = true)
            MergeChoiceGrid(state.records, selected = { chosen == it.id }) { record, selected, modifier ->
                val value = record.values[field.id]
                MergeChoiceCard(selected, modifier) {
                    MonarchRadioRow(
                        selected = selected,
                        onClick = { onPick(field.id, record.id) },
                        label = if (tokens.isEmpty(value)) empty else value.orEmpty(),
                        enabled = state.enabled,
                        description = state.labels.from?.invoke(record.title)
                            ?: stringResource(R.string.monarch_mergedialog_from, record.title),
                    )
                }
            }
        }
    }
}

@Composable
private fun MergeSameValues(
    fields: List<MonarchMergeField>,
    primary: MonarchMergeRecord,
    labels: MonarchMergeDialogLabels,
) {
    val tokens = MonarchMergeDialogTokens
    val empty = monarchString(labels.empty, R.string.monarch_mergedialog_empty)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MergeSectionHeading(monarchString(labels.same, R.string.monarch_mergedialog_same))
        fields.forEach { field ->
            val value = primary.values[field.id]
            MergeSameRow(field.label, if (tokens.isEmpty(value)) empty else value.orEmpty())
        }
    }
}

@Composable
private fun MergeSameRow(label: String, value: String) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchMergeDialogTokens.Gap),
    ) {
        MonarchText(text = label, color = MonarchMergeDialogTokens.mutedColor(MonarchTheme.colors))
        MonarchText(text = value)
    }
}

@Suppress("LongParameterList")
@Composable
private fun MergeDialogFooter(
    recordCount: Int,
    canMerge: Boolean,
    merging: Boolean,
    onMerge: () -> Unit,
    onCancel: (() -> Unit)?,
    labels: MonarchMergeDialogLabels,
) {
    val tokens = MonarchMergeDialogTokens
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(tokens.Padding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (onCancel != null) {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_mergedialog_cancel),
                onClick = onCancel,
                variant = MonarchButtonVariant.Ghost,
                enabled = !merging,
            )
        }
        MonarchButton(
            text = labels.confirm?.invoke(recordCount)
                ?: pluralStringResource(R.plurals.monarch_mergedialog_confirm, recordCount, recordCount),
            onClick = onMerge,
            variant = MonarchButtonVariant.Primary,
            enabled = canMerge || merging,
            loading = merging,
        )
    }
}
