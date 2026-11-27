/// Replacements for the built-in strings of ``MonarchScreenShareView``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchScreenShareViewLabels: Sendable, Hashable {
    /// The whole visible presenter line. Built in: "Ana is presenting".
    public var presenting: String?
    /// The presenter line when the local person is sharing. Built in: "You
    /// are presenting".
    public var youArePresenting: String?
    /// The whole spoken name of the shared screen. Built in: "Screen shared
    /// by Ana".
    public var region: String?
    /// The spoken name of the group of controls. Built in: "Shared screen
    /// controls".
    public var controls: String?
    /// Shown while there is no picture yet. Built in: "Waiting for the
    /// shared screen…"
    public var waiting: String?
    /// Shown instead of the picture when the local person is sharing.
    public var localPreviewHidden: String?
    /// The title of the button that shows the local picture anyway. Built
    /// in: "Show preview".
    public var showPreview: String?
    /// The title of the button that fits the whole screen. Built in: "Fit to view".
    public var fit: String?
    /// The spoken name of the zoom-in button. Built in: "Zoom in".
    public var zoomIn: String?
    /// The spoken name of the zoom-out button. Built in: "Zoom out".
    public var zoomOut: String?
    /// The spoken name of the picture-in-picture button. Built in: "Picture
    /// in picture".
    public var pictureInPicture: String?
    /// The spoken name of the button while picture in picture is active.
    /// Built in: "Exit picture in picture".
    public var exitPictureInPicture: String?
    /// The spoken name of the full-screen button. Built in: "Full screen".
    public var fullScreen: String?
    /// The spoken name of the button while full screen. Built in: "Exit
    /// full screen".
    public var exitFullScreen: String?
    /// The title of the stop button. Built in: "Stop sharing".
    public var stopSharing: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        presenting: String? = nil,
        youArePresenting: String? = nil,
        region: String? = nil,
        controls: String? = nil,
        waiting: String? = nil,
        localPreviewHidden: String? = nil,
        showPreview: String? = nil,
        fit: String? = nil,
        zoomIn: String? = nil,
        zoomOut: String? = nil,
        pictureInPicture: String? = nil,
        exitPictureInPicture: String? = nil,
        fullScreen: String? = nil,
        exitFullScreen: String? = nil,
        stopSharing: String? = nil
    ) {
        self.presenting = presenting
        self.youArePresenting = youArePresenting
        self.region = region
        self.controls = controls
        self.waiting = waiting
        self.localPreviewHidden = localPreviewHidden
        self.showPreview = showPreview
        self.fit = fit
        self.zoomIn = zoomIn
        self.zoomOut = zoomOut
        self.pictureInPicture = pictureInPicture
        self.exitPictureInPicture = exitPictureInPicture
        self.fullScreen = fullScreen
        self.exitFullScreen = exitFullScreen
        self.stopSharing = stopSharing
    }
}
