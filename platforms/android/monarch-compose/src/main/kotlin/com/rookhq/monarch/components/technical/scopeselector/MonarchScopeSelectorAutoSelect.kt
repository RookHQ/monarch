package com.rookhq.monarch.components.technical.scopeselector

/**
 * Which groups get every scope selected once their scopes have loaded, when none of their scopes is selected,
 * so a grant never silently has no access.
 */
public enum class MonarchScopeSelectorAutoSelect {
    /** Every group. */
    All,

    /** Only groups added after the first composition, for editing an existing grant. */
    Added,
}
