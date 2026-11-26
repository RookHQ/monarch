package com.rookhq.monarch.components.collaboration.decisioncard

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardGroup
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardLayout
import com.rookhq.monarch.components.forms.radiocardgroup.MonarchRadioCardOption
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Records a decision about an item (keep, review, let lapse; approve, defer,
 * reject) with a reason, next to the evidence for it.
 *
 * Built by Monarch from `MonarchCard`, `MonarchRadioCardGroup`, `MonarchTextField`,
 * `MonarchButton` and `MonarchBadge`. It shows what the selected choice will
 * do before saving, who decided and when, and a read-only view for people who
 * cannot change it. The choice and the reason being edited are kept by the
 * card and reset whenever the app passes a new [value] or [reason].
 *
 * Saving is hoisted: [onDecide] receives the decision, the app sets [saving]
 * while it stores it, then passes the new [value] and [reason], or an
 * [errorText]. When [saving] ends without an error, "Decision saved." is shown
 * and announced.
 *
 * Times are epoch milliseconds in UTC, as in the Material 3 `DatePickerState`.
 *
 * @param options the choices, in order. Two to five work best.
 * @param value the saved decision, or `null` for none.
 * @param modifier the modifier of the decision card.
 * @param title the heading of the card, for example "Renewal decision".
 * @param reason the saved reason.
 * @param decidedAt when the saved decision was made, in epoch milliseconds. Shown as a relative time.
 * @param now the moment the relative time is measured from, in epoch milliseconds. `null` is the current time, kept
 * up to date; pass a fixed value in previews and tests.
 * @param decidedBy who made the saved decision.
 * @param allowNone offers a "No decision" choice that clears the decision.
 * @param reasonRequired every choice needs a reason. An option's own `reasonRequired` overrides it.
 * @param readOnly shows the decision as a badge with the reason, without the form.
 * @param saving the app is storing the decision: the form is disabled and the button shows progress.
 * @param errorText the reason the last save failed. The choice and the reason are kept.
 * @param reasonLimit the longest reason accepted, in characters. Longer input is cut.
 * @param labels replacements for the built-in strings.
 * @param onDecide saves the decision. Not called when nothing changed or a required reason is missing.
 * Without it the card is read-only.
 * @param context evidence for the decision, shown above the choices: usage, risk, cost.
 */
@Composable
public fun MonarchDecisionCard(
    options: List<MonarchDecisionOption>,
    value: String?,
    modifier: Modifier = Modifier,
    title: String? = null,
    reason: String? = null,
    decidedAt: Long? = null,
    now: Long? = null,
    decidedBy: String? = null,
    allowNone: Boolean = true,
    reasonRequired: Boolean = false,
    readOnly: Boolean = false,
    saving: Boolean = false,
    errorText: String? = null,
    reasonLimit: Int = MonarchDecisionCardTokens.DEFAULT_REASON_LIMIT,
    labels: MonarchDecisionCardLabels = MonarchDecisionCardLabels(),
    onDecide: ((MonarchDecision) -> Unit)? = null,
    context: (@Composable ColumnScope.() -> Unit)? = null,
) {
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchDecisionCardTokens.Gap)) {
            if (title != null) MonarchText(text = title, style = MonarchTextStyle.Subheading, heading = true)
            if (context != null) Column(content = context)
            val meta = decisionMeta(decidedAt, decidedBy, labels, now)
            if (readOnly || onDecide == null) {
                DecisionReadOnly(options = options, value = value, reason = reason, meta = meta, labels = labels)
            } else {
                DecisionForm(
                    state = DecisionFormInput(options, value, reason, allowNone, reasonRequired, reasonLimit),
                    saving = saving,
                    errorText = errorText,
                    meta = meta,
                    labels = labels,
                    onDecide = onDecide,
                )
            }
        }
    }
}

/** The inputs of the form, grouped to keep the signatures short. */
private class DecisionFormInput(
    val options: List<MonarchDecisionOption>,
    val value: String?,
    val reason: String?,
    val allowNone: Boolean,
    val reasonRequired: Boolean,
    val reasonLimit: Int,
)

/** What is remembered between compositions to notice the end of a save. */
private class SaveTracker {
    var saving: Boolean = false
    var saved: Boolean = false
}

/** The decisions as a row of selectable tiles, with the "no decision" tile last when the form allows it. */
@Composable
private fun DecisionChoices(
    state: DecisionFormInput,
    choice: String?,
    saving: Boolean,
    labels: MonarchDecisionCardLabels,
    onChoose: (String?) -> Unit,
) {
    val none = "\u0000none"
    val noneText = monarchString(labels.none, R.string.monarch_decisioncard_none)
    val cards = state.options.map {
        MonarchRadioCardOption<String>(it.value, it.label, it.description, enabled = it.enabled && !saving)
    } + if (state.allowNone) {
        listOf(MonarchRadioCardOption(none, noneText, enabled = !saving))
    } else {
        emptyList()
    }
    MonarchRadioCardGroup(
        options = cards,
        selected = choice ?: if (state.allowNone) none else null,
        onSelect = { onChoose(if (it == none) null else it) },
        label = monarchString(labels.choices, R.string.monarch_decisioncard_choices),
        columns = cards.size.coerceIn(1, MonarchDecisionCardTokens.MAX_TILE_COLUMNS),
        layout = MonarchRadioCardLayout.Tile,
    )
}

@Composable
private fun DecisionForm(
    state: DecisionFormInput,
    saving: Boolean,
    errorText: String?,
    meta: DecisionMetaText?,
    labels: MonarchDecisionCardLabels,
    onDecide: (MonarchDecision) -> Unit,
) {
    val tokens = MonarchDecisionCardTokens
    val muted = tokens.mutedText(MonarchTheme.colors)
    var choice by rememberSaveable(state.value, state.reason) { mutableStateOf(state.value) }
    var text by rememberSaveable(state.value, state.reason) { mutableStateOf(state.reason.orEmpty()) }
    var showMissing by rememberSaveable(state.value, state.reason) { mutableStateOf(false) }
    val tracker = remember { SaveTracker() }
    val saved = (tracker.saving && !saving && errorText == null) || (tracker.saved && !saving)
    SideEffect {
        tracker.saving = saving
        tracker.saved = saved
    }
    val needsReason = tokens.needsReason(choice, state.options, state.reasonRequired)
    val missing = needsReason && text.isBlank()
    val dirty = tokens.dirty(choice, text, state.value, state.reason)
    val edit = { tracker.saved = false }

    DecisionChoices(state, choice, saving, labels) {
        edit()
        choice = it
        showMissing = false
    }
    val note = state.options.firstOrNull { it.value == choice }?.note
    if (note != null) {
        Text(
            text = note,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MonarchTheme.typography.body,
            color = muted,
        )
    }
    ReasonField(
        text = text,
        onTextChange = {
            edit()
            text = it.take(state.reasonLimit)
        },
        required = needsReason,
        showError = showMissing && missing,
        limit = state.reasonLimit,
        enabled = !saving,
        labels = labels,
    )
    if (errorText != null) {
        MonarchBanner(message = errorText, tone = MonarchTone.Danger, announcement = MonarchBannerAnnouncement.Polite)
    }
    DecisionFooter(
        meta = meta,
        saved = if (saved) monarchString(labels.saved, R.string.monarch_decisioncard_saved) else null,
        save = monarchString(labels.save, R.string.monarch_decisioncard_save),
        enabled = dirty,
        saving = saving,
        onSave = {
            if (missing) {
                showMissing = true
            } else {
                onDecide(tokens.decision(choice, text))
            }
        },
    )
}

@Composable
private fun ReasonField(
    text: String,
    onTextChange: (String) -> Unit,
    required: Boolean,
    showError: Boolean,
    limit: Int,
    enabled: Boolean,
    labels: MonarchDecisionCardLabels,
) {
    val reasonLabel = monarchString(labels.reason, R.string.monarch_decisioncard_reason)
    val label = if (required) {
        reasonLabel
    } else {
        stringResource(
            R.string.monarch_decisioncard_reason_optional,
            reasonLabel,
            monarchString(labels.optional, R.string.monarch_decisioncard_optional),
        )
    }
    val left = limit - text.length
    val helper = if (MonarchDecisionCardTokens.showsCharactersLeft(limit, text.length)) {
        labels.charactersLeft?.invoke(left)
            ?: pluralStringResource(R.plurals.monarch_decisioncard_characters_left, left, left)
    } else {
        null
    }
    MonarchTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier.fillMaxWidth(),
        label = label,
        placeholder = monarchString(labels.reasonPlaceholder, R.string.monarch_decisioncard_reason_placeholder),
        helperText = helper,
        errorText = if (showError) {
            monarchString(labels.reasonRequired, R.string.monarch_decisioncard_reason_required)
        } else {
            null
        },
        enabled = enabled,
        singleLine = false,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionFooter(
    meta: DecisionMetaText?,
    saved: String?,
    save: String,
    enabled: Boolean,
    saving: Boolean,
    onSave: () -> Unit,
) {
    val muted = MonarchDecisionCardTokens.mutedText(MonarchTheme.colors)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MonarchDecisionCardTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchDecisionCardTokens.SmallGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            if (meta != null) DecisionMeta(meta)
            // Always present, so the saved message is announced when it appears.
            Text(
                text = saved.orEmpty(),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = muted,
            )
        }
        MonarchButton(
            text = save,
            onClick = onSave,
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            loading = saving,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionReadOnly(
    options: List<MonarchDecisionOption>,
    value: String?,
    reason: String?,
    meta: DecisionMetaText?,
    labels: MonarchDecisionCardLabels,
) {
    val colors = MonarchTheme.colors
    val savedOption = options.firstOrNull { it.value == value }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchDecisionCardTokens.Gap),
        verticalArrangement = Arrangement.spacedBy(MonarchDecisionCardTokens.SmallGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (savedOption != null) {
            MonarchBadge(text = savedOption.label, tone = savedOption.tone)
        } else {
            Text(
                text = monarchString(labels.undecided, R.string.monarch_decisioncard_undecided),
                style = MonarchTheme.typography.body,
                color = MonarchDecisionCardTokens.mutedText(colors),
            )
        }
        if (reason != null) Text(text = reason, style = MonarchTheme.typography.body, color = colors.textDefault)
    }
    if (meta != null) DecisionMeta(meta)
}

/** "Decided 3 hours ago by Ana", and the same with the absolute time for TalkBack. */
private class DecisionMetaText(val shown: String, val spoken: String)

@Composable
private fun DecisionMeta(meta: DecisionMetaText) {
    Text(
        text = meta.shown,
        modifier = Modifier.clearAndSetSemantics { contentDescription = meta.spoken },
        style = MonarchTheme.typography.caption,
        color = MonarchDecisionCardTokens.mutedText(MonarchTheme.colors),
    )
}

@Composable
private fun decisionMeta(
    decidedAt: Long?,
    decidedBy: String?,
    labels: MonarchDecisionCardLabels,
    fixedNow: Long?,
): DecisionMetaText? {
    if (decidedAt == null && decidedBy == null) return null
    val clock = minuteClock(running = decidedAt != null && fixedNow == null, time = decidedAt)
    val now = fixedNow ?: clock
    val decided = monarchString(labels.decided, R.string.monarch_decisioncard_decided)
    val by = decidedBy?.let { name ->
        labels.decidedBy?.invoke(name) ?: stringResource(R.string.monarch_decisioncard_decided_by, name)
    }
    val context = LocalContext.current
    val relative = decidedAt?.let { MonarchDecisionCardTokens.relativeTime(it, now) }
    val absolute = decidedAt?.let {
        DateUtils.formatDateTime(
            context,
            it,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME,
        )
    }
    fun line(time: String?) = listOfNotNull(time?.let { "$decided $it" }, by).joinToString(" ")
    return DecisionMetaText(shown = line(relative), spoken = line(absolute))
}

/** The current time, redrawn as often as the age of [time] needs while [running], so the relative time is true. */
@Composable
private fun minuteClock(running: Boolean, time: Long?): Long =
    rememberMonarchRelativeClock(listOf(time), running = running)
