/// Built-in strings of ``MonarchCallControls``, from the
/// `MonarchCallControls` strings table.
enum MonarchCallControlsStrings {
    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.callControls." + key, table: "MonarchCallControls")
    }

    /// The name of the group.
    static var toolbar: String { string("toolbar") }
    /// The microphone toggle.
    static var microphone: String { string("microphone") }
    /// The camera toggle.
    static var camera: String { string("camera") }
    /// The screen sharing toggle.
    static var shareScreen: String { string("shareScreen") }
    /// The raised hand toggle.
    static var raiseHand: String { string("raiseHand") }
    /// The reactions menu.
    static var react: String { string("react") }
    /// One reaction: "React 👍".
    static func reactWith(_ emoji: String) -> String {
        MonarchStrings.string("monarch.callControls.reactWith", table: "MonarchCallControls", emoji)
    }
    /// The More menu.
    static var more: String { string("more") }
    /// The layout choice.
    static var layout: String { string("layout") }
    /// The name of a layout.
    static func layoutName(_ layout: MonarchCallControlsLayout) -> String {
        switch layout {
        case .automatic: string("layout.automatic")
        case .grid: string("layout.grid")
        case .speaker: string("layout.speaker")
        }
    }
    /// The low data toggle.
    static var lowData: String { string("lowData") }
    /// The device settings item.
    static var devices: String { string("devices") }
    /// The connection details item.
    static var stats: String { string("stats") }
    /// The picture in picture item.
    static var pictureInPicture: String { string("pictureInPicture") }
    /// The leave button.
    static var leave: String { string("leave") }
}
