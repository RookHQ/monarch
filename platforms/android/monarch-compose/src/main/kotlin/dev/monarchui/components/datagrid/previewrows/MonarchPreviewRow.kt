package dev.monarchui.components.datagrid.previewrows

import androidx.compose.runtime.Immutable

/**
 * One row of an import file as the import will treat it, for a `MonarchPreviewRows`.
 *
 * @property row the 1-based number of the data row in the file; the header row is not counted.
 * @property values the cell values by column id. A missing or blank value shows a dash.
 * @property status overrides the status worked out from [errors] and [warnings], for example `Duplicate`.
 * @property errors the problems that stop the row from being imported.
 * @property warnings the problems that let the row be imported with changes.
 * @property note extra text under the problems, for example which record a duplicate matches.
 */
@Immutable
public class MonarchPreviewRow(
    public val row: Int,
    public val values: Map<String, String?>,
    public val status: MonarchPreviewRowStatus? = null,
    public val errors: List<MonarchPreviewRowIssue> = emptyList(),
    public val warnings: List<MonarchPreviewRowIssue> = emptyList(),
    public val note: String? = null,
) {
    /** What the import will do: [status] if set, else `Error` with errors, `Warning` with warnings, else `Ready`. */
    public val outcome: MonarchPreviewRowStatus
        get() = status ?: when {
            errors.isNotEmpty() -> MonarchPreviewRowStatus.Error
            warnings.isNotEmpty() -> MonarchPreviewRowStatus.Warning
            else -> MonarchPreviewRowStatus.Ready
        }
}
