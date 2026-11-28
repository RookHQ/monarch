package com.rookhq.monarch.components.intelligence.aiknowledgescope

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.R
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchAiKnowledgeScope, built from primitives and roles only. */
internal object MonarchAiKnowledgeScopeTokens {
    /** The indent of each level below the first. */
    val Indent: Dp = MonarchTokens.Space.space6.dp
    val RowGap: Dp = MonarchTokens.Space.space1.dp
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp
    val VerticalPadding: Dp = MonarchTokens.Space.space1.dp
    val SummaryGap: Dp = MonarchTokens.Space.space2.dp

    /** The quarter turn of the chevron while a group is open. */
    const val EXPANDED_ROTATION: Float = 90f

    /** Joins the kind and the description on the second line. */
    const val SEPARATOR: String = " · "

    /** The colour of a source name. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the kind, the description and the summary. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The colour of the chevron. Groups open in every state, so it is never disabled. */
    fun iconColor(colors: MonarchColors): Color = colors.textMuted

    /** The built-in name of a kind. */
    fun kindRes(kind: MonarchAiKnowledgeSourceKind): Int = when (kind) {
        MonarchAiKnowledgeSourceKind.Space -> R.string.monarch_aiknowledgescope_kind_space
        MonarchAiKnowledgeSourceKind.Document -> R.string.monarch_aiknowledgescope_kind_document
        MonarchAiKnowledgeSourceKind.App -> R.string.monarch_aiknowledgescope_kind_app
    }
}
