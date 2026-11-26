/// Replacements for the built-in strings of ``MonarchCallTile``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchCallTileLabels: Sendable {
    /// The name of your own tile. Receives the name. Built in: "Ana (you)".
    public var you: (@Sendable (String) -> String)?
    /// The muted microphone. Built in: "Muted".
    public var muted: String?
    /// A shared screen. Built in: "Sharing screen".
    public var sharingScreen: String?
    /// The visible hand badge. Built in: "Hand raised".
    public var handRaised: String?
    /// The speaking state, read by VoiceOver. Built in: "Speaking".
    public var speaking: String?
    /// The pin button. Receives the name. Built in: "Pin Ana".
    public var pin: (@Sendable (String) -> String)?
    /// The unpin button. Receives the name. Built in: "Unpin Ana".
    public var unpin: (@Sendable (String) -> String)?
    /// The full-screen button. Receives the name. Built in: "Show Ana full screen".
    public var fullScreen: (@Sendable (String) -> String)?
    /// Announced when a reaction arrives. Receives the name and the emoji.
    /// Built in: "Ana reacted 👏".
    public var reacted: (@Sendable (String, String) -> String)?
    /// The connection quality. Built in: "Good connection", "Unstable
    /// connection", "Poor connection".
    public var quality: (@Sendable (MonarchCallQuality) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        you: (@Sendable (String) -> String)? = nil,
        muted: String? = nil,
        sharingScreen: String? = nil,
        handRaised: String? = nil,
        speaking: String? = nil,
        pin: (@Sendable (String) -> String)? = nil,
        unpin: (@Sendable (String) -> String)? = nil,
        fullScreen: (@Sendable (String) -> String)? = nil,
        reacted: (@Sendable (String, String) -> String)? = nil,
        quality: (@Sendable (MonarchCallQuality) -> String)? = nil
    ) {
        self.you = you
        self.muted = muted
        self.sharingScreen = sharingScreen
        self.handRaised = handRaised
        self.speaking = speaking
        self.pin = pin
        self.unpin = unpin
        self.fullScreen = fullScreen
        self.reacted = reacted
        self.quality = quality
    }
}
