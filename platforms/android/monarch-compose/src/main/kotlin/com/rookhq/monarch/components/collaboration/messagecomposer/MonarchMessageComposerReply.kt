package com.rookhq.monarch.components.collaboration.messagecomposer

import androidx.compose.runtime.Immutable

/**
 * The message a `MonarchMessageComposer` answers, shown above the text.
 *
 * @property author who wrote it, read as "Replying to Ana Ruiz".
 * @property excerpt a short quote of it, cut to one line.
 */
@Immutable
public class MonarchMessageComposerReply(
    public val author: String,
    public val excerpt: String? = null,
)
