/// Replacements for the built-in strings of ``MonarchPreJoinLobby``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchPreJoinLobbyLabels: Sendable {
    /// The heading. Built in: "Join the call".
    public var title: String?
    /// The name of the preview area. Built in: "Your camera preview".
    public var preview: String?
    /// Shown in the preview while the camera is off. Built in: "Camera is off".
    public var cameraOff: String?
    /// Shown in the preview while the camera starts. Built in: "Starting camera…".
    public var startingCamera: String?
    /// Shown in the preview when there is a problem. Built in: "No preview".
    public var noPreview: String?
    /// The name of the microphone and camera toggles. Built in: "Microphone and camera".
    public var controls: String?
    /// The name of the microphone level meter. Built in: "Microphone level".
    public var micLevel: String?
    /// The people already in the call. Receives the count. Built in: "3 already in the call".
    public var alreadyIn: (@Sendable (Int) -> String)?
    /// The cancel button. Built in: "Cancel".
    public var cancel: String?
    /// The join button. Built in: "Join now".
    public var join: String?
    /// The join button while joining. Built in: "Joining…".
    public var joining: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        preview: String? = nil,
        cameraOff: String? = nil,
        startingCamera: String? = nil,
        noPreview: String? = nil,
        controls: String? = nil,
        micLevel: String? = nil,
        alreadyIn: (@Sendable (Int) -> String)? = nil,
        cancel: String? = nil,
        join: String? = nil,
        joining: String? = nil
    ) {
        self.title = title
        self.preview = preview
        self.cameraOff = cameraOff
        self.startingCamera = startingCamera
        self.noPreview = noPreview
        self.controls = controls
        self.micLevel = micLevel
        self.alreadyIn = alreadyIn
        self.cancel = cancel
        self.join = join
        self.joining = joining
    }
}
