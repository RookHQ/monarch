package dev.monarchui.components.identity.appaccesspicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A checklist of apps to grant, for inviting someone to a workspace or
 * editing a member's access: each row has a checkbox, the app's logo, its
 * name, an optional tag and a description, with "Select all" or "Clear all"
 * and a count of the selection.
 *
 * Built by Monarch from Material 3 `Checkbox` rows (the whole row toggles, as
 * in `MonarchCheckboxRow`), `MonarchAvatar` for the logo, `MonarchBadge` for
 * the tag, a ghost `MonarchButton` for the bulk action and `MonarchSkeleton`
 * rows while loading. The rows are a plain column, so the picker can sit in
 * the app's own scrolling form. For the read-only line of granted apps in a
 * member list, use `MonarchAppAccessSummary`.
 *
 * @param apps the apps that can be granted, in display order.
 * @param value the ids of the selected apps.
 * @param onValueChange called with the new list of selected ids. A newly selected id is added at the end.
 * @param modifier the modifier of the picker.
 * @param enabled `false` disables every row and the bulk action, for example while saving.
 * @param loading shows placeholder rows instead of the apps.
 * @param hideLabel hides the visible heading; TalkBack still reads the label as the name of the group.
 * @param showBulkActions shows the count and "Select all" or "Clear all" above the list.
 * @param description help text under the list, such as when the grants take effect.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList")
public fun MonarchAppAccessPicker(
    apps: List<MonarchAppAccessOption>,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    hideLabel: Boolean = false,
    showBulkActions: Boolean = true,
    description: String? = null,
    labels: MonarchAppAccessPickerLabels = MonarchAppAccessPickerLabels(),
) {
    val rules = MonarchAppAccessPickerRules
    val tokens = MonarchAppAccessPickerTokens
    val label = monarchString(labels.label, R.string.monarch_appaccesspicker_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                isTraversalGroup = true
                if (hideLabel) contentDescription = label
            },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        if (!hideLabel) MonarchText(text = label, style = MonarchTextStyle.Subheading, heading = true)
        if (showBulkActions && !loading && apps.isNotEmpty()) {
            AppAccessPickerToolbar(apps, value, onValueChange, enabled, labels)
        }
        when {
            loading -> AppAccessPickerPlaceholder(
                monarchString(labels.loading, R.string.monarch_appaccesspicker_loading),
            )
            apps.isEmpty() -> Text(
                text = monarchString(labels.empty, R.string.monarch_appaccesspicker_empty),
                style = MonarchTheme.typography.body,
                color = tokens.noteColor(MonarchTheme.colors),
            )
            else -> Column {
                apps.forEach { app ->
                    AppAccessPickerRow(
                        app = app,
                        checked = app.id in value,
                        enabled = enabled && app.enabled,
                        onCheckedChange = { onValueChange(rules.toggle(value, app.id, it)) },
                    )
                }
            }
        }
        if (description != null) {
            Text(
                text = description,
                style = MonarchTheme.typography.caption,
                color = tokens.noteColor(MonarchTheme.colors),
            )
        }
    }
}

/** The count, announced as it changes, and the bulk button. */
@Composable
private fun AppAccessPickerToolbar(
    apps: List<MonarchAppAccessOption>,
    value: List<String>,
    onValueChange: (List<String>) -> Unit,
    enabled: Boolean,
    labels: MonarchAppAccessPickerLabels,
) {
    val rules = MonarchAppAccessPickerRules
    val selected = rules.count(apps, value)
    val count = labels.count?.invoke(selected, apps.size)
        ?: pluralStringResource(R.plurals.monarch_appaccesspicker_count, selected, selected, apps.size)
    val all = rules.allSelected(apps, value)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = count,
            modifier = Modifier
                .weight(1f)
                .semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.caption,
            color = MonarchAppAccessPickerTokens.noteColor(MonarchTheme.colors),
        )
        MonarchButton(
            text = if (all) {
                monarchString(labels.clearAll, R.string.monarch_appaccesspicker_clear_all)
            } else {
                monarchString(labels.selectAll, R.string.monarch_appaccesspicker_select_all)
            },
            onClick = { onValueChange(rules.toggleAll(apps, value)) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled && rules.changeable(apps).isNotEmpty(),
        )
    }
}

/** One app: the whole row toggles the checkbox and is one TalkBack element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppAccessPickerRow(
    app: MonarchAppAccessOption,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tokens = MonarchAppAccessPickerTokens
    val colors = tokens.rowColors(enabled, MonarchTheme.colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.RowMinHeight)
            .monarchFocusRing(MonarchTheme.shapes.sm)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchCheckbox(checked = checked, onCheckedChange = null, enabled = enabled)
        MonarchAvatar(
            image = app.logo,
            name = app.name,
            shape = MonarchAvatarShape.Square,
            size = MonarchAvatarSize.Sm,
            seed = app.id,
            decorative = true,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.TextGap),
                verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = app.name, style = MonarchTheme.typography.body, color = colors.name)
                app.badge?.let { MonarchBadge(text = it) }
            }
            app.description?.let {
                Text(text = it, style = MonarchTheme.typography.caption, color = colors.description)
            }
        }
    }
}

/** Placeholder rows, read once as "Loading apps". */
@Composable
private fun AppAccessPickerPlaceholder(loading: String) {
    val tokens = MonarchAppAccessPickerTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = loading
                liveRegion = LiveRegionMode.Polite
            },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        repeat(tokens.PLACEHOLDER_ROWS) {
            MonarchSkeleton(modifier = Modifier.fillMaxWidth().height(tokens.PlaceholderHeight))
        }
    }
}
