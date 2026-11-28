package dev.monarchui.components.scheduling.timeoffpanel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/** One entry of MonarchTimeOffPanel: who, badges and when as one element, and its remove button. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
internal fun TimeOffPanelRow(
    entry: MonarchTimeOffEntry,
    who: String,
    period: String,
    past: Boolean,
    onRemove: (() -> Unit)?,
    removeFocus: FocusRequester,
    removeIcon: (@Composable () -> Unit)?,
    labels: MonarchTimeOffPanelLabels,
) {
    val tokens = MonarchTimeOffPanelTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalArrangement = Arrangement.spacedBy(tokens.RowGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (who.isNotEmpty()) {
                    Text(
                        text = who,
                        style = MonarchTheme.typography.bodyStrong,
                        color = if (past) tokens.muted(colors) else tokens.text(colors),
                    )
                }
                if (entry.personId == null) {
                    MonarchBadge(
                        text = monarchString(labels.closure, R.string.monarch_timeoffpanel_closure),
                        tone = MonarchTone.Warning,
                    )
                }
                entry.type?.let { MonarchBadge(text = it) }
                entry.status?.let { MonarchBadge(text = it, tone = MonarchTone.Info) }
                if (past) {
                    MonarchBadge(
                        text = monarchString(labels.past, R.string.monarch_timeoffpanel_past),
                        variant = MonarchBadgeVariant.Outline,
                    )
                }
            }
            Text(
                text = listOfNotNull(period, entry.reason).joinToString(" · "),
                style = MonarchTheme.typography.caption,
                color = tokens.muted(colors),
            )
        }
        if (onRemove != null) {
            val name = labels.removeEntry?.invoke(who, period)
                ?: stringResource(R.string.monarch_timeoffpanel_remove_entry, who, period)
            if (removeIcon != null) {
                MonarchIconButton(
                    label = name,
                    onClick = onRemove,
                    modifier = Modifier.focusRequester(removeFocus),
                ) {
                    Box(Modifier.clearAndSetSemantics { }) { removeIcon() }
                }
            } else {
                MonarchButton(
                    text = monarchString(labels.remove, R.string.monarch_timeoffpanel_remove),
                    onClick = onRemove,
                    modifier = Modifier.focusRequester(removeFocus).semantics { contentDescription = name },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}
