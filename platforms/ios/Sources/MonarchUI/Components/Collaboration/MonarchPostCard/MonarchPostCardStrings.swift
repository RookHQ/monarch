/// Built-in strings of the post card, from the `MonarchPostCard` strings table.
enum MonarchPostCardStrings {
    private static let table = "MonarchPostCard"

    /// The pinned marker.
    static var pinned: String { MonarchStrings.string("monarch.postCard.pinned", table: table) }

    /// The answered badge.
    static var answered: String { MonarchStrings.string("monarch.postCard.answered", table: table) }

    /// The spoken reaction count.
    static func reactions(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.postCard.reactionsOne", table: table)
            : MonarchStrings.string("monarch.postCard.reactionsOther", table: table, count)
    }

    /// The spoken comment count.
    static func comments(_ count: Int) -> String {
        count == 1
            ? MonarchStrings.string("monarch.postCard.commentsOne", table: table)
            : MonarchStrings.string("monarch.postCard.commentsOther", table: table, count)
    }
}
