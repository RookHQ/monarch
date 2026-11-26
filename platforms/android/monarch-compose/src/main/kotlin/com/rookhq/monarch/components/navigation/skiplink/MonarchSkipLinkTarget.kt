package com.rookhq.monarch.components.navigation.skiplink

import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Marks the element a [MonarchSkipLink] moves focus to, normally the first element of the main content: attaches
 * [requester] and makes the element focusable so it can take focus.
 *
 * @param requester the requester shared with the skip link.
 */
public fun Modifier.monarchSkipLinkTarget(requester: FocusRequester): Modifier =
    this.focusRequester(requester).focusable()
