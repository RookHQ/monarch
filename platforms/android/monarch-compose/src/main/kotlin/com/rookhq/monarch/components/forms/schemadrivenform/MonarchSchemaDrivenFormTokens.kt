package com.rookhq.monarch.components.forms.schemadrivenform

import android.icu.text.DateFormat
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and pure logic of MonarchSchemaDrivenForm, built from primitives only. */
internal object MonarchSchemaDrivenFormTokens {
    val FieldGap: Dp = MonarchTokens.Space.space5.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    /** The questions typed into, whose errors first show when the person leaves them. */
    private val Typed = setOf(
        MonarchFormFieldType.ShortText,
        MonarchFormFieldType.LongText,
        MonarchFormFieldType.Email,
        MonarchFormFieldType.Phone,
        MonarchFormFieldType.Url,
        MonarchFormFieldType.Number,
    )

    /** Whether a question counts as visited when focus leaves it (typed), rather than when it is answered. */
    fun visitedOnFocusLoss(type: MonarchFormFieldType): Boolean = type in Typed

    /**
     * Whether a question's own problem is shown: always after a submit attempt, and with
     * [MonarchSchemaDrivenFormValidateOn.Blur] once the question was visited.
     */
    fun showsProblem(validateOn: MonarchSchemaDrivenFormValidateOn, submitted: Boolean, visited: Boolean): Boolean =
        submitted || (validateOn == MonarchSchemaDrivenFormValidateOn.Blur && visited)

    /** The message of a failed submit: the error's own message, or [fallback]. */
    fun failureText(error: Throwable, fallback: String): String = error.message?.takeIf { it.isNotBlank() } ?: fallback

    /**
     * An answer as text for the read-only view: option labels for choices, [yes] or [no] for flags, a medium
     * date in UTC, a locale number, file names, and `null` for no answer.
     */
    fun display(
        field: MonarchFormFieldDefinition,
        value: MonarchFormFieldValue?,
        locale: Locale,
        yes: String,
        no: String,
    ): String? {
        if (value == null || value.isEmpty) return null
        fun label(option: String) = field.options.firstOrNull { it.value == option }?.label ?: option
        return when (value) {
            is MonarchFormFieldValue.Flag -> if (value.checked) yes else no
            is MonarchFormFieldValue.Text -> label(value.text)
            is MonarchFormFieldValue.Number -> NumberFormat.getInstance(locale).format(value.number)
            is MonarchFormFieldValue.Date -> DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(value.millis)
            is MonarchFormFieldValue.Values -> value.values.joinToString(", ") { label(it) }
            is MonarchFormFieldValue.Files -> value.files.joinToString(", ") { it.name }
        }
    }
}
