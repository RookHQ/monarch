package com.rookhq.monarch.components.files.filetypeicon

/**
 * The family of a file, as drawn by `MonarchFileTypeIcon`. The same eleven
 * names exist on every platform, and a file gets the same family everywhere
 * because the extension and MIME tables are the web's.
 */
public enum class MonarchFileTypeFamily {
    /** A picture: PNG, JPEG, GIF, WebP, SVG, HEIC and so on. */
    Image,

    /** A video: MP4, MOV, WebM, MKV and so on. */
    Video,

    /** A sound file: MP3, WAV, M4A, FLAC and so on. */
    Audio,

    /** A PDF document. */
    Pdf,

    /** A word-processing document: DOC, DOCX, ODT, RTF, Pages. */
    Document,

    /** A spreadsheet: XLS, XLSX, ODS, CSV, TSV, Numbers. */
    Spreadsheet,

    /** A slide deck: PPT, PPTX, ODP, Keynote. */
    Presentation,

    /** A compressed archive: ZIP, RAR, 7Z, TAR, GZ and so on. */
    Archive,

    /** Source code or structured data: JS, TS, JSON, HTML, Kotlin, YAML and so on. */
    Code,

    /** Plain text: TXT, Markdown, logs. */
    Text,

    /** Any other file. */
    Other,
    ;

    /** Works out a family from a file name and a MIME type. */
    public companion object {
        /**
         * The family of a file. A specific MIME type wins; an empty type or
         * `application/octet-stream` falls back to the extension of
         * [fileName]. An extension that the shared table does not list is
         * looked up in the platform `MimeTypeMap`.
         *
         * @param fileName the file name or path, for example `"report.pdf"`.
         * @param contentType the MIME type, for example from `ContentResolver.getType`.
         */
        public fun of(fileName: String?, contentType: String? = null): MonarchFileTypeFamily =
            MonarchFileTypeRules.family(fileName, contentType)
    }
}
