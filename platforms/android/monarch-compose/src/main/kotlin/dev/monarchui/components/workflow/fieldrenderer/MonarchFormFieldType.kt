package dev.monarchui.components.workflow.fieldrenderer

/** The kinds of question a `MonarchFieldRenderer` can ask. [Section] is a heading with no answer. */
public enum class MonarchFormFieldType {
    /** One line of text. */
    ShortText,

    /** Several lines of text. */
    LongText,

    /** An email address, with the email keyboard. */
    Email,

    /** A phone number, with the phone keyboard. */
    Phone,

    /** A web address, with the address keyboard. */
    Url,

    /** A number. */
    Number,

    /** A day, picked in the Material 3 date picker. */
    Date,

    /** A time of day, picked in the Material 3 time picker. */
    Time,

    /** One option from a menu. */
    Select,

    /** Several options from a searchable list. */
    MultiSelect,

    /** One option from radio buttons. */
    Radio,

    /** Several options from checkboxes. */
    Checkboxes,

    /** Yes or no, as two radio buttons. */
    YesNo,

    /** One checkbox to agree to a statement. */
    Consent,

    /** A rating on a scale. */
    Rating,

    /** One or more files. */
    File,

    /** A heading between questions, with no answer. */
    Section,
}
