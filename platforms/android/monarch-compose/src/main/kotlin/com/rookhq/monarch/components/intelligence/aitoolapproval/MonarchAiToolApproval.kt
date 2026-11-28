package com.rookhq.monarch.components.intelligence.aitoolapproval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallPart
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallRisk
import com.rookhq.monarch.components.intelligence.aitoolcall.MonarchAiToolCallTokens
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The approval gate for a tool call the assistant wants to run ("Send email
 * to 3 people", "Delete 12 records"): what it will do, its arguments as
 * labelled values, the risk, and Approve and Deny.
 *
 * Built by Monarch from `MonarchCard`, `MonarchBadge`, `MonarchButton`,
 * `MonarchTextField` and `MonarchCheckboxRow`. People can edit the
 * arguments as JSON before approving, add a reason when denying and, when
 * [onAlwaysAllow] is set, always allow the tool. A destructive call uses the
 * danger tone and a danger Approve button, states that it cannot be undone,
 * and never offers Always allow. Once decided, or when [part] is no longer
 * waiting, the buttons are replaced by the outcome ("Approved", "Denied",
 * "Cancelled"). The card is announced through a polite live region when it
 * appears ("Approval needed: Send email").
 *
 * The decision is kept per [MonarchAiToolCallPart.id]: a new call resets the card.
 *
 * @param part the tool call waiting for a decision, in the `PendingApproval` state.
 * @param onApprove called on Approve with the edited arguments, or `null` when they were not edited, so
 * `conversation.approveTool(part.id, input)` passes them on.
 * @param onDeny called on Deny with the trimmed reason, or `null` when none was given.
 * @param modifier the modifier of the card.
 * @param risk how much harm approving can do. Defaults to the part's risk, else medium.
 * @param consequence what happens if this is approved. `null` uses the risk's default line.
 * @param toolLabels display names per tool name; other tools get a humanized name.
 * @param argumentLabels labels per argument key, such as `"to" to "Recipients"`; other keys are humanized.
 * @param editable offers Edit arguments.
 * @param onAlwaysAllow shows "Always allow this tool"; when ticked it is called with the tool name just before
 * [onApprove]. Never offered for a destructive call.
 * @param icon an icon before the eyebrow, such as a warning sign. Decorative.
 * @param labels replacements for the built-in strings.
 * @param describeTool the title in plain words for a call, such as "Send email to 3 people". `null`, or `null`
 * from it, uses the tool's display name.
 */
@ExperimentalMonarchApi
@Composable
@Suppress("LongParameterList")
public fun MonarchAiToolApproval(
    part: MonarchAiToolCallPart,
    onApprove: (input: MonarchAiJSON?) -> Unit,
    onDeny: (reason: String?) -> Unit,
    modifier: Modifier = Modifier,
    risk: MonarchAiToolCallRisk = part.risk ?: MonarchAiToolCallRisk.Medium,
    consequence: String? = null,
    toolLabels: Map<String, String> = emptyMap(),
    argumentLabels: Map<String, String> = emptyMap(),
    editable: Boolean = true,
    onAlwaysAllow: ((toolName: String) -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    labels: MonarchAiToolApprovalLabels = MonarchAiToolApprovalLabels(),
    describeTool: ((MonarchAiToolCallPart) -> String?)? = null,
) {
    val tokens = MonarchAiToolApprovalTokens
    val title = describeTool?.invoke(part) ?: MonarchAiToolCallTokens.label(part.name, toolLabels)
    val state = rememberAiToolApprovalState(part)
    val outcome = tokens.outcome(part.state, state.decision.value)
    val announcement = aiToolApprovalAnnouncement(part.id, title, pending = outcome == null, labels = labels)
    MonarchCard(modifier = modifier.semantics { isTraversalGroup = true }) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            AiToolApprovalHeader(title, risk, icon, labels)
            val line = consequence ?: labels.consequence?.invoke(risk)
                ?: tokens.consequenceText(risk)?.let { stringResource(it) }
            if (line != null) {
                Text(
                    text = line,
                    style = MonarchTheme.typography.body,
                    color = tokens.consequenceColor(risk, MonarchTheme.colors),
                )
            }
            AiToolApprovalArguments(part.input, argumentLabels, labels)
            if (outcome != null) {
                AiToolApprovalOutcomeLine(outcome, labels)
            } else {
                AiToolApprovalControls(
                    part = part,
                    state = state,
                    config = AiToolApprovalConfig(risk, editable, onAlwaysAllow, onApprove, onDeny),
                    labels = labels,
                )
            }
            // Always present, so TalkBack announces the card once it is on screen.
            MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
        }
    }
}

/** The local state of one card, reset for each call id. */
internal class AiToolApprovalState(
    val decision: MutableState<AiToolApprovalDecision?>,
    val editing: MutableState<Boolean>,
    val json: MutableState<String>,
    val jsonError: MutableState<Boolean>,
    val reasonOpen: MutableState<Boolean>,
    val reason: MutableState<String>,
    val always: MutableState<Boolean>,
)

@Composable
private fun rememberAiToolApprovalState(part: MonarchAiToolCallPart): AiToolApprovalState {
    val original = MonarchAiToolApprovalFormat.editableText(part.input)
    return AiToolApprovalState(
        decision = rememberSaveable(part.id) { mutableStateOf(null) },
        editing = rememberSaveable(part.id) { mutableStateOf(false) },
        json = rememberSaveable(part.id) { mutableStateOf(original) },
        jsonError = rememberSaveable(part.id) { mutableStateOf(false) },
        reasonOpen = rememberSaveable(part.id) { mutableStateOf(false) },
        reason = rememberSaveable(part.id) { mutableStateOf("") },
        always = rememberSaveable(part.id) { mutableStateOf(false) },
    )
}

/** The text of the live region: set once the card is on screen, while the call waits. */
@Composable
private fun aiToolApprovalAnnouncement(
    id: String,
    title: String,
    pending: Boolean,
    labels: MonarchAiToolApprovalLabels,
): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = stringResource(R.string.monarch_aitoolapproval_announce)
    val text = labels.announce?.invoke(title) ?: String.format(locale, format, title)
    var shown by rememberSaveable(id) { mutableStateOf(false) }
    LaunchedEffect(id) { shown = true }
    return if (shown && pending) text else ""
}

@Composable
private fun AiToolApprovalHeader(
    title: String,
    risk: MonarchAiToolCallRisk,
    icon: (@Composable () -> Unit)?,
    labels: MonarchAiToolApprovalLabels,
) {
    val tokens = MonarchAiToolApprovalTokens
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.InnerGap)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.InnerGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.invoke()
            Text(
                text = monarchString(labels.eyebrow, R.string.monarch_aitoolapproval_eyebrow),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
        }
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.bodyStrong,
            color = colors.textDefault,
        )
        MonarchBadge(
            text = labels.risk?.invoke(risk) ?: stringResource(tokens.riskText(risk)),
            tone = tokens.tone(risk),
        )
    }
}

@Composable
private fun AiToolApprovalArguments(
    input: MonarchAiJSON,
    argumentLabels: Map<String, String>,
    labels: MonarchAiToolApprovalLabels,
) {
    val tokens = MonarchAiToolApprovalTokens
    val colors = MonarchTheme.colors
    val rows = MonarchAiToolApprovalFormat.rows(input, argumentLabels)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.InnerGap)) {
        Text(
            text = monarchString(labels.details, R.string.monarch_aitoolapproval_details),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.label,
            color = colors.textMuted,
        )
        when {
            !rows.isNullOrEmpty() -> rows.forEach { (key, value) ->
                Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    Text(text = key, style = MonarchTheme.typography.caption, color = colors.textMuted)
                    Text(text = value, style = MonarchTheme.typography.body, color = colors.textDefault)
                }
            }
            rows != null || input == MonarchAiJSON.Null -> Text(
                text = monarchString(labels.noArguments, R.string.monarch_aitoolapproval_no_arguments),
                style = MonarchTheme.typography.body,
                color = colors.textMuted,
            )
            else -> Text(
                text = MonarchAiToolApprovalFormat.readable(input),
                style = MonarchTheme.typography.code,
                color = colors.textDefault,
            )
        }
    }
}

@Composable
private fun AiToolApprovalOutcomeLine(outcome: AiToolApprovalOutcome, labels: MonarchAiToolApprovalLabels) {
    val text = when (outcome) {
        AiToolApprovalOutcome.Approved -> monarchString(labels.approved, R.string.monarch_aitoolapproval_approved)
        AiToolApprovalOutcome.Denied -> monarchString(labels.denied, R.string.monarch_aitoolapproval_denied)
        AiToolApprovalOutcome.Cancelled -> monarchString(labels.cancelled, R.string.monarch_aitoolapproval_cancelled)
    }
    Text(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MonarchTheme.typography.bodyStrong,
        color = MonarchAiToolApprovalTokens.outcomeColor(outcome, MonarchTheme.colors),
    )
}
