package dev.monarchui.components.technical.scopeselector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroup
import dev.monarchui.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

private typealias Tokens = MonarchScopeSelectorTokens

/**
 * Chooses the scopes (permissions) to grant, grouped by resource, such as the APIs an API key or an OAuth app
 * may call. Each group collapses, shows how many of its scopes are chosen, and has Select all or Clear all.
 * Long lists show more groups in steps.
 *
 * Built by Monarch from existing components: each group is an outlined card with a `MonarchCollapsible`
 * (header with the group's name, expanded state and expand and collapse actions) holding a
 * `MonarchCheckboxGroup` of the scopes, then a line with the group's description, the count ("2 of 5 scopes")
 * and a ghost `MonarchButton` for Select all or Clear all. A group with selected scopes takes the accent border,
 * and the count says the same in words. "Show 10 more of 14" is a ghost `MonarchButton`.
 *
 * The selection is controlled: [onValueChange] gets the whole new list of scope values. With [autoSelect], a
 * group whose scopes have loaded and of which nothing is selected gets every scope it offers, once per group.
 *
 * @param groups the groups to choose from, in display order.
 * @param value the selected scope values.
 * @param onValueChange called with the whole new selection.
 * @param modifier the modifier of the selector.
 * @param enabled whether scopes can be changed. Groups can still be expanded to review.
 * @param showLabel shows the "Scopes" heading. Without it the label is only the TalkBack name.
 * @param defaultExpanded the ids of the groups expanded at first.
 * @param pageSize how many groups show before "Show more", and how many more each press shows.
 * @param autoSelect selects every scope of a group that has none selected once its scopes load; `null` for off.
 * @param expandIcon an icon after each group's name, such as a chevron pointing down; it turns while expanded.
 * Monarch bundles no icons.
 * @param emptyContent replaces the note shown when there are no groups.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchScopeSelector(
    groups: List<MonarchScopeGroup>,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showLabel: Boolean = true,
    defaultExpanded: Set<String> = emptySet(),
    pageSize: Int = Tokens.DEFAULT_PAGE_SIZE,
    autoSelect: MonarchScopeSelectorAutoSelect? = null,
    expandIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.ChevronDown,
    emptyContent: (@Composable () -> Unit)? = null,
    labels: MonarchScopeSelectorLabels = MonarchScopeSelectorLabels(),
) {
    val resources = LocalResources.current
    val text = remember(labels, resources) { MonarchScopeSelectorText(labels, resources) }
    var expanded by remember { mutableStateOf(defaultExpanded) }
    var limit by rememberSaveable { mutableIntStateOf(pageSize) }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val seen = remember {
        if (autoSelect == MonarchScopeSelectorAutoSelect.Added) groups.map { it.id }.toMutableSet() else mutableSetOf()
    }
    LaunchedEffect(groups, autoSelect) {
        if (autoSelect != null) Tokens.autoSelected(groups, currentValue, seen)?.let(currentOnValueChange)
    }
    Column(
        modifier = modifier.semantics { if (!showLabel) contentDescription = text.label },
        verticalArrangement = Arrangement.spacedBy(Tokens.SectionGap),
    ) {
        if (showLabel) MonarchText(text = text.label, style = MonarchTextStyle.Subheading)
        if (groups.isEmpty()) {
            if (emptyContent != null) {
                emptyContent()
            } else {
                MonarchText(text = text.empty, color = Tokens.mutedColor(MonarchTheme.colors))
            }
        }
        groups.take(limit).forEach { group ->
            key(group.id) {
                ScopeGroupCard(
                    group = group,
                    value = value,
                    onValueChange = onValueChange,
                    expanded = group.id in expanded,
                    onExpandedChange = { open -> expanded = if (open) expanded + group.id else expanded - group.id },
                    enabled = enabled,
                    expandIcon = expandIcon,
                    text = text,
                )
            }
        }
        val remaining = groups.size - limit
        if (remaining > 0) {
            MonarchButton(
                text = text.showMore(minOf(pageSize, remaining), remaining),
                onClick = { limit += pageSize },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

/** One group: the collapsible checklist, then the description, the count and Select all or Clear all. */
@Composable
@Suppress("LongParameterList")
private fun ScopeGroupCard(
    group: MonarchScopeGroup,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    expandIcon: (@Composable () -> Unit)?,
    text: MonarchScopeSelectorText,
) {
    val colors = MonarchTheme.colors
    val count = Tokens.selectedCount(group, value)
    val total = group.scopes.size
    val summary = when {
        group.loading -> text.loading
        total == 0 -> group.emptyMessage ?: text.noScopes
        else -> text.count(count, total)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(Tokens.groupColors(colors), MonarchShapeDefaults.Container)
            .padding(Tokens.GroupPadding),
        verticalArrangement = Arrangement.spacedBy(Tokens.GroupGap),
    ) {
        // As on the web: the chevron at the start, the name with its description and count, Select all at the end.
        Box(modifier = Modifier.fillMaxWidth()) {
            MonarchCollapsible(
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                header = { ScopeGroupTitle(group, summary, expanded, expandIcon) },
            ) {
                if (!group.loading && total > 0) ScopeGroupChecks(group, value, onValueChange, enabled)
            }
            if (enabled && !group.loading && Tokens.offered(group).isNotEmpty()) {
                ScopeGroupSelectAll(group, value, onValueChange, text, Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

/** The Select all or Clear all button of a group, at the end of its header. */
@Composable
private fun ScopeGroupSelectAll(
    group: MonarchScopeGroup,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    text: MonarchScopeSelectorText,
    modifier: Modifier,
) {
    val allOn = Tokens.allOn(group, value)
    MonarchButton(
        text = if (allOn) text.clearAll else text.selectAll,
        onClick = { onValueChange(Tokens.toggledAll(group, value)) },
        modifier = modifier.semantics {
            contentDescription = if (allOn) text.clearAllIn(group.label) else text.selectAllIn(group.label)
        },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
    )
}

/** The header of a group: the chevron, then the name over its description and count. */
@Composable
private fun RowScope.ScopeGroupTitle(
    group: MonarchScopeGroup,
    summary: String,
    expanded: Boolean,
    expandIcon: (@Composable () -> Unit)?,
) {
    val muted = Tokens.mutedColor(MonarchTheme.colors)
    if (expandIcon != null) {
        Box(Modifier.graphicsLayer { rotationZ = if (expanded) 0f else Tokens.COLLAPSED_ROTATION }) { expandIcon() }
    }
    Column(modifier = Modifier.weight(1f, fill = false).padding(end = Tokens.SelectAllRoom)) {
        MonarchText(text = group.label, style = MonarchTextStyle.BodyStrong)
        group.description?.let { MonarchText(text = it, style = MonarchTextStyle.Caption, color = muted) }
        MonarchText(text = summary, style = MonarchTextStyle.Caption, color = muted)
    }
}

/** The checklist of a group's scopes. */
@Composable
private fun ScopeGroupChecks(
    group: MonarchScopeGroup,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    enabled: Boolean,
) {
    MonarchCheckboxGroup(
        options = group.scopes.map {
            MonarchCheckboxGroupOption(it.value, it.label ?: it.value, it.description, it.enabled)
        },
        value = group.scopes.map { it.value }.filter { it in value },
        onValueChange = { picked -> onValueChange(Tokens.withGroup(group, value, picked)) },
        enabled = enabled,
    )
}
