/// Replacements for the built-in strings of ``MonarchCallStage``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchCallStageLabels: Sendable {
    /// The strings of each tile: "(you)", Pin, full screen, muted, quality…
    public var tile: MonarchCallTileLabels
    /// The name of the stage without a title. Built in: "Call".
    public var region: String?
    /// The spoken name of the timer. Built in: "Call duration".
    public var duration: String?
    /// Shown while you are alone. Built in: "Waiting for others…".
    public var waiting: String?
    /// The head count. Receives the number of people, you included. Built in: "3 in call".
    public var inCall: (@Sendable (Int) -> String)?
    /// The low data badge. Built in: "Low data".
    public var lowData: String?
    /// The notice while the connection recovers. Built in: "You're offline. Trying to reconnect…".
    public var reconnecting: String?
    /// The hint when you talk while muted. Built in: "You're muted. Turn on your microphone to talk.".
    public var mutedHint: String?
    /// The picture in picture button. Built in: "Picture in picture".
    public var pictureInPicture: String?
    /// The button that docks the stage. Built in: "Shrink call to the corner".
    public var shrink: String?
    /// The button that returns to the full stage. Built in: "Expand call".
    public var expand: String?
    /// The button that minimizes the docked call. Built in: "Minimize call".
    public var minimize: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        tile: MonarchCallTileLabels = MonarchCallTileLabels(),
        region: String? = nil,
        duration: String? = nil,
        waiting: String? = nil,
        inCall: (@Sendable (Int) -> String)? = nil,
        lowData: String? = nil,
        reconnecting: String? = nil,
        mutedHint: String? = nil,
        pictureInPicture: String? = nil,
        shrink: String? = nil,
        expand: String? = nil,
        minimize: String? = nil
    ) {
        self.tile = tile
        self.region = region
        self.duration = duration
        self.waiting = waiting
        self.inCall = inCall
        self.lowData = lowData
        self.reconnecting = reconnecting
        self.mutedHint = mutedHint
        self.pictureInPicture = pictureInPicture
        self.shrink = shrink
        self.expand = expand
        self.minimize = minimize
    }
}
