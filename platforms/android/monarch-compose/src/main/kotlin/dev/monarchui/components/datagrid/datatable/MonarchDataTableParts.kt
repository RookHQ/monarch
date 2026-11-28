package dev.monarchui.components.datagrid.datatable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/** The header row of the table layout: a cell per shown column, sortable ones as buttons. */
@Composable
internal fun <T> MonarchDataTableHeaderRow(
    columns: List<MonarchDataTableColumn<T>>,
    weights: List<Float>,
    sort: MonarchDataTableSort?,
    onSortChange: ((MonarchDataTableSort) -> Unit)?,
    selecting: Boolean,
    hasActions: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataTableTexts,
    selectAll: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchDataTableTokens
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = tokens.RowPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) Box(Modifier.size(MonarchTouchTarget.Minimum)) { selectAll?.invoke() }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                columns.forEachIndexed { index, column ->
                    val direction = sort?.direction?.takeIf { sort.columnId == column.id }
                    HeaderCell(column, weights[index], direction, onSortChange?.takeIf { column.sortable }, sort, texts)
                }
            }
            // An invisible copy of the actions button keeps the header cells over their columns.
            if (hasActions) {
                Box(Modifier.clearAndSetSemantics { }.drawWithContent { }) {
                    ActionsButton(name = "", enabled = false, actionsIcon = actionsIcon) { }
                }
            }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

@Composable
private fun <T> RowScope.HeaderCell(
    column: MonarchDataTableColumn<T>,
    weight: Float,
    direction: MonarchDataTableSortDirection?,
    onSortChange: ((MonarchDataTableSort) -> Unit)?,
    sort: MonarchDataTableSort?,
    texts: MonarchDataTableTexts,
) {
    val color = MonarchDataTableTokens.headerColor(MonarchTheme.colors)
    val interaction = if (onSortChange != null) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(
                interactionSource = null,
                indication = ripple(),
                role = Role.Button,
                onClick = { onSortChange(MonarchDataTableRules.nextSort(sort, column.id)) },
            )
            .semantics { stateDescription = texts.sortState(direction) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier.weight(weight).then(interaction).semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = if (column.numeric) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = column.title, style = MonarchTextStyle.Label, color = color)
        if (direction != null) {
            MonarchText(
                text = texts.mark(direction),
                modifier = Modifier.clearAndSetSemantics { },
                style = MonarchTextStyle.Label,
                color = color,
            )
        } else if (onSortChange != null) {
            // The web draws a chevron on every sortable header that is not the sorted one.
            Box(Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(LocalContentColor provides color, content = MonarchDefaultIcons.ChevronDown)
            }
        }
    }
}

/** One row of the table layout: a checkbox, a cell per shown column and the actions button. */
@Composable
internal fun <T> MonarchDataTableTableRow(
    row: T,
    columns: List<MonarchDataTableColumn<T>>,
    weights: List<Float>,
    model: MonarchDataTableRowModel,
    selecting: Boolean,
    hasActions: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataTableTexts,
) {
    val tokens = MonarchDataTableTokens
    RowFrame(model, selecting, hasActions, actionsIcon, texts) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = tokens.RowPaddingVertical),
            horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEachIndexed { index, column ->
                val custom = column.cell
                if (custom != null) {
                    Box(Modifier.weight(weights[index]), contentAlignment = Alignment.CenterStart) { custom(row) }
                    return@forEachIndexed
                }
                Cell(
                    text = column.value(row),
                    modifier = Modifier.weight(weights[index]),
                    numeric = column.numeric,
                    strong = index == 0,
                    openable = index == 0 && model.onOpen != null,
                    texts = texts,
                    tone = column.tone?.invoke(row),
                )
            }
        }
    }
}

/** One record of the list layout: its name, then a field name and value per other column. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun <T> MonarchDataTableRecordRow(
    row: T,
    fields: List<MonarchDataTableColumn<T>>,
    model: MonarchDataTableRowModel,
    selecting: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataTableTexts,
) {
    val tokens = MonarchDataTableTokens
    val colors = MonarchTheme.colors
    RowFrame(model, selecting, model.actions.isNotEmpty(), actionsIcon, texts) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = tokens.RowPaddingVertical),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            MonarchText(
                text = model.name,
                style = MonarchTextStyle.BodyStrong,
                color = if (model.onOpen != null) tokens.openableNameColor(colors) else colors.textDefault,
            )
            fields.forEach { column ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.CellGap)) {
                    MonarchText(
                        text = column.title,
                        style = MonarchTextStyle.Caption,
                        color = tokens.headerColor(colors),
                    )
                    Cell(
                        column.value(row),
                        Modifier,
                        numeric = false,
                        strong = false,
                        openable = false,
                        texts = texts,
                        tone = column.tone?.invoke(row),
                    )
                }
            }
        }
    }
}

/** The parts every row shares: the fill, the checkbox, the content as one element, the actions, the divider. */
@Composable
private fun RowFrame(
    model: MonarchDataTableRowModel,
    selecting: Boolean,
    hasActions: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataTableTexts,
    content: @Composable () -> Unit,
) {
    val tokens = MonarchDataTableTokens
    val colors = MonarchTheme.colors
    val open = model.onOpen
    val contentModifier = Modifier
        .then(
            if (open != null) {
                Modifier
                    .monarchFocusRing(MonarchShapeDefaults.Row)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(),
                        onClickLabel = texts.open,
                        role = Role.Button,
                        onClick = open,
                    )
            } else {
                Modifier
            },
        )
        .semantics(mergeDescendants = true) {
            val actions = model.actions.filter { it.enabled }
            if (actions.isNotEmpty()) {
                customActions = actions.map { action ->
                    CustomAccessibilityAction(action.title) {
                        action.onClick()
                        true
                    }
                }
            }
        }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tokens.rowColor(model.selected == true, colors))
                .padding(horizontal = tokens.RowPaddingHorizontal),
            horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selecting) {
                val onChange = model.onSelectedChange
                if (model.selected != null && onChange != null) {
                    MonarchCheckbox(
                        checked = model.selected,
                        onCheckedChange = onChange,
                        modifier = Modifier.semantics { contentDescription = texts.selectRow(model.name) },
                    )
                } else {
                    Box(Modifier.size(MonarchTouchTarget.Minimum))
                }
            }
            Box(modifier = Modifier.weight(1f).then(contentModifier)) { content() }
            if (hasActions) {
                if (model.actions.isNotEmpty()) {
                    RowActionsMenu(model, actionsIcon, texts)
                } else {
                    Box(Modifier.clearAndSetSemantics { }.drawWithContent { }) {
                        ActionsButton(name = "", enabled = false, actionsIcon = actionsIcon) { }
                    }
                }
            }
        }
        MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

@Composable
private fun Cell(
    text: String,
    modifier: Modifier,
    numeric: Boolean,
    strong: Boolean,
    openable: Boolean,
    texts: MonarchDataTableTexts,
    tone: MonarchTone? = null,
) {
    val tokens = MonarchDataTableTokens
    val colors = MonarchTheme.colors
    if (tone != null && text.isNotBlank()) {
        Box(modifier, contentAlignment = if (numeric) Alignment.CenterEnd else Alignment.CenterStart) {
            MonarchBadge(text = text, tone = tone)
        }
        return
    }
    if (text.isBlank()) {
        MonarchText(
            text = texts.emptyCell,
            modifier = modifier.semantics { contentDescription = texts.noValue },
            color = tokens.headerColor(colors),
            textAlign = if (numeric) TextAlign.End else TextAlign.Start,
        )
        return
    }
    val style = if (strong) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body
    Text(
        text = text,
        modifier = modifier,
        color = if (openable) tokens.openableNameColor(colors) else colors.textDefault,
        textAlign = if (numeric) TextAlign.End else TextAlign.Start,
        style = MonarchTheme.typography.style(style).let {
            if (numeric) it.copy(fontFeatureSettings = MonarchDataTableTokens.TABULAR_FIGURES) else it
        },
    )
}

@Composable
private fun RowActionsMenu(
    model: MonarchDataTableRowModel,
    actionsIcon: (@Composable () -> Unit)?,
    texts: MonarchDataTableTexts,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        ActionsButton(texts.rowActions(model.name), enabled = true, actionsIcon = actionsIcon) {
            open = true
        }
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            model.actions.forEach { action ->
                MonarchMenuItem(
                    text = action.title,
                    onClick = {
                        open = false
                        action.onClick()
                    },
                    enabled = action.enabled,
                    destructive = action.danger,
                )
            }
        }
    }
}

@Composable
private fun ActionsButton(
    name: String,
    enabled: Boolean,
    actionsIcon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    MonarchIconButton(
        label = name,
        onClick = onClick,
        enabled = enabled,
        icon = actionsIcon ?: MonarchDefaultIcons.More,
    )
}

/** The select-all checkbox with its label and the selected count, as one control. */
@Composable
internal fun MonarchDataTableSelectAll(
    state: ToggleableState,
    selectedCount: Int,
    onClick: () -> Unit,
    texts: MonarchDataTableTexts,
    labelled: Boolean = true,
) {
    val tokens = MonarchDataTableTokens
    if (!labelled) {
        MonarchCheckbox(
            state = state,
            onClick = onClick,
            modifier = Modifier.monarchMinimumTouchTarget().semantics { contentDescription = texts.selectAll },
        )
        return
    }
    Row(
        modifier = Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .triStateToggleable(state = state, role = Role.Checkbox, onClick = onClick)
            .padding(end = tokens.CellGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.CellGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchCheckbox(state = state, onClick = null, modifier = Modifier.monarchMinimumTouchTarget())
        MonarchText(text = texts.selectAll)
        if (selectedCount > 0) {
            MonarchText(
                text = texts.selectedCount?.invoke(selectedCount)
                    ?: pluralStringResource(R.plurals.monarch_datatable_selected_count, selectedCount, selectedCount),
                style = MonarchTextStyle.Caption,
                color = tokens.headerColor(MonarchTheme.colors),
            )
        }
    }
}

/** The list layout's sort button and its menu of sortable columns. */
@Composable
internal fun <T> MonarchDataTableSortMenu(
    columns: List<MonarchDataTableColumn<T>>,
    sort: MonarchDataTableSort?,
    onSortChange: (MonarchDataTableSort) -> Unit,
    texts: MonarchDataTableTexts,
) {
    var open by remember { mutableStateOf(false) }
    val sorted = columns.firstOrNull { it.id == sort?.columnId }
    val state = sorted?.let { "${it.title}, ${texts.sortState(sort?.direction)}" } ?: texts.notSorted
    Box {
        MonarchButton(
            text = texts.sort,
            onClick = { open = true },
            modifier = Modifier.semantics { stateDescription = state },
            size = MonarchButtonSize.Sm,
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            columns.forEach { column ->
                val direction = sort?.direction?.takeIf { sort.columnId == column.id }
                MonarchMenuItem(
                    onClick = {
                        open = false
                        onSortChange(MonarchDataTableRules.nextSort(sort, column.id))
                    },
                    modifier = Modifier.semantics {
                        if (direction != null) stateDescription = texts.sortState(direction)
                    },
                    trailingContent = if (direction != null) {
                        { Text(texts.mark(direction), modifier = Modifier.clearAndSetSemantics { }) }
                    } else {
                        null
                    },
                ) { Text(column.title) }
            }
        }
    }
}

/** Placeholder rows while rows load, named for TalkBack as one busy element. */
@Composable
internal fun MonarchDataTableLoadingRows(label: String) {
    val tokens = MonarchDataTableTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .padding(tokens.RowPaddingHorizontal),
        verticalArrangement = Arrangement.spacedBy(tokens.CellGap),
    ) {
        repeat(tokens.SKELETON_ROWS) {
            MonarchSkeleton(
                modifier = Modifier.fillMaxWidth().height(tokens.SkeletonHeight),
                shape = MonarchSkeletonShape.Rect,
            )
        }
    }
}
