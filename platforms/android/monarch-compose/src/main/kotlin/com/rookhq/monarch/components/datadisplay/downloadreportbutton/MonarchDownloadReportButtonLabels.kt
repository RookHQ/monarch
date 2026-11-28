package com.rookhq.monarch.components.datadisplay.downloadreportbutton

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDownloadReportButton`.
 * Every `null` keeps the built-in string `monarch_downloadreportbutton_<key>`.
 *
 * @property label the button text with several formats.
 * @property single the button text with one format, given its label: "Download CSV".
 * @property preparing the state of the button and the announcement while a file is prepared, given the format.
 * @property downloaded announced when the file is saved, given the format.
 * @property queued the note when the file is prepared in the background.
 * @property error the failure text, given the format.
 * @property progress the name of the progress bar.
 */
@Immutable
public class MonarchDownloadReportButtonLabels(
    public val label: String? = null,
    public val single: ((format: String) -> String)? = null,
    public val preparing: ((format: String) -> String)? = null,
    public val downloaded: ((format: String) -> String)? = null,
    public val queued: String? = null,
    public val error: ((format: String) -> String)? = null,
    public val progress: String? = null,
)
