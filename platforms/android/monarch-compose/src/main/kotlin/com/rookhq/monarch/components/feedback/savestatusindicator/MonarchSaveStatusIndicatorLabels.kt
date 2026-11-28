package com.rookhq.monarch.components.feedback.savestatusindicator

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchSaveStatusIndicator`.
 * Every `null` uses the built-in string named after it in
 * `strings_savestatusindicator.xml`.
 *
 * @property unsaved "Unsaved changes".
 * @property saving "Saving…".
 * @property saved "Saved".
 * @property error "Couldn't save", used when there is no error message.
 * @property retry the label of the retry button: "Retry".
 */
@Immutable
public class MonarchSaveStatusIndicatorLabels(
    public val unsaved: String? = null,
    public val saving: String? = null,
    public val saved: String? = null,
    public val error: String? = null,
    public val retry: String? = null,
)
