package com.rookhq.monarch.components.technical.domainprogress

/** Where a custom domain is in its connection, as reported by the product. Shown by `MonarchDomainProgress`. */
public enum class MonarchDomainStage {
    /** Waiting for the routing record. */
    Dns,

    /** Waiting for a verification record. */
    Ownership,

    /** Issuing the HTTPS certificate. */
    Certificate,

    /** Checking that the site loads. */
    Page,

    /** Connected. */
    Live,
}
