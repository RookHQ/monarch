package com.rookhq.monarch.components.intelligence.aitoolresultcard

/**
 * What a tool did. Picks the label above the title of a `MonarchAiToolResultCard`.
 * The web and iOS use the same five.
 */
public enum class MonarchAiToolResultKind {
    /** A record was created: "Record created". */
    RecordCreated,

    /** An email was sent: "Email sent". */
    EmailSent,

    /** An event was booked: "Event booked". */
    EventBooked,

    /** A ticket was filed: "Ticket filed". */
    TicketFiled,

    /** Anything else: "Done". The default. */
    Generic,
}
