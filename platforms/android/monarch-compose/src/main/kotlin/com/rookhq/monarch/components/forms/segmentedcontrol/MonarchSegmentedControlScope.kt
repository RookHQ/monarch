package com.rookhq.monarch.components.forms.segmentedcontrol

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier

/**
 * The segments of a [MonarchSegmentedControl]. Call [Segment] once per option, in order.
 */
@Immutable
public interface MonarchSegmentedControlScope {
    /**
     * One option, with any content as its label.
     *
     * Migrating from Material: `SegmentedButton(selected, onClick, shape = ..., enabled = e, icon = i, label = l)`
     * becomes `Segment(selected, onClick, enabled = e, icon = i, label = l)`: drop `shape`, `colors` and `border`.
     * Material's check mark is not drawn: the chosen segment is the raised white pill, as on the web, and TalkBack
     * says "selected".
     *
     * @param selected whether this segment is the chosen one. State is hoisted.
     * @param onClick called when the user chooses it. Not called when not [enabled].
     * @param modifier the modifier of the segment.
     * @param enabled whether this segment can be chosen; a disabled one is drawn in the disabled text colour.
     * @param icon a decorative 16dp icon before the label, for example a `MonarchIcon`; hidden from TalkBack.
     * @param count a number after the label in a small pill, for example the open items of a filter.
     * @param suffix a short success-coloured text after the label, for example "Billed yearly".
     * @param interactionSource the source of the press and focus interactions.
     * @param label the label, usually a `Text`; it takes the colour and style of the segment.
     */
    @Composable
    public fun Segment(
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: (@Composable () -> Unit)? = null,
        count: Int? = null,
        suffix: String? = null,
        interactionSource: MutableInteractionSource? = null,
        label: @Composable () -> Unit,
    )

    /**
     * One option with a text label. See the overload with a `label` slot.
     *
     * @param selected whether this segment is the chosen one.
     * @param onClick called when the user chooses it.
     * @param label the text.
     * @param modifier the modifier of the segment.
     * @param enabled whether this segment can be chosen.
     * @param icon a decorative 16dp icon before the label.
     * @param count a number after the label in a small pill.
     * @param suffix a short success-coloured text after the label.
     * @param interactionSource the source of the press and focus interactions.
     */
    @Composable
    public fun Segment(
        selected: Boolean,
        onClick: () -> Unit,
        label: String,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        icon: (@Composable () -> Unit)? = null,
        count: Int? = null,
        suffix: String? = null,
        interactionSource: MutableInteractionSource? = null,
    )
}
