package dev.monarchui.components.identity.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.theme.MonarchTheme

/**
 * Overlapping avatars of the people on an item, with a "+N" tile for those
 * not shown, for example the assignees of a task.
 *
 * Built by Monarch from a `Row` with negative spacing. Each avatar is ringed
 * in the surface colour so the overlap stays readable. The group is named by
 * [label] for TalkBack, then each avatar is read by its own name and the tile
 * as "2 more". The avatars are placed from the start edge.
 *
 * @param items the people or entities, in order.
 * @param label names the group, for example "Assignees".
 * @param modifier the modifier of the group.
 * @param maxVisible show at most this many avatars, then the tile. With exactly one too many, all are shown.
 * `null` shows all.
 * @param size the size of the avatars; give each `MonarchAvatar` the same.
 * @param shape the shape of the avatars; give each `MonarchAvatar` the same.
 * @param overflowLabel what TalkBack reads for the tile, given the number hidden. `null` uses the built-in
 * string `monarch_avatar_more`, "2 more".
 * @param avatar the avatar of one item, usually a `MonarchAvatar` with the same [size] and [shape].
 */
@Composable
public fun <T> MonarchAvatarGroup(
    items: List<T>,
    label: String,
    modifier: Modifier = Modifier,
    maxVisible: Int? = null,
    size: MonarchAvatarSize = MonarchAvatarSize.Md,
    shape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    overflowLabel: ((Int) -> String)? = null,
    avatar: @Composable (T) -> Unit,
) {
    val tokens = MonarchAvatarTokens
    val colors = MonarchTheme.colors
    val side = tokens.side(size) * LocalDensity.current.fontScale
    val visible = MonarchAvatarLogic.visibleCount(items.size, maxVisible)
    val hidden = items.size - visible
    val ringShape = tokens.ringShape(shape, size)
    Row(
        modifier = modifier.semantics {
            contentDescription = label
            isTraversalGroup = true
        },
        horizontalArrangement = Arrangement.spacedBy(-side * tokens.overlapRatio(size)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.take(visible).forEach { item ->
            Box(modifier = Modifier.background(tokens.ringColor(colors), ringShape).padding(tokens.RingWidth)) {
                avatar(item)
            }
        }
        if (hidden > 0) {
            val spoken = overflowLabel?.invoke(hidden)
                ?: pluralStringResource(R.plurals.monarch_avatar_more, hidden, hidden)
            val tile = tokens.tileColors(colors)
            val style = tokens.initialsStyle(size, MonarchTheme.typography)
            Box(modifier = Modifier.background(tokens.ringColor(colors), ringShape).padding(tokens.RingWidth)) {
                Box(
                    modifier = Modifier
                        .size(side)
                        .clip(tokens.shape(shape, size))
                        .background(tile.container)
                        .clearAndSetSemantics {
                            contentDescription = spoken
                            role = Role.Image
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(R.string.monarch_avatar_more_short, hidden),
                        style = style.copy(color = tile.content, textAlign = TextAlign.Center),
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = style.fontSize * tokens.INITIALS_MINIMUM_SCALE,
                            maxFontSize = style.fontSize,
                        ),
                    )
                }
            }
        }
    }
}
