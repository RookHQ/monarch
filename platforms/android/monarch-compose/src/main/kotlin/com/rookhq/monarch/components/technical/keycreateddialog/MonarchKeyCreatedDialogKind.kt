package com.rookhq.monarch.components.technical.keycreateddialog

/** What a `MonarchKeyCreatedDialog` shows: a secret seen only once, or a value that can be viewed again. */
public enum class MonarchKeyCreatedDialogKind {
    /** Shown once: a warning notice, and the dialog cannot close until the person confirms they saved it. */
    Secret,

    /** Meant to be public, such as a publishable key: an information notice and nothing to confirm. */
    Public,
}
