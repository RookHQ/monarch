/// Built-in strings of the avatar, from the `MonarchAvatar` strings table.
enum MonarchAvatarStrings {
    /// The spoken name of an avatar with a presence.
    static func withStatus(name: String, status: String) -> String {
        MonarchStrings.string("monarch.avatar.withStatus", table: "MonarchAvatar", name, status)
    }

    /// The spoken name of the tile that stands for the hidden avatars of a group.
    static func more(_ count: Int) -> String {
        MonarchStrings.string("monarch.avatar.more", table: "MonarchAvatar", count)
    }

    /// The spoken name of a group without a label: "3 people", by the plural rules of the locale.
    static func groupSummary(_ count: Int) -> String {
        MonarchStrings.plural("monarch.avatar.groupSummary", count)
    }

    /// The visible text of the tile that stands for the hidden avatars of a group.
    static func moreShort(_ count: Int) -> String {
        MonarchStrings.string("monarch.avatar.moreShort", table: "MonarchAvatar", count)
    }
}
