package com.rookhq.monarch.components.workflow.actioneditor

import androidx.compose.runtime.Immutable

/**
 * One action of a rule.
 *
 * @property id a stable, unique id.
 * @property type the `key` of a [MonarchActionType].
 * @property parameters the parameter values, keyed by parameter `key`. A missing key is an empty value.
 */
@Immutable
public data class MonarchRuleAction(
    public val id: String,
    public val type: String,
    public val parameters: Map<String, MonarchActionValue> = emptyMap(),
) {
    /**
     * What is wrong with this action: empty required parameters and numbers out of range (only parameters that
     * are visible, and none when the type has a custom editor), then the type's own validate. Empty when the
     * action is complete or its type is unknown.
     */
    public fun problems(actionTypes: List<MonarchActionType>): List<MonarchActionProblem> {
        val definition = actionTypes.firstOrNull { it.key == type } ?: return emptyList()
        val found = mutableListOf<MonarchActionProblem>()
        if (definition.editor == null) {
            definition.parameters.filter { it.isVisible(parameters) }.forEach { param ->
                val value = parameters[param.key]
                when {
                    param.required && (value == null || value.isEmpty) ->
                        found += MonarchActionProblem.Missing(param.key)
                    param.kind == MonarchActionParameterKind.Number && value != null && !value.isEmpty &&
                        !inRange(value, param) -> found += MonarchActionProblem.OutOfRange(param.key)
                }
            }
        }
        definition.validate?.invoke(parameters)?.let { found += MonarchActionProblem.Invalid(it) }
        return found
    }

    public companion object {
        /** A new action of [type] with its default parameters. */
        public fun create(id: String, type: MonarchActionType?): MonarchRuleAction =
            MonarchRuleAction(id, type?.key.orEmpty(), type?.defaults.orEmpty())

        /** The problems of every incomplete action, keyed by action id. Empty when all are complete. */
        public fun problems(
            actions: List<MonarchRuleAction>,
            actionTypes: List<MonarchActionType>,
        ): Map<String, List<MonarchActionProblem>> = buildMap {
            actions.forEach { a -> a.problems(actionTypes).takeIf { it.isNotEmpty() }?.let { put(a.id, it) } }
        }

        private fun inRange(value: MonarchActionValue, param: MonarchActionParameter): Boolean {
            val number = (value as? MonarchActionValue.Number)?.parsed ?: return false
            return (param.min == null || number >= param.min) && (param.max == null || number <= param.max)
        }
    }
}
