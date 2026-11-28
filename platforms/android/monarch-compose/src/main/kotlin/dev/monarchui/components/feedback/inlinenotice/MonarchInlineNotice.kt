package dev.monarchui.components.feedback.inlinenotice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchRemoveButton
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.internal.monarchToneName
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * A one-line message with a tone, attached to the thing it is about: a field,
 * a row, a card or a panel.
 *
 * Built by Monarch: Material 3 has no inline notice. Smaller than
 * `MonarchBanner`: an icon, an optional bold lead-in, one sentence and at most
 * one action. The notice does not hide itself: remove it in [onDismiss].
 *
 * TalkBack reads the tone name, the title and the message as one element.
 *
 * @param message the message. One sentence.
 * @param modifier the modifier of the notice.
 * @param tone the colour and the spoken tone name.
 * @param variant a tinted strip, or plain coloured text.
 * @param title a short bold line above the message, for example "Conflict."
 * @param icon an icon at the start in the tone colour, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description. Monarch bundles no icons.
 * @param action one small action at the end, for example a ghost `MonarchButton` of size `Sm`.
 * @param onDismiss adds a dismiss button at the end. The app removes the notice here.
 * @param dismissIcon the close mark of the dismiss button. Without it the button shows its label as text.
 * @param announcement whether TalkBack announces the notice on its own. Leave `None` for a notice that is
 * on the screen from the start.
 * @param toneLabel the tone name TalkBack reads first. `null` uses the shared Monarch tone names
 * (`monarch_internal_tone_*`; the older `monarch_inlinenotice_tone_*` keys are deprecated aliases).
 * The neutral tone has no spoken tone.
 * @param dismissLabel the label of the dismiss button. `null` uses `monarch_inlinenotice_dismiss`.
 */
@Composable
public fun MonarchInlineNotice(
    message: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Info,
    variant: MonarchInlineNoticeVariant = MonarchInlineNoticeVariant.Subtle,
    title: String? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.notice(tone),
    action: (@Composable () -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    announcement: MonarchBannerAnnouncement = MonarchBannerAnnouncement.None,
    toneLabel: String? = null,
    dismissLabel: String? = null,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchInlineNoticeTokens.colors(tone, variant, themeColors)
    val spokenTone = noticeToneName(tone, toneLabel)
    val dismissText = monarchString(dismissLabel, R.string.monarch_inlinenotice_dismiss)
    Row(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Control)
            .padding(MonarchInlineNoticeTokens.padding(variant)),
        horizontalArrangement = Arrangement.spacedBy(MonarchInlineNoticeTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    when (announcement) {
                        MonarchBannerAnnouncement.None -> Unit
                        MonarchBannerAnnouncement.Polite -> liveRegion = LiveRegionMode.Polite
                        MonarchBannerAnnouncement.Assertive -> liveRegion = LiveRegionMode.Assertive
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (spokenTone != null) Box(Modifier.semantics { this.text = AnnotatedString(spokenTone) })
            val textColor = MonarchInlineNoticeTokens.textColor(tone, variant, themeColors)
            // As the web alert: the title is a line of its own above the message.
            Column {
                if (title != null) {
                    Text(
                        text = title,
                        style = MonarchTheme.typography.label.copy(
                            fontWeight = FontWeight(MonarchTokens.FontWeight.semibold),
                        ),
                        color = textColor,
                    )
                }
                Text(text = message, style = MonarchTheme.typography.label, color = textColor)
            }
        }
        action?.invoke()
        if (onDismiss != null) {
            MonarchRemoveButton(
                name = dismissText,
                text = dismissText,
                onClick = onDismiss,
                enabled = true,
                icon = dismissIcon,
            )
        }
    }
}

/**
 * The tone name TalkBack reads first, or `null` for the neutral tone. The default comes from the shared tone
 * names; the deprecated `monarch_inlinenotice_tone_*` keys still work as app overrides (removed no earlier than
 * 2027-02-13, 90-day window).
 */
@Composable
private fun noticeToneName(tone: MonarchTone, override: String?): String? = monarchToneName(tone, override) {
    when (it) {
        MonarchTone.Info -> R.string.monarch_inlinenotice_tone_info
        MonarchTone.Success -> R.string.monarch_inlinenotice_tone_success
        MonarchTone.Warning -> R.string.monarch_inlinenotice_tone_warning
        MonarchTone.Danger -> R.string.monarch_inlinenotice_tone_danger
        MonarchTone.Neutral, MonarchTone.Accent, MonarchTone.Ai -> null
    }
}
