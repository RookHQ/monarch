package dev.monarchui.components.identity.dataexportrequest

import androidx.compose.runtime.Immutable

/**
 * One kind of data the person can choose to include in an export.
 *
 * @property id the stable id passed in [MonarchDataExportRequestDetails.scope], such as `messages`.
 * @property label the short name, such as "Messages and comments". Already localised.
 * @property description one line on what it contains.
 */
@Immutable
public class MonarchDataExportScopeOption(
    public val id: String,
    public val label: String,
    public val description: String? = null,
)
