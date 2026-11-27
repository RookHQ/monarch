/// Built-in strings of the link card, from the `MonarchLinkCard` strings table.
enum MonarchLinkCardStrings {
    /// The spoken name of the remove button, for a link title.
    static func dismiss(_ title: String) -> String {
        MonarchStrings.string("monarch.linkCard.dismiss", table: "MonarchLinkCard", title)
    }

    /// Added to the spoken name of a link to a video.
    static var video: String { MonarchStrings.string("monarch.linkCard.video", table: "MonarchLinkCard") }
}
