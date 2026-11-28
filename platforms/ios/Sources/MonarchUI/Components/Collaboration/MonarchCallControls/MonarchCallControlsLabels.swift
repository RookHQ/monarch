/// Replacements for the built-in strings of ``MonarchCallControls``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCallControlsLabels: Sendable {
    /// The spoken name of the group. Built in: "Call controls".
    public var toolbar: String?
    /// The microphone toggle. Built in: "Microphone".
    public var microphone: String?
    /// The camera toggle. Built in: "Camera".
    public var camera: String?
    /// The screen sharing toggle. Built in: "Share screen".
    public var shareScreen: String?
    /// The raised hand toggle. Built in: "Raise hand".
    public var raiseHand: String?
    /// The reactions menu. Built in: "Send a reaction".
    public var react: String?
    /// The spoken name of one reaction. Receives the emoji. Built in: "React 👍".
    public var reactWith: (@Sendable (String) -> String)?
    /// The More menu. Built in: "More options".
    public var more: String?
    /// The layout choice. Built in: "Layout".
    public var layout: String?
    /// The name of a layout. Built in: "Automatic", "Grid", "Speaker".
    public var layoutName: (@Sendable (MonarchCallControlsLayout) -> String)?
    /// The low data toggle. Built in: "Low data mode (audio only)".
    public var lowData: String?
    /// The device settings item. Built in: "Devices".
    public var devices: String?
    /// The connection details item. Built in: "Connection and data use".
    public var stats: String?
    /// The picture in picture item. Built in: "Picture in picture".
    public var pictureInPicture: String?
    /// The leave button. Built in: "Leave call".
    public var leave: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        toolbar: String? = nil,
        microphone: String? = nil,
        camera: String? = nil,
        shareScreen: String? = nil,
        raiseHand: String? = nil,
        react: String? = nil,
        reactWith: (@Sendable (String) -> String)? = nil,
        more: String? = nil,
        layout: String? = nil,
        layoutName: (@Sendable (MonarchCallControlsLayout) -> String)? = nil,
        lowData: String? = nil,
        devices: String? = nil,
        stats: String? = nil,
        pictureInPicture: String? = nil,
        leave: String? = nil
    ) {
        self.toolbar = toolbar
        self.microphone = microphone
        self.camera = camera
        self.shareScreen = shareScreen
        self.raiseHand = raiseHand
        self.react = react
        self.reactWith = reactWith
        self.more = more
        self.layout = layout
        self.layoutName = layoutName
        self.lowData = lowData
        self.devices = devices
        self.stats = stats
        self.pictureInPicture = pictureInPicture
        self.leave = leave
    }
}
