package dev.monarchui.components.forms.switchsettingslist

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** What a `MonarchSwitchSettingsList` gives its rows. */
@Immutable
internal class MonarchSwitchSettingsListConfig(
    val labels: MonarchSwitchSettingsListLabels,
    val savedDurationMillis: Long,
    /** Set inside a list: the rows report where they are, so the card draws a divider above every row but the first. */
    val card: SettingsCardGeometry? = null,
)

/** Where the rows of one list card are, on screen, to draw the dividers between them. */
@Stable
internal class SettingsCardGeometry {
    var originY: Float by mutableFloatStateOf(0f)
    val rowTops = mutableStateMapOf<Any, Float>()

    /** The tops of every row but the first, relative to the card. */
    fun dividers(): List<Float> = rowTops.values.map { it - originY }.sorted().drop(1)
}

/** The list around a row; a row outside a list uses the defaults. */
internal val LocalMonarchSwitchSettingsList = staticCompositionLocalOf {
    MonarchSwitchSettingsListConfig(
        labels = MonarchSwitchSettingsListLabels(),
        savedDurationMillis = MonarchSwitchSettingsListDefaults.SavedDurationMillis,
    )
}

/** Component tokens and logic of MonarchSwitchSettingsList, built from primitives and roles only. */
internal object MonarchSwitchSettingsListTokens {
    /** The gap between the title and the description: 2dp, as on the web. */
    val HeaderGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between the header and the card: 12dp, as the web's list gap. */
    val HeaderBottomPadding: Dp = MonarchTokens.Space.space3.dp

    /** A row inside the card: 16dp at the sides; the 48dp row adds its own vertical room (web 12/16). */
    val RowHorizontalPadding: Dp = MonarchTokens.Space.space4.dp
    val RowVerticalPadding: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the spinner or the error and its text, and between the error and the button. */
    val StatusGap: Dp = MonarchTokens.Space.space2.dp

    /** The size of the saving spinner. */
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp

    /** The colour of the status text. */
    fun statusColor(status: MonarchSwitchSettingsStatus, colors: MonarchColors): Color = when (status) {
        MonarchSwitchSettingsStatus.Idle, MonarchSwitchSettingsStatus.Saving -> colors.textMuted
        MonarchSwitchSettingsStatus.Saved -> colors.successStrong
        MonarchSwitchSettingsStatus.Error -> colors.dangerStrong
    }

    /** The colour of the description under the title. */
    fun descriptionColor(colors: MonarchColors): Color = colors.textMuted

    /** The value the switch shows: the one being saved, or the saved one. */
    fun shownValue(checked: Boolean, pending: Boolean?): Boolean = pending ?: checked

    /** The text after the failed-save sentence: the replacement's, or the exception's message. */
    fun errorDetail(error: Throwable, errorMessage: ((Throwable) -> String?)?): String? {
        val text = if (errorMessage != null) errorMessage(error) else error.message
        return text?.trim()?.takeIf { it.isNotEmpty() }
    }
}
