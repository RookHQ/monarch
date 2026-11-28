package com.rookhq.monarch.components.typography.tabularnumberstyle

import java.text.NumberFormat

/** The logic of MonarchTabularNumber. It has no colour or size decisions of its own. */
internal object MonarchTabularNumberTokens {
    /** [value] formatted with [format], or `null` when it is missing: `null`, NaN or infinite. */
    fun format(value: Number?, format: NumberFormat): String? = when {
        value == null -> null
        value is Double && !value.isFinite() -> null
        value is Float && !value.isFinite() -> null
        else -> format.format(value)
    }
}
