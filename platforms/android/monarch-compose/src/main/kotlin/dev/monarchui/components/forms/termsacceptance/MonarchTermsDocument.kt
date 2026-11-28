package dev.monarchui.components.forms.termsacceptance

import androidx.compose.runtime.Immutable

/**
 * A document the person agrees to in a `MonarchTermsAcceptance`, such as the
 * terms of service or the privacy policy.
 *
 * @property title the document's name, already localised, such as "Terms of Service". It is the link text.
 * @property url where the document is published.
 * @property version the version the person sees, such as "2026-11-01". Reported when the box changes.
 * @property id a stable key for the reported versions. `null` uses [url].
 */
@Immutable
public class MonarchTermsDocument(
    public val title: String,
    public val url: String,
    public val version: String? = null,
    public val id: String? = null,
) {
    /** The key of this document in the reported versions: [id], or [url] when there is none. */
    public val key: String get() = id ?: url

    /** Versions of a set of documents. */
    public companion object {
        /**
         * The version of each document that has one, keyed by [key].
         * Documents without a version are left out.
         *
         * @param documents the documents.
         */
        public fun versions(documents: List<MonarchTermsDocument>): Map<String, String> =
            documents.mapNotNull { document -> document.version?.let { document.key to it } }.toMap()
    }
}
