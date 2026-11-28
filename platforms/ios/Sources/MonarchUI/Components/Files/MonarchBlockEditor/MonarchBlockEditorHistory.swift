/// The undo and redo history of ``MonarchBlockEditor``: whole-document
/// snapshots, with typing in one block kept as one step.
struct MonarchBlockEditorHistory: Equatable {
    /// The most steps kept; the oldest are dropped.
    static let limit = 100

    private(set) var undoStack: [[MonarchBlock]] = []
    private(set) var redoStack: [[MonarchBlock]] = []
    private var lastKey: String?

    /// Whether there is a step to undo.
    var canUndo: Bool { !undoStack.isEmpty }
    /// Whether there is a step to redo.
    var canRedo: Bool { !redoStack.isEmpty }

    /// Records the document before a change. A change with the same
    /// coalescing key as the previous one (more typing in the same block)
    /// adds no step. Any new change clears redo.
    mutating func record(_ before: [MonarchBlock], key: String?) {
        redoStack.removeAll()
        if let key, key == lastKey, !undoStack.isEmpty { return }
        undoStack.append(before)
        if undoStack.count > Self.limit { undoStack.removeFirst(undoStack.count - Self.limit) }
        lastKey = key
    }

    /// Ends the current typing step, for example when focus moves to another block.
    mutating func breakCoalescing() {
        lastKey = nil
    }

    /// The document to show for undo, or `nil`. `current` becomes redoable.
    mutating func undo(_ current: [MonarchBlock]) -> [MonarchBlock]? {
        guard let previous = undoStack.popLast() else { return nil }
        redoStack.append(current)
        lastKey = nil
        return previous
    }

    /// The document to show for redo, or `nil`. `current` becomes undoable.
    mutating func redo(_ current: [MonarchBlock]) -> [MonarchBlock]? {
        guard let next = redoStack.popLast() else { return nil }
        undoStack.append(current)
        lastKey = nil
        return next
    }
}
