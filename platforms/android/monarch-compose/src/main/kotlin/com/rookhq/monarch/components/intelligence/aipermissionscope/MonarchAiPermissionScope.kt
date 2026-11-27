package com.rookhq.monarch.components.intelligence.aipermissionscope

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFavicon
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControl
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.navigation.link.MonarchLink
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * What an AI agent may read or edit, app by app: each row names the app and
 * the resources it covers and shows No access, Can read or Can read and edit;
 * with [onScopesChange] each row has a None / Read / Write control instead.
 *
 * Built from a `Column` of rows; the editable control is a
 * `MonarchSegmentedControl`, so each choice keeps its radio button role and
 * its selected state, shown by the raised pill. The list is one TalkBack group named
 * "What the AI can access". A read-only row is one element read as the app
 * and its resources, with the access as its state. The access is written and
 * can carry an icon, so it never depends on colour. When the access or the
 * control does not fit beside the app name, it moves under it.
 *
 * @param scopes one row per app, in order. App names must be unique.
 * @param modifier the modifier of the list.
 * @param onScopesChange makes the list editable: called with the whole list, one row changed, when the person
 * picks another level. The list is hoisted: pass the new list back in [scopes]. `null` shows the access as text.
 * @param enabled whether the editable controls take input.
 * @param accessIcon an icon before the access text of a read-only row, for example a lock, an eye or a pencil
 * as a `MonarchIcon` with a `null` content description. Monarch bundles no icons.
 * @param labels replaces any of the built-in strings.
 * @param onManage adds a "Manage permissions" link under the list, which opens the app's permission settings.
 */
@Composable
public fun MonarchAiPermissionScope(
    scopes: List<MonarchAiPermissionScopeItem>,
    modifier: Modifier = Modifier,
    onScopesChange: ((List<MonarchAiPermissionScopeItem>) -> Unit)? = null,
    enabled: Boolean = true,
    accessIcon: (@Composable (MonarchAiAccessLevel) -> Unit)? = null,
    labels: MonarchAiPermissionScopeLabels = MonarchAiPermissionScopeLabels(),
    onManage: (() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val tokens = MonarchAiPermissionScopeTokens
    Column(modifier = modifier) {
        if (scopes.isEmpty()) {
            MonarchText(
                text = monarchString(labels.empty, R.string.monarch_aipermissionscope_empty),
                color = tokens.mutedColor(colors),
            )
        } else {
            val name = monarchString(labels.label, R.string.monarch_aipermissionscope_label)
            Column(
                modifier = Modifier.fillMaxWidth().semantics {
                    isTraversalGroup = true
                    contentDescription = name
                },
            ) {
                scopes.forEachIndexed { index, item ->
                    if (index > 0) {
                        MonarchDivider(variant = MonarchDividerVariant.Subtle)
                    }
                    if (onScopesChange != null) {
                        EditableRow(item, enabled, labels) { level ->
                            tokens.changed(scopes, index, level)?.let(onScopesChange)
                        }
                    } else {
                        ReadOnlyRow(item, accessIcon, labels)
                    }
                }
            }
        }
        if (onManage != null) {
            MonarchLink(
                text = monarchString(labels.manage, R.string.monarch_aipermissionscope_manage),
                onClick = onManage,
                variant = MonarchLinkVariant.Standalone,
            )
        }
    }
}

@Composable
private fun ReadOnlyRow(
    item: MonarchAiPermissionScopeItem,
    accessIcon: (@Composable (MonarchAiAccessLevel) -> Unit)?,
    labels: MonarchAiPermissionScopeLabels,
) {
    val access = accessText(item.access, labels)
    RowLayout(
        item = item,
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = MonarchAiPermissionScopeTokens.appDescription(item)
            stateDescription = access
        },
    ) {
        MonarchBadge(
            text = access,
            tone = when (item.access) {
                MonarchAiAccessLevel.Read -> MonarchTone.Ai
                MonarchAiAccessLevel.Write -> MonarchTone.Warning
                MonarchAiAccessLevel.None -> MonarchTone.Neutral
            },
            leadingIcon = accessIcon?.let { icon -> { icon(item.access) } },
        )
    }
}

@Composable
private fun EditableRow(
    item: MonarchAiPermissionScopeItem,
    enabled: Boolean,
    labels: MonarchAiPermissionScopeLabels,
    onPick: (MonarchAiAccessLevel) -> Unit,
) {
    val controlName = labels.control?.invoke(item.app)
        ?: stringResource(R.string.monarch_aipermissionscope_control, item.app)
    val levels = MonarchAiAccessLevel.entries
    RowLayout(item = item, modifier = Modifier) {
        MonarchSegmentedControl(
            modifier = Modifier.semantics {
                isTraversalGroup = true
                contentDescription = controlName
            },
        ) {
            levels.forEach { level ->
                Segment(
                    selected = item.access == level,
                    onClick = { onPick(level) },
                    label = optionText(level, labels),
                    enabled = enabled,
                )
            }
        }
    }
}

/** The app (favicon, name, resources) and its access or control beside it, or under it when it does not fit. */
@Composable
private fun RowLayout(
    item: MonarchAiPermissionScopeItem,
    modifier: Modifier,
    trailing: @Composable () -> Unit,
) {
    val tokens = MonarchAiPermissionScopeTokens
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(vertical = tokens.RowPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.icon != null) MonarchFavicon(image = item.icon, name = item.app)
            Column {
                MonarchText(text = item.app, style = MonarchTextStyle.BodyStrong)
                item.resources?.takeIf { it.isNotBlank() }?.let {
                    MonarchText(
                        text = it,
                        style = MonarchTextStyle.Caption,
                        color = tokens.mutedColor(MonarchTheme.colors),
                    )
                }
            }
        }
        trailing()
    }
}

@Composable
private fun accessText(level: MonarchAiAccessLevel, labels: MonarchAiPermissionScopeLabels): String =
    labels.access?.invoke(level) ?: stringResource(accessResource(level))

@Composable
private fun optionText(level: MonarchAiAccessLevel, labels: MonarchAiPermissionScopeLabels): String =
    labels.option?.invoke(level) ?: stringResource(optionResource(level))

@StringRes
private fun accessResource(level: MonarchAiAccessLevel): Int = when (level) {
    MonarchAiAccessLevel.None -> R.string.monarch_aipermissionscope_access_none
    MonarchAiAccessLevel.Read -> R.string.monarch_aipermissionscope_access_read
    MonarchAiAccessLevel.Write -> R.string.monarch_aipermissionscope_access_write
}

@StringRes
private fun optionResource(level: MonarchAiAccessLevel): Int = when (level) {
    MonarchAiAccessLevel.None -> R.string.monarch_aipermissionscope_option_none
    MonarchAiAccessLevel.Read -> R.string.monarch_aipermissionscope_option_read
    MonarchAiAccessLevel.Write -> R.string.monarch_aipermissionscope_option_write
}
