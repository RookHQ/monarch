package dev.monarchui.foundation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/**
 * The measured sizes of Monarch's own controls, in one place, so a primitive never writes a raw `dp`.
 *
 * Web sizes come from `MonarchTokens.Control` and the component CSS. Native differs on purpose where a
 * thumb needs more room: fields are 40, 48 and 52dp (the web's 32, 36 and 40 are pointer sizes), and every
 * control keeps a 48dp hit area (`monarchMinimumTouchTarget`) around a smaller visible shape. All sizes are
 * minimums: text uses `sp`, so a control grows with the font scale instead of clipping.
 */
internal object MonarchControlMetrics {
    // Fields.
    private val FieldSmall = MonarchTokens.Space.space10.dp
    private val FieldMedium = MonarchTokens.Space.space12.dp
    private val FieldLarge = (MonarchTokens.Space.space12 + MonarchTokens.Space.space1).dp

    /** The minimum visible height of a field of [size]. */
    fun fieldMinHeight(size: MonarchFieldSize): Dp = when (size) {
        MonarchFieldSize.Small -> FieldSmall
        MonarchFieldSize.Medium -> FieldMedium
        MonarchFieldSize.Large -> FieldLarge
    }

    /** Horizontal padding inside a field: 10, 12 and 16dp. */
    fun fieldHorizontalPadding(size: MonarchFieldSize): Dp = when (size) {
        MonarchFieldSize.Small -> MonarchTokens.Space.space2_5.dp
        MonarchFieldSize.Medium -> MonarchTokens.Space.space3.dp
        MonarchFieldSize.Large -> MonarchTokens.Space.space4.dp
    }

    /** Vertical padding of a multi-line field. */
    val MultilineVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The minimum height of a multi-line field: 72dp, three lines of field text at the default scale. */
    val TextAreaMinHeight: Dp = (MonarchTokens.Control.md * 2).dp

    /** Space between a field's slots (leading icon, text, trailing icon). */
    val FieldGap: Dp = MonarchTokens.Space.space2.dp

    /** The size of an icon inside a field, button or menu row. */
    val IconSize: Dp = MonarchTokens.Space.space4.dp

    /** The width of the border of a field and of every other bordered control. */
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /**
     * The width of the focus ring in the standard themes: 2dp. Draw with `MonarchTheme.focusRingWidth`, which is
     * 3dp in the high-contrast themes; this value is for layout reservations and tests.
     */
    val FieldRingWidth: Dp = MonarchTokens.Space.space0_5.dp

    /**
     * The gap between a control's edge and its focus ring in the standard themes: 2dp, the ring width, as
     * `monarchFocusRing` draws it (3dp in high contrast). The web draws a field's ring flush; native uses the
     * offset of buttons and checks for every control, so a focused field reads as one clean ring around an
     * unchanged box (D11, PREMIUM.md rule 10).
     */
    val FocusRingOffset: Dp = MonarchTokens.Space.space0_5.dp

    // Buttons: the visible height; the 48dp hit area is reserved around it.

    /** Visible button heights: 28, 32, 36 and 40dp. */
    val ButtonExtraSmall: Dp = MonarchTokens.Control.xs.dp
    val ButtonSmall: Dp = MonarchTokens.Control.sm.dp
    val ButtonMedium: Dp = MonarchTokens.Control.md.dp
    val ButtonLarge: Dp = MonarchTokens.Control.lg.dp

    // Selection controls.

    /** The visible box of a checkbox and the ring of a radio: 16dp. */
    val CheckSize: Dp = MonarchTokens.Space.space4.dp

    /** The dot of a selected radio: 10dp. */
    val RadioDotSize: Dp = MonarchTokens.Space.space2_5.dp

    /** The switch track keeps the Material 52 by 32dp touch-friendly geometry, in Monarch colours. */
    val SwitchTrackWidth: Dp = 52.dp
    val SwitchTrackHeight: Dp = 32.dp

    /** The switch thumb: 16dp off, 24dp on and 28dp while pressed, as in Material 3. */
    val SwitchThumbOff: Dp = MonarchTokens.Space.space4.dp
    val SwitchThumbOn: Dp = MonarchTokens.Space.space6.dp
    val SwitchThumbPressed: Dp = 28.dp

    /** The close mark of a removable chip, drawn: 14dp. */
    val CloseMark: Dp = 14.dp

    /** The smallest width of the count pill of a segmented control: 20dp. */
    val CountPillMinWidth: Dp = MonarchTokens.Space.space5.dp

    /** The slider track (6dp) and thumb (18dp). */
    val SliderTrack: Dp = MonarchTokens.Space.space1_5.dp
    val SliderThumb: Dp = 18.dp

    // Feedback.

    /** The height of a progress bar: 4dp small, 8dp default. */
    val ProgressBarSmall: Dp = MonarchTokens.Space.space1.dp
    val ProgressBarMedium: Dp = MonarchTokens.Space.space2.dp

    /** The diameter of a spinner: 14, 16 and 24dp, with a 2dp stroke. */
    val SpinnerSmall: Dp = 14.dp
    val SpinnerMedium: Dp = MonarchTokens.Space.space4.dp
    val SpinnerLarge: Dp = MonarchTokens.Space.space6.dp
    val SpinnerStroke: Dp = MonarchTokens.Space.space0_5.dp

    // Navigation.

    /** The visible height of a tab (36dp) and the thickness of its indicator (2dp). */
    val TabHeight: Dp = MonarchTokens.Control.md.dp
    val TabIndicator: Dp = MonarchTokens.Space.space0_5.dp
}
