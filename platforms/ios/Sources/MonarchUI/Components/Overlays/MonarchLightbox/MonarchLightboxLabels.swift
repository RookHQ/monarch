/// Replacements for the built-in strings of ``MonarchLightbox``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchLightboxLabels: Sendable {
    /// The spoken name of the viewer. Built in: "Media viewer".
    public var title: String?
    /// The spoken name of the button that shows the item before. Built in:
    /// "Previous".
    public var previous: String?
    /// The spoken name of the button that shows the item after. Built in:
    /// "Next".
    public var next: String?
    /// The spoken name of the zoom-in button. Built in: "Zoom in".
    public var zoomIn: String?
    /// The spoken name of the zoom-out button. Built in: "Zoom out".
    public var zoomOut: String?
    /// The spoken name of the button that shows the zoom level and resets
    /// it. Built in: "Reset zoom".
    public var resetZoom: String?
    /// The spoken name of the close button. Built in: "Close".
    public var close: String?
    /// The spoken name of the share button. Built in: "Share".
    public var share: String?
    /// Where the item sits. Receives the position, starting at 1, and the
    /// number of items. Built in: "2 of 5".
    public var position: (@Sendable (Int, Int) -> String)?
    /// Shown instead of media that has no source or did not load. Built in:
    /// "This file can’t be shown."
    public var unavailable: String?
    /// The spoken name of the progress indicator while an image loads.
    /// Built in: "Loading".
    public var loading: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        previous: String? = nil,
        next: String? = nil,
        zoomIn: String? = nil,
        zoomOut: String? = nil,
        resetZoom: String? = nil,
        close: String? = nil,
        share: String? = nil,
        position: (@Sendable (Int, Int) -> String)? = nil,
        unavailable: String? = nil,
        loading: String? = nil
    ) {
        self.title = title
        self.previous = previous
        self.next = next
        self.zoomIn = zoomIn
        self.zoomOut = zoomOut
        self.resetZoom = resetZoom
        self.close = close
        self.share = share
        self.position = position
        self.unavailable = unavailable
        self.loading = loading
    }
}
