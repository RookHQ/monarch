package dev.monarchui.components.files.videothumbnail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * A poster for a video, with a round play mark in the middle and a badge
 * that shows its length. With [onPlay] the whole thumbnail is one button.
 *
 * Built by Monarch: Android has no poster component. The thumbnail takes the
 * poster as a [Painter] that the app loads; to make one from the video itself
 * call [captureVideoFrame]. While [image] is `null` an inverse fill with a
 * muted play glyph is shown.
 *
 * TalkBack reads one element: "Play <title>" as a button with [onPlay], or
 * the title as an image without it, with the length in words as its state.
 *
 * @param title what the video shows, already localised. Read by TalkBack, not drawn. Empty gives the
 * built-in "Video".
 * @param image the poster, or `null` while there is none.
 * @param modifier the modifier of the thumbnail. It fills the width it is given.
 * @param onPlay makes the thumbnail a button that calls this.
 * @param durationMillis the length of the video, shown as `1:05` or `1:01:05`.
 * @param aspectRatio the width divided by the height. 16 to 9 when `null` or not positive.
 * @param contentScale how the poster fills the frame: `Crop` fills it, `Fit` letterboxes it.
 * @param showPlayIcon draws the round play mark over the poster.
 * @param enabled whether [onPlay] can be activated. A disabled thumbnail mutes its play mark.
 * @param playLabel replaces the built-in spoken name of the button, "Play <title>".
 * @param interactionSource the source of the press and focus interactions of the button.
 */
@Composable
public fun MonarchVideoThumbnail(
    title: String,
    image: Painter?,
    modifier: Modifier = Modifier,
    onPlay: (() -> Unit)? = null,
    durationMillis: Long? = null,
    aspectRatio: Float? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showPlayIcon: Boolean = true,
    enabled: Boolean = true,
    playLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MonarchVideoThumbnailTokens.colors(enabled, MonarchTheme.colors)
    val locale = LocalConfiguration.current.locales[0]
    val name = MonarchVideoThumbnailTokens.name(title, stringResource(R.string.monarch_videothumbnail_fallback))
    val label = if (onPlay != null) {
        playLabel?.takeIf { it.isNotBlank() } ?: stringResource(R.string.monarch_videothumbnail_play, name)
    } else {
        name
    }
    val spokenDuration = remember(durationMillis, locale) {
        MonarchVideoThumbnailTokens.spokenDuration(durationMillis, locale)
    }
    val durationText = MonarchVideoThumbnailTokens.durationText(durationMillis, locale)
    val shape = MonarchShapeDefaults.Container
    val interaction = if (onPlay != null) {
        Modifier
            .monarchFocusRing(shape)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Button,
                onClick = onPlay,
            )
    } else {
        Modifier.clip(shape)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(MonarchVideoThumbnailTokens.aspectRatio(aspectRatio))
            .then(interaction)
            .background(colors.background)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                if (onPlay == null) role = Role.Image
                if (spokenDuration != null) stateDescription = spokenDuration
            },
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                painter = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
            if (showPlayIcon) {
                Box(
                    modifier = Modifier
                        .size(MonarchVideoThumbnailTokens.PlaySize)
                        .monarchShadow(MonarchTheme.elevation.md, CircleShape)
                        .background(colors.playDisc, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayGlyph(colors.playGlyph, MonarchVideoThumbnailTokens.PlayGlyphSize)
                }
            }
        } else {
            PlayGlyph(colors.placeholderGlyph, MonarchVideoThumbnailTokens.PlaceholderGlyphSize)
        }
        if (durationText != null) {
            Text(
                text = durationText,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MonarchVideoThumbnailTokens.BadgeInset)
                    .background(colors.badge, MonarchTheme.shapes.sm)
                    .padding(horizontal = MonarchVideoThumbnailTokens.BadgeHorizontalPadding)
                    .clearAndSetSemantics { },
                color = colors.badgeText,
                style = MonarchTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
            )
        }
    }
}

/** The play triangle. Not mirrored: media controls keep their direction in right-to-left layouts. */
@Composable
private fun PlayGlyph(color: Color, size: Dp) {
    val path = remember { PathParser().parsePathString(MonarchVideoThumbnailTokens.PLAY_PATH).toPath() }
    Canvas(modifier = Modifier.size(size)) {
        scale(this.size.minDimension / MonarchVideoThumbnailTokens.VIEWPORT, pivot = Offset.Zero) {
            drawPath(path, color)
        }
    }
}
