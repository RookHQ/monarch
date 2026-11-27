package com.rookhq.monarch.components.intelligence.aiagentcard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and pure logic of MonarchAiAgentCard, built from primitives and roles only. */
internal object MonarchAiAgentCardTokens {
    /** The dot before a data-access item. */
    val BulletSize: Dp = MonarchTokens.Space.space1.dp

    /** The bar at the start of a tool. */
    val ToolBarWidth: Dp = MonarchTokens.Space.space0_5.dp

    val Gap: Dp = MonarchTokens.Space.space3.dp
    val HeaderGap: Dp = MonarchTokens.Space.space3.dp
    val SectionGap: Dp = MonarchTokens.Space.space1.dp
    val ItemGap: Dp = MonarchTokens.Space.space2.dp
    val ToolGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The avatar is large on the full card and medium on the compact one, as on the web. */
    fun avatarSize(variant: MonarchAiAgentCardVariant): MonarchAvatarSize = when (variant) {
        MonarchAiAgentCardVariant.Full -> MonarchAvatarSize.Lg
        MonarchAiAgentCardVariant.Compact -> MonarchAvatarSize.Md
    }

    /** The line under the name: the role and the kind, "Support · AI agent"; the kind alone without a role. */
    fun subtitle(role: String?, kind: String): String =
        listOfNotNull(role?.takeIf { it.isNotBlank() }, kind).joinToString(" · ")

    /** What TalkBack reads for a tool: its name, then what it does. */
    fun toolText(tool: MonarchAiAgentTool): String =
        listOfNotNull(tool.name, tool.description?.takeIf { it.isNotBlank() }).joinToString(", ")

    /** What TalkBack reads for the owner: "Owner, Sam Rivera". */
    fun ownerText(label: String, owner: String): String = "$label, $owner"

    fun nameColor(colors: MonarchColors): Color = colors.textDefault

    fun mutedColor(colors: MonarchColors): Color = colors.textMuted

    fun headingColor(colors: MonarchColors): Color = colors.textSubtle
}
