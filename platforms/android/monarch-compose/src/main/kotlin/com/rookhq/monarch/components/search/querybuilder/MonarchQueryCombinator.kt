package com.rookhq.monarch.components.search.querybuilder

/** How the rules and sub-groups of a [MonarchQueryGroup] combine. */
public enum class MonarchQueryCombinator {
    /** Every rule and sub-group must hold. */
    And,

    /** At least one rule or sub-group must hold. */
    Or,
}
