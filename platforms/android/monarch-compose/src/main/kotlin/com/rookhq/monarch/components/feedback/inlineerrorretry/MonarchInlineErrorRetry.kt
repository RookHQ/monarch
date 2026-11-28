package com.rookhq.monarch.components.feedback.inlineerrorretry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.internal.monarchToneName
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A compact row that says a part of the screen did not load, with a retry
 * button.
 *
 * Built by Monarch from a `FlowRow` and a `MonarchButton`: the button moves
 * under the message when both do not fit on one line, so nothing is cut off at
 * large font sizes. The busy state is controlled by the app with [retrying].
 *
 * The tone name and the message are one TalkBack element and a polite live
 * region, so a new message is announced. While [retrying] the button keeps
 * its place in the focus order, ignores taps and reports [retryingLabel] as
 * its state.
 *
 * @param message the failure in plain words, for example "Your earnings did not load."
 * @param onRetry called when the user activates the retry button. Not called while [retrying].
 * @param modifier the modifier of the row.
 * @param tone `Danger` when nothing could load; `Warning` when stale data is still shown.
 * @param variant a compact row, or a tinted strip.
 * @param retrying shows the button as busy while a retry runs.
 * @param icon an icon before the message in the tone colour, for example a warning `MonarchIcon` of size
 * `Sm` with a `null` content description. Monarch bundles no icons.
 * @param toneLabel the tone name TalkBack reads first. `null` uses the shared Monarch tone names
 * (`monarch_internal_tone_*`; the older `monarch_inlineerrorretry_tone_*` keys are deprecated aliases).
 * The neutral tone has no spoken tone.
 * @param retryLabel the label of the button. `null` uses `monarch_inlineerrorretry_retry`.
 * @param retryingLabel the state of the button while [retrying]. `null` uses
 * `monarch_inlineerrorretry_retrying`.
 */
@Composable
public fun MonarchInlineErrorRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Danger,
    variant: MonarchInlineErrorRetryVariant = MonarchInlineErrorRetryVariant.Inline,
    retrying: Boolean = false,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Warning,
    toneLabel: String? = null,
    retryLabel: String? = null,
    retryingLabel: String? = null,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchInlineErrorRetryTokens.colors(tone, variant, themeColors)
    val spokenTone = retryToneName(tone, toneLabel)
    FlowRow(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Container)
            .padding(MonarchInlineErrorRetryTokens.padding(variant)),
        horizontalArrangement = Arrangement.spacedBy(MonarchInlineErrorRetryTokens.ColumnGap),
        verticalArrangement = Arrangement.spacedBy(MonarchInlineErrorRetryTokens.RowGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchInlineErrorRetryTokens.IconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides colors.content) {
                    Box(Modifier.clearAndSetSemantics { }) { icon() }
                }
            }
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (spokenTone != null) Box(Modifier.semantics { text = AnnotatedString(spokenTone) })
                Text(text = message, style = MonarchTheme.typography.label, color = themeColors.textDefault)
            }
        }
        MonarchButton(
            text = monarchString(retryLabel, R.string.monarch_inlineerrorretry_retry),
            onClick = onRetry,
            size = MonarchButtonSize.Sm,
            loading = retrying,
            leadingIcon = MonarchDefaultIcons.Retry,
            loadingLabel = monarchString(retryingLabel, R.string.monarch_inlineerrorretry_retrying),
        )
    }
}

/**
 * The tone name TalkBack reads first, or `null` for the neutral tone. The default comes from the shared tone
 * names; the deprecated `monarch_inlineerrorretry_tone_*` keys still work as app overrides (removed no earlier than
 * 2027-02-13, 90-day window).
 */
@Composable
private fun retryToneName(tone: MonarchTone, override: String?): String? = monarchToneName(tone, override) {
    when (it) {
        MonarchTone.Info -> R.string.monarch_inlineerrorretry_tone_info
        MonarchTone.Success -> R.string.monarch_inlineerrorretry_tone_success
        MonarchTone.Warning -> R.string.monarch_inlineerrorretry_tone_warning
        MonarchTone.Danger -> R.string.monarch_inlineerrorretry_tone_danger
        MonarchTone.Neutral, MonarchTone.Accent, MonarchTone.Ai -> null
    }
}
