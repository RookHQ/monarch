package com.rookhq.monarch.components.files.filepreview

import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeFamily

/** How a `MonarchFilePreview` shows a file. The same six names exist on the web. */
public enum class MonarchFilePreviewKind {
    /** A picture, from the `image` painter. */
    Image,

    /** A PDF, rendered page by page with the platform `PdfRenderer`. */
    Pdf,

    /** Plain text or code, from the `text` string. */
    Text,

    /** A video, in the app's `player`. */
    Video,

    /** A sound file, in the app's `player`. */
    Audio,

    /** No preview: the file type icon, the name and a download action. */
    None,
    ;

    /** Works out a kind from a file name and a MIME type. */
    public companion object {
        /**
         * The kind for a file: images, PDFs, video and audio by their family,
         * text and code as text, anything else none.
         *
         * @param fileName the file name or path.
         * @param contentType the MIME type, which takes precedence over the extension when it is specific.
         */
        public fun of(fileName: String?, contentType: String? = null): MonarchFilePreviewKind =
            when (MonarchFileTypeFamily.of(fileName, contentType)) {
                MonarchFileTypeFamily.Image -> Image
                MonarchFileTypeFamily.Pdf -> Pdf
                MonarchFileTypeFamily.Video -> Video
                MonarchFileTypeFamily.Audio -> Audio
                MonarchFileTypeFamily.Text, MonarchFileTypeFamily.Code -> Text
                else -> None
            }
    }
}
