package com.rookhq.monarch.components.technical.domainprogress

import androidx.compose.runtime.Immutable

/**
 * What public DNS shows for a host right now, shown by `MonarchDomainProgress` as one line ("CNAME →
 * domains.example.net"). All lists empty means no record was found.
 *
 * @property cnames the CNAME targets found. The first one is shown.
 * @property a the A (IPv4) addresses found.
 * @property aaaa the AAAA (IPv6) addresses found.
 */
@Immutable
public class MonarchDomainDnsSnapshot(
    public val cnames: List<String> = emptyList(),
    public val a: List<String> = emptyList(),
    public val aaaa: List<String> = emptyList(),
)
