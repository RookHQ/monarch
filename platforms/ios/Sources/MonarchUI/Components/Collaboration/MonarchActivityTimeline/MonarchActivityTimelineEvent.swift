import Foundation

/// One event of a ``MonarchActivityTimeline``: who did what, to what, and when.
public struct MonarchActivityTimelineEvent: Sendable, Hashable, Identifiable {
    /// A stable identity.
    public let id: String
    /// Who did it, or `nil` for an event without an actor ("Ticket created from email").
    public let actor: MonarchActivityTimelineActor?
    /// What they did, already localised, such as "changed the status of".
    public let verb: String
    /// What it was done to, such as a record name or "Open → Closed".
    public let object: String?
    /// When it happened.
    public let time: Date
    /// A decorative SF Symbol drawn as the marker instead of the dot.
    public let systemImage: String?
    /// The marker's tone.
    public let tone: MonarchTone
    /// Tags after the time, such as a source or a stage.
    public let tags: [String]
    /// Text always shown under the sentence, such as a comment.
    public let body: String?
    /// Text behind a Details disclosure, such as a field change or a payload.
    public let details: String?
    /// The leading part of ``object`` drawn as a link, such as the record name in "Pricing page refresh to In review".
    public let link: String?

    /// An event.
    ///
    /// - Parameters:
    ///   - id: A stable identity.
    ///   - actor: Who did it, or `nil`.
    ///   - verb: What they did, already localised.
    ///   - object: What it was done to.
    ///   - time: When it happened.
    ///   - systemImage: A decorative SF Symbol drawn as the marker.
    ///   - tone: The marker's tone.
    ///   - tags: Tags after the time.
    ///   - body: Text always shown under the sentence.
    ///   - details: Text behind a Details disclosure.
    public init(
        id: String,
        actor: MonarchActivityTimelineActor? = nil,
        verb: String,
        object: String? = nil,
        time: Date,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        tags: [String] = [],
        body: String? = nil,
        details: String? = nil
    ) {
        self.id = id
        self.actor = actor
        self.verb = verb
        self.object = object
        self.time = time
        self.systemImage = systemImage
        self.tone = tone
        self.tags = tags
        self.body = body
        self.details = details
        self.link = nil
    }

    /// An event whose object starts with a link.
    ///
    /// - Parameters:
    ///   - id: A stable identity.
    ///   - actor: Who did it, or `nil`.
    ///   - verb: What they did, already localised.
    ///   - object: What it was done to, such as "Pricing page refresh to In review".
    ///   - link: The leading part of `object` drawn as a link, such as "Pricing page refresh".
    ///   - time: When it happened.
    ///   - systemImage: A decorative SF Symbol drawn as the marker.
    ///   - tone: The marker's tone.
    ///   - tags: Tags after the time.
    ///   - body: Text always shown under the sentence.
    ///   - details: Text behind a Details disclosure; a tab inside a line separates a label from its value.
    public init(
        id: String,
        actor: MonarchActivityTimelineActor? = nil,
        verb: String,
        object: String?,
        link: String?,
        time: Date,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        tags: [String] = [],
        body: String? = nil,
        details: String? = nil
    ) {
        self.id = id
        self.actor = actor
        self.verb = verb
        self.object = object
        self.time = time
        self.systemImage = systemImage
        self.tone = tone
        self.tags = tags
        self.body = body
        self.details = details
        self.link = link
    }
}
