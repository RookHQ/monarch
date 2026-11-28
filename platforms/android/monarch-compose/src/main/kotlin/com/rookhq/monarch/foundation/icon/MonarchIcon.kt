package com.rookhq.monarch.foundation.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * An icon from an [ImageVector], at a Monarch size and in the content colour.
 *
 * Monarch does not bundle an icon set. Use Material Symbols exported as
 * vector drawables, or the app's own vectors. An image vector mirrors itself
 * in right-to-left layouts when it was created with `autoMirror = true`.
 *
 * @param imageVector the icon.
 * @param contentDescription what the icon means, for TalkBack. Pass `null` when the icon is decoration
 * next to text that says the same.
 * @param modifier the modifier of the icon.
 * @param size the width and height of the icon.
 * @param tint the colour of the icon. The default is the content colour of the surrounding surface.
 */
@Composable
public fun MonarchIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: MonarchIconSize = MonarchIconSize.Md,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp),
        tint = tint,
    )
}

/**
 * An icon from a [Painter], at a Monarch size and in the content colour.
 *
 * Use it with `painterResource(R.drawable.…)` for a vector drawable.
 *
 * @param painter the icon.
 * @param contentDescription what the icon means, for TalkBack. Pass `null` when the icon is decoration
 * next to text that says the same.
 * @param modifier the modifier of the icon.
 * @param size the width and height of the icon.
 * @param tint the colour of the icon. The default is the content colour of the surrounding surface.
 * @param mirrorInRtl flips the icon horizontally in right-to-left layouts. Set it for icons that show a
 * direction, such as a back arrow or a chevron. A vector drawable with `android:autoMirrored="true"`
 * already mirrors itself and does not need it.
 */
@Composable
public fun MonarchIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: MonarchIconSize = MonarchIconSize.Md,
    tint: Color = LocalContentColor.current,
    mirrorInRtl: Boolean = false,
) {
    val mirrored = mirrorInRtl && LocalLayoutDirection.current == LayoutDirection.Rtl
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size.dp)
            .then(if (mirrored) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier),
        tint = tint,
    )
}
