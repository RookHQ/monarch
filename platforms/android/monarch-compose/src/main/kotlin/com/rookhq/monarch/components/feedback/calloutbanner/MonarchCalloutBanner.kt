package com.rookhq.monarch.components.feedback.calloutbanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchTouchTargetOverhang
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.banner.MonarchBannerTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchRemoveButton
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchToneName
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A banner for a screen or a section: a trial, a maintenance window, a billing
 * problem, a failed sync, or a notice that the view is narrowed.
 *
 * Built by Monarch: Material 3 has no inline banner. It shares the colours of
 * `MonarchBanner` and adds a page placement, a compact scope form and a
 * dismiss button. The banner does not hide itself: remove it in [onDismiss].
 *
 * TalkBack reads the tone name, the title and the message as one element, so
 * the tone does not depend on colour. The action and the dismiss button are
 * separate elements.
 *
 * @param message the message. One or two sentences.
 * @param modifier the modifier of the banner.
 * @param tone the colour and the spoken tone name.
 * @param variant a callout, or a compact scope notice.
 * @param placement a rounded callout in the content, or a full-width strip at the top of a screen.
 * @param title a bold first line.
 * @param icon an icon at the start, in the tone colour, for example a `MonarchIcon` with a `null` content
 * description. Monarch bundles no icons, so there is no default.
 * @param action an action, for example a small `MonarchButton`: under the text in the callout, at the end in
 * the scope notice.
 * @param onDismiss adds a dismiss button at the end. The app removes the banner here and stores any "do not
 * show again" choice.
 * @param dismissIcon the close mark of the dismiss button. Without it the button shows its label as text.
 * @param announcement whether TalkBack announces the banner on its own. Leave `None` for a banner that is
 * on the screen from the start.
 * @param toneLabel the tone name TalkBack reads first. `null` uses the shared Monarch tone names
 * (`monarch_internal_tone_*`; the older `monarch_calloutbanner_tone_*` keys are deprecated aliases).
 * The neutral tone and the scope variant have no spoken tone.
 * @param dismissLabel the label of the dismiss button. `null` uses `monarch_calloutbanner_dismiss`.
 */
@Composable
public fun MonarchCalloutBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Info,
    variant: MonarchCalloutBannerVariant = MonarchCalloutBannerVariant.Callout,
    placement: MonarchCalloutBannerPlacement = MonarchCalloutBannerPlacement.Section,
    title: String? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.tone(tone),
    action: (@Composable () -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    announcement: MonarchBannerAnnouncement = MonarchBannerAnnouncement.None,
    toneLabel: String? = null,
    dismissLabel: String? = null,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchCalloutBannerTokens.colors(tone, themeColors)
    val scope = variant == MonarchCalloutBannerVariant.Scope
    val spokenTone = if (scope) null else calloutToneName(tone, toneLabel)
    val dismissText = monarchString(dismissLabel, R.string.monarch_calloutbanner_dismiss)
    // An announcement strip (a page strip with an action and neither icon nor title) is one centred line, as on
    // the web: the message, the action beside it and the dismiss button at the end.
    if (action != null && isAnnouncementStrip(placement, scope, title, icon)) {
        AnnouncementStrip(message, colors, variant, announcement, action, onDismiss, dismissIcon, dismissText, modifier)
        return
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calloutSurface(colors, variant, placement)
            .padding(MonarchCalloutBannerTokens.padding(variant, placement)),
        horizontalArrangement = Arrangement.spacedBy(MonarchCalloutBannerTokens.contentGap(variant)),
        verticalAlignment = if (scope) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchCalloutBannerTokens.ActionGap),
        ) {
            CalloutText(message, title, spokenTone, scope, announcement)
            // The 48dp targets of the small buttons overhang into the padding, so the callout is as deep under
            // its action as above its title.
            if (action != null && !scope) Row(Modifier.monarchTouchTargetOverhang()) { action() }
        }
        if (action != null && scope) Box(Modifier.monarchTouchTargetOverhang()) { action() }
        if (onDismiss != null) {
            // A separate node, so the button keeps its 48dp target in the layout while it overhangs the padding.
            Box(Modifier.monarchTouchTargetOverhang()) {
                if (dismissIcon != null) {
                    MonarchIconButton(label = dismissText, onClick = onDismiss, size = MonarchButtonSize.Sm) {
                        CompositionLocalProvider(
                            LocalContentColor provides themeColors.textMuted,
                            content = dismissIcon,
                        )
                    }
                } else {
                    MonarchRemoveButton(
                        name = dismissText,
                        text = dismissText,
                        onClick = onDismiss,
                        enabled = true,
                        icon = null,
                    )
                }
            }
        }
    }
}

/** A page strip with an action and neither an icon nor a title: an announcement, one centred line. */
private fun isAnnouncementStrip(
    placement: MonarchCalloutBannerPlacement,
    scope: Boolean,
    title: String?,
    icon: (@Composable () -> Unit)?,
): Boolean = placement == MonarchCalloutBannerPlacement.Page && !scope && title == null && icon == null

/** The one-line announcement strip: message, action and dismiss button, centred, on the tone colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnnouncementStrip(
    message: String,
    colors: MonarchSurfaceColors,
    variant: MonarchCalloutBannerVariant,
    announcement: MonarchBannerAnnouncement,
    action: @Composable () -> Unit,
    onDismiss: (() -> Unit)?,
    dismissIcon: (@Composable () -> Unit)?,
    dismissText: String,
    modifier: Modifier,
) {
    val placement = MonarchCalloutBannerPlacement.Page
    Row(
        modifier = modifier
            .fillMaxWidth()
            .calloutSurface(colors, variant, placement)
            .padding(MonarchCalloutBannerTokens.padding(variant, placement)),
        horizontalArrangement = Arrangement.spacedBy(MonarchCalloutBannerTokens.contentGap(variant)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // One centred line where it fits (the web); the action drops under the message on a narrow window.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(
                MonarchCalloutBannerTokens.contentGap(variant),
                Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    when (announcement) {
                        MonarchBannerAnnouncement.None -> Unit
                        MonarchBannerAnnouncement.Polite -> liveRegion = LiveRegionMode.Polite
                        MonarchBannerAnnouncement.Assertive -> liveRegion = LiveRegionMode.Assertive
                    }
                },
            ) {
                Text(
                    text = message,
                    style = MonarchBannerTokens.messageStyle(MonarchTheme.typography),
                    color = MonarchTheme.colors.textDefault,
                )
            }
            Box(Modifier.monarchTouchTargetOverhang()) { action() }
        }
        if (onDismiss != null) {
            Box(Modifier.monarchTouchTargetOverhang()) {
                if (dismissIcon != null) {
                    MonarchIconButton(label = dismissText, onClick = onDismiss, size = MonarchButtonSize.Sm) {
                        CompositionLocalProvider(
                            LocalContentColor provides MonarchTheme.colors.textMuted,
                            content = dismissIcon,
                        )
                    }
                } else {
                    MonarchRemoveButton(
                        name = dismissText,
                        text = dismissText,
                        onClick = onDismiss,
                        enabled = true,
                        icon = null,
                    )
                }
            }
        }
    }
}

/** The tone name, title and message as one TalkBack element. */
@Composable
private fun CalloutText(
    message: String,
    title: String?,
    spokenTone: String?,
    scope: Boolean,
    announcement: MonarchBannerAnnouncement,
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            when (announcement) {
                MonarchBannerAnnouncement.None -> Unit
                MonarchBannerAnnouncement.Polite -> liveRegion = LiveRegionMode.Polite
                MonarchBannerAnnouncement.Assertive -> liveRegion = LiveRegionMode.Assertive
            }
        },
        verticalArrangement = Arrangement.spacedBy(MonarchCalloutBannerTokens.TitleGap),
    ) {
        if (spokenTone != null) Box(Modifier.semantics { text = AnnotatedString(spokenTone) })
        if (title != null) {
            Text(
                text = title,
                style = MonarchBannerTokens.titleStyle(MonarchTheme.typography),
                color = colors.textDefault,
            )
        }
        Text(
            text = message,
            style = if (scope) {
                MonarchTheme.typography.caption
            } else {
                MonarchBannerTokens.messageStyle(MonarchTheme.typography)
            },
            color = if (title != null || scope) colors.textMuted else colors.textDefault,
        )
    }
}

/**
 * The tone name TalkBack reads first, or `null` for the neutral tone. The default comes from the shared tone
 * names; the deprecated `monarch_calloutbanner_tone_*` keys still work as app overrides (removed no earlier than
 * 2027-02-13, 90-day window).
 */
@Composable
private fun calloutToneName(tone: MonarchTone, override: String?): String? = monarchToneName(tone, override) {
    when (it) {
        MonarchTone.Info -> R.string.monarch_calloutbanner_tone_info
        MonarchTone.Success -> R.string.monarch_calloutbanner_tone_success
        MonarchTone.Warning -> R.string.monarch_calloutbanner_tone_warning
        MonarchTone.Danger -> R.string.monarch_calloutbanner_tone_danger
        MonarchTone.Neutral, MonarchTone.Accent, MonarchTone.Ai -> null
    }
}

/** The fill and border: all round for a section, only at the bottom for a page strip. */
private fun Modifier.calloutSurface(
    colors: MonarchSurfaceColors,
    variant: MonarchCalloutBannerVariant,
    placement: MonarchCalloutBannerPlacement,
): Modifier {
    val shape = MonarchCalloutBannerTokens.shape(variant, placement)
    val border = colors.border ?: return background(colors.container, shape)
    return if (placement == MonarchCalloutBannerPlacement.Page) {
        background(colors.container).drawBehind {
            val stroke = MonarchShapeDefaults.BorderWidth.toPx()
            val y = size.height - stroke / 2
            drawLine(color = border, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke)
        }
    } else {
        background(colors.container, shape).border(MonarchShapeDefaults.BorderWidth, border, shape)
    }
}
