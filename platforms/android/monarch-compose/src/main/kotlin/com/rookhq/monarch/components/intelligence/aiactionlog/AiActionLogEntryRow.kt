package com.rookhq.monarch.components.intelligence.aiactionlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.components.intelligence.aiagentavatar.MonarchAiAgentAvatar
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * One entry of `MonarchAiActionLog`: the decorative avatar, the sentence with
 * the "AI agent" badge, the time and Undo. The sentence, time and failure are
 * one TalkBack element; Undo is a separate button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AiActionLogEntryRow(
    entry: MonarchAiActionLogEntry,
    state: MonarchAiActionLogUndo?,
    canUndo: Boolean,
    time: String,
    failure: String,
    labels: MonarchAiActionLogLabels,
    onUndo: () -> Unit,
    onOpen: (() -> Unit)?,
) {
    val tokens = MonarchAiActionLogTokens
    val palette = MonarchAiActionLogColors
    val colors = MonarchTheme.colors
    val undone = tokens.undone(entry, state)
    val failed = state == MonarchAiActionLogUndo.Failed
    val actorKind = monarchString(labels.actorKind, R.string.monarch_aiactionlog_actor_kind)
    val undoneText = monarchString(labels.undone, R.string.monarch_aiactionlog_undone)
    val target = entry.target?.takeIf { it.isNotBlank() }
    val openName = target?.let {
        labels.openTarget?.invoke(it) ?: stringResource(R.string.monarch_aiactionlog_open_target, it)
    }
    val meta = if (undone) "$time · $undoneText" else time
    val spokenState = listOfNotNull(meta, failure.takeIf { failed }).joinToString(", ")
    Row(
        modifier = Modifier.padding(vertical = tokens.EntryPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.EntryGap),
        verticalAlignment = Alignment.Top,
    ) {
        MonarchAiAgentAvatar(
            agent = entry.agent,
            image = entry.agentImage,
            size = MonarchAvatarSize.Sm,
            decorative = true,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = tokens.spoken(entry, actorKind)
                    stateDescription = spokenState
                    if (onOpen != null && openName != null) {
                        customActions = listOf(
                            CustomAccessibilityAction(openName) {
                                onOpen()
                                true
                            },
                        )
                    }
                },
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.agent.name,
                    style = MonarchTheme.typography.bodyStrong,
                    color = palette.nameColor(colors),
                )
                MonarchBadge(text = actorKind, variant = MonarchBadgeVariant.Outline, tone = MonarchTone.Ai)
            }
            val linkColor = palette.linkColor(colors)
            Text(
                text = buildAnnotatedString {
                    val struck = if (undone) SpanStyle(textDecoration = TextDecoration.LineThrough) else SpanStyle()
                    withStyle(struck) {
                        append(entry.action)
                        if (target != null) {
                            append(" ")
                            if (onOpen != null) {
                                val linkDecoration = if (undone) {
                                    TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                                } else {
                                    TextDecoration.Underline
                                }
                                val link = LinkAnnotation.Clickable(
                                    tag = entry.id,
                                    styles = TextLinkStyles(
                                        SpanStyle(color = linkColor, textDecoration = linkDecoration),
                                    ),
                                ) { onOpen() }
                                withLink(link) { append(target) }
                            } else {
                                append(target)
                            }
                        }
                    }
                },
                style = MonarchTheme.typography.body,
                color = palette.actionColor(undone, colors),
            )
            Text(text = meta, style = MonarchTheme.typography.caption, color = palette.metaColor(colors))
            if (failed) {
                Text(text = failure, style = MonarchTheme.typography.caption, color = palette.failureColor(colors))
            }
        }
        if (canUndo) {
            val undoName = labels.undoLabel?.invoke(tokens.description(entry))
                ?: stringResource(R.string.monarch_aiactionlog_undo_label, tokens.description(entry))
            MonarchButton(
                text = monarchString(labels.undo, R.string.monarch_aiactionlog_undo),
                onClick = onUndo,
                modifier = Modifier.semantics { contentDescription = undoName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                loading = state == MonarchAiActionLogUndo.Pending,
            )
        }
    }
}
