/// Where the tiles of a ``MonarchCallStage`` go: an even grid, or one main
/// tile with a filmstrip.
///
/// ``automatic(selfID:ids:mode:pinnedID:sharerID:activeSpeakerID:)`` is the
/// stage's own choice. It is pure, so a product can call it to decide how
/// much video to ask for from each person.
@_spi(Experimental) public enum MonarchCallStageLayout: Sendable, Hashable {
    /// Every participant, in order, in a grid with this many columns.
    case grid(ids: [String], columns: Int)
    /// One participant large, the others in a filmstrip.
    case spotlight(main: String, strip: [String])

    /// The grid columns for a number of tiles: one, two up to four people, then three.
    public static func gridColumns(for count: Int) -> Int {
        if count <= 1 { return 1 }
        if count <= 4 { return 2 }
        return 3
    }

    /// The automatic layout. A pin wins; then someone else's shared screen;
    /// then, in speaker mode, the active speaker (never yourself, the first
    /// other person when nobody speaks); then, in automatic mode with one
    /// other person, that person large. Otherwise a grid.
    ///
    /// - Parameters:
    ///   - selfID: Your id, if you are in the list.
    ///   - ids: Every participant's id in display order, yourself included.
    ///   - mode: The layout chosen in the call controls.
    ///   - pinnedID: The pinned participant.
    ///   - sharerID: Whoever shares a screen.
    ///   - activeSpeakerID: Who is talking.
    /// - Returns: A spotlight of the chosen person, or a grid of everyone.
    public static func automatic(
        selfID: String?,
        ids: [String],
        mode: MonarchCallControlsLayout,
        pinnedID: String? = nil,
        sharerID: String? = nil,
        activeSpeakerID: String? = nil
    ) -> MonarchCallStageLayout {
        let others = ids.filter { $0 != selfID }
        var main: String?
        if let pinnedID, ids.contains(pinnedID) {
            main = pinnedID
        } else if let sharerID, sharerID != selfID, others.contains(sharerID) {
            main = sharerID
        } else if mode == .speaker, let first = others.first {
            main = activeSpeakerID.flatMap { others.contains($0) ? $0 : nil } ?? first
        } else if mode == .automatic, others.count == 1 {
            main = others.first
        }
        if let main {
            return .spotlight(main: main, strip: ids.filter { $0 != main })
        }
        return .grid(ids: ids, columns: gridColumns(for: ids.count))
    }
}
