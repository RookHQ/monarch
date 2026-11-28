package dev.monarchui.components.intelligence.aistructuredanswer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.text.NumberFormat
import java.util.Locale

/** Component tokens and the pure logic of MonarchAiStructuredAnswer. */
internal object MonarchAiStructuredAnswerTokens {
    /** The padding inside the card. */
    val Padding: Dp = MonarchTokens.Space.space4.dp

    /** The gap between the header, the fields and the actions. */
    val SectionGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap inside the header and between list items and badges. */
    val SmallGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between the bullet and the text of a list item. */
    val BulletGap: Dp = MonarchTokens.Space.space2.dp

    /** The height of the placeholder bar of a field still to come. */
    val PlaceholderHeight: Dp = MonarchTokens.Space.space3.dp

    /** The width of the placeholder bar, as a fraction of the value column. */
    const val PLACEHOLDER_FRACTION: Float = 0.6f

    /** The columns of the grid layout from 640dp wide. */
    const val GRID_COLUMNS: Int = 2

    /** The bullet before each item of a list field. Decorative. */
    const val BULLET: String = "•"

    /** Whether a value is missing: absent, JSON null, an empty string or an empty array. */
    fun isMissing(value: MonarchAiJSON?): Boolean = when (value) {
        null, MonarchAiJSON.Null -> true
        is MonarchAiJSON.Text -> value.value.isEmpty()
        is MonarchAiJSON.Array -> value.values.isEmpty()
        else -> false
    }

    /** A value as text: strings as they are, numbers and booleans as JSON writes them, the rest as JSON. */
    fun asText(value: MonarchAiJSON): String = when (value) {
        is MonarchAiJSON.Text -> value.value
        else -> value.toJsonString()
    }

    /** A value as list items: the array's present values, or the value itself. */
    fun asList(value: MonarchAiJSON): List<String> {
        val items = if (value is MonarchAiJSON.Array) value.values else listOf(value)
        return items.filterNot { isMissing(it) }.map { asText(it) }
    }

    /** A value as a number: a JSON number, or text such as "1,200" or "4.2". `null` when it is neither. */
    fun asNumber(value: MonarchAiJSON): Double? = when (value) {
        is MonarchAiJSON.Number -> value.value.takeIf { it.isFinite() }
        is MonarchAiJSON.Text -> value.value.replace(",", "").trim().toDoubleOrNull()?.takeIf { it.isFinite() }
        else -> null
    }

    /** The text of a `Number` field: [format], else the number in [locale]; the value as text when it is no number. */
    fun numberText(value: MonarchAiJSON, format: ((Double) -> String)?, locale: Locale): String {
        val number = asNumber(value) ?: return asText(value)
        return format?.invoke(number) ?: NumberFormat.getNumberInstance(locale).format(number)
    }

    /** Whether a field takes a whole row of the grid. */
    fun isWide(field: MonarchAiStructuredField): Boolean = field.wide
        ?: (field.type == MonarchAiStructuredFieldType.List || field.type == MonarchAiStructuredFieldType.Markdown)

    /** The colour of the provenance label and its icon. */
    fun eyebrowColor(colors: MonarchColors): Color = colors.aiStrong

    /** The colour of the placeholder bar of a field still to come. */
    fun placeholderColor(colors: MonarchColors): Color = colors.aiSubtle

    /** The fill of the card. */
    fun containerColor(colors: MonarchColors): Color = colors.bgSurface

    /** The border of the card. */
    fun borderColor(colors: MonarchColors): Color = colors.borderDefault
}
