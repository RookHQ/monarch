import Foundation

/// A block of time a ``MonarchTimeGrid`` draws hatched and labelled, such as
/// time off or a closure.
///
/// ```swift
/// MonarchTimeGridUnavailable(id: "lunch", start: noon, end: oneOClock, label: "Closed")
/// ```
public struct MonarchTimeGridUnavailable: Identifiable, Hashable, Sendable {
    /// The identity of the block, unique in the grid.
    public let id: String
    /// When the block starts.
    public var start: Date
    /// When the block ends.
    public var end: Date
    /// The text shown in the block and read by VoiceOver, already localised.
    public var label: String?

    /// An unavailable block.
    ///
    /// - Parameters:
    ///   - id: The identity of the block, unique in the grid.
    ///   - start: When the block starts.
    ///   - end: When the block ends. A block over several days is drawn on each.
    ///   - label: The text shown in the block, already localised.
    public init(id: String, start: Date, end: Date, label: String? = nil) {
        self.id = id
        self.start = start
        self.end = end
        self.label = label
    }
}
