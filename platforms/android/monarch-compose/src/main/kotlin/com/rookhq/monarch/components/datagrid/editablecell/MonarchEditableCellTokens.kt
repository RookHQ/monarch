package com.rookhq.monarch.components.datagrid.editablecell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.accessibility.MonarchTouchTarget
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/** What a commit attempt leads to. */
internal sealed interface EditableCellOutcome<out T> {
    /** The value did not change: close the editor without saving. */
    data object Unchanged : EditableCellOutcome<Nothing>

    /** The value is refused with [message]; the editor stays open. */
    data class Refused(val message: String) : EditableCellOutcome<Nothing>

    /** Save [value]. */
    data class Commit<T>(val value: T?) : EditableCellOutcome<T>
}

/** The resolved error messages of an editable cell. */
internal class EditableCellMessages(
    val required: String,
    val invalidNumber: String,
    val belowMin: (String) -> String,
    val aboveMax: (String) -> String,
)

/** Component tokens and the pure rules of MonarchEditableCell. */
internal object MonarchEditableCellTokens {
    /** The least height and width of the cell in view mode: the touch target. */
    val MinSize: Dp = MonarchTouchTarget.Minimum

    /** The padding of the value in view mode. */
    val ViewPadding: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the parts of the cell. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The size of the progress shown while saving. */
    val SpinnerSize: Dp = MonarchTokens.Space.space4.dp

    /** The font feature that gives digits one width, for number cells. */
    const val TABULAR_FIGURES: String = "tnum"

    /** The colour of the value in view mode: subtle for the placeholder, disabled when disabled. */
    fun viewText(empty: Boolean, enabled: Boolean, colors: MonarchColors): Color = when {
        !enabled -> colors.textDisabled
        empty -> colors.textSubtle
        else -> colors.textDefault
    }

    /** The colour of the error under a select or date cell. */
    fun errorText(colors: MonarchColors): Color = colors.dangerStrong

    /** The text a number editor starts from: no grouping, a dot, no trailing zeros. */
    fun numberDraft(value: Double?): String =
        value?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()

    /** A number for view mode, in the [locale]'s format. */
    fun formatNumber(value: Double, locale: Locale): String = NumberFormat.getInstance(locale).format(value)

    /** A date for view mode: the medium date in the [locale], read in UTC. */
    fun formatDate(millis: Long, locale: Locale): String {
        val format = android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.MEDIUM, locale)
        format.timeZone = android.icu.util.TimeZone.getTimeZone("UTC")
        return format.format(java.util.Date(millis))
    }

    /**
     * Reads a number draft. Blank is an empty cell. Spaces are ignored, and the [locale]'s decimal comma is read
     * as a dot when the draft has no dot. Succeeds with `null` for a blank draft, and fails when the text is not
     * a finite number.
     */
    fun parseNumber(draft: String, locale: Locale): Result<Double?> {
        val text = draft.filterNot { it.isWhitespace() }
        if (text.isEmpty()) return Result.success(null)
        val comma = DecimalFormatSymbols.getInstance(locale).decimalSeparator == ','
        val normalised = if (comma && '.' !in text) text.replace(',', '.') else text
        val number = normalised.toDoubleOrNull()?.takeIf { it.isFinite() }
        return if (number != null) Result.success(number) else Result.failure(IllegalArgumentException(draft))
    }

    /** A text draft as a value: trimmed, and `null` when blank. */
    fun textValue(draft: String): String? = draft.trim().ifEmpty { null }

    /** The error when [next] is outside the editor's range, with the limit formatted for the [locale]. */
    fun <T : Any> rangeError(
        editor: MonarchEditableCellEditor<T>,
        next: T?,
        locale: Locale,
        messages: EditableCellMessages,
    ): String? = when {
        next == null -> null
        editor is MonarchEditableCellEditor.Number ->
            bounds(next as Double, editor.min, editor.max, messages) { formatNumber(it, locale) }
        editor is MonarchEditableCellEditor.Date ->
            bounds(next as Long, editor.min, editor.max, messages) { formatDate(it, locale) }
        else -> null
    }

    /** The error when [value] is below [min] or above [max], with the limit written by [format]. */
    private fun <V : Comparable<V>> bounds(
        value: V,
        min: V?,
        max: V?,
        messages: EditableCellMessages,
        format: (V) -> String,
    ): String? = when {
        min != null && value < min -> messages.belowMin(format(min))
        max != null && value > max -> messages.aboveMax(format(max))
        else -> null
    }

    /**
     * Decides what a commit of [next] does, in the web's order: a required cell refuses empty, an unchanged value
     * closes, then the range and the app's [validate] can refuse, else the value is saved.
     */
    @Suppress("LongParameterList")
    fun <T : Any> decide(
        next: T?,
        current: T?,
        required: Boolean,
        rangeError: String?,
        validate: ((T?) -> String?)?,
        messages: EditableCellMessages,
    ): EditableCellOutcome<T> {
        return when {
            required && next == null -> EditableCellOutcome.Refused(messages.required)
            next == current -> EditableCellOutcome.Unchanged
            rangeError != null -> EditableCellOutcome.Refused(rangeError)
            else -> validate?.invoke(next)?.let { EditableCellOutcome.Refused(it) } ?: EditableCellOutcome.Commit(next)
        }
    }

    /** Whether a date picker offers [utcMillis], given the editor's range. */
    fun selectable(editor: MonarchEditableCellEditor.Date, utcMillis: Long): Boolean =
        (editor.min == null || utcMillis >= editor.min) && (editor.max == null || utcMillis <= editor.max)
}
