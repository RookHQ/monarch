package com.rookhq.monarch.components.datadisplay.downloadreportbutton

/**
 * A file format offered by `MonarchDownloadReportButton`.
 *
 * @property label the name shown and read, for example "CSV" or "Excel".
 * @property id the app's id of the format, for example "csv".
 * @property description a line under the label in the menu, for example "Opens in any spreadsheet".
 * @property mimeType the MIME type of the saved file, given to the system save dialog.
 */
public data class MonarchDownloadReportFormat(
    val label: String,
    val id: String,
    val description: String? = null,
    val mimeType: String = "application/octet-stream",
) {
    /** The formats most reports offer. */
    public companion object {
        /** Comma-separated values, the default format. */
        public val Csv: MonarchDownloadReportFormat = MonarchDownloadReportFormat("CSV", "csv", mimeType = "text/csv")
    }
}
