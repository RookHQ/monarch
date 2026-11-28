package com.rookhq.monarch.components.technical.domainprogress

import androidx.compose.runtime.Immutable

/**
 * Something the product found while checking a domain, shown by `MonarchDomainProgress`.
 *
 * @property message what is wrong and how to fix it, in plain words. Already localised.
 * @property code a machine code from the product, kept for analytics. Not shown.
 * @property severity blocking (shown as a warning, marks its step) or info (shown quietly).
 * @property step the step the problem belongs to. `null` is the current step.
 */
@Immutable
public class MonarchDomainProblem(
    public val message: String,
    public val code: String? = null,
    public val severity: MonarchDomainProblemSeverity = MonarchDomainProblemSeverity.Blocking,
    public val step: MonarchDomainStep? = null,
)
