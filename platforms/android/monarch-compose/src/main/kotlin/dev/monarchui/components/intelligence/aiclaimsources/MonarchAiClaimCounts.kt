package dev.monarchui.components.intelligence.aiclaimsources

import androidx.compose.runtime.Immutable

/**
 * How many claims there are at each support level, for the summary line.
 *
 * @property total every claim.
 * @property needsChecking the claims that are not fully supported.
 * @property supported the supported claims.
 * @property partial the partly supported claims.
 * @property unsupported the claims no source supports.
 * @property conflicting the claims whose sources disagree.
 * @property unchecked the claims not checked yet.
 */
@Immutable
public data class MonarchAiClaimCounts(
    public val total: Int,
    public val needsChecking: Int,
    public val supported: Int,
    public val partial: Int,
    public val unsupported: Int,
    public val conflicting: Int,
    public val unchecked: Int,
) {
    /** Counting claims. */
    public companion object {
        /**
         * Counts [claims] by support level.
         *
         * @param claims the claims to count.
         */
        public fun of(claims: List<MonarchAiClaim>): MonarchAiClaimCounts {
            fun count(support: MonarchAiClaimSupport) = claims.count { it.support == support }
            return MonarchAiClaimCounts(
                total = claims.size,
                needsChecking = claims.count { it.needsChecking },
                supported = count(MonarchAiClaimSupport.Supported),
                partial = count(MonarchAiClaimSupport.Partial),
                unsupported = count(MonarchAiClaimSupport.Unsupported),
                conflicting = count(MonarchAiClaimSupport.Conflicting),
                unchecked = count(MonarchAiClaimSupport.Unchecked),
            )
        }
    }
}
