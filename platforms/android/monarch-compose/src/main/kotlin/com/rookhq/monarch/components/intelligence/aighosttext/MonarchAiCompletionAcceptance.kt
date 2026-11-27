package com.rookhq.monarch.components.intelligence.aighosttext

/**
 * What accepting a suggestion, or one word of it, changes.
 *
 * @property inserted the text that goes in at the cursor.
 * @property text the whole text after the insertion.
 * @property cursor the cursor after the insertion, as a UTF-16 offset.
 */
public data class MonarchAiCompletionAcceptance(
    public val inserted: String,
    public val text: String,
    public val cursor: Int,
)
