package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable

/**
 * Why an answer is not valid, from `MonarchFormFieldDefinition.problem`. A `MonarchFieldRenderer` shows its message.
 */
@Immutable
public sealed interface MonarchFormFieldProblem {
    /** A required question has no answer. */
    public data object Required : MonarchFormFieldProblem

    /** A required consent is not ticked. */
    public data object MustAgree : MonarchFormFieldProblem

    /** The text is not an email address. */
    public data object Email : MonarchFormFieldProblem

    /** The text is not an `http` or `https` web address. */
    public data object Url : MonarchFormFieldProblem

    /** The answer is not a number. */
    public data object NotANumber : MonarchFormFieldProblem

    /**
     * The number is below the minimum.
     *
     * @property min the minimum.
     */
    public data class Min(public val min: Double) : MonarchFormFieldProblem

    /**
     * The number is above the maximum.
     *
     * @property max the maximum.
     */
    public data class Max(public val max: Double) : MonarchFormFieldProblem

    /**
     * The text is shorter than the minimum, ignoring spaces at either end.
     *
     * @property min the fewest characters.
     */
    public data class MinLength(public val min: Int) : MonarchFormFieldProblem

    /**
     * The text is longer than the maximum, ignoring spaces at either end.
     *
     * @property max the most characters.
     */
    public data class MaxLength(public val max: Int) : MonarchFormFieldProblem

    /**
     * Fewer options are chosen than the minimum.
     *
     * @property min the fewest options.
     */
    public data class MinSelections(public val min: Int) : MonarchFormFieldProblem

    /**
     * More options are chosen than the maximum.
     *
     * @property max the most options.
     */
    public data class MaxSelections(public val max: Int) : MonarchFormFieldProblem
}
