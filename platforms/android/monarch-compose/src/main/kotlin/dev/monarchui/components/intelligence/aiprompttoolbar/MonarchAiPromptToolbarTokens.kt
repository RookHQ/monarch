package dev.monarchui.components.intelligence.aiprompttoolbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The fill, text and border of a toolbar toggle in one state. A border of `Color.Transparent` is no border. */
internal data class MonarchAiPromptToolbarToggleRoles(val container: Color, val content: Color, val border: Color)

/** The colours of a toolbar toggle in its four states. */
internal data class MonarchAiPromptToolbarToggleColors(
    val off: MonarchAiPromptToolbarToggleRoles,
    val on: MonarchAiPromptToolbarToggleRoles,
    val disabledOff: MonarchAiPromptToolbarToggleRoles,
    val disabledOn: MonarchAiPromptToolbarToggleRoles,
)

/** Component tokens of MonarchAiPromptToolbar and its controls. */
internal object MonarchAiPromptToolbarTokens {
    /** The gap between controls, across and down. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The visible height of a toggle: the height of the small `MonarchButton`. */
    val ToggleMinHeight: Dp = MonarchTokens.Control.md.dp

    /** The space at the start and end of a toggle, inside its border. */
    val TogglePadding: Dp = MonarchTokens.Space.space2_5.dp

    /** The space between a toggle's icon and its label. */
    val ToggleGap: Dp = MonarchTokens.Space.space1_5.dp

    /**
     * Off: a ghost control in muted text with no border. On: the assistant
     * subtle fill, the assistant border and the strong assistant text.
     * Disabled: the disabled text role; a toggle that is on keeps a muted fill
     * and the subtle border, so its state stays visible.
     */
    fun toggleColors(colors: MonarchColors): MonarchAiPromptToolbarToggleColors =
        MonarchAiPromptToolbarToggleColors(
            off = MonarchAiPromptToolbarToggleRoles(Color.Transparent, colors.textMuted, Color.Transparent),
            on = MonarchAiPromptToolbarToggleRoles(colors.aiSubtle, colors.aiStrong, colors.aiBorder),
            disabledOff = MonarchAiPromptToolbarToggleRoles(Color.Transparent, colors.textDisabled, Color.Transparent),
            disabledOn = MonarchAiPromptToolbarToggleRoles(colors.bgMuted, colors.textDisabled, colors.borderSubtle),
        )
}
