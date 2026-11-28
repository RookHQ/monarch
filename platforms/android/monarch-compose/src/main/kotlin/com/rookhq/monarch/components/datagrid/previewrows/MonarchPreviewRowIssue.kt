package com.rookhq.monarch.components.datagrid.previewrows

import androidx.compose.runtime.Immutable

/**
 * A problem with one row of a `MonarchPreviewRows`, optionally tied to a field.
 *
 * @property message what is wrong, for example "Not a valid email address".
 * @property field the label of the field, for example "Email". Shown before the message.
 */
@Immutable
public class MonarchPreviewRowIssue(
    public val message: String,
    public val field: String? = null,
)
