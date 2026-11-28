package dev.monarchui.components.technical.dnsrecordtable

import androidx.compose.runtime.Immutable

/**
 * One DNS record the user has to create at their DNS provider, shown by `MonarchDnsRecordTable`.
 *
 * @property type the record type: `CNAME`, `TXT`, `A`, `AAAA`, `MX`, `CAA`.
 * @property name the full record name, such as `book.example.com`. Shown relative to the zone when the table has one.
 * @property value the record value or target. Always copied in full.
 * @property purpose what the record is for, shown as the heading of its card. `null` uses the verification heading.
 * @property ttl the TTL to set, as the provider writes it ("3600", "Auto").
 * @property priority the MX or SRV priority.
 * @property found whether public DNS already shows the record: `true` is Found, `false` is Waiting, `null` when the
 * app has not checked.
 * @property id a stable identifier. By default the type, name and value joined.
 */
@Immutable
public class MonarchDnsRecord(
    public val type: String,
    public val name: String,
    public val value: String,
    public val purpose: MonarchDnsRecordPurpose? = null,
    public val ttl: String? = null,
    public val priority: Int? = null,
    public val found: Boolean? = null,
    public val id: String = "$type:$name:$value",
)
