package dev.monarchui.components.collaboration.callstage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.calltile.MonarchCallTileIcons

/**
 * Icons for a `MonarchCallStage`. Monarch bundles no icons: a header button without an icon writes its word
 * instead, so nothing is lost. Give each icon a `null` content description: the buttons are named by Monarch.
 *
 * @property tile the icons of every `MonarchCallTile`.
 * @property pictureInPicture the picture in picture button.
 * @property shrink the button that docks the stage.
 * @property expand the button that brings the docked stage back.
 * @property minimize the button that minimizes the docked stage.
 */
@Immutable
public class MonarchCallStageIcons(
    public val tile: MonarchCallTileIcons = MonarchCallTileIcons(),
    public val pictureInPicture: (@Composable () -> Unit)? = null,
    public val shrink: (@Composable () -> Unit)? = null,
    public val expand: (@Composable () -> Unit)? = null,
    public val minimize: (@Composable () -> Unit)? = null,
)
