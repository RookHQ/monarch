package com.rookhq.monarch.components.intelligence.aimentioncontext

import com.rookhq.monarch.components.intelligence.aicontextchips.MonarchAiContextKind

/** What an @ mention in an AI prompt points at. */
public enum class MonarchAiMentionKind {
    /** A document or file. */
    Doc,

    /** A person. */
    Person,

    /** A record, such as a deal or a ticket. */
    Record,

    /** A connected app. */
    App,

    ;

    /** The kind of context chip the mention becomes: a document becomes a file. */
    public val contextKind: MonarchAiContextKind
        get() = when (this) {
            Doc -> MonarchAiContextKind.File
            Person -> MonarchAiContextKind.Person
            Record -> MonarchAiContextKind.Record
            App -> MonarchAiContextKind.App
        }
}
