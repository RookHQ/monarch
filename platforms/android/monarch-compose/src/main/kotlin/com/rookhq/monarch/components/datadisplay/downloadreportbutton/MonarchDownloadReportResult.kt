package com.rookhq.monarch.components.datadisplay.downloadreportbutton

import java.io.OutputStream

/** What `onDownload` of a `MonarchDownloadReportButton` returns when the file is ready. */
public sealed class MonarchDownloadReportResult {
    /** The app saved or shared the file itself. "CSV downloaded" is announced. */
    public data object Downloaded : MonarchDownloadReportResult()

    /**
     * The file is prepared in the background, for example sent by email later. A note stays under the button.
     *
     * @property message the note; `null` uses the built-in one.
     */
    public data class Queued(val message: String? = null) : MonarchDownloadReportResult()

    /**
     * A file to save: the system save dialog opens with [fileName], and [write]
     * fills the file the person chose. A cancelled dialog returns to idle
     * without a message.
     *
     * @property fileName the suggested file name, for example "invoices-2026-11.csv".
     * @property write writes the file's bytes to the stream, on a background thread. Monarch closes the stream.
     */
    public class File(
        public val fileName: String,
        public val write: suspend (OutputStream) -> Unit,
    ) : MonarchDownloadReportResult()
}
