package com.rookhq.monarch.components.technical.domainprogress

/** Values and helpers for `MonarchDomainProgress`. */
public object MonarchDomainProgressDefaults {
    /**
     * How long to wait before checking a domain again, in milliseconds: the product's suggested delay kept between 15
     * seconds and 5 minutes (30 seconds when it suggests none), 15 seconds before the first stage is known, and `null`
     * once live or when checking has stopped. `MonarchDomainProgress` never checks by itself: the app polls and passes
     * the new stage.
     *
     * @param stage the current stage, or `null` before the first check.
     * @param suggestedMillis the product's suggested delay.
     * @param stopped whether the product stopped checking.
     */
    public fun recheckIntervalMillis(
        stage: MonarchDomainStage?,
        suggestedMillis: Long? = null,
        stopped: Boolean = false,
    ): Long? = MonarchDomainProgressRules.recheckIntervalMillis(stage, suggestedMillis, stopped)
}
