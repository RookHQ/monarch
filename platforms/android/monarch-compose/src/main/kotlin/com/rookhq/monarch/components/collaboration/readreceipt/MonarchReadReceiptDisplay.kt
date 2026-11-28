package com.rookhq.monarch.components.collaboration.readreceipt

/** What a `MonarchReadReceipt` shows. Every display gives TalkBack the full list of names. */
public enum class MonarchReadReceiptDisplay {
    /** "Seen", the usual choice in a one-to-one conversation. The default. */
    Text,

    /** "Seen by 3", for groups. */
    Count,

    /** The readers' avatars, then "+N" and "Seen". */
    Avatars,
}
