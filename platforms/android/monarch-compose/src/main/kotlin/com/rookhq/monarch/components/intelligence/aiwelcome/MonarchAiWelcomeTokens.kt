package com.rookhq.monarch.components.intelligence.aiwelcome

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and colours of MonarchAiWelcome. */
internal object MonarchAiWelcomeTokens {
    /** The padding above and below the welcome. */
    val VerticalPadding: Dp = MonarchTokens.Space.space6.dp

    /** The padding at the start and end of the welcome. */
    val HorizontalPadding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the parts of the welcome. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between capabilities, and between a capability's mark and its text. */
    val CapabilityGap: Dp = MonarchTokens.Space.space1_5.dp

    /** The side of the AI mark shown without an agent. */
    val MarkSize: Dp = MonarchTokens.Space.space12.dp

    /** The side of the dot before a capability when the app gives no icon. */
    val DotSize: Dp = MonarchTokens.Space.space1_5.dp

    /** The fill, content and border of the AI mark. */
    fun markColors(colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(container = colors.aiSubtle, content = colors.aiStrong, border = colors.aiBorder)

    /** The colour of the agent's name. */
    fun agentNameColor(colors: MonarchColors): Color = colors.aiStrong

    /** The colour of the agent's role, the description and the capability marks. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
