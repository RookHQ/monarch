package dev.monarchui.components.workflow.fieldrenderer

import androidx.compose.runtime.Immutable

/**
 * A choice of a select, multi-select, radio or checkboxes question.
 *
 * @property value the stable value stored in the answer.
 * @property label the visible text.
 */
@Immutable
public data class MonarchFormFieldOption(
    public val value: String,
    public val label: String,
)
