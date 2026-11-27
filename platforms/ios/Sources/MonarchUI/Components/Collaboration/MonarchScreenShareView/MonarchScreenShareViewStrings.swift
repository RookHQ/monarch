/// Built-in strings of the screen share view, from the
/// `MonarchScreenShareView` strings table.
enum MonarchScreenShareViewStrings {
    private static let table = "MonarchScreenShareView"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.screenShareView." + key, table: table)
    }

    /// The visible presenter line.
    static func presenting(_ name: String) -> String {
        MonarchStrings.string("monarch.screenShareView.presenting", table: table, name)
    }

    /// The spoken name of the shared screen.
    static func region(_ name: String) -> String {
        MonarchStrings.string("monarch.screenShareView.region", table: table, name)
    }

    /// The presenter line when the local person is sharing.
    static var youArePresenting: String { string("youArePresenting") }
    /// The spoken name of the group of controls.
    static var controls: String { string("controls") }
    /// Shown while there is no picture yet.
    static var waiting: String { string("waiting") }
    /// Shown instead of the picture when the local person is sharing.
    static var localPreviewHidden: String { string("localPreviewHidden") }
    /// The title of the button that shows the local picture anyway.
    static var showPreview: String { string("showPreview") }
    /// The title of the button that fits the whole screen.
    static var fit: String { string("fit") }
    /// The spoken name of the zoom-in button.
    static var zoomIn: String { string("zoomIn") }
    /// The spoken name of the zoom-out button.
    static var zoomOut: String { string("zoomOut") }
    /// The spoken name of the picture-in-picture button.
    static var pictureInPicture: String { string("pictureInPicture") }
    /// The spoken name of the button while picture in picture is active.
    static var exitPictureInPicture: String { string("exitPictureInPicture") }
    /// The spoken name of the full-screen button.
    static var fullScreen: String { string("fullScreen") }
    /// The spoken name of the button while full screen.
    static var exitFullScreen: String { string("exitFullScreen") }
    /// The title of the stop button.
    static var stopSharing: String { string("stopSharing") }
}
