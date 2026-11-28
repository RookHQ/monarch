package dev.monarchui.components.forms.typetoconfirminput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens
import dev.monarchui.components.forms.textfield.MonarchTextFieldLabelVisibility
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString

/**
 * The "type the name to confirm" field for an irreversible action, such as deleting a site.
 *
 * A `MonarchTextField` whose label names the phrase to type, with
 * autocorrection and capitalisation off so the keyboard does not change the
 * typed name. Matching is case-sensitive and ignores white space around the
 * text and the phrase ([MonarchTypeToConfirmInputDefaults.matches]). The
 * field shows no error while the person types; set [showMismatch] after a
 * submit attempt. When the text starts to match, TalkBack hears
 * [matchAnnouncement] through a polite live region. The field does not
 * disable any button: use [onMatchChange] or
 * [MonarchTypeToConfirmInputDefaults.matches] for that.
 *
 * @param phrase the exact text the person must type, usually the record's own name.
 * @param value the typed text.
 * @param onValueChange called with the new text on every change. Paste is allowed.
 * @param modifier the modifier of the component.
 * @param label the visible label. `null` uses the built-in "Type “phrase” to confirm"
 * (`monarch_typetoconfirminput_label`).
 * @param helperText guidance under the field, for example what is about to be deleted.
 * @param showMismatch shows the error and the error state while the text does not match.
 * @param enabled whether the person can edit the field.
 * @param onMatchChange called each time the match state flips, not for the first state.
 * @param mismatchMessage the error. `null` uses the built-in "The text does not match."
 * (`monarch_typetoconfirminput_mismatch`).
 * @param matchAnnouncement announced when the text starts to match. `null` uses the built-in
 * "Text matches" (`monarch_typetoconfirminput_matches`); an empty string is silence.
 * @param mismatchAnnouncement announced when the text stops matching. Nothing by default.
 * @param keyboardActions what happens when the person triggers the IME action.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
public fun MonarchTypeToConfirmInput(
    phrase: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    helperText: String? = null,
    showMismatch: Boolean = false,
    enabled: Boolean = true,
    onMatchChange: ((Boolean) -> Unit)? = null,
    mismatchMessage: String? = null,
    matchAnnouncement: String? = null,
    mismatchAnnouncement: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val matches = MonarchTypeToConfirmInputDefaults.matches(value, phrase)
    val shownLabel = label ?: stringResource(R.string.monarch_typetoconfirminput_label, phrase.trim())
    val error = monarchString(mismatchMessage, R.string.monarch_typetoconfirminput_mismatch)
    val onMatch = monarchString(matchAnnouncement, R.string.monarch_typetoconfirminput_matches)
    val onMismatch = mismatchAnnouncement.orEmpty()
    var announcement by remember { mutableStateOf("") }
    var previous by remember { mutableStateOf(matches) }
    val currentOnMatchChange by rememberUpdatedState(onMatchChange)
    // Announce and report only real flips, never the first state.
    LaunchedEffect(matches) {
        if (previous != matches) {
            previous = matches
            announcement = if (matches) onMatch else onMismatch
            currentOnMatchChange?.invoke(matches)
        }
    }
    // The built-in label draws the phrase in a monospace chip, as the web does; the field still carries the whole
    // sentence as its name for TalkBack. A caller's own label is shown as given.
    val chipped = label == null
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(PhraseTokens.LabelGap)) {
        if (chipped) {
            PhraseLabel(
                template = stringResource(R.string.monarch_typetoconfirminput_label, PhraseMark),
                phrase = phrase.trim(),
                spoken = shownLabel,
                enabled = enabled,
            )
        }
        MonarchTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = shownLabel,
            labelVisibility = if (chipped) {
                MonarchTextFieldLabelVisibility.Hidden
            } else {
                MonarchTextFieldLabelVisibility.Visible
            },
            helperText = helperText,
            errorText = if (MonarchTypeToConfirmInputDefaults.showsError(matches, showMismatch)) error else null,
            enabled = enabled,
            keyboardOptions = MonarchTypeToConfirmInputDefaults.KeyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
        )
        // Always present, so each change is announced once as a change of this live region.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

private const val PhraseMark = "\u0001"

private object PhraseTokens {
    val LabelGap = MonarchTokens.Space.space1_5.dp
    val ChipGap = MonarchTokens.Space.space1.dp
    val ChipPaddingX = MonarchTokens.Space.space1.dp
    val ChipPaddingY = MonarchTokens.Space.space0_5.dp
}

/** "Type [phrase] to confirm": the localised sentence around a monospace chip holding the phrase. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhraseLabel(template: String, phrase: String, spoken: String, enabled: Boolean) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val color = if (enabled) colors.textDefault else colors.textDisabled
    // The chip frames the phrase, so the quotation marks of the sentence around it are dropped.
    val before = template.substringBefore(PhraseMark).trimEnd().trimEnd('\u201C', '"').trimEnd()
    val after = template.substringAfter(PhraseMark, "").trimStart().trimStart('\u201D', '"').trimStart()
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(spoken) },
        horizontalArrangement = Arrangement.spacedBy(PhraseTokens.ChipGap),
        verticalArrangement = Arrangement.spacedBy(PhraseTokens.ChipGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (before.isNotBlank()) Text(text = before.trim(), style = typography.label, color = color)
        Text(
            text = phrase,
            modifier = Modifier
                .background(colors.bgMuted, MonarchTheme.shapes.xs)
                .padding(
                    horizontal = PhraseTokens.ChipPaddingX,
                    vertical = PhraseTokens.ChipPaddingY,
                ),
            style = typography.code.merge(TextStyle(fontWeight = FontWeight.Medium)),
            color = color,
        )
        if (after.isNotBlank()) Text(text = after.trim(), style = typography.label, color = color)
    }
}
