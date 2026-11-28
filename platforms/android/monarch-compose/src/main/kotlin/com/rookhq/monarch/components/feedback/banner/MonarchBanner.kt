package com.rookhq.monarch.components.feedback.banner

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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.monarchTouchTargetOverhang
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/**
 * An inline message about the state of a screen or a section.
 *
 * Built by Monarch: Material 3 has no inline status component (a `Snackbar`
 * is transient and floats). The banner stays in the layout until the app
 * removes it.
 *
 * TalkBack reads the title and the message as one element. Set [announcement]
 * when the banner appears in response to something the user did, so that it
 * is announced without the user having to find it.
 *
 * @param message the message. One or two sentences; `null` for a banner that is only a [title], as the web's
 * `<Alert title="Saved" />`.
 * @param modifier the modifier of the banner.
 * @param tone the status the banner conveys. The tone is never the only carrier of meaning: say it in
 * the text.
 * @param title a short summary above the message.
 * @param icon the icon at the start, in the tone colour. By default the tone's icon, as on the web and iOS: an
 * info circle for neutral and info, a check circle for success, a warning triangle for warning and danger. Pass
 * another `MonarchIcon` with a `null` content description to replace it, or `null` for none.
 * @param action an action at the end, for example a `MonarchButton` of size `Sm` and variant `Ghost`.
 * It stays a separate element for TalkBack.
 * @param announcement whether and how TalkBack announces the banner on its own.
 */
@Composable
public fun MonarchBanner(
    message: String?,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Info,
    title: String? = null,
    icon: (@Composable () -> Unit)? = { MonarchBannerToneIcon(tone) },
    action: (@Composable () -> Unit)? = null,
    announcement: MonarchBannerAnnouncement = MonarchBannerAnnouncement.None,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchBannerTokens.colors(tone, themeColors)
    Row(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Container)
            .padding(
                horizontal = MonarchBannerTokens.HorizontalPadding,
                vertical = MonarchBannerTokens.VerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchBannerTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            // As the web alert: the icon sits at the top, on the first line of text.
            Box(Modifier.align(Alignment.Top).padding(top = MonarchBannerTokens.IconTopOffset)) {
                CompositionLocalProvider(LocalContentColor provides colors.content, content = icon)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) {
                    when (announcement) {
                        MonarchBannerAnnouncement.None -> Unit
                        MonarchBannerAnnouncement.Polite -> liveRegion = LiveRegionMode.Polite
                        MonarchBannerAnnouncement.Assertive -> liveRegion = LiveRegionMode.Assertive
                    }
                },
            verticalArrangement = Arrangement.spacedBy(MonarchBannerTokens.TitleGap),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MonarchBannerTokens.titleStyle(MonarchTheme.typography),
                    color = themeColors.textDefault,
                )
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MonarchBannerTokens.messageStyle(MonarchTheme.typography),
                    color = if (title != null) themeColors.textMuted else themeColors.textDefault,
                )
            }
        }
        if (action != null) {
            // The action's 48dp target overhangs into the padding, so a banner with an action is not taller
            // than one without.
            Box(Modifier.monarchTouchTargetOverhang()) {
                CompositionLocalProvider(LocalContentColor provides themeColors.textDefault, content = action)
            }
        }
    }
}

/** The tone's icon: the glyph the web alert shows by default for the tone (platforms/icons.json). */
@Composable
private fun MonarchBannerToneIcon(tone: MonarchTone) {
    MonarchIcon(MonarchBannerTokens.glyph(tone), contentDescription = null, size = MonarchIconSize.Sm)
}
