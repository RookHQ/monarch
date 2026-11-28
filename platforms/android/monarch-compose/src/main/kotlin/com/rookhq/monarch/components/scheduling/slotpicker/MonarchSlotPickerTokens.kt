package com.rookhq.monarch.components.scheduling.slotpicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTypography
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** The fill, the border and the text of one time. */
internal class SlotColors(val container: Color, val border: Color, val content: Color)

/** Component tokens, colours and the pure logic of MonarchSlotPicker. */
internal object MonarchSlotPickerTokens {
    val Gap: Dp = MonarchTokens.Space.space4.dp
    val SectionGap: Dp = MonarchTokens.Space.space2.dp
    val SlotGap: Dp = MonarchTokens.Space.space2.dp
    /** The times of a row; a short last row keeps the same column widths as the full ones. */
    const val SLOTS_PER_ROW: Int = 5
    val SlotMinHeight: Dp = MonarchTokens.Control.md.dp
    val SlotHorizontalPadding: Dp = MonarchTokens.Space.space2.dp
    val SlotVerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val PlaceholderWidth: Dp = MonarchTokens.Space.space24.dp
    val PlaceholderHeight: Dp = MonarchTokens.Control.xl.dp
    const val PLACEHOLDERS: Int = 6
    private const val DAY_MILLIS: Long = 86_400_000L

    /** The colours of a time: solid accent when chosen, accent tint and border on hover, muted when taken. */
    fun slotColors(selected: Boolean, enabled: Boolean, highlighted: Boolean, colors: MonarchColors): SlotColors =
        when {
            selected && enabled -> SlotColors(colors.accentSolid, colors.accentSolid, colors.accentOnSolid)
            selected -> SlotColors(colors.bgMuted, colors.borderDefault, colors.textDisabled)
            !enabled -> SlotColors(colors.bgSurface, colors.borderSubtle, colors.textDisabled)
            highlighted -> SlotColors(colors.accentSubtle, colors.accentBorder, colors.textDefault)
            else -> SlotColors(colors.bgSurface, colors.borderDefault, colors.textDefault)
        }

    /** The time: 14 medium with tabular figures, struck through when it is taken. */
    fun timeStyle(enabled: Boolean, typography: MonarchTypography): TextStyle = typography.label.copy(
        fontFeatureSettings = "tnum",
        textDecoration = if (enabled) TextDecoration.None else TextDecoration.LineThrough,
    )

    /** The day's title. */
    fun heading(colors: MonarchColors): Color = colors.textDefault

    /** The hints, the time zone notes and the seats line. */
    fun muted(colors: MonarchColors): Color = colors.textMuted

    /** The UTC midnight of the date [instant] falls on in a zone with [offsetMillis] at that instant. */
    fun dayOf(instant: Long, offsetMillis: Int): Long = Math.floorDiv(instant + offsetMillis, DAY_MILLIS) * DAY_MILLIS

    /** Whether [day] can be chosen: it is one of [available] (when given) and not disabled by the app. */
    fun dayEnabled(day: Long, available: Set<Long>?, isDayDisabled: ((Long) -> Boolean)?): Boolean =
        (available == null || day in available) && isDayDisabled?.invoke(day) != true

    /** The number of times that can be chosen. */
    fun openCount(slots: List<MonarchSlotPickerSlot>): Int = slots.count { it.enabled }

    /** The value after pressing [slot]: the slot, or nothing when it was selected and deselecting is allowed. */
    fun next(selection: Long?, slot: MonarchSlotPickerSlot, allowDeselect: Boolean): Long? =
        if (selection == slot.start && allowDeselect) null else slot.start

    /**
     * Whether the selection is a time on [day] in the zone but none of its slots, so the picker warns that it is
     * no longer open. [dayOfSelection] is that time's date in the zone.
     */
    fun offList(selection: Long?, dayOfSelection: Long?, day: Long?, slots: List<MonarchSlotPickerSlot>): Boolean =
        selection != null && day != null && dayOfSelection == day && slots.none { it.start == selection }
}
