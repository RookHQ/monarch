package dev.monarchui.components.forms.radiocardgroup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One card of a `MonarchRadioCardGroup`.
 *
 * @property value the value passed to `onSelect`. Unique within the group.
 * @property title the heading of the card, already localised. It is the card's name for TalkBack.
 * @property description supporting text under the title.
 * @property meta a short extra detail at the end of the title row, such as a price or a count.
 * @property enabled whether this card can be chosen.
 * @property icon a decorative icon at the start of the card, for example a `MonarchIcon` with a `null` content
 * description. It is hidden from TalkBack.
 */
@Immutable
public class MonarchRadioCardOption<out T>(
    public val value: T,
    public val title: String,
    public val description: String? = null,
    public val meta: String? = null,
    public val enabled: Boolean = true,
    public val icon: (@Composable () -> Unit)? = null,
)
