package com.rookhq.monarch.buildlogic.inventory

/**
 * A validator for the subset of JSON Schema that inventory/schema.json uses:
 * type, enum, required, properties, additionalProperties, items, minItems,
 * minLength and pattern. The same subset as the iOS generator.
 */
object Schema {
    fun validate(value: Any?, schema: Map<*, *>, where: String, errors: MutableList<String>) {
        val types = when (val declared = schema["type"]) {
            is String -> listOf(declared)
            is List<*> -> declared.map { it.toString() }
            else -> emptyList()
        }
        if (types.isNotEmpty() && types.none { matches(value, it) }) {
            errors += "$where: expected ${types.joinToString(" or ")}, found ${describe(value)}"
            return
        }
        (schema["enum"] as? List<*>)?.let { allowed ->
            if (value !in allowed) errors += "$where: $value is not one of ${allowed.joinToString(", ")}"
        }
        when (value) {
            is String -> validateString(value, schema, where, errors)
            is List<*> -> validateList(value, schema, where, errors)
            is Map<*, *> -> validateObject(value, schema, where, errors)
        }
    }

    private fun validateString(value: String, schema: Map<*, *>, where: String, errors: MutableList<String>) {
        val minLength = (schema["minLength"] as? Number)?.toInt() ?: 0
        if (value.trim().length < minLength) errors += "$where: must not be empty"
        (schema["pattern"] as? String)?.let { pattern ->
            if (!Regex(pattern).containsMatchIn(value)) errors += "$where: \"$value\" does not match $pattern"
        }
    }

    private fun validateList(value: List<*>, schema: Map<*, *>, where: String, errors: MutableList<String>) {
        val minItems = (schema["minItems"] as? Number)?.toInt() ?: 0
        if (value.size < minItems) errors += "$where: needs at least $minItems item(s)"
        (schema["items"] as? Map<*, *>)?.let { items ->
            value.forEachIndexed { index, item -> validate(item, items, "$where[$index]", errors) }
        }
    }

    private fun validateObject(value: Map<*, *>, schema: Map<*, *>, where: String, errors: MutableList<String>) {
        val properties = schema["properties"] as? Map<*, *> ?: emptyMap<Any, Any>()
        (schema["required"] as? List<*>).orEmpty().forEach { key ->
            if (!value.containsKey(key)) errors += "$where: missing field \"$key\""
        }
        value.forEach { (key, item) ->
            val property = properties[key] as? Map<*, *>
            when {
                property != null -> validate(item, property, "$where.$key", errors)
                schema["additionalProperties"] == false -> errors += "$where: unknown field \"$key\""
            }
        }
    }

    private fun matches(value: Any?, type: String): Boolean = when (type) {
        "null" -> value == null
        "string" -> value is String
        "boolean" -> value is Boolean
        "array" -> value is List<*>
        "object" -> value is Map<*, *>
        "number" -> value is Number
        "integer" -> value is Int || value is Long
        else -> false
    }

    private fun describe(value: Any?): String = when (value) {
        null -> "null"
        is String -> "a string"
        is List<*> -> "an array"
        is Map<*, *> -> "an object"
        else -> value.toString()
    }
}
