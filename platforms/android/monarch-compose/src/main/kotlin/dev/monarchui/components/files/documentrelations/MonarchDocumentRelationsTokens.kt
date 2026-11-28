package dev.monarchui.components.files.documentrelations

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.internal.MonarchSearchText
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchDocumentRelations. */
internal object MonarchDocumentRelationsTokens {
    /** The most candidates the add sheet lists, as on the web. */
    const val MAX_RESULTS: Int = 12

    /** The relation type chosen when the sheet opens, when the app gives none and it exists. */
    const val DEFAULT_TYPE: String = "related-to"

    /** The least height of a row: the touch target. */
    val RowMinHeight: Dp = MonarchTouchTarget.Minimum

    /** The gap between the parts of the component, of a row and of the sheet. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of a row. */
    val TextGap: Dp = MonarchTokens.Space.space0_5.dp

    /** The gap between relation chips, in a row and between rows. */
    val ChipGap: Dp = MonarchTokens.Space.space2.dp

    /** The vertical padding of a row. */
    val RowVerticalPadding: Dp = MonarchTokens.Space.space2.dp

    /** The padding of the sheet's content. */
    val SheetPadding: Dp = MonarchTokens.Space.space4.dp

    /** The size of the progress shown while a relation is added or removed. */
    val ProgressSize: Dp = MonarchTokens.Space.space5.dp

    /**
     * The type a relation is read as and its label: the type itself when outgoing, its inverse when incoming.
     * An unknown type is shown as its value.
     */
    fun labelOf(relation: MonarchDocumentRelation, types: List<MonarchDocumentRelationType>): Pair<String, String> {
        val type = types.firstOrNull { it.value == relation.type }
        val value = if (relation.direction == MonarchDocumentRelationDirection.Incoming) {
            type?.inverse ?: relation.type
        } else {
            relation.type
        }
        return value to (types.firstOrNull { it.value == value }?.label ?: value)
    }

    /** The type chosen when the sheet opens: [defaultType], else related-to when it exists, else the first. */
    fun initialType(defaultType: String?, types: List<MonarchDocumentRelationType>): String =
        defaultType ?: types.firstOrNull { it.value == DEFAULT_TYPE }?.value ?: types.firstOrNull()?.value.orEmpty()

    /**
     * The candidates the sheet lists: those not already related, filtered by title (ignoring case and accents)
     * unless the app searches on its server, at most [MAX_RESULTS].
     */
    fun candidates(
        candidates: List<MonarchDocumentRelationTarget>,
        relations: List<MonarchDocumentRelation>,
        query: String,
        searchesOnServer: Boolean,
    ): List<MonarchDocumentRelationTarget> {
        val related = relations.map { it.target.id }.toSet()
        val needle = MonarchSearchText.normalize(query.trim())
        return candidates
            .filter { it.id !in related }
            .filter { searchesOnServer || needle.isEmpty() || MonarchSearchText.normalize(it.title).contains(needle) }
            .take(MAX_RESULTS)
    }

    /** The colour of a relation's type label. */
    fun typeColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a document's title: the link colour when it opens, the default text otherwise. */
    fun titleColor(openable: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        openable -> colors.textLink
        else -> colors.textDefault
    }

    /** The colour of a document's description and of the sheet's secondary text. */
    fun secondaryColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** The background of the sheet. */
    fun sheetColor(colors: MonarchColors): Color = colors.bgSurface
}
