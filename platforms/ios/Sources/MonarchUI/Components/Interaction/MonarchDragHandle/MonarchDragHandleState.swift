/// Where an item is in a drag.
public enum MonarchDragHandleState: Sendable, Hashable, CaseIterable {
    /// At rest.
    case idle
    /// Picked up and moving.
    case grabbed
    /// Just dropped. Show it briefly as a confirmation, then return to `idle`.
    case dropped
}
