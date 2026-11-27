package com.rookhq.monarch.components.identity.spaceswitcher

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One Space of the workspace that a `MonarchSpaceSwitcher` offers.
 *
 * @property id the stable id, such as the Rook ID Space id.
 * @property name the Space's name, already localised.
 * @property description secondary text in the list, such as the member count.
 * @property icon the Space's icon, decorative. `null` shows the first letter on a tinted tile.
 */
@Immutable
public class MonarchSpaceOption(
    public val id: String,
    public val name: String,
    public val description: String? = null,
    public val icon: Painter? = null,
)
