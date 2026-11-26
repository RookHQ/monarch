package com.rookhq.monarch.components.files.fileupload

/** Why a `MonarchFileUpload` refused a file. */
public enum class MonarchFileRejectionCode {
    /** The type is not in `accept`. */
    InvalidType,

    /** The file is larger than `maxSize`. */
    TooLarge,

    /** Adding the file would go over `maxFiles`. */
    TooMany,

    /** The app's own `validate` refused it. */
    Custom,
}
