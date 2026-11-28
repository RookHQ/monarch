package dev.monarchui.components.intelligence.aiassistantpanel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationStatus
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchAiAssistantPanel. */
internal object MonarchAiAssistantPanelTokens {
    val Padding: Dp = MonarchTokens.Space.space3.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp

    /** Narrower windows show the panel as a sheet when the placement is Auto, as the web does under 640px. */
    val SheetBelow: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The turn of the minimize glyph while the pane is collapsed, so a downward chevron points up. */
    const val CollapsedIconTurn: Float = 180f

    /** Whether [placement] is a sheet in a window [windowWidth] wide. */
    fun asSheet(placement: MonarchAiAssistantPanelPlacement, windowWidth: Dp): Boolean = when (placement) {
        MonarchAiAssistantPanelPlacement.Sheet -> true
        MonarchAiAssistantPanelPlacement.Inline -> false
        MonarchAiAssistantPanelPlacement.Auto -> windowWidth < SheetBelow
    }

    /** Whether a reply is running: the avatar shows Working and Send becomes Stop. */
    fun busy(status: MonarchAiConversationStatus): Boolean =
        status == MonarchAiConversationStatus.Submitted || status == MonarchAiConversationStatus.Streaming

    /** Whether a starter may be sent: nothing is running and no tool call waits for approval. */
    fun startersEnabled(status: MonarchAiConversationStatus): Boolean =
        !busy(status) && status != MonarchAiConversationStatus.AwaitingApproval

    /** Whether the pane shows the minimize button: only an inline pane with a handler collapses. */
    fun showsMinimize(sheet: Boolean, hasHandler: Boolean): Boolean = !sheet && hasHandler

    /** The line under the title: the agent's name and role, or `null` when there are neither. */
    fun agentLine(name: String?, role: String?): String? =
        listOfNotNull(name?.takeIf { it.isNotBlank() }, role?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
            .ifEmpty { null }

    fun containerColor(colors: MonarchColors): Color = colors.bgSurface

    fun titleColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
