package dev.monarchui.components.collaboration.calltile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonTokens
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * One participant in a call: their video or avatar, name, muted microphone,
 * raised hand, status, connection quality and floating reactions, with Pin
 * and full-screen buttons.
 *
 * Built by Monarch from a `Box` with overlays: Material 3 has no call tile.
 * The video is a slot for the renderer of the app's call SDK (a
 * `SurfaceView` or `TextureView` in an `AndroidView`, or the SDK's own
 * composable); without it, or with [showVideo] `false`, the avatar is shown.
 * Full screen belongs to the app, which runs it through [onFullScreen]. The
 * tile is at least 16 by 9 and grows taller when its text needs room.
 *
 * TalkBack reads the name bar first, as "Ana (you)" with what is true now as
 * its state ("Speaking, Muted, Hand raised, Poor connection"), then the
 * buttons. A new reaction is announced through a polite live region.
 *
 * @param name the participant's display name. Names the tile and its buttons.
 * @param modifier the modifier of the tile. Give it a width; the height follows.
 * @param statusTone the colour of [status].
 * @param size the filmstrip, grid or main-spot size. The filmstrip size has no buttons.
 * @param self this is the person using the app: adds "(you)". Mirroring the camera is the renderer's job.
 * @param micOff the microphone is off.
 * @param speaking the participant is speaking: a thicker accent border, and "Speaking" is read.
 * @param handRaised the participant raised a hand: a badge at the top start corner.
 * @param screen the video is a shared screen.
 * @param pinned the tile is pinned to the main spot. Switches the Pin button to Unpin and marks it selected.
 * @param showVideo shows [video]. Pass `false` while connecting or when the camera is off, so a frozen
 * frame never shows.
 * @param status a short status in the name bar, "Reconnecting…" or "Presenting".
 * @param quality the connection quality bars; `null` when unknown.
 * @param reaction a reaction that rises over the tile for three seconds from when it was sent.
 * @param now the current time in epoch milliseconds, for tests. `null` uses the clock.
 * @param image the participant's picture for the avatar, loaded by the app. `null` shows initials.
 * @param onPinnedChange shows the Pin button, called with the next pinned state.
 * @param onFullScreen shows the full-screen button.
 * @param icons icons for the muted, screen, hand, Pin and full-screen marks. Without one the word is written.
 * @param labels replacements for the built-in strings.
 * @param video the participant's video, from the app's call SDK. It fills the tile.
 */
@Composable
public fun MonarchCallTile(
    name: String,
    modifier: Modifier = Modifier,
    statusTone: MonarchTone = MonarchTone.Neutral,
    size: MonarchCallTileSize = MonarchCallTileSize.Md,
    self: Boolean = false,
    micOff: Boolean = false,
    speaking: Boolean = false,
    handRaised: Boolean = false,
    screen: Boolean = false,
    pinned: Boolean = false,
    showVideo: Boolean = true,
    status: String? = null,
    quality: MonarchCallQuality? = null,
    reaction: MonarchCallReaction? = null,
    now: Long? = null,
    image: Painter? = null,
    onPinnedChange: ((Boolean) -> Unit)? = null,
    onFullScreen: (() -> Unit)? = null,
    icons: MonarchCallTileIcons = MonarchCallTileIcons(),
    labels: MonarchCallTileLabels = MonarchCallTileLabels(),
    video: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchCallTileTokens
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.xl
    val tile = tokens.tileColors(colors)
    val border = if (speaking) {
        Modifier.border(BorderStroke(tokens.SpeakingBorder, tokens.speakingColor(colors)), shape)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(tile.container)
            .then(border)
            .semantics { isTraversalGroup = true },
    ) {
        Box(modifier = Modifier.matchParentSize().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            if (video != null && showVideo) {
                video()
            } else {
                MonarchAvatar(
                    image = image,
                    name = name,
                    size = MonarchCallTileLogic.avatarSize(size),
                    decorative = true,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .callTileMinimumHeight()
                .padding(tokens.Padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TopRow(name, size, handRaised, pinned, onPinnedChange, onFullScreen, icons, labels)
            // Room for the avatar between the badges and the name, so none of them cover it.
            Spacer(modifier = Modifier.height(MonarchAvatarTokens.side(MonarchCallTileLogic.avatarSize(size))))
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                Reaction(name = name, reaction = reaction, now = now, labels = labels)
                NameBar(
                    name = name,
                    statusTone = statusTone,
                    flags = CallTileFlags(self, micOff, speaking, handRaised, screen),
                    status = status,
                    quality = quality,
                    icons = icons,
                    labels = labels,
                )
            }
        }
    }
}

/** What is true of the participant now. */
private class CallTileFlags(
    val self: Boolean,
    val micOff: Boolean,
    val speaking: Boolean,
    val handRaised: Boolean,
    val screen: Boolean,
)

/** At least 16 by 9 of the width it is given; taller when the content needs it. */
private fun Modifier.callTileMinimumHeight(): Modifier = layout { measurable, constraints ->
    val wanted = if (constraints.hasBoundedWidth) {
        (constraints.maxWidth / MonarchCallTileTokens.ASPECT_RATIO).roundToInt()
    } else {
        0
    }
    val minHeight = wanted.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = minHeight))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@Composable
private fun TopRow(
    name: String,
    size: MonarchCallTileSize,
    handRaised: Boolean,
    pinned: Boolean,
    onPinnedChange: ((Boolean) -> Unit)?,
    onFullScreen: (() -> Unit)?,
    icons: MonarchCallTileIcons,
    labels: MonarchCallTileLabels,
) {
    val tokens = MonarchCallTileTokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.Top,
    ) {
        if (handRaised) HandBadge(size = size, icon = icons.handRaised, labels = labels)
        Spacer(modifier = Modifier.weight(1f))
        if (MonarchCallTileLogic.showsActions(size, onPinnedChange != null, onFullScreen != null)) {
            if (onPinnedChange != null) {
                ActionButton(
                    spoken = if (pinned) {
                        labels.unpin?.invoke(name) ?: stringResource(R.string.monarch_calltile_unpin, name)
                    } else {
                        labels.pin?.invoke(name) ?: stringResource(R.string.monarch_calltile_pin, name)
                    },
                    text = if (pinned) {
                        monarchString(labels.unpinButton, R.string.monarch_calltile_unpin_button)
                    } else {
                        monarchString(labels.pinButton, R.string.monarch_calltile_pin_button)
                    },
                    icon = if (pinned) icons.unpin ?: icons.pin else icons.pin,
                    selected = pinned,
                    onClick = { onPinnedChange(!pinned) },
                )
            }
            if (onFullScreen != null) {
                ActionButton(
                    spoken = labels.fullScreen?.invoke(name)
                        ?: stringResource(R.string.monarch_calltile_full_screen, name),
                    text = monarchString(labels.fullScreenButton, R.string.monarch_calltile_full_screen_button),
                    icon = icons.fullScreen,
                    selected = null,
                    onClick = onFullScreen,
                )
            }
        }
    }
}

/** An icon button on the inverse surface when there is an icon; a small text button otherwise. */
@Composable
private fun ActionButton(
    spoken: String,
    text: String,
    icon: (@Composable () -> Unit)?,
    selected: Boolean?,
    onClick: () -> Unit,
) {
    val described = Modifier.semantics {
        contentDescription = spoken
        if (selected != null) this.selected = selected
    }
    if (icon != null) {
        val bar = MonarchCallTileTokens.barColors(MonarchTheme.colors)
        val shape = MonarchShapeDefaults.Control
        val side = MonarchButtonTokens.iconButtonSide(MonarchButtonSize.Md)
        MonarchIconButton(
            label = spoken,
            onClick = onClick,
            // The inverse fill under the ghost button, the size of the visible button, so it reads on the video.
            modifier = Modifier
                .semantics { if (selected != null) this.selected = selected }
                .drawBehind {
                    val px = side.toPx()
                    val outline = shape.createOutline(Size(px, px), layoutDirection, this)
                    translate((size.width - px) / 2, (size.height - px) / 2) { drawOutline(outline, bar.container) }
                },
            shape = shape,
        ) { CompositionLocalProvider(LocalContentColor provides bar.content, content = icon) }
    } else {
        MonarchButton(text = text, onClick = onClick, modifier = described, size = MonarchButtonSize.Sm)
    }
}

@Composable
private fun HandBadge(size: MonarchCallTileSize, icon: (@Composable () -> Unit)?, labels: MonarchCallTileLabels) {
    val tokens = MonarchCallTileTokens
    val hand = tokens.handColors(MonarchTheme.colors)
    Row(
        modifier = Modifier
            .clip(MonarchShapeDefaults.Mark)
            .background(hand.container)
            .padding(horizontal = tokens.BarPaddingHorizontal, vertical = tokens.BarPaddingVertical)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CompositionLocalProvider(LocalContentColor provides hand.content) { icon() }
        if (icon == null || size != MonarchCallTileSize.Sm) {
            Text(
                text = monarchString(labels.handRaised, R.string.monarch_calltile_hand_raised),
                style = MonarchTheme.typography.caption,
                color = hand.content,
            )
        }
    }
}

@Composable
private fun Reaction(name: String, reaction: MonarchCallReaction?, now: Long?, labels: MonarchCallTileLabels) {
    val clock = reactionClock(now, reaction)
    val shown = reaction?.takeIf { MonarchCallTileLogic.reactionVisible(it, clock) }
    val announcement = shown?.let {
        labels.reacted?.invoke(name, it.emoji) ?: stringResource(R.string.monarch_calltile_reacted, name, it.emoji)
    }
    val motion = MonarchTheme.motion
    Box(
        modifier = Modifier.clearAndSetSemantics {
            liveRegion = LiveRegionMode.Polite
            if (announcement != null) contentDescription = announcement
        },
    ) {
        AnimatedVisibility(
            visible = shown != null,
            enter = fadeIn(motion.spec(motion.slow)) +
                slideInVertically(motion.spec(motion.slow, motion.enter)) { it / MonarchCallTileTokens.REACTION_RISE },
            exit = fadeOut(motion.spec(motion.slow)),
        ) {
            Text(text = reaction?.emoji.orEmpty(), style = MonarchTheme.typography.title)
        }
    }
}

/** The reference time for the reaction: [fixed], or the clock read when a reaction arrives and when it ends. */
@Composable
private fun reactionClock(fixed: Long?, reaction: MonarchCallReaction?): Long {
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(fixed, reaction) {
        if (fixed == null && reaction != null) {
            clock = System.currentTimeMillis()
            val remaining = MonarchCallTileLogic.reactionRemaining(reaction, clock)
            if (remaining > 0) {
                delay(remaining)
                clock = System.currentTimeMillis()
            }
        }
    }
    return fixed ?: clock
}

@Composable
private fun NameBar(
    name: String,
    statusTone: MonarchTone,
    flags: CallTileFlags,
    status: String?,
    quality: MonarchCallQuality?,
    icons: MonarchCallTileIcons,
    labels: MonarchCallTileLabels,
) {
    val tokens = MonarchCallTileTokens
    val colors = MonarchTheme.colors
    val bar = tokens.barColors(colors)
    val label = if (flags.self) {
        labels.you?.invoke(name) ?: stringResource(R.string.monarch_calltile_you, name)
    } else {
        name
    }
    // One line with an ellipsis, so the bar stays low; at a large font size the text wraps instead of being cut.
    val lines = if (LocalDensity.current.fontScale > tokens.WRAP_FONT_SCALE) Int.MAX_VALUE else 1
    val muted = monarchString(labels.muted, R.string.monarch_calltile_muted)
    val sharing = monarchString(labels.sharingScreen, R.string.monarch_calltile_sharing_screen)
    val state = MonarchCallTileLogic.spokenState(
        listOf(
            monarchString(labels.speaking, R.string.monarch_calltile_speaking).takeIf { flags.speaking },
            muted.takeIf { flags.micOff },
            monarchString(labels.handRaised, R.string.monarch_calltile_hand_raised).takeIf { flags.handRaised },
            sharing.takeIf { flags.screen },
            status,
            quality?.let { labels.quality?.invoke(it) ?: stringResource(qualityResource(it)) },
        ),
    )
    Row(
        modifier = Modifier
            .clip(MonarchShapeDefaults.Control)
            .background(bar.container)
            .padding(horizontal = tokens.BarPaddingHorizontal, vertical = tokens.BarPaddingVertical)
            .clearAndSetSemantics {
                contentDescription = label
                if (state.isNotEmpty()) stateDescription = state
                traversalIndex = -1f
            },
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (flags.micOff) Mark(text = muted, icon = icons.micOff, color = tokens.mutedColor(colors))
        if (flags.screen) Mark(text = sharing, icon = icons.screen, color = bar.content)
        Text(
            text = label,
            modifier = Modifier.weight(1f, fill = false),
            style = MonarchTheme.typography.caption,
            color = bar.content,
            maxLines = lines,
            overflow = TextOverflow.Ellipsis,
        )
        if (status != null) {
            Text(
                text = status,
                modifier = Modifier.weight(1f, fill = false),
                style = MonarchTheme.typography.caption,
                color = tokens.statusColor(statusTone, colors),
                maxLines = lines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (quality != null) QualityBars(quality)
    }
}

/** An icon in [color], or the word when there is no icon. */
@Composable
private fun Mark(text: String, icon: (@Composable () -> Unit)?, color: Color) {
    if (icon != null) {
        CompositionLocalProvider(LocalContentColor provides color) { icon() }
    } else {
        Text(text = text, style = MonarchTheme.typography.caption, color = color)
    }
}

@Composable
private fun QualityBars(quality: MonarchCallQuality) {
    val tokens = MonarchCallTileTokens
    val colors = MonarchTheme.colors
    val lit = MonarchCallTileLogic.litBars(quality)
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.QualityBarGap), verticalAlignment = Alignment.Bottom) {
        tokens.QualityBarHeights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(tokens.QualityBarWidth)
                    .height(height)
                    .clip(MonarchShapeDefaults.Mark)
                    .background(if (index < lit) tokens.qualityColor(quality, colors) else tokens.unlitColor(colors)),
            )
        }
    }
}

private fun qualityResource(quality: MonarchCallQuality): Int = when (quality) {
    MonarchCallQuality.Good -> R.string.monarch_calltile_quality_good
    MonarchCallQuality.Fair -> R.string.monarch_calltile_quality_fair
    MonarchCallQuality.Poor -> R.string.monarch_calltile_quality_poor
}
