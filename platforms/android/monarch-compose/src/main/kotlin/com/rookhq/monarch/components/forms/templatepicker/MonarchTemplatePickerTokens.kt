package com.rookhq.monarch.components.forms.templatepicker

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.text.Normalizer
import java.util.Locale

/** Component tokens and the pure logic of MonarchTemplatePicker. */
internal object MonarchTemplatePickerTokens {
    /** The narrowest a card gets at the default font size before the grid drops a column (9rem on the web). */
    val MinCardWidth: Dp = MonarchTokens.Space.space24.dp + MonarchTokens.Space.space12.dp

    /** The gap between cards and between the header rows. */
    val Gap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between category filters. */
    val FilterGap: Dp = MonarchTokens.Space.space2.dp

    /** The padding inside a card. */
    val CardPadding: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the parts of a card. */
    val CardGap: Dp = MonarchTokens.Space.space2.dp

    /** The least height of a preview. */
    val PreviewHeight: Dp = MonarchTokens.Space.space16.dp

    private val Marks = Regex("\\p{Mn}+")
    private val Spaces = Regex("\\s+")

    /** The narrowest card at [fontScale], so larger text gets fewer, wider columns. */
    fun minCardWidth(fontScale: Float): Dp = MinCardWidth * maxOf(1f, fontScale)

    /** The background of a preview. */
    fun previewColor(enabled: Boolean, colors: MonarchColors): Color = if (enabled) colors.bgSubtle else colors.bgMuted

    /** The colour of a card's description. */
    fun descriptionColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    /** [text] in lower case without accents, for matching. */
    fun fold(text: String): String =
        Marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

    /**
     * Whether [item] matches [query]: every word of the query appears in the
     * name, the description or the keywords, in any order, ignoring case and
     * accents. A blank query matches everything.
     */
    fun matches(item: MonarchTemplatePickerItem, query: String): Boolean {
        val words = fold(query).split(Spaces).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = fold((listOfNotNull(item.name, item.description) + item.keywords).joinToString(" "))
        return words.all { it in haystack }
    }

    /** The templates in [category] (all when `null`) that match [query], in their order. */
    fun filter(
        templates: List<MonarchTemplatePickerItem>,
        category: String?,
        query: String,
    ): List<MonarchTemplatePickerItem> =
        templates.filter { (category == null || it.category == category) && matches(it, query) }
}
