package dev.monarchui.components.forms.termsacceptance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.LabelledControlRowTokens
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchTermsAcceptance. */
internal object MonarchTermsAcceptanceTokens {
    /** The gap between the checkbox row and the error. */
    val Gap: Dp = MonarchTokens.Space.space0_5.dp

    /**
     * The start indent of the links and the error, so they line up with the
     * sentence: the Material 3 checkbox (24dp with its padding) and the row's gap.
     */
    val TextIndent: Dp = MonarchTokens.Space.space6.dp + LabelledControlRowTokens.Gap

    /** The colour of the error. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** Whether the error shows: the box is required, the app asked for the error and the box is not ticked. */
    fun showsError(required: Boolean, showRequiredError: Boolean, accepted: Boolean): Boolean =
        required && showRequiredError && !accepted

    /**
     * Joins [items] as a list: one item as it is, two with [two], more with
     * [middle] between all but the last two and [last] before the last one.
     */
    fun join(
        items: List<String>,
        two: (String, String) -> String,
        middle: (String, String) -> String,
        last: (String, String) -> String,
    ): String = when (items.size) {
        0 -> ""
        1 -> items[0]
        2 -> two(items[0], items[1])
        else -> last(items.subList(1, items.size - 1).fold(items[0], middle), items.last())
    }

    /**
     * The sentence with a link on the title of each document, found in order from where the joined [titles] start.
     * With null [styles] (a disabled box) the sentence has no links.
     */
    fun linkedSentence(
        sentence: String,
        titles: String,
        documents: List<MonarchTermsDocument>,
        styles: TextLinkStyles?,
        onOpen: (MonarchTermsDocument) -> Unit,
    ): AnnotatedString = buildAnnotatedString {
        append(sentence)
        if (styles == null) return@buildAnnotatedString
        var cursor = sentence.indexOf(titles).coerceAtLeast(0)
        documents.forEach { document ->
            val start = sentence.indexOf(document.title, cursor)
            if (start >= 0) {
                val end = start + document.title.length
                addLink(LinkAnnotation.Clickable(document.key, styles) { onOpen(document) }, start, end)
                cursor = end
            }
        }
    }
}
