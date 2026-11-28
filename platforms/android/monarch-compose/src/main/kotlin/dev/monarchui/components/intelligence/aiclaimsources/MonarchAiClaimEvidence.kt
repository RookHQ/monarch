package dev.monarchui.components.intelligence.aiclaimsources

import androidx.compose.runtime.Immutable

/**
 * One passage from a source, tied to a claim.
 *
 * @property id a stable id, unique within the claim.
 * @property sourceId the id of the `MonarchAiClaimSource` the passage comes from.
 * @property quote the exact passage, word for word. Shown as a quotation and copied by Copy quote.
 * @property location where the passage is in the source, such as "Page 4".
 * @property stance whether the passage backs the claim or speaks against it; `null` when not stated.
 */
@Immutable
public data class MonarchAiClaimEvidence(
    public val id: String,
    public val sourceId: String,
    public val quote: String,
    public val location: String? = null,
    public val stance: MonarchAiClaimEvidenceStance? = null,
)
