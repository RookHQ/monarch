package dev.monarchui.components.technical.domainprogress

/** One of the three steps `MonarchDomainProgress` shows: DNS record found, HTTPS certificate issued, live. */
public enum class MonarchDomainStep {
    /** The DNS record (routing or ownership) is found. */
    Dns,

    /** The HTTPS certificate is issued. */
    Certificate,

    /** The site loads on the domain. */
    Live,
}
