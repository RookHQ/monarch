/// Built-in strings of the call stage, from the `MonarchCallStage` strings table.
enum MonarchCallStageStrings {
    private static let table = "MonarchCallStage"
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.callStage.\(key)", table: table)
    }

    /// The name of the stage without a title.
    static var region: String { string("region") }
    /// The spoken name of the timer.
    static var duration: String { string("duration") }
    /// Shown while you are alone.
    static var waiting: String { string("waiting") }
    /// The low data badge.
    static var lowData: String { string("lowData") }
    /// The reconnecting notice.
    static var reconnecting: String { string("reconnecting") }
    /// The hint when you talk while muted.
    static var mutedHint: String { string("mutedHint") }
    /// The picture in picture button.
    static var pictureInPicture: String { string("pictureInPicture") }
    /// The shrink button.
    static var shrink: String { string("shrink") }
    /// The expand button.
    static var expand: String { string("expand") }
    /// The minimize button.
    static var minimize: String { string("minimize") }

    /// The head count.
    static func inCall(_ count: String) -> String {
        MonarchStrings.string("monarch.callStage.inCall", table: table, count)
    }
}
