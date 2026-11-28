package dev.monarchui.components.feedback.savestatusindicator

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchRelativeTime
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and rules of MonarchSaveStatusIndicator, built from primitives and roles only. */
internal object MonarchSaveStatusIndicatorTokens {
    val Gap: Dp = MonarchTokens.Space.space1_5.dp

    /** The size of the mark before the text at the default font size; it grows with the font size setting. */
    val MarkSize: Dp = MonarchTokens.Space.space2.dp

    /** The size of the progress ring while saving at the default font size. */
    val SpinnerSize: Dp = MonarchTokens.Space.space3.dp
    val SpinnerStroke: Dp = MonarchTokens.Space.space0_5.dp


    /** The middle dot between "Saved" and the time. Decorative. */
    const val SEPARATOR: String = "·"

    /**
     * The colour of the mark: subtle text while saving, the strong colour of
     * success, warning and danger for saved, unsaved and error. `null` when
     * nothing is drawn.
     */
    fun markColor(status: MonarchSaveStatus, colors: MonarchColors): Color? = when (status) {
        MonarchSaveStatus.Idle -> null
        MonarchSaveStatus.Saving -> colors.textSubtle
        MonarchSaveStatus.Unsaved -> colors.warningStrong
        MonarchSaveStatus.Saved -> colors.successStrong
        MonarchSaveStatus.Error -> colors.dangerStrong
    }

    /** The colour of the text: muted, and the strong danger colour for an error. */
    fun textColor(status: MonarchSaveStatus, colors: MonarchColors): Color =
        if (status == MonarchSaveStatus.Error) colors.dangerStrong else colors.textMuted

    /** Whether the spinner is drawn: while saving with animations on. Otherwise a still dot. */
    fun showsSpinner(status: MonarchSaveStatus, reducedMotion: Boolean): Boolean =
        status == MonarchSaveStatus.Saving && !reducedMotion

    /** Whether the time is shown: after "Saved" when the app gives one. */
    fun showsTime(status: MonarchSaveStatus, savedAt: Long?): Boolean =
        status == MonarchSaveStatus.Saved && savedAt != null

    /** [time] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)
}
