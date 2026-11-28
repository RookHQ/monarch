package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchFieldRenderer`. Every `null` keeps the built-in string
 * `monarch_fieldrenderer_<key>`. Limits in [min] and [max] arrive formatted for the locale.
 *
 * @property required the message of a required question without an answer.
 * @property mustAgree the message of a required consent that is not ticked.
 * @property email the message of text that is not an email address.
 * @property url the message of text that is not a web address.
 * @property number the message of an answer that is not a number.
 * @property min the message of a number below the minimum.
 * @property max the message of a number above the maximum.
 * @property minLength the message of text shorter than the minimum.
 * @property maxLength the message of text longer than the maximum.
 * @property minSelections the message of fewer options than the minimum.
 * @property maxSelections the message of more options than the maximum.
 * @property yes the yes option of a yes-or-no question.
 * @property no the no option of a yes-or-no question.
 * @property choose the placeholder of a menu, "Choose…".
 * @property chooseDate the button of a date question without an answer, "Choose a date".
 * @property chooseTime the button of a time question without an answer, "Choose a time".
 * @property confirm the confirm button of the date and time dialogs, "OK".
 * @property cancel the dismiss button of the date and time dialogs, "Cancel".
 * @property aiFilled the note on an answer filled automatically with enough confidence.
 * @property aiCheck the note on an answer filled automatically with low confidence.
 * @property remove the visible text of a file's remove button, "Remove".
 * @property removeFile the TalkBack name of a file's remove button, from the file name.
 */
@Immutable
public class MonarchFieldRendererLabels(
    public val required: String? = null,
    public val mustAgree: String? = null,
    public val email: String? = null,
    public val url: String? = null,
    public val number: String? = null,
    public val min: ((min: String) -> String)? = null,
    public val max: ((max: String) -> String)? = null,
    public val minLength: ((min: Int) -> String)? = null,
    public val maxLength: ((max: Int) -> String)? = null,
    public val minSelections: ((min: Int) -> String)? = null,
    public val maxSelections: ((max: Int) -> String)? = null,
    public val yes: String? = null,
    public val no: String? = null,
    public val choose: String? = null,
    public val chooseDate: String? = null,
    public val chooseTime: String? = null,
    public val confirm: String? = null,
    public val cancel: String? = null,
    public val aiFilled: String? = null,
    public val aiCheck: String? = null,
    public val remove: String? = null,
    public val removeFile: ((name: String) -> String)? = null,
)
