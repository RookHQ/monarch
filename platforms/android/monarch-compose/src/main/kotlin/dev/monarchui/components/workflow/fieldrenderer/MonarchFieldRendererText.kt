package dev.monarchui.components.workflow.fieldrenderer

import android.content.res.Resources
import android.icu.text.NumberFormat
import dev.monarchui.R
import java.util.Locale

/** The strings of a MonarchFieldRenderer: each override from [labels], or the built-in resource. */
internal class MonarchFieldRendererText(
    private val labels: MonarchFieldRendererLabels,
    private val resources: Resources,
    locale: Locale,
) {
    private val numbers = NumberFormat.getInstance(locale)
    private fun string(override: String?, id: Int): String = override ?: resources.getString(id)

    val yes: String get() = string(labels.yes, R.string.monarch_fieldrenderer_yes)
    val no: String get() = string(labels.no, R.string.monarch_fieldrenderer_no)
    val choose: String get() = string(labels.choose, R.string.monarch_fieldrenderer_choose)
    val chooseDate: String get() = string(labels.chooseDate, R.string.monarch_fieldrenderer_choose_date)
    val chooseTime: String get() = string(labels.chooseTime, R.string.monarch_fieldrenderer_choose_time)
    val confirm: String get() = string(labels.confirm, R.string.monarch_fieldrenderer_confirm)
    val cancel: String get() = string(labels.cancel, R.string.monarch_fieldrenderer_cancel)
    val aiFilled: String get() = string(labels.aiFilled, R.string.monarch_fieldrenderer_ai_filled)
    val aiCheck: String get() = string(labels.aiCheck, R.string.monarch_fieldrenderer_ai_check)
    val remove: String get() = string(labels.remove, R.string.monarch_fieldrenderer_remove)

    fun removeFile(name: String): String =
        labels.removeFile?.invoke(name) ?: resources.getString(R.string.monarch_fieldrenderer_remove_file, name)

    /** The message of [problem]. */
    @Suppress("CyclomaticComplexMethod") // One branch per problem.
    fun message(problem: MonarchFormFieldProblem): String = when (problem) {
        MonarchFormFieldProblem.Required -> string(labels.required, R.string.monarch_fieldrenderer_required)
        MonarchFormFieldProblem.MustAgree -> string(labels.mustAgree, R.string.monarch_fieldrenderer_must_agree)
        MonarchFormFieldProblem.Email -> string(labels.email, R.string.monarch_fieldrenderer_email)
        MonarchFormFieldProblem.Url -> string(labels.url, R.string.monarch_fieldrenderer_url)
        MonarchFormFieldProblem.NotANumber -> string(labels.number, R.string.monarch_fieldrenderer_number)
        is MonarchFormFieldProblem.Min -> numbers.format(problem.min).let {
            labels.min?.invoke(it) ?: resources.getString(R.string.monarch_fieldrenderer_min, it)
        }
        is MonarchFormFieldProblem.Max -> numbers.format(problem.max).let {
            labels.max?.invoke(it) ?: resources.getString(R.string.monarch_fieldrenderer_max, it)
        }
        is MonarchFormFieldProblem.MinLength -> labels.minLength?.invoke(problem.min)
            ?: resources.getQuantityString(R.plurals.monarch_fieldrenderer_min_length, problem.min, problem.min)
        is MonarchFormFieldProblem.MaxLength -> labels.maxLength?.invoke(problem.max)
            ?: resources.getQuantityString(R.plurals.monarch_fieldrenderer_max_length, problem.max, problem.max)
        is MonarchFormFieldProblem.MinSelections -> labels.minSelections?.invoke(problem.min)
            ?: resources.getString(R.string.monarch_fieldrenderer_min_selections, problem.min)
        is MonarchFormFieldProblem.MaxSelections -> labels.maxSelections?.invoke(problem.max)
            ?: resources.getString(R.string.monarch_fieldrenderer_max_selections, problem.max)
    }
}
