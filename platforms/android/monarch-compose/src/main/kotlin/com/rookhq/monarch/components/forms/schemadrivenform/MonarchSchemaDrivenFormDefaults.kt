package com.rookhq.monarch.components.forms.schemadrivenform

import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldProblem
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue

/**
 * The rules of a `MonarchSchemaDrivenForm`, for apps that validate or submit the same answers elsewhere, such as
 * from a top app bar or on the server's reply.
 *
 * A [MonarchFormFieldType.Section] question starts a section that lasts until the next one: when the section's
 * conditions do not hold, its questions are hidden with it. Hidden questions are not validated or submitted.
 */
public object MonarchSchemaDrivenFormDefaults {
    /** The questions shown for [values], sections included, in schema order. */
    public fun visibleFields(
        fields: List<MonarchFormFieldDefinition>,
        values: Map<String, MonarchFormFieldValue?>,
    ): List<MonarchFormFieldDefinition> {
        var sectionShown = true
        return fields.filter { field ->
            if (field.type == MonarchFormFieldType.Section) {
                sectionShown = field.isVisible(values)
                sectionShown
            } else {
                sectionShown && field.isVisible(values)
            }
        }
    }

    /**
     * The problem of every shown question, keyed by question id, as `MonarchFormFieldDefinition.problem` finds
     * it. Empty when the answers can be submitted.
     */
    public fun problems(
        fields: List<MonarchFormFieldDefinition>,
        values: Map<String, MonarchFormFieldValue?>,
    ): Map<String, MonarchFormFieldProblem> = buildMap {
        visibleFields(fields, values).forEach { field ->
            if (field.type != MonarchFormFieldType.Section) field.problem(values[field.id])?.let { put(field.id, it) }
        }
    }

    /** The answers to the shown questions, keyed by question id, `null` for an unanswered one. */
    public fun submittedValues(
        fields: List<MonarchFormFieldDefinition>,
        values: Map<String, MonarchFormFieldValue?>,
    ): Map<String, MonarchFormFieldValue?> =
        visibleFields(fields, values)
            .filter { it.type != MonarchFormFieldType.Section }
            .associate { it.id to values[it.id] }
}
