import Foundation

/// One update posted during an incident, shown by ``MonarchIncidentTimeline``.
public struct MonarchIncidentUpdate: Identifiable, Sendable, Hashable {
    /// A stable identifier.
    public let id: String
    /// The stage key: a ``MonarchIncidentStage`` raw value or a product stage.
    public var stage: String
    /// The update text. Line breaks are kept. Already localised.
    public var body: String
    /// When it was posted.
    public var time: Date
    /// Whether the system posted it rather than a person. Tagged "Automated".
    public var isAutomated: Bool
    /// Who posted it, when the product shows authors.
    public var author: String?

    /// An update with a stage key, standard or product-defined.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - stage: The stage key, such as `"investigating"` or a product stage.
    ///   - body: The update text.
    ///   - time: When it was posted.
    ///   - isAutomated: Whether the system posted it.
    ///   - author: Who posted it.
    public init(
        id: String,
        stage: String,
        body: String,
        time: Date,
        isAutomated: Bool = false,
        author: String? = nil
    ) {
        self.id = id
        self.stage = stage
        self.body = body
        self.time = time
        self.isAutomated = isAutomated
        self.author = author
    }

    /// An update with a standard stage.
    ///
    /// - Parameters:
    ///   - id: A stable identifier.
    ///   - stage: The standard stage.
    ///   - body: The update text.
    ///   - time: When it was posted.
    ///   - isAutomated: Whether the system posted it.
    ///   - author: Who posted it.
    public init(
        id: String,
        stage: MonarchIncidentStage,
        body: String,
        time: Date,
        isAutomated: Bool = false,
        author: String? = nil
    ) {
        self.init(id: id, stage: stage.rawValue, body: body, time: time, isAutomated: isAutomated, author: author)
    }
}
