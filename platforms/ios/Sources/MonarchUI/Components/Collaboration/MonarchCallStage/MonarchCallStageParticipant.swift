import Foundation

/// One person on a ``MonarchCallStage``: what their ``MonarchCallTile`` shows.
@_spi(Experimental) public struct MonarchCallStageParticipant: Sendable, Hashable, Identifiable {
    /// A stable id: used for the layout, pinning and the video slot.
    public let id: String
    /// The display name, already localised.
    public let name: String
    /// The picture shown without video, loaded with `AsyncImage`.
    public let avatarURL: URL?
    /// Whether the video slot is shown. False while connecting or with the camera off.
    public let showsVideo: Bool
    /// This is you: "(you)" after the name, a mirrored camera, and never spotlighted by your own screen.
    public let isSelf: Bool
    /// Shows the muted microphone.
    public let isMicOff: Bool
    /// Highlights the tile.
    public let isSpeaking: Bool
    /// Shows the raised hand badge.
    public let isHandRaised: Bool
    /// The video is a shared screen. Someone else's screen is spotlighted.
    public let isScreen: Bool
    /// A short status in the name bar, such as "Connecting…", already localised.
    public let status: String?
    /// The colour of the status.
    public let statusTone: MonarchTone
    /// The connection quality bars. None when `nil`.
    public let quality: MonarchCallQuality?
    /// A reaction that floats up for three seconds after it was sent.
    public let reaction: MonarchCallReaction?

    /// A participant.
    ///
    /// - Parameters:
    ///   - id: A stable id.
    ///   - name: The display name, already localised.
    ///   - avatarURL: The picture shown without video.
    ///   - showsVideo: Whether the video slot is shown.
    ///   - isSelf: This is you.
    ///   - isMicOff: Shows the muted microphone.
    ///   - isSpeaking: Highlights the tile.
    ///   - isHandRaised: Shows the raised hand badge.
    ///   - isScreen: The video is a shared screen.
    ///   - status: A short status in the name bar, already localised.
    ///   - statusTone: The colour of the status.
    ///   - quality: The connection quality bars.
    ///   - reaction: A reaction that floats up.
    public init(
        _ id: String,
        name: String,
        avatarURL: URL? = nil,
        showsVideo: Bool = true,
        isSelf: Bool = false,
        isMicOff: Bool = false,
        isSpeaking: Bool = false,
        isHandRaised: Bool = false,
        isScreen: Bool = false,
        status: String? = nil,
        statusTone: MonarchTone = .neutral,
        quality: MonarchCallQuality? = nil,
        reaction: MonarchCallReaction? = nil
    ) {
        self.id = id
        self.name = name
        self.avatarURL = avatarURL
        self.showsVideo = showsVideo
        self.isSelf = isSelf
        self.isMicOff = isMicOff
        self.isSpeaking = isSpeaking
        self.isHandRaised = isHandRaised
        self.isScreen = isScreen
        self.status = status
        self.statusTone = statusTone
        self.quality = quality
        self.reaction = reaction
    }
}
