package dev.monarchui.components.intelligence.aiclaimsources

/** Whether a passage backs its claim or speaks against it. */
public enum class MonarchAiClaimEvidenceStance {
    /** The passage backs the claim. */
    Supports,

    /** The passage speaks against the claim. */
    Contradicts,
}
