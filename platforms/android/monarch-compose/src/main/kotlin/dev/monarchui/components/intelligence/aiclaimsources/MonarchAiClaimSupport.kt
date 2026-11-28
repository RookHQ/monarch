package dev.monarchui.components.intelligence.aiclaimsources

/** How well the sources back a `MonarchAiClaim`. */
public enum class MonarchAiClaimSupport {
    /** At least one passage backs it and none contradicts it. */
    Supported,

    /** Passages back only part of it. */
    Partial,

    /** No source backs it. */
    Unsupported,

    /** Sources disagree about it. */
    Conflicting,

    /** It has not been compared with the sources yet. */
    Unchecked,
}
