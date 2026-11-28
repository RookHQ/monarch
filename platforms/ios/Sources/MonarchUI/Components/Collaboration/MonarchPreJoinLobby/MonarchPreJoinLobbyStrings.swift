/// Built-in strings of the pre-join lobby, from the `MonarchPreJoinLobby` strings table.
enum MonarchPreJoinLobbyStrings {
    private static let table = "MonarchPreJoinLobby"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.preJoinLobby.\(key)", table: table)
    }

    /// The heading.
    static var title: String { string("title") }
    /// The name of the preview area.
    static var preview: String { string("preview") }
    /// The preview while the camera is off.
    static var cameraOff: String { string("cameraOff") }
    /// The preview while the camera starts.
    static var startingCamera: String { string("startingCamera") }
    /// The preview when there is a problem.
    static var noPreview: String { string("noPreview") }
    /// The name of the toggles.
    static var controls: String { string("controls") }
    /// The name of the level meter.
    static var micLevel: String { string("micLevel") }
    /// The cancel button.
    static var cancel: String { string("cancel") }
    /// The join button.
    static var join: String { string("join") }
    /// The join button while joining.
    static var joining: String { string("joining") }

    /// "3 already in the call" (English plural rule).
    static func alreadyIn(_ count: Int) -> String {
        let key = count == 1 ? "monarch.preJoinLobby.alreadyIn.one" : "monarch.preJoinLobby.alreadyIn.other"
        return MonarchStrings.string(key, table: table, count)
    }
}
