package com.rookhq.monarch.components.intelligence.aiclaimsources

import androidx.compose.runtime.Immutable

/**
 * One statement of an assistant's answer and the passages that back or contradict it.
 *
 * @property id a stable id, reported by `onSelectClaim`, `onFlag` and `onVerify`.
 * @property text the claim as the answer states it.
 * @property support how well the sources back it.
 * @property evidence the passages found for it, in the order to show them. Empty when there are none.
 */
@Immutable
public data class MonarchAiClaim(
    public val id: String,
    public val text: String,
    public val support: MonarchAiClaimSupport,
    public val evidence: List<MonarchAiClaimEvidence> = emptyList(),
) {
    /** Whether the claim is not fully supported: the claims that Needs checking shows. */
    public val needsChecking: Boolean get() = support != MonarchAiClaimSupport.Supported
}
