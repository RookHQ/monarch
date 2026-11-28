import Foundation

/// A site whose pages ``MonarchEmbedBlock`` may show in a web view.
///
/// The URL someone pastes is checked against `hosts` (exact host names,
/// https only) and turned into the provider's own embed address by
/// `embedURL`; any other URL is never loaded.
public struct MonarchEmbedProvider: Sendable, Identifiable {
    /// A stable id, for example `youtube`.
    public var id: String
    /// The name shown and read for the content, for example `YouTube`.
    public var name: String
    /// The exact host names of share URLs, for example `["youtube.com", "www.youtube.com", "youtu.be"]`.
    public var hosts: [String]
    /// The width of the web view divided by its height.
    public var aspectRatio: CGFloat
    /// The embed address for a share URL, or `nil` when it has no embeddable form. Must be an https URL.
    public var embedURL: @Sendable (URL) -> URL?

    /// A provider.
    ///
    /// - Parameters:
    ///   - id: A stable id, for example `youtube`.
    ///   - name: The name shown and read for the content.
    ///   - hosts: The exact host names of share URLs, in lower case.
    ///   - aspectRatio: The width of the web view divided by its height.
    ///   - embedURL: The embed address for a share URL, or `nil`.
    public init(
        id: String,
        name: String,
        hosts: [String],
        aspectRatio: CGFloat = MonarchEmbedProvider.videoAspectRatio,
        embedURL: @escaping @Sendable (URL) -> URL?
    ) {
        self.id = id
        self.name = name
        self.hosts = hosts
        self.aspectRatio = aspectRatio
        self.embedURL = embedURL
    }

    /// The shape of a video: 16 by 9.
    public static let videoAspectRatio: CGFloat = 16.0 / 9.0
    /// The shape of a design or a code demo: 4 by 3.
    public static let documentAspectRatio: CGFloat = 4.0 / 3.0

    /// The providers shown out of the box: YouTube (through
    /// youtube-nocookie.com), Vimeo, Loom, Figma and CodePen. Pass your own
    /// list to add or remove sites.
    public static let defaults: [MonarchEmbedProvider] = [
        MonarchEmbedProvider(
            id: "youtube",
            name: "YouTube",
            hosts: ["youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"],
            embedURL: MonarchEmbedBlockTokens.youTubeEmbed
        ),
        MonarchEmbedProvider(
            id: "vimeo",
            name: "Vimeo",
            hosts: ["vimeo.com", "www.vimeo.com", "player.vimeo.com"],
            embedURL: MonarchEmbedBlockTokens.vimeoEmbed
        ),
        MonarchEmbedProvider(
            id: "loom",
            name: "Loom",
            hosts: ["loom.com", "www.loom.com"],
            embedURL: MonarchEmbedBlockTokens.loomEmbed
        ),
        MonarchEmbedProvider(
            id: "figma",
            name: "Figma",
            hosts: ["figma.com", "www.figma.com"],
            aspectRatio: MonarchEmbedProvider.documentAspectRatio,
            embedURL: MonarchEmbedBlockTokens.figmaEmbed
        ),
        MonarchEmbedProvider(
            id: "codepen",
            name: "CodePen",
            hosts: ["codepen.io"],
            aspectRatio: MonarchEmbedProvider.documentAspectRatio,
            embedURL: MonarchEmbedBlockTokens.codePenEmbed
        ),
    ]
}
