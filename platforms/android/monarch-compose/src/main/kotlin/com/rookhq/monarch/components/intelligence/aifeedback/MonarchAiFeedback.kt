package com.rookhq.monarch.components.intelligence.aifeedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.tag.MonarchChip
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroup
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiMessageFeedback
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Thumbs up and down for an AI answer, then reasons and an optional comment
 * in a sheet, then a thank-you. Rating and sending are separate: the rating
 * changes as soon as a thumb is pressed, and [onSubmit] sends the details.
 *
 * Each thumb is a toggle `MonarchIconButton` when the app gives [icon], or a
 * `MonarchChip` with the thumb's name when it does not (`null` icon). The
 * details are a `MonarchBottomSheet` with a
 * `MonarchCheckboxGroup` of reasons, a multi-line `MonarchTextField` and
 * `MonarchButton`s; the sheet cannot be dismissed while sending. The
 * thank-you and errors are announced through a polite live region that is
 * always present under the thumbs.
 *
 * @param selection the rating, `null` for none.
 * @param onSelectionChange called with the new rating, or `null` when the pressed thumb is pressed again.
 * @param modifier the modifier of the thumbs.
 * @param reasons the reasons offered for each rating; a rating without an entry offers none. `null` offers
 * Inaccurate, Not helpful, Incomplete, Hard to follow and Harmful or unsafe after a thumbs down and none after a
 * thumbs up.
 * @param asksDetails which ratings open the sheet; the others are sent at once.
 * @param enabled whether the thumbs can be pressed.
 * @param icon the glyph of a thumb, given the rating and whether it is pressed (show a filled glyph when pressed).
 * `null` shows each thumb as a chip with its name.
 * @param labels replacements for the built-in strings.
 * @param onSubmit sends the rating, the chosen reason ids in listed order and the trimmed comment. Throw to show
 * the error.
 */
@Composable
@Suppress("LongParameterList")
public fun MonarchAiFeedback(
    selection: MonarchAiMessageFeedback?,
    onSelectionChange: (MonarchAiMessageFeedback?) -> Unit,
    modifier: Modifier = Modifier,
    reasons: Map<MonarchAiMessageFeedback, List<MonarchCheckboxGroupOption<String>>>? = null,
    asksDetails: MonarchAiFeedbackDetails = MonarchAiFeedbackDetails.Down,
    enabled: Boolean = true,
    icon: (@Composable (value: MonarchAiMessageFeedback, checked: Boolean) -> Unit)? = MonarchDefaultIcons.AiFeedback,
    labels: MonarchAiFeedbackLabels = MonarchAiFeedbackLabels(),
    onSubmit: (suspend (MonarchAiFeedbackSubmission) -> Unit)? = null,
) {
    val rules = MonarchAiFeedbackRules
    val texts = aiFeedbackTexts(labels)
    var phase by rememberSaveable { mutableStateOf(AiFeedbackPhase.Idle) }
    var failed by rememberSaveable { mutableStateOf(false) }
    var chosen by rememberSaveable { mutableStateOf(listOf<String>()) }
    var comment by rememberSaveable { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val submit by rememberUpdatedState(onSubmit)
    val send: (MonarchAiFeedbackSubmission, Boolean) -> Unit = { submission, fromSheet ->
        failed = false
        phase = AiFeedbackPhase.Sending
        coroutineScope.launch {
            val outcome = runCatching { submit?.invoke(submission) }
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            failed = error != null
            phase = rules.afterSend(succeeded = error == null, fromSheet = fromSheet)
        }
    }
    val rate: (MonarchAiMessageFeedback) -> Unit = { pressed ->
        val next = rules.next(selection, pressed)
        onSelectionChange(next)
        chosen = emptyList()
        comment = ""
        failed = false
        val after = rules.afterRating(next, asksDetails)
        when {
            after != null -> phase = after
            next != null -> send(MonarchAiFeedbackSubmission(next, emptyList(), ""), false)
        }
    }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.semantics {
                isTraversalGroup = true
                contentDescription = texts.group
            },
            horizontalArrangement = Arrangement.spacedBy(MonarchAiFeedbackTokens.ThumbGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbsEnabled = enabled && phase != AiFeedbackPhase.Sending
            AiFeedbackThumb(MonarchAiMessageFeedback.Up, texts.good, selection, thumbsEnabled, icon, rate)
            AiFeedbackThumb(MonarchAiMessageFeedback.Down, texts.bad, selection, thumbsEnabled, icon, rate)
        }
        val message = when (rules.message(phase, failed)) {
            true -> texts.error
            false -> texts.thanks
            null -> null
        }
        AiFeedbackMessage(message, failed)
    }
    val value = selection
    if (value != null && rules.showsSheet(phase, value, asksDetails)) {
        val offered = rules.reasonsFor(reasons, value, aiFeedbackDefaultReasons())
        AiFeedbackSheet(
            question = if (value == MonarchAiMessageFeedback.Up) texts.sheet.reasonsUp else texts.sheet.reasonsDown,
            offered = offered,
            state = AiFeedbackSheetState(chosen, comment, phase == AiFeedbackPhase.Sending, failed),
            texts = texts,
            onChosenChange = { chosen = it },
            onCommentChange = { comment = it },
            onSend = { send(rules.submission(value, offered, chosen, comment), true) },
            onCancel = {
                phase = AiFeedbackPhase.Idle
                failed = false
            },
        )
    }
}

/** One thumb: an icon toggle button with the app's glyph, or a filter chip with the thumb's name. */
@Composable
@Suppress("LongParameterList")
private fun AiFeedbackThumb(
    value: MonarchAiMessageFeedback,
    name: String,
    selection: MonarchAiMessageFeedback?,
    enabled: Boolean,
    icon: (@Composable (MonarchAiMessageFeedback, Boolean) -> Unit)?,
    onPress: (MonarchAiMessageFeedback) -> Unit,
) {
    val checked = selection == value
    if (icon != null) {
        MonarchIconButton(
            label = name,
            checked = checked,
            onCheckedChange = { onPress(value) },
            enabled = enabled,
        ) { icon(value, checked) }
    } else {
        MonarchChip(
            text = name,
            selected = checked,
            onSelectedChange = { onPress(value) },
            enabled = enabled,
        )
    }
}

/** The thank-you or the error, in a polite live region that is always present. */
@Composable
private fun AiFeedbackMessage(message: String?, failed: Boolean) {
    Column(
        modifier = Modifier.clearAndSetSemantics {
            if (message != null) contentDescription = message
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        if (message != null) {
            Text(
                text = message,
                style = MonarchTheme.typography.caption,
                color = MonarchAiFeedbackTokens.messageColor(failed, MonarchTheme.colors),
            )
        }
    }
}

/** What the sheet shows. */
private class AiFeedbackSheetState(
    val chosen: List<String>,
    val comment: String,
    val sending: Boolean,
    val failed: Boolean,
)

/** The sheet with the reasons, the comment, an error, Send feedback and Cancel. */
@Composable
@Suppress("LongParameterList")
private fun AiFeedbackSheet(
    question: String,
    offered: List<MonarchCheckboxGroupOption<String>>,
    state: AiFeedbackSheetState,
    texts: AiFeedbackTexts,
    onChosenChange: (List<String>) -> Unit,
    onCommentChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val sending by rememberUpdatedState(state.sending)
    val tokens = MonarchAiFeedbackTokens
    MonarchBottomSheet(
        onDismissRequest = { if (!sending) onCancel() },
        title = texts.group,
        dismissible = !state.sending,
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(tokens.SheetGap),
        ) {
            if (offered.isNotEmpty()) {
                MonarchCheckboxGroup(
                    options = offered,
                    value = state.chosen,
                    onValueChange = onChosenChange,
                    enabled = !state.sending,
                    label = question,
                )
            }
            MonarchTextField(
                value = state.comment,
                onValueChange = onCommentChange,
                modifier = Modifier.fillMaxWidth(),
                label = texts.sheet.comment,
                placeholder = texts.sheet.commentPlaceholder,
                enabled = !state.sending,
                singleLine = false,
            )
            if (state.failed) {
                MonarchInlineNotice(
                    message = texts.error,
                    tone = MonarchTone.Danger,
                    announcement = MonarchBannerAnnouncement.Polite,
                )
            }
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(tokens.ThumbGap),
            ) {
                MonarchButton(
                    text = texts.sheet.cancel,
                    onClick = onCancel,
                    variant = MonarchButtonVariant.Ghost,
                    enabled = !state.sending,
                )
                MonarchButton(
                    text = texts.sheet.submit,
                    onClick = onSend,
                    variant = MonarchButtonVariant.Primary,
                    loading = state.sending,
                )
            }
        }
    }
}

/** The built-in strings, or their replacements. */
private class AiFeedbackTexts(
    val group: String,
    val good: String,
    val bad: String,
    val thanks: String,
    val error: String,
    val sheet: AiFeedbackSheetTexts,
)

/** The texts of the sheet. */
private class AiFeedbackSheetTexts(
    val reasonsDown: String,
    val reasonsUp: String,
    val comment: String,
    val commentPlaceholder: String,
    val submit: String,
    val cancel: String,
)

@Composable
private fun aiFeedbackTexts(labels: MonarchAiFeedbackLabels): AiFeedbackTexts = AiFeedbackTexts(
    group = monarchString(labels.group, R.string.monarch_aifeedback_group),
    good = monarchString(labels.good, R.string.monarch_aifeedback_good),
    bad = monarchString(labels.bad, R.string.monarch_aifeedback_bad),
    thanks = monarchString(labels.thanks, R.string.monarch_aifeedback_thanks),
    error = monarchString(labels.error, R.string.monarch_aifeedback_error),
    sheet = AiFeedbackSheetTexts(
        reasonsDown = monarchString(labels.reasonsDown, R.string.monarch_aifeedback_reasons_down),
        reasonsUp = monarchString(labels.reasonsUp, R.string.monarch_aifeedback_reasons_up),
        comment = monarchString(labels.comment, R.string.monarch_aifeedback_comment),
        commentPlaceholder = monarchString(labels.commentPlaceholder, R.string.monarch_aifeedback_comment_placeholder),
        submit = monarchString(labels.submit, R.string.monarch_aifeedback_submit),
        cancel = monarchString(labels.cancel, R.string.monarch_aifeedback_cancel),
    ),
)

/** The built-in reasons after a thumbs down, with the web's ids. */
@Composable
private fun aiFeedbackDefaultReasons(): List<MonarchCheckboxGroupOption<String>> = listOf(
    MonarchCheckboxGroupOption("inaccurate", monarchString(null, R.string.monarch_aifeedback_reason_inaccurate)),
    MonarchCheckboxGroupOption("not-helpful", monarchString(null, R.string.monarch_aifeedback_reason_not_helpful)),
    MonarchCheckboxGroupOption("incomplete", monarchString(null, R.string.monarch_aifeedback_reason_incomplete)),
    MonarchCheckboxGroupOption(
        "hard-to-follow",
        monarchString(null, R.string.monarch_aifeedback_reason_hard_to_follow),
    ),
    MonarchCheckboxGroupOption("unsafe", monarchString(null, R.string.monarch_aifeedback_reason_unsafe)),
)
