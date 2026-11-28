package dev.monarchui.components.collaboration.readreceipt

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * One person who has read a message, for `MonarchReadReceipt`.
 *
 * @property name the display name, read in the full "Seen by …" sentence.
 * @property id a stable id, used as the avatar colour seed. `null` uses the name.
 * @property image the avatar picture for the avatars display, loaded by the app. `null` shows initials.
 */
@Immutable
public class MonarchReadReceiptReader(
    public val name: String,
    public val id: String? = null,
    public val image: Painter? = null,
)
