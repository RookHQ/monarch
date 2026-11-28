package dev.monarchui.components.intelligence.airelatedentities

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.R
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.avatar.MonarchAvatarTokens
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and rules of MonarchAiRelatedEntities, built from primitives and roles only. */
internal object MonarchAiRelatedEntitiesTokens {
    /** The graph is four node widths across. */
    const val NODES_ACROSS: Int = 4

    /** The widest graph, as on the web. */
    val GraphMaxSize: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space8).dp
    val RowGap: Dp = MonarchTokens.Space.space3.dp
    val ListGap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp
    val RowPadding: Dp = MonarchTokens.Space.space3.dp
    val WeightBarHeight: Dp = MonarchTokens.Space.space1.dp
    val NodeGap: Dp = MonarchTokens.Space.space1.dp
    val EdgeWidth: Dp = MonarchTokens.Space.px.dp

    /** The avatar of the centre entity and of the others in the graph. */
    val CenterAvatar: MonarchAvatarSize = MonarchAvatarSize.Lg
    val RingAvatar: MonarchAvatarSize = MonarchAvatarSize.Md

    /** The diameter of a graph avatar at the default font size, as MonarchAvatar draws it. */
    fun avatarDiameter(size: MonarchAvatarSize): Dp = MonarchAvatarTokens.side(size)

    /** The built-in name of a common kind, or `null` for a custom one. */
    fun kindRes(kind: MonarchAiRelatedEntityKind): Int? = when (kind) {
        MonarchAiRelatedEntityKind.Person -> R.string.monarch_airelatedentities_kind_person
        MonarchAiRelatedEntityKind.Org -> R.string.monarch_airelatedentities_kind_org
        MonarchAiRelatedEntityKind.Project -> R.string.monarch_airelatedentities_kind_project
        MonarchAiRelatedEntityKind.Document -> R.string.monarch_airelatedentities_kind_document
        MonarchAiRelatedEntityKind.Record -> R.string.monarch_airelatedentities_kind_record
        MonarchAiRelatedEntityKind.Place -> R.string.monarch_airelatedentities_kind_place
        MonarchAiRelatedEntityKind.Other -> R.string.monarch_airelatedentities_kind_other
        else -> null
    }

    /** The outline of a list row. */
    fun rowBorder(colors: MonarchColors): Color = colors.borderSubtle

    /** The relevance bar: the strong border colour on the muted track, so a list of bars stays quiet. */
    fun weightFill(colors: MonarchColors): Color = colors.aiSolid

    /** The track of the relevance bar. */
    fun weightTrack(colors: MonarchColors): Color = colors.bgMuted

    /** The relation lines of the graph. */
    fun edge(colors: MonarchColors): Color = colors.borderDefault
}
