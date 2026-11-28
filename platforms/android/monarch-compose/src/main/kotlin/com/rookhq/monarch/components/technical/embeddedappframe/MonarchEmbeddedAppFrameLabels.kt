package com.rookhq.monarch.components.technical.embeddedappframe

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEmbeddedAppFrame`. Every `null` uses the built-in string
 * named after it in `strings_embeddedappframe.xml`.
 *
 * @property loading the text while loading: "Loading…".
 * @property errorTitle the title of the error: "This page didn’t open".
 * @property errorDescription the explanation of the error.
 * @property retry the retry button: "Try again".
 * @property open the button that opens the page outside the frame: "Open the page".
 */
@Immutable
public class MonarchEmbeddedAppFrameLabels(
    public val loading: String? = null,
    public val errorTitle: String? = null,
    public val errorDescription: String? = null,
    public val retry: String? = null,
    public val open: String? = null,
)
