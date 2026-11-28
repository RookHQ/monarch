package com.rookhq.monarch.components.technical.domainprogress

/** How a `MonarchDomainProblem` is shown. */
public enum class MonarchDomainProblemSeverity {
    /** A mistake the user must fix. Marks its step as needing attention and is shown as a warning. */
    Blocking,

    /** A normal wait, such as "certificate pending". Shown quietly and does not change a step. */
    Info,
}
