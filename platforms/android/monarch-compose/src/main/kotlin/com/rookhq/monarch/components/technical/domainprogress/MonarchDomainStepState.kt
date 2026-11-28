package com.rookhq.monarch.components.technical.domainprogress

/** The state of one step of `MonarchDomainProgress`. */
public enum class MonarchDomainStepState {
    /** The step is finished. */
    Done,

    /** The step is in progress. */
    Current,

    /** The step has not started. */
    Todo,

    /** A blocking problem belongs to the step. */
    Error,
}
