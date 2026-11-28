/// How many items of a row or stack are shown before a "+N" item: avatar
/// groups, presence avatars, rosters, chip rows.
enum MonarchOverflow {
    /// How many items show. A "+1" item would take the space of the item it
    /// hides, so that item is shown instead. `nil` shows every item.
    static func visibleCount(total: Int, maxVisible: Int?) -> Int {
        guard let maxVisible else { return total }
        let limit = max(0, maxVisible)
        return total - limit == 1 ? total : min(total, limit)
    }
}
