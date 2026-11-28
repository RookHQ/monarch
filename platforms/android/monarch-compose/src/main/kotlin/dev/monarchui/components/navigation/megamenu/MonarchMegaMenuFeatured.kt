package dev.monarchui.components.navigation.megamenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * The featured card at the end of an expanded [MonarchMegaMenuItem].
 *
 * @property title the title of the card.
 * @property link an app-defined destination, passed to `onNavigate` when the user taps the card.
 * @property description a muted line under the title.
 * @property badge a short accent badge, such as "Updated".
 * @property actionLabel the call to action at the bottom of the card. `null` uses "Explore".
 * @property external marks a destination outside the app.
 * @property icon an icon in a tile at the top of the card.
 */
@Immutable
public class MonarchMegaMenuFeatured(
    public val title: String,
    public val link: String,
    public val description: String? = null,
    public val badge: String? = null,
    public val actionLabel: String? = null,
    public val external: Boolean = false,
    public val icon: (@Composable () -> Unit)? = null,
)
