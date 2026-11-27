package com.rookhq.monarch.components.collaboration.readreceipt

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchReadReceipt`. Every
 * `null` keeps the built-in string `monarch_readreceipt_<key>`.
 *
 * @property seen the visible text of the text and avatars displays, "Seen".
 * @property seenByCount the visible text of the count display, given the number of readers: "Seen by 3".
 * @property seenBy the full sentence TalkBack reads, given the joined names: "Seen by Ana and Ben".
 * @property list joins the names of the readers: "Ana", "Ana and Ben", "Ana, Ben and Chloé".
 */
@Immutable
public class MonarchReadReceiptLabels(
    public val seen: String? = null,
    public val seenByCount: ((Int) -> String)? = null,
    public val seenBy: ((String) -> String)? = null,
    public val list: ((List<String>) -> String)? = null,
)
