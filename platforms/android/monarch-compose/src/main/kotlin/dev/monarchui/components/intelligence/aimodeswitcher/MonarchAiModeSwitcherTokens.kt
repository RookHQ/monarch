package dev.monarchui.components.intelligence.aimodeswitcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiModeSwitcher, built from primitives and roles only. */
internal object MonarchAiModeSwitcherTokens {
    /** The gap between the control and the description. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The size of the segmented control for a switcher size. */
    fun controlSize(size: MonarchAiModeSwitcherSize): MonarchSegmentedControlSize = when (size) {
        MonarchAiModeSwitcherSize.Sm -> MonarchSegmentedControlSize.Sm
        MonarchAiModeSwitcherSize.Md -> MonarchSegmentedControlSize.Md
    }

    /** Whether the description line is shown: asked for, and at least one mode has a description. */
    fun describes(modes: List<MonarchAiMode>, showDescription: Boolean): Boolean =
        showDescription && modes.any { !it.description.isNullOrEmpty() }

    /** The description of the selected mode, or `null` when the selection matches no mode or it has none. */
    fun description(selection: String, modes: List<MonarchAiMode>): String? =
        modes.firstOrNull { it.id == selection }?.description

    /** Whether choosing [id] may change the selection: it names a mode that is enabled. */
    fun accepts(id: String, modes: List<MonarchAiMode>): Boolean = modes.any { it.id == id && it.enabled }

    /** Whether a mode's segment shows only its icon. */
    fun showsIconOnly(mode: MonarchAiMode): Boolean = mode.iconOnly && mode.icon != null

    /** The colour of the description line. */
    fun descriptionColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled
}
