package com.rookhq.monarch.components.technical.dnsrecordtable

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDnsRecordTable`. Every `null` uses the built-in string named
 * after it in `strings_dnsrecordtable.xml`.
 *
 * @property type the heading of the record type: "Type".
 * @property name the heading of the record name: "Name".
 * @property value the heading of the record value: "Value".
 * @property ttl the heading of the TTL: "TTL".
 * @property priority the heading of the priority: "Priority".
 * @property status the heading of the status column of the table: "Status".
 * @property found the status of a record public DNS shows: "Found".
 * @property waiting the status of a record public DNS does not show yet: "Waiting".
 * @property purpose the heading of a card, from its purpose (`null` when the record has none): "Points your address
 * here", "Proves you own the domain", "Allows the HTTPS certificate", or the custom heading.
 * @property copyName the spoken name of a record's name and its copy button, from the record and the name as shown:
 * "name of the CNAME record".
 * @property copyValue the spoken name of a record's value and its copy button, from the record and the name as shown:
 * "value of the CNAME record book".
 * @property fullName the line under a card whose name is shown relative to the zone: "Full name: book.example.com".
 * @property label the spoken name of the records, from the zone: "DNS records for example.com", or "DNS records".
 * @property summary announced when the number of found records changes, from the found and checked counts: "1 of 2
 * records found."
 */
@Immutable
public class MonarchDnsRecordTableLabels(
    public val type: String? = null,
    public val name: String? = null,
    public val value: String? = null,
    public val ttl: String? = null,
    public val priority: String? = null,
    public val status: String? = null,
    public val found: String? = null,
    public val waiting: String? = null,
    public val purpose: ((purpose: MonarchDnsRecordPurpose?) -> String)? = null,
    public val copyName: ((record: MonarchDnsRecord, shownName: String) -> String)? = null,
    public val copyValue: ((record: MonarchDnsRecord, shownName: String) -> String)? = null,
    public val fullName: ((name: String) -> String)? = null,
    public val label: ((zone: String?) -> String)? = null,
    public val summary: ((found: Int, checked: Int) -> String)? = null,
)
