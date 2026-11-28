package com.rookhq.monarch.components.identity.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.rookhq.monarch.R
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDot
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceDotTokens
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.internal.MonarchImage
import com.rookhq.monarch.internal.MonarchPresenceStrings
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A picture of a person or an entity, with initials or an icon when there is
 * no picture, and an optional presence dot.
 *
 * Built by Monarch: Material 3 has no avatar. Monarch ships no image loader:
 * the app loads the picture with its own pipeline and passes a [Painter], or
 * `null` while it has none (loading, failed or no picture), when the avatar
 * shows the initials of [name] on a colour picked from [seed]. A name without
 * a letter or a digit shows [icon] instead, or the colour alone.
 *
 * The avatar is one TalkBack element with the image role, named by [name] and
 * by the presence when there is one ("Ana Silva, Online"). It grows with the
 * font size setting. It is not interactive.
 *
 * @param image the picture, or `null` to show the fallback.
 * @param name the person or entity. Read by TalkBack and used for the initials.
 * @param modifier the modifier of the avatar.
 * @param shape a circle for people, a rounded square for workspaces, organisations and brands.
 * @param size 24, 32 or 40dp at the default font size.
 * @param status a presence dot at the bottom end corner; its name is added to what TalkBack reads.
 * @param seed picks the fallback colour; the name by default. Pass a stable id so a renamed person keeps
 * their colour.
 * @param decorative hides the avatar from TalkBack, when the name is written next to it.
 * @param icon shown instead of the initials when there is no picture, for an entity without a logo or a
 * name without letters. Monarch bundles no icons; it takes the content colour of the fallback.
 * @param statusLabel the spoken name of the presence. `null` uses the built-in strings of
 * `MonarchPresenceDot`, for example "Online".
 */
@Composable
public fun MonarchAvatar(
    image: Painter?,
    name: String,
    modifier: Modifier = Modifier,
    shape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    size: MonarchAvatarSize = MonarchAvatarSize.Md,
    status: MonarchPresenceStatus? = null,
    seed: String? = null,
    decorative: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    statusLabel: String? = null,
) {
    AvatarFrame(name, modifier, size, status, decorative, statusLabel) {
        MonarchImage(
            painter = image,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            shape = MonarchAvatarTokens.shape(shape, size),
        ) {
            AvatarFallback(name = name, seed = seed, size = size, icon = icon)
        }
    }
}

/**
 * A person or entity avatar whose picture the app composes itself, for
 * example with its image loader's composable. The picture fills the avatar
 * and is clipped to its shape. See the other overload for the behavior.
 *
 * @param image the picture, filling the avatar. Show the app's own placeholder while it loads.
 * @param name the person or entity. Read by TalkBack.
 * @param modifier the modifier of the avatar.
 * @param shape a circle for people, a rounded square for workspaces, organisations and brands.
 * @param size 24, 32 or 40dp at the default font size.
 * @param status a presence dot at the bottom end corner; its name is added to what TalkBack reads.
 * @param decorative hides the avatar from TalkBack, when the name is written next to it.
 * @param statusLabel the spoken name of the presence. `null` uses the built-in strings of `MonarchPresenceDot`.
 */
@Composable
public fun MonarchAvatar(
    image: @Composable () -> Unit,
    name: String,
    modifier: Modifier = Modifier,
    shape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    size: MonarchAvatarSize = MonarchAvatarSize.Md,
    status: MonarchPresenceStatus? = null,
    decorative: Boolean = false,
    statusLabel: String? = null,
) {
    AvatarFrame(name, modifier, size, status, decorative, statusLabel) {
        Box(
            modifier = Modifier.matchParentSize().clip(MonarchAvatarTokens.shape(shape, size)),
            contentAlignment = Alignment.Center,
        ) { image() }
    }
}

/** The sized box, the semantics and the presence dot shared by both overloads. */
@Composable
private fun AvatarFrame(
    name: String,
    modifier: Modifier,
    size: MonarchAvatarSize,
    status: MonarchPresenceStatus?,
    decorative: Boolean,
    statusLabel: String?,
    picture: @Composable BoxScope.() -> Unit,
) {
    val side = MonarchAvatarTokens.side(size) * LocalDensity.current.fontScale
    val dot = MonarchAvatarTokens.dotSize(size)
    val custom = statusLabel?.takeIf { it.isNotBlank() }
    val presence = status?.let { monarchString(custom, MonarchPresenceStrings.resource(it)) }
    val spoken = if (presence == null) name else stringResource(R.string.monarch_avatar_with_status, name, presence)
    Box(
        modifier = modifier
            .size(side)
            .clearAndSetSemantics {
                if (!decorative) {
                    contentDescription = spoken
                    role = Role.Image
                }
            },
    ) {
        picture()
        if (status != null) {
            MonarchPresenceDot(
                status = status,
                modifier = Modifier.align(Alignment.BottomEnd).offset {
                    val small = size == MonarchAvatarSize.Xs || size == MonarchAvatarSize.Sm
                    val share = if (small) MonarchAvatarTokens.SMALL_DOT_OUTSET else 0f
                    val outset = MonarchPresenceDotTokens.diameter(dot) * share
                    IntOffset(outset.roundToPx(), outset.roundToPx())
                },
                size = dot,
                overlay = true,
                decorative = true,
                statusLabel = presence,
            )
        }
    }
}

/** Initials, the icon, or the colour alone, on the colour of the seed. */
@Composable
private fun BoxScope.AvatarFallback(
    name: String,
    seed: String?,
    size: MonarchAvatarSize,
    icon: (@Composable () -> Unit)?,
) {
    val colors = MonarchAvatarTokens.hueColors(MonarchAvatarLogic.hue(seed ?: name), MonarchTheme.isDark)
    val initials = MonarchAvatarLogic.initials(name)
    Box(modifier = Modifier.matchParentSize().background(colors.container), contentAlignment = Alignment.Center) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides colors.content) { icon() }
        } else if (MonarchAvatarLogic.hasReadableInitials(initials)) {
            val style = MonarchAvatarTokens.initialsStyle(size, MonarchTheme.typography)
            BasicText(
                text = initials,
                style = style.copy(color = colors.content, textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = style.fontSize * MonarchAvatarTokens.INITIALS_MINIMUM_SCALE,
                    maxFontSize = style.fontSize,
                ),
            )
        }
    }
}
