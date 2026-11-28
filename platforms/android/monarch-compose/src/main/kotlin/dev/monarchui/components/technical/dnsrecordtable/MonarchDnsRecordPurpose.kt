package dev.monarchui.components.technical.dnsrecordtable

import androidx.compose.runtime.Immutable

/**
 * What a DNS record in a `MonarchDnsRecordTable` is for. The three common purposes have built-in headings; any other
 * is shown as given.
 */
@Immutable
public sealed interface MonarchDnsRecordPurpose {
    /** Points the domain at the product: "Points your address here". */
    public data object Routing : MonarchDnsRecordPurpose

    /** Proves the user owns the domain: "Proves you own the domain". */
    public data object Verification : MonarchDnsRecordPurpose

    /** Allows the HTTPS certificate to be issued: "Allows the HTTPS certificate". */
    public data object Certificate : MonarchDnsRecordPurpose

    /**
     * Any other purpose, with the heading to show.
     *
     * @property heading the heading of the card, already localised.
     */
    public data class Custom(public val heading: String) : MonarchDnsRecordPurpose
}
