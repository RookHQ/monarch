package dev.monarchui.components.intelligence.aiautofillfield

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeter
import dev.monarchui.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterSize
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadge
import dev.monarchui.components.intelligence.aigeneratedbadge.MonarchAiGeneratedBadgeSize
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.field.LocalMonarchFieldAiTint
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.theme.MonarchTheme

/**
 * A form field that AI filled in: the app's control, marked with the AI
 * badge and a confidence badge, where the value came from, and Accept and
 * Clear.
 *
 * Built by Monarch around any control, with `MonarchAiGeneratedBadge`,
 * `MonarchAiConfidenceMeter` and `MonarchButton`. While [state] is suggested a
 * bar in the AI border colour runs beside the control; the badge says
 * "AI suggested" in words, so the bar's colour is not the only signal. The app
 * drives [state]: set it to accepted in [onAccept], to cleared in [onClear]
 * (after emptying the control) and to edited when the person changes the
 * value. The status line is one TalkBack element read after the control, and
 * "Suggestion accepted" or "Suggestion cleared" is announced.
 *
 * @param state where the field stands.
 * @param suggestion the value AI suggested, shown as "AI suggested: …" once the person edits it.
 * @param onAccept the person accepted the value.
 * @param onClear the person cleared the value.
 * @param modifier the modifier of the field.
 * @param fieldName the field's name, so the actions are read as "Accept suggested Company".
 * @param source where the value came from, shown as "From …" until the person edits it.
 * @param confidence how sure the AI is, 0 through 1, shown as a confidence badge while suggested.
 * @param lowThreshold the confidence below which it reads as low.
 * @param highThreshold the confidence from which it reads as high.
 * @param confidenceExplanation why the AI chose the value, read with the confidence badge.
 * @param model the model, offered by the AI badge.
 * @param dateMillis when the value was produced, in epoch milliseconds (UTC), offered by the AI badge.
 * @param enabled disables Accept and Clear.
 * @param labels replacements for the built-in strings.
 * @param control the form control, usually a `MonarchTextField`.
 * @param acceptIcon an icon before the text of the Accept button, such as a check mark. Monarch bundles no icons.
 * @param clearIcon an icon before the text of the Clear button, such as a cross.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiAutofillField(
    state: MonarchAiAutofillState,
    suggestion: String,
    onAccept: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    fieldName: String? = null,
    source: String? = null,
    confidence: Float? = null,
    lowThreshold: Float = 0.6f,
    highThreshold: Float = 0.85f,
    confidenceExplanation: String? = null,
    model: String? = null,
    dateMillis: Long? = null,
    enabled: Boolean = true,
    labels: MonarchAiAutofillFieldLabels = MonarchAiAutofillFieldLabels(),
    acceptIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Check,
    clearIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    control: @Composable () -> Unit,
) {
    val tokens = MonarchAiAutofillFieldTokens
    val announcer = rememberMonarchAnnouncer()
    val acceptedText = monarchString(labels.accepted, R.string.monarch_aiautofillfield_accepted)
    val clearedText = monarchString(labels.cleared, R.string.monarch_aiautofillfield_cleared)
    val marked = tokens.showsActions(state)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        CompositionLocalProvider(LocalMonarchFieldAiTint provides marked) { control() }
        if (tokens.showsStatus(state)) {
            FlowRow(
                modifier = Modifier.semantics(mergeDescendants = true) { },
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAiGeneratedBadge(
                    kind = tokens.badgeKind(state),
                    model = model,
                    dateMillis = dateMillis,
                    size = MonarchAiGeneratedBadgeSize.Sm,
                    label = labels.badge?.invoke(state) ?: stringResource(badgeResource(state)),
                )
                if (tokens.showsConfidence(state, confidence) && confidence != null) {
                    MonarchAiConfidenceMeter(
                        value = confidence,
                        size = MonarchAiConfidenceMeterSize.Sm,
                        explanation = confidenceExplanation,
                        lowThreshold = lowThreshold,
                        highThreshold = highThreshold,
                    )
                }
                StatusText(state, suggestion, source, labels)
            }
        }
        if (tokens.showsActions(state)) {
            val name = fieldName?.takeIf { it.isNotBlank() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.LineGap, Alignment.End),
            ) {
                MonarchButton(
                    text = monarchString(labels.accept, R.string.monarch_aiautofillfield_accept),
                    onClick = {
                        announcer.announce(acceptedText)
                        onAccept()
                    },
                    modifier = Modifier.actionName(name, labels.acceptField, AcceptField),
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = enabled,
                    leadingIcon = acceptIcon,
                )
                MonarchButton(
                    text = monarchString(labels.clear, R.string.monarch_aiautofillfield_clear),
                    onClick = {
                        announcer.announce(clearedText)
                        onClear()
                    },
                    modifier = Modifier.actionName(name, labels.clearField, ClearField),
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = enabled,
                    leadingIcon = clearIcon,
                )
            }
        }
        // Always present, so the result of Accept and Clear is announced as a change of this region.
        MonarchAnnouncerNode(announcer)
    }
}

/** The source line while suggested or accepted, the original suggestion once edited. */
@Composable
private fun StatusText(
    state: MonarchAiAutofillState,
    suggestion: String,
    source: String?,
    labels: MonarchAiAutofillFieldLabels,
) {
    val tokens = MonarchAiAutofillFieldTokens
    val text = when {
        state == MonarchAiAutofillState.Edited ->
            labels.original?.invoke(suggestion) ?: stringResource(R.string.monarch_aiautofillfield_original, suggestion)
        source != null && tokens.showsSource(state, source) ->
            labels.source?.invoke(source) ?: stringResource(R.string.monarch_aiautofillfield_source, source)
        else -> null
    }
    if (text != null) {
        Text(text = text, style = MonarchTheme.typography.caption, color = tokens.lineColor(MonarchTheme.colors))
    }
}

private val AcceptField = R.string.monarch_aiautofillfield_accept_field
private val ClearField = R.string.monarch_aiautofillfield_clear_field

/** Names an action after the field, "Accept suggested Company", when there is a field name. */
@Composable
private fun Modifier.actionName(name: String?, override: ((String) -> String)?, resource: Int): Modifier {
    if (name == null) return this
    val spoken = override?.invoke(name) ?: stringResource(resource, name)
    return semantics { contentDescription = spoken }
}

private fun badgeResource(state: MonarchAiAutofillState): Int = when (state) {
    MonarchAiAutofillState.Edited -> R.string.monarch_aiautofillfield_badge_edited
    MonarchAiAutofillState.Accepted -> R.string.monarch_aiautofillfield_badge_accepted
    MonarchAiAutofillState.Suggested, MonarchAiAutofillState.Cleared -> R.string.monarch_aiautofillfield_badge_suggested
}
