package com.rookhq.monarch.components.forms.schemadrivenform

/** When a `MonarchSchemaDrivenForm` first shows a field's error. */
public enum class MonarchSchemaDrivenFormValidateOn {
    /**
     * When the person leaves a text field, or as soon as they answer a choice, a date or a file question. After
     * a submit attempt, every error shows and follows the answers.
     */
    Blur,

    /** Only after a submit attempt; then every error shows and follows the answers. */
    Submit,
}
