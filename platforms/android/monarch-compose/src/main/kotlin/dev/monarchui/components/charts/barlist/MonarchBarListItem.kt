package dev.monarchui.components.charts.barlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One row of a `MonarchBarList`.
 *
 * @property label what the row is, such as a page path or a referrer. Already localised.
 * @property value the measured value. `null`, or a value that is not finite, shows a dash read as "No value"
 * and no bar.
 * @property id a stable key. Defaults to the label, so give one when two rows have the same label.
 * @property description a second, muted line under the label.
 * @property displayValue shown instead of the written value, such as "12% · 4 of 33".
 * @property highlighted emphasises the row with a stronger bar and heavier text. Say why in the label too:
 * the emphasis is not spoken.
 * @property icon a small decorative visual before the label, such as a flag. Hidden from TalkBack.
 */
@Immutable
public class MonarchBarListItem(
    public val label: String,
    public val value: Double?,
    public val id: String = label,
    public val description: String? = null,
    public val displayValue: String? = null,
    public val highlighted: Boolean = false,
    public val icon: (@Composable () -> Unit)? = null,
)
