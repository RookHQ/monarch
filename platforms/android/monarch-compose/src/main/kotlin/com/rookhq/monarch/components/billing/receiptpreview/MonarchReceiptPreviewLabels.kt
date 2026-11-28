package com.rookhq.monarch.components.billing.receiptpreview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchReceiptPreview`. Every
 * `null` uses the built-in string named after it in `strings_receiptpreview.xml`.
 *
 * @property view the button: "View receipt".
 * @property none shown instead of the button when there is no receipt: "No receipt".
 * @property loading the state TalkBack reads while the receipt loads: "Loading receipt".
 * @property loadError the message when loading fails: "The receipt could not be loaded.".
 * @property retry the retry button after a failure. `null` uses the built-in "Try again" of
 * `MonarchInlineErrorRetry`.
 * @property title the heading of the full-screen preview: "Receipt".
 * @property close the button that closes the preview: "Close".
 */
@Immutable
public class MonarchReceiptPreviewLabels(
    public val view: String? = null,
    public val none: String? = null,
    public val loading: String? = null,
    public val loadError: String? = null,
    public val retry: String? = null,
    public val title: String? = null,
    public val close: String? = null,
)
