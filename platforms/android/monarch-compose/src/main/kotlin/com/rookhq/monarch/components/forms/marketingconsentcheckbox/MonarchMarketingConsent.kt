package com.rookhq.monarch.components.forms.marketingconsentcheckbox

import androidx.compose.runtime.Immutable

/**
 * The person's choice, with the wording they saw, for proof of consent.
 *
 * @property channels one entry per channel shown: `true` when ticked.
 * @property text the exact wording shown (the question, the channel labels, the description and the notes), with
 * whitespace collapsed.
 * @property at when the choice changed, in epoch milliseconds (UTC).
 */
@Immutable
public class MonarchMarketingConsent(
    public val channels: Map<MonarchMarketingChannel, Boolean>,
    public val text: String,
    public val at: Long,
) {
    /** The channels the person agreed to. */
    public val granted: Set<MonarchMarketingChannel> get() = channels.filterValues { it }.keys.toSet()

    override fun equals(other: Any?): Boolean =
        other is MonarchMarketingConsent && other.channels == channels && other.text == text && other.at == at

    override fun hashCode(): Int = (channels.hashCode() * HASH_PRIME + text.hashCode()) * HASH_PRIME + at.hashCode()

    override fun toString(): String = "MonarchMarketingConsent(channels=$channels, text=$text, at=$at)"

    private companion object {
        const val HASH_PRIME = 31
    }
}
