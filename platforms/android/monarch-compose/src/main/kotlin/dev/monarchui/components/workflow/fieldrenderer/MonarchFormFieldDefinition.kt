package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable
import dev.monarchui.components.workflow.conditioneditor.MonarchCondition
import dev.monarchui.components.workflow.conditioneditor.MonarchConditionMatch

/**
 * One question of a form, as data, rendered by `MonarchFieldRenderer`.
 *
 * @property id a stable, unique id: the key of the answer.
 * @property type the kind of question.
 * @property label the visible label, or the heading of a [MonarchFormFieldType.Section].
 * @property description help under the label.
 * @property placeholder the placeholder of a text field, or of a menu while nothing is chosen.
 * @property required whether an answer is needed: a mark after the label, and [problem] reports a missing one.
 * @property options the choices of select, multi-select, radio and checkboxes questions.
 * @property minLength the fewest characters of a text answer.
 * @property maxLength the most characters of a text answer; typing stops there.
 * @property min the smallest number, or the first value of a rating (1 by default).
 * @property max the largest number, or the last value of a rating (5 by default).
 * @property minDate the earliest day that can be picked, as UTC epoch milliseconds.
 * @property maxDate the latest day that can be picked, as UTC epoch milliseconds.
 * @property minSelections the fewest options of a checkboxes or multi-select question.
 * @property maxSelections the most options of a checkboxes or multi-select question.
 * @property accept the accepted MIME types of a file question, such as `application/pdf` or `image/`*.
 * @property maxFileSize the largest file in bytes.
 * @property maxFiles the most files.
 * @property consentText the statement a consent question asks people to agree to; the label by default.
 * @property conditions the question is shown only when these hold, tested against other answers by question id.
 * @property conditionMatch whether all [conditions] or at least one must hold.
 * @property readOnly shown but not editable, such as a verified email address. Text questions stay readable and
 * selectable; other questions are disabled.
 */
@Immutable
public class MonarchFormFieldDefinition(
    public val id: String,
    public val type: MonarchFormFieldType,
    public val label: String,
    public val description: String? = null,
    public val placeholder: String? = null,
    public val required: Boolean = false,
    public val options: List<MonarchFormFieldOption> = emptyList(),
    public val minLength: Int? = null,
    public val maxLength: Int? = null,
    public val min: Double? = null,
    public val max: Double? = null,
    public val minDate: Long? = null,
    public val maxDate: Long? = null,
    public val minSelections: Int? = null,
    public val maxSelections: Int? = null,
    public val accept: List<String> = emptyList(),
    public val maxFileSize: Long? = null,
    public val maxFiles: Int = 1,
    public val consentText: String? = null,
    public val conditions: List<MonarchCondition> = emptyList(),
    public val conditionMatch: MonarchConditionMatch = MonarchConditionMatch.All,
    public val readOnly: Boolean = false,
) {
    /**
     * Checks [value] against this question: required, the email, web address and number formats, the limits and
     * the selections, in the web's order. Returns the first problem, or `null` when the answer is valid.
     */
    public fun problem(value: MonarchFormFieldValue?): MonarchFormFieldProblem? =
        MonarchFieldRendererTokens.problem(this, value)

    /**
     * Whether this question is shown, given every answer keyed by question id: its [conditions] must hold, as
     * `MonarchCondition.evaluate` tests them. A question without conditions is always shown.
     */
    public fun isVisible(values: Map<String, MonarchFormFieldValue?>): Boolean =
        conditions.isEmpty() ||
            MonarchCondition.evaluate(conditions, values.mapValues { it.value?.raw }, conditionMatch)
}
