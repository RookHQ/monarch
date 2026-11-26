package com.rookhq.monarch.components.workflow.fieldrenderer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.net.URI
import java.net.URISyntaxException
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldProblem as Problem
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType as Type

/** Component tokens and the pure logic of MonarchFieldRenderer. */
internal object MonarchFieldRendererTokens {
    /** The gap between the control and the notes around it. */
    val Gap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between options and between files. */
    val ControlGap: Dp = MonarchTokens.Space.space2.dp

    /** The widest a number field grows: 192dp, as the web's short count field. */
    val NumberMaxWidth: Dp = (MonarchTokens.Space.space24 * 2).dp

    /** The default confidence under which the note asks for a check, as on the web. */
    const val DEFAULT_CONFIDENCE_THRESHOLD: Float = 0.6f

    /** The default first value of a rating. */
    const val DEFAULT_RATING_MIN: Int = 1

    /** The default last value of a rating. */
    const val DEFAULT_RATING_MAX: Int = 5

    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_DAY = 24
    private val EmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val TimePattern = Regex("^(\\d{1,2}):(\\d{2})$")

    /** The types whose input can be read-only; other types are disabled when read-only, as on the web. */
    private val TextLike = setOf(Type.ShortText, Type.LongText, Type.Email, Type.Phone, Type.Url, Type.Number)

    /** Whether a read-only question of [type] keeps a readable, read-only input instead of being disabled. */
    fun keepsReadOnlyInput(type: Type): Boolean = type in TextLike

    /** The keyboard of a text question. */
    fun keyboard(type: Type): KeyboardType = when (type) {
        Type.Email -> KeyboardType.Email
        Type.Phone -> KeyboardType.Phone
        Type.Url -> KeyboardType.Uri
        else -> KeyboardType.Text
    }

    /** The gap between the assistant mark and the "filled in automatically" note. */
    val NoteGap: Dp = MonarchTokens.Space.space1.dp

    /** The colour of the "filled in automatically" note: the AI text, or the warning text when unsure. */
    fun aiNoteColor(colors: MonarchColors, unsure: Boolean): Color =
        if (unsure) colors.warningStrong else colors.aiStrong

    /** The colour of a section's description and of a file's size. */
    fun noteColor(colors: MonarchColors): Color = colors.textMuted

    /** The colour of a consent error. */
    fun errorColor(colors: MonarchColors): Color = colors.dangerStrong

    /** Minutes from midnight of a `HH:MM` answer, or `null` when it is not one. */
    fun minutesOf(text: String?): Int? {
        val match = text?.let { TimePattern.matchEntire(it.trim()) } ?: return null
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        return if (hours < HOURS_PER_DAY && minutes < MINUTES_PER_HOUR) hours * MINUTES_PER_HOUR + minutes else null
    }

    /** A time answer, `HH:MM` on a 24-hour clock, from an hour and a minute. */
    fun timeAnswer(hour: Int, minute: Int): String = "%02d:%02d".format(java.util.Locale.ROOT, hour, minute)

    /** The checks of `MonarchFormFieldDefinition.problem`, in the web's order. */
    @Suppress("CyclomaticComplexMethod", "ReturnCount")
    fun problem(field: MonarchFormFieldDefinition, value: MonarchFormFieldValue?): Problem? {
        if (field.type == Type.Section) return null
        if (field.type == Type.Consent) {
            val ticked = (value as? MonarchFormFieldValue.Flag)?.checked == true
            return if (field.required && !ticked) Problem.MustAgree else null
        }
        val unansweredYesNo = field.type == Type.YesNo && value !is MonarchFormFieldValue.Flag
        if (value == null || value.isEmpty || unansweredYesNo) {
            if (value is MonarchFormFieldValue.Number && value.number.isNaN()) return Problem.NotANumber
            return if (field.required) Problem.Required else null
        }
        return when (value) {
            is MonarchFormFieldValue.Text -> textProblem(field, value.text.trim())
            is MonarchFormFieldValue.Number -> numberProblem(field, value.number)
            is MonarchFormFieldValue.Values -> selectionProblem(field, value.values.size)
            else -> null
        }
    }

    private fun textProblem(field: MonarchFormFieldDefinition, text: String): Problem? {
        val minLength = field.minLength
        val maxLength = field.maxLength
        return when {
            field.type == Type.Email && !EmailPattern.matches(text) -> Problem.Email
            field.type == Type.Url && !isWebAddress(text) -> Problem.Url
            minLength != null && text.length < minLength -> Problem.MinLength(minLength)
            maxLength != null && text.length > maxLength -> Problem.MaxLength(maxLength)
            else -> null
        }
    }

    private fun numberProblem(field: MonarchFormFieldDefinition, number: Double): Problem? {
        if (field.type != Type.Number) return null
        val min = field.min
        val max = field.max
        return when {
            min != null && number < min -> Problem.Min(min)
            max != null && number > max -> Problem.Max(max)
            else -> null
        }
    }

    private fun selectionProblem(field: MonarchFormFieldDefinition, count: Int): Problem? {
        if (field.type != Type.Checkboxes && field.type != Type.MultiSelect) return null
        val min = field.minSelections
        val max = field.maxSelections
        return when {
            min != null && count < min -> Problem.MinSelections(min)
            max != null && count > max -> Problem.MaxSelections(max)
            else -> null
        }
    }

    /** Whether [text] is an absolute `http` or `https` address with a host. */
    fun isWebAddress(text: String): Boolean = try {
        val uri = URI(text)
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrEmpty()
    } catch (_: URISyntaxException) {
        false
    }
}
