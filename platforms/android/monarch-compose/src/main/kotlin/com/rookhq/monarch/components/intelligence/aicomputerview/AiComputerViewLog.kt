package com.rookhq.monarch.components.intelligence.aicomputerview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The log of actions, oldest first: a heading, or the "Actions (n)" button
 * of the compact view, then the newest [visibleActions] actions with "Show
 * earlier" for the rest. Each action is one TalkBack element read as its
 * kind, label, detail and time; with [onSelectAction] it is selectable.
 */
@Composable
internal fun AiComputerViewLog(
    actions: List<MonarchAiComputerAction>,
    selectedActionId: String?,
    onSelectAction: ((String) -> Unit)?,
    showTypedText: Boolean,
    compact: Boolean,
    logExpanded: Boolean?,
    onLogExpandedChange: ((Boolean) -> Unit)?,
    visibleActions: Int,
    texts: AiComputerViewTexts,
    clock: AiComputerViewClock,
) {
    val tokens = MonarchAiComputerViewTokens
    val colors = MonarchTheme.colors
    var ownExpanded by rememberSaveable { mutableStateOf(false) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val expanded = logExpanded ?: ownExpanded
    if (compact) {
        val state = if (expanded) texts.expanded else texts.collapsed
        MonarchButton(
            text = texts.logToggle(actions.size),
            onClick = {
                if (logExpanded == null) ownExpanded = !expanded
                onLogExpandedChange?.invoke(!expanded)
            },
            modifier = Modifier.semantics { stateDescription = state },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
        if (!expanded) return
    } else {
        Text(
            text = texts.log,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = tokens.textColor(colors),
        )
    }
    if (actions.isEmpty()) {
        Text(text = texts.empty, style = MonarchTheme.typography.body, color = tokens.mutedColor(colors))
        return
    }
    val hiddenBefore = AiComputerViewContent.hiddenBefore(actions.size, visibleActions, showAll)
    Column(verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
        if (hiddenBefore > 0) {
            MonarchButton(
                text = texts.showEarlier(hiddenBefore),
                onClick = { showAll = true },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        actions.drop(hiddenBefore).forEach { action ->
            AiComputerViewLogRow(
                action = action,
                selected = action.id == selectedActionId,
                onSelect = onSelectAction,
                showTypedText = showTypedText,
                texts = texts,
                clock = clock,
            )
        }
    }
}

@Composable
private fun AiComputerViewLogRow(
    action: MonarchAiComputerAction,
    selected: Boolean,
    onSelect: ((String) -> Unit)?,
    showTypedText: Boolean,
    texts: AiComputerViewTexts,
    clock: AiComputerViewClock,
) {
    val tokens = MonarchAiComputerViewTokens
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Row
    val kind = texts.kind(action.kind)
    val label = AiComputerViewContent.actionLabel(action, texts.sensitiveLabel)
    val detail = AiComputerViewContent.detail(action, showTypedText)
    val detailText = detail.text ?: if (detail.hidden) texts.hidden else null
    val time = clock.time(action.at)
    val interaction = if (onSelect != null) {
        Modifier
            .monarchFocusRing(shape)
            .selectable(selected = selected, onClick = { onSelect(action.id) })
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .height(IntrinsicSize.Min)
            .background(tokens.rowColor(selected, colors), shape)
            .then(interaction)
            .semantics { contentDescription = tokens.rowText(kind, label, detailText, time) },
    ) {
        // The bar marks the selected action in shape as well as colour.
        Box(
            Modifier
                .width(tokens.SelectedBarWidth)
                .fillMaxHeight()
                .background(tokens.barColor(selected, colors)),
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = tokens.RowPaddingHorizontal, vertical = tokens.RowPaddingVertical)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(tokens.LogIconGap),
        ) {
            Box { AiComputerViewKindIcon(action.kind) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
                Text(text = label, style = MonarchTheme.typography.body, color = tokens.textColor(colors))
                if (detailText != null) {
                    Text(text = detailText, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
                }
            }
            Text(text = time, style = MonarchTheme.typography.caption, color = tokens.mutedColor(colors))
        }
    }
}

/** The glyph of an action kind, as the web: pointer, keyboard, arrows, globe, key, clock, eye, dot. */
@Composable
private fun AiComputerViewKindIcon(kind: MonarchAiComputerActionKind) {
    when (kind) {
        MonarchAiComputerActionKind.Click -> MonarchDefaultIcons.Pointer()
        MonarchAiComputerActionKind.Type -> MonarchDefaultIcons.Keyboard()
        MonarchAiComputerActionKind.Scroll -> MonarchDefaultIcons.Scroll()
        MonarchAiComputerActionKind.Navigate -> MonarchDefaultIcons.Globe()
        MonarchAiComputerActionKind.Key -> MonarchDefaultIcons.KeyPress()
        MonarchAiComputerActionKind.Wait -> MonarchDefaultIcons.Clock()
        MonarchAiComputerActionKind.Read -> MonarchDefaultIcons.Eye()
        MonarchAiComputerActionKind.Other -> MonarchDefaultIcons.Dot()
    }
}
