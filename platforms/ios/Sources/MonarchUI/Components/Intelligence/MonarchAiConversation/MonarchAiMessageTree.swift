/// The branching message tree behind ``MonarchAiConversation``: every
/// regenerate or edit adds a sibling, and the visible conversation is one
/// path through it.
struct MonarchAiMessageTree: Equatable {
    /// The id of the root, which is the parent of the first messages.
    static let root = ""

    /// Every message by id.
    private(set) var nodes: [String: MonarchAiConversationMessage] = [:]
    /// The parent of each message. A message under the root has no entry.
    private(set) var parent: [String: String] = [:]
    /// The children of each parent id, in the order they were added.
    private(set) var children: [String: [String]] = [:]
    /// The index of the shown child of each parent id.
    private(set) var active: [String: Int] = [:]

    /// A tree with one branch: the messages in order.
    init(_ messages: [MonarchAiConversationMessage] = []) {
        var previous = Self.root
        for var message in messages {
            // A stored conversation is one branch; positions are worked out from the tree.
            message.branch = nil
            nodes[message.id] = message
            if previous != Self.root { parent[message.id] = previous }
            children[previous, default: []].append(message.id)
            active[previous] = 0
            previous = message.id
        }
    }

    /// The parent id of a message, or the root.
    func parentId(of id: String) -> String {
        parent[id] ?? Self.root
    }

    /// Adds a message under a parent and shows it.
    mutating func addChild(_ message: MonarchAiConversationMessage, under parentId: String) {
        nodes[message.id] = message
        if parentId != Self.root { parent[message.id] = parentId }
        children[parentId, default: []].append(message.id)
        active[parentId] = (children[parentId]?.count ?? 1) - 1
    }

    /// Replaces one message. Does nothing when the message is not in the tree.
    mutating func replace(_ message: MonarchAiConversationMessage) {
        guard nodes[message.id] != nil else { return }
        nodes[message.id] = message
    }

    /// Shows another alternative of a message. Returns whether the index was valid.
    @discardableResult
    mutating func activate(index: Int, ofSiblingsOf id: String) -> Bool {
        let parentId = parentId(of: id)
        guard nodes[id] != nil, index >= 0, index < (children[parentId]?.count ?? 0) else { return false }
        active[parentId] = index
        return true
    }

    /// The ids on the visible path, oldest first.
    var visibleIds: [String] {
        var ids: [String] = []
        var current = Self.root
        while let siblings = children[current], !siblings.isEmpty {
            let id = siblings[min(max(active[current] ?? 0, 0), siblings.count - 1)]
            guard nodes[id] != nil else { break }
            ids.append(id)
            current = id
        }
        return ids
    }

    /// The visible conversation: the active child at every level, oldest
    /// first. Messages with alternatives carry `branch`.
    var visiblePath: [MonarchAiConversationMessage] {
        visibleIds.compactMap { id in
            guard var message = nodes[id] else { return nil }
            let siblings = children[parentId(of: id)] ?? []
            if siblings.count > 1, let index = siblings.firstIndex(of: id) {
                message.branch = MonarchAiBranchPosition(index: index, count: siblings.count)
            } else {
                message.branch = nil
            }
            return message
        }
    }

    /// The last id on the visible path, or the root.
    var lastVisibleId: String {
        visibleIds.last ?? Self.root
    }

    /// The ids of the ancestors of a message, nearest first.
    func ancestors(of id: String) -> [String] {
        var ids: [String] = []
        var current = parent[id]
        while let ancestor = current {
            ids.append(ancestor)
            current = parent[ancestor]
        }
        return ids
    }
}
