package com.rookhq.monarch.components.search.searchresultitem

/** The most words a query highlights, as on the web. */
private const val MaxTerms = 8

private val Word = Regex("[\\p{L}\\p{N}]+")

/**
 * Splits what the user typed into the words to highlight in a
 * `MonarchSearchResultItem`: letters and digits only, in lower case, each
 * word once, at most eight. The same rule as the web `queryTerms`.
 *
 * @param query the text of the search field.
 */
public fun monarchQueryTerms(query: String): List<String> =
    Word.findAll(query.lowercase()).map { it.value }.distinct().take(MaxTerms).toList()
