package com.rookhq.monarch.components.intelligence.aisuggestedactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchAnnouncerNode
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.rememberMonarchAnnouncer
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A short list of next best actions suggested for the current record, each
 * with a Run button. Actions that need approval, and every destructive one,
 * are confirmed in a dialog first, so nothing consequential runs on one tap.
 *
 * A Monarch pattern built from Monarch primitives: a heading with the origin
 * label ("Suggested by AI" in the assistant colours, or "Rule-based"), then
 * one block per action with its label, description and text tags for risk
 * and approval, a `MonarchButton` Run and an optional dismiss button. The
 * approval dialog is a `MonarchDialog` with the description, the
 * consequences and a danger confirm button for destructive actions.
 *
 * [onRun] is a suspending function: the action shows "Running…" while it
 * runs, "Done" when it returns and a line that says to try again when it
 * throws. Inside the approval dialog a failure keeps the dialog open with
 * that line, so the person can retry or cancel. A finished action is
 * announced through a polite live region.
 *
 * The actions are laid out in a `Column`, not a lazy list, because the list
 * is short and usually sits on a scrolling record screen.
 *
 * @param actions the suggestions, most useful first.
 * @param onRun runs an action by id, after approval when it needs it. Throw to mark it failed.
 * @param modifier the modifier of the list.
 * @param origin where the suggestions came from. Always shown.
 * @param title the heading; "Suggested actions" without it.
 * @param onDismiss adds a dismiss button to each action. Remove the action from [actions]; keyboard focus moves
 * to the next action.
 * @param originIcon a mark before the origin label, such as a bot icon for the AI origin. Decorative.
 * @param riskIcon an icon in the risk badge of an action, by its risk, such as a warning sign for a medium or high
 * risk. Monarch bundles no icons.
 * @param approvalIcon an icon in the "Needs approval" badge, such as a lock.
 * @param dismissIcon the dismiss button's icon; the button shows "Dismiss" without it. Monarch bundles no icons.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchAiSuggestedActions(
    actions: List<MonarchAiSuggestedAction>,
    onRun: suspend (id: String) -> Unit,
    modifier: Modifier = Modifier,
    origin: MonarchAiSuggestedActionsOrigin = MonarchAiSuggestedActionsOrigin.Ai,
    title: String? = null,
    onDismiss: ((String) -> Unit)? = null,
    originIcon: (@Composable () -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = null,
    riskIcon: (@Composable (MonarchAiToolCallRisk) -> Unit)? = null,
    approvalIcon: (@Composable () -> Unit)? = null,
    labels: MonarchAiSuggestedActionsLabels = MonarchAiSuggestedActionsLabels(),
) {
    val tokens = MonarchAiSuggestedActionsTokens
    val colors = MonarchTheme.colors
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val runHandler by rememberUpdatedState(onRun)
    val tracked = remember { mutableStateMapOf<String, MonarchAiSuggestedActionStatus>() }
    val focus = remember { mutableMapOf<String, FocusRequester>() }
    val announcer = rememberMonarchAnnouncer()
    var approving by remember { mutableStateOf<MonarchAiSuggestedAction?>(null) }
    var dismissed by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val doneAnnouncement: (String) -> String = labels.doneAnnouncement
        ?: { label -> resources.getString(R.string.monarch_aisuggestedactions_done_announcement, label) }

    suspend fun execute(action: MonarchAiSuggestedAction): Boolean {
        tracked[action.id] = MonarchAiSuggestedActionStatus.Running
        val error = runCatching { runHandler(action.id) }.exceptionOrNull()
        if (error is CancellationException) throw error
        if (error != null) {
            tracked[action.id] = MonarchAiSuggestedActionStatus.Failed
            return false
        }
        tracked[action.id] = MonarchAiSuggestedActionStatus.Done
        announcer.announce(doneAnnouncement(action.label))
        return true
    }

    LaunchedEffect(actions) {
        val (id, index) = dismissed ?: return@LaunchedEffect
        if (actions.any { it.id == id }) return@LaunchedEffect
        dismissed = null
        tokens.focusAfterDismiss(actions, index)?.let { next -> focus[next]?.let { runCatching { it.requestFocus() } } }
    }

    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        AiSuggestedActionsHeader(
            title = title ?: monarchString(labels.title, R.string.monarch_aisuggestedactions_title),
            origin = origin,
            labels = labels,
            icon = originIcon,
        )
        if (actions.isEmpty()) {
            MonarchText(
                text = monarchString(labels.empty, R.string.monarch_aisuggestedactions_empty),
                color = tokens.mutedColor(colors),
            )
        }
        if (actions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(MonarchShapeDefaults.BorderWidth, colors.borderSubtle, MonarchTheme.shapes.md)
                    .clip(MonarchTheme.shapes.md),
            ) {
            actions.forEachIndexed { index, action ->
                key(action.id) {
                    if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
                    val requester = remember { FocusRequester() }
                    focus[action.id] = requester
                    AiSuggestedActionItem(
                        action = action,
                        status = tokens.status(action, tracked[action.id]),
                        labels = labels,
                        focusRequester = requester,
                        dismissIcon = dismissIcon,
                        riskIcon = riskIcon,
                        approvalIcon = approvalIcon,
                        onRun = {
                            if (action.needsApproval) {
                                approving = action
                            } else {
                                scope.launch { execute(action) }
                            }
                        },
                        onDismiss = onDismiss?.let { dismiss ->
                            { focused ->
                                if (focused) dismissed = action.id to index
                                dismiss(action.id)
                            }
                        },
                    )
                }
            }
            }
        }
        // Always present, so a finished action is announced as a change of this region.
        MonarchAnnouncerNode(announcer)
    }

    approving?.let { action ->
        AiSuggestedActionApproval(
            action = action,
            labels = labels,
            onConfirm = { execute(action) },
            onClose = { approving = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiSuggestedActionsHeader(
    title: String,
    origin: MonarchAiSuggestedActionsOrigin,
    labels: MonarchAiSuggestedActionsLabels,
    icon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchAiSuggestedActionsTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = title, style = MonarchTextStyle.Subheading)
        val colors = tokens.origin(origin, MonarchTheme.colors)
        val text = when (origin) {
            MonarchAiSuggestedActionsOrigin.Ai ->
                monarchString(labels.originAi, R.string.monarch_aisuggestedactions_origin_ai)
            MonarchAiSuggestedActionsOrigin.Rule ->
                monarchString(labels.originRule, R.string.monarch_aisuggestedactions_origin_rule)
        }
        Row(
            modifier = Modifier
                .monarchSurface(colors, MonarchShapeDefaults.Mark)
                .padding(horizontal = tokens.OriginHorizontalPadding, vertical = tokens.OriginVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(tokens.OriginGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.content) {
                    Box(Modifier.clearAndSetSemantics { }) { icon() }
                }
            }
            Text(text = text, style = MonarchTheme.typography.caption, color = colors.content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongParameterList")
@Composable
private fun AiSuggestedActionItem(
    action: MonarchAiSuggestedAction,
    status: MonarchAiSuggestedActionStatus,
    labels: MonarchAiSuggestedActionsLabels,
    focusRequester: FocusRequester,
    dismissIcon: (@Composable () -> Unit)?,
    riskIcon: (@Composable (MonarchAiToolCallRisk) -> Unit)?,
    approvalIcon: (@Composable () -> Unit)?,
    onRun: () -> Unit,
    onDismiss: ((focused: Boolean) -> Unit)?,
) {
    val tokens = MonarchAiSuggestedActionsTokens
    val colors = MonarchTheme.colors
    val resources = LocalResources.current
    val done = status == MonarchAiSuggestedActionStatus.Done
    // The text at the start, Run and dismiss at the end of the same row, as on the web.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.ItemPadding, vertical = tokens.ItemPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.ItemGap)) {
            AiSuggestedActionText(action, done, labels, riskIcon, approvalIcon)
            if (status == MonarchAiSuggestedActionStatus.Failed) {
                MonarchText(
                    text = monarchString(labels.failed, R.string.monarch_aisuggestedactions_failed),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MonarchTextStyle.Caption,
                    color = tokens.failedColor(colors),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.InlineGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (done) {
                // As tall as a button's touch target, so the row does not jump when Run is replaced.
                Box(
                    modifier = Modifier.heightIn(min = MonarchTouchTarget.Minimum),
                    contentAlignment = Alignment.Center,
                ) {
                    MonarchText(
                        text = monarchString(labels.done, R.string.monarch_aisuggestedactions_done),
                        style = MonarchTextStyle.Label,
                        color = tokens.doneColor(colors),
                    )
                }
            } else {
                val runName = labels.runAction?.invoke(action.label)
                    ?: resources.getString(R.string.monarch_aisuggestedactions_run_action, action.label)
                MonarchButton(
                    text = monarchString(labels.run, R.string.monarch_aisuggestedactions_run),
                    onClick = onRun,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = runName },
                    size = MonarchButtonSize.Sm,
                    loading = status == MonarchAiSuggestedActionStatus.Running,
                    loadingLabel = monarchString(labels.running, R.string.monarch_aisuggestedactions_running),
                )
            }
            if (onDismiss != null) {
                AiSuggestedActionDismiss(
                    name = labels.dismiss?.invoke(action.label)
                        ?: resources.getString(R.string.monarch_aisuggestedactions_dismiss, action.label),
                    text = monarchString(labels.dismissText, R.string.monarch_aisuggestedactions_dismiss_text),
                    icon = dismissIcon,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** The label, description and tags of one action, read as one TalkBack element. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiSuggestedActionText(
    action: MonarchAiSuggestedAction,
    done: Boolean,
    labels: MonarchAiSuggestedActionsLabels,
    riskIcon: (@Composable (MonarchAiToolCallRisk) -> Unit)?,
    approvalIcon: (@Composable () -> Unit)?,
) {
    val tokens = MonarchAiSuggestedActionsTokens
    val colors = MonarchTheme.colors
    val resources = LocalResources.current
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
    ) {
        MonarchText(
            text = action.label,
            style = MonarchTextStyle.BodyStrong,
            color = tokens.labelColor(done, colors),
        )
        if (action.description != null) {
            MonarchText(
                text = action.description,
                style = MonarchTextStyle.Caption,
                color = tokens.mutedColor(colors),
            )
        }
        val riskTone = tokens.riskTone(action.risk)
        val riskText = labels.risk?.invoke(action.risk)
            ?: tokens.riskResource(action.risk)?.let { resources.getString(it) }
        if ((riskTone != null && riskText != null) || action.needsApproval) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
                verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            ) {
                if (riskTone != null && riskText != null) {
                    MonarchBadge(
                        text = riskText,
                        tone = riskTone,
                        leadingIcon = riskIcon?.let { icon -> { icon(action.risk) } },
                    )
                }
                if (action.needsApproval) {
                    MonarchBadge(
                        text = monarchString(
                            labels.needsApproval,
                            R.string.monarch_aisuggestedactions_needs_approval,
                        ),
                        tone = MonarchTone.Neutral,
                        leadingIcon = approvalIcon,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSuggestedActionDismiss(
    name: String,
    text: String,
    icon: (@Composable () -> Unit)?,
    onDismiss: (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val modifier = Modifier
        .onFocusChanged { focused = it.hasFocus }
        .semantics { contentDescription = name }
    if (icon != null) {
        MonarchIconButton(label = name, onClick = { onDismiss(focused) }, modifier = modifier, icon = icon)
    } else {
        MonarchButton(
            text = text,
            onClick = { onDismiss(focused) },
            modifier = modifier,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

@Composable
private fun AiSuggestedActionApproval(
    action: MonarchAiSuggestedAction,
    labels: MonarchAiSuggestedActionsLabels,
    onConfirm: suspend () -> Boolean,
    onClose: () -> Unit,
) {
    val resources = LocalResources.current
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    var running by remember(action.id) { mutableStateOf(false) }
    var failed by remember(action.id) { mutableStateOf(false) }
    MonarchDialog(
        onDismissRequest = { if (!running) onClose() },
        title = labels.approvalTitle?.invoke(action.label)
            ?: resources.getString(R.string.monarch_aisuggestedactions_approval_title, action.label),
        dismissible = !running,
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aisuggestedactions_cancel),
                onClick = onClose,
                variant = MonarchButtonVariant.Ghost,
                enabled = !running,
            )
            MonarchButton(
                text = action.confirmLabel
                    ?: monarchString(labels.approve, R.string.monarch_aisuggestedactions_approve),
                onClick = {
                    running = true
                    failed = false
                    scope.launch {
                        val ok = onConfirm()
                        running = false
                        if (ok) onClose() else failed = true
                    }
                },
                variant = if (action.risk == MonarchAiToolCallRisk.Destructive) {
                    MonarchButtonVariant.Danger
                } else {
                    MonarchButtonVariant.Primary
                },
                loading = running,
                loadingLabel = monarchString(labels.running, R.string.monarch_aisuggestedactions_running),
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchAiSuggestedActionsTokens.ItemGap)) {
            MonarchText(
                text = action.description
                    ?: monarchString(
                        labels.approvalDescription,
                        R.string.monarch_aisuggestedactions_approval_description,
                    ),
            )
            action.consequences.forEach { consequence -> MonarchText(text = "• $consequence") }
            if (failed) {
                MonarchText(
                    text = monarchString(labels.failed, R.string.monarch_aisuggestedactions_failed),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MonarchAiSuggestedActionsTokens.failedColor(colors),
                )
            }
        }
    }
}
