package com.rookhq.monarch.components.identity.consenthistory

import androidx.compose.runtime.Immutable

/**
 * One recorded consent decision.
 *
 * @property id a stable id of the decision.
 * @property dateMillis when it was made, in epoch milliseconds (UTC).
 * @property method how it was made.
 * @property policyVersion the version of the policy the person saw, such as "2026-03".
 * @property regime the rules that applied.
 * @property categories whether each category was allowed, by category id.
 * @property globalPrivacyControl whether a browser or device privacy signal was present.
 */
@Immutable
public class MonarchConsentRecord(
    public val id: String,
    public val dateMillis: Long,
    public val method: MonarchConsentMethod,
    public val policyVersion: String,
    public val regime: MonarchConsentRegime,
    public val categories: Map<String, Boolean>,
    public val globalPrivacyControl: Boolean = false,
)
