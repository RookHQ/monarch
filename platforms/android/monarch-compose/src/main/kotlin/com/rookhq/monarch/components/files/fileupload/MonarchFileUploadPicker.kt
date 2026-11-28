package com.rookhq.monarch.components.files.fileupload

/** Which system picker a `MonarchFileUpload` opens. */
public enum class MonarchFileUploadPicker {
    /** The document picker (`OpenMultipleDocuments`): any file the providers offer, filtered by `accept`. */
    Documents,

    /** The photo picker (`PickMultipleVisualMedia`): photos and videos, without a storage permission. */
    Media,
}
