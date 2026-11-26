import Foundation
import MonarchTokens
import SwiftUI

/// What the web view of ``MonarchEmbedBlock`` does with a navigation.
enum MonarchEmbedBlockPolicy: Equatable {
    /// Load it in the web view.
    case allow
    /// Open it in the system browser and stay.
    case openExternally
    /// Ignore it.
    case cancel
}

/// Component tokens and pure logic of ``MonarchEmbedBlock``.
enum MonarchEmbedBlockTokens {
    /// The gap between the content and the caption.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The gap inside the placeholder shown before loading.
    static let placeholderSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding inside the placeholder.
    static let placeholderPadding: CGFloat = MonarchTokens.Space.space4
    /// The SF Symbol of the placeholder.
    static let placeholderSymbol = "play.rectangle"
    /// The SF Symbol of the load button.
    static let loadSymbol = "play.fill"
    /// The fill behind the web view and of the placeholder.
    static var frameBackground: MonarchColorRole { \.bgSubtle }
    /// The border of the web view.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of the caption.
    static var caption: MonarchColorRole { \.textMuted }
    /// The colour of the placeholder's symbol.
    static var placeholderSymbolColor: MonarchColorRole { \.textMuted }

    // MARK: Resolving

    /// Decides how a pasted URL is shown. See ``MonarchEmbedBlock/resolve(_:providers:)``.
    static func resolve(_ raw: String, providers: [MonarchEmbedProvider]) -> MonarchEmbedResolution {
        let value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        guard
            let components = URLComponents(string: value),
            let scheme = components.scheme?.lowercased(),
            scheme == "https" || scheme == "http",
            let host = components.host?.lowercased(), !host.isEmpty,
            (components.user ?? "").isEmpty, (components.password ?? "").isEmpty,
            let url = components.url
        else { return .invalid(value) }
        if scheme == "https" && components.port == nil {
            for provider in providers where provider.hosts.contains(host) {
                if let embed = provider.embedURL(url), embed.scheme?.lowercased() == "https" {
                    return .frame(url: url, embedURL: embed, provider: provider)
                }
            }
        }
        return .link(url: url, host: host.hasPrefix("www.") ? String(host.dropFirst(4)) : host)
    }

    /// The non-empty path segments of a URL.
    static func segments(_ url: URL) -> [String] {
        url.path.split(separator: "/").map(String.init)
    }

    /// Whether every character is an ASCII letter, digit, underscore or one
    /// of `extra`, and the text is not empty.
    static func isWord(_ text: String, extra: Set<Character> = []) -> Bool {
        !text.isEmpty
            && text.allSatisfy { ($0.isASCII && ($0.isLetter || $0.isNumber)) || $0 == "_" || extra.contains($0) }
    }

    /// youtube.com/watch?v=, youtu.be/, /embed/, /shorts/ and /live/ links, as
    /// a youtube-nocookie.com embed.
    @Sendable static func youTubeEmbed(_ url: URL) -> URL? {
        let path = segments(url)
        let host = url.host?.lowercased()
        let id: String? =
            if host == "youtu.be" {
                path.first
            } else if path.first == "watch" {
                URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first { $0.name == "v" }?.value
            } else if ["embed", "shorts", "live"].contains(path.first ?? "") && path.count > 1 {
                path[1]
            } else {
                nil
            }
        guard let id, id.count == 11, isWord(id, extra: ["-"]) else { return nil }
        return URL(string: "https://www.youtube-nocookie.com/embed/\(id)")
    }

    /// Any Vimeo link with a numeric video id, as a player.vimeo.com embed.
    @Sendable static func vimeoEmbed(_ url: URL) -> URL? {
        guard let id = segments(url).first(where: { !$0.isEmpty && $0.allSatisfy { $0.isASCII && $0.isNumber } })
        else { return nil }
        return URL(string: "https://player.vimeo.com/video/\(id)")
    }

    /// loom.com/share/ and /embed/ links with a hexadecimal id.
    @Sendable static func loomEmbed(_ url: URL) -> URL? {
        let path = segments(url)
        guard path.count > 1, path[0] == "share" || path[0] == "embed" else { return nil }
        let id = path[1]
        guard (16...64).contains(id.count), id.allSatisfy(\.isHexDigit) else { return nil }
        return URL(string: "https://www.loom.com/embed/\(id)")
    }

    /// Figma file, design, prototype, board and slides links, as a figma.com embed.
    @Sendable static func figmaEmbed(_ url: URL) -> URL? {
        guard ["file", "design", "proto", "board", "slides"].contains(segments(url).first ?? "") else { return nil }
        return URL(string: "https://www.figma.com/embed?embed_host=monarch&url=\(encodeComponent(url.absoluteString))")
    }

    /// codepen.io/<user>/pen/<id> links, as a codepen.io embed of the result.
    @Sendable static func codePenEmbed(_ url: URL) -> URL? {
        let path = segments(url)
        guard path.count > 2, path[1] == "pen" || path[1] == "embed", isWord(path[2]), isWord(path[0], extra: ["-"])
        else { return nil }
        return URL(string: "https://codepen.io/\(path[0])/embed/\(path[2])?default-tab=result")
    }

    /// Percent-encodes a URL for a query value, as JavaScript's `encodeURIComponent`.
    static func encodeComponent(_ text: String) -> String {
        var allowed = CharacterSet(charactersIn: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
        allowed.insert(charactersIn: "-_.!~*'()")
        return text.addingPercentEncoding(withAllowedCharacters: allowed) ?? text
    }

    // MARK: Web view

    /// What the web view does with a navigation. The main frame stays on the
    /// embed's host; a link the person taps elsewhere opens in the browser;
    /// frames inside the embed may load https pages only.
    static func policy(for url: URL?, isMainFrame: Bool, isLinkActivation: Bool, embedHost: String?)
        -> MonarchEmbedBlockPolicy
    {
        guard let url, let scheme = url.scheme?.lowercased() else { return .cancel }
        if scheme == "about" { return .allow }
        let isWeb = scheme == "https" || scheme == "http"
        if isMainFrame {
            if scheme == "https", let host = url.host?.lowercased(), host == embedHost?.lowercased(), !isLinkActivation
            {
                return .allow
            }
            return isWeb && isLinkActivation ? .openExternally : .cancel
        }
        if scheme == "https" { return .allow }
        return isWeb && isLinkActivation ? .openExternally : .cancel
    }

    /// The Referer sent with the embed request: the app's bundle id as an
    /// https origin, as video sites ask of apps, or `nil` without a bundle id.
    static func referer(bundleIdentifier: String?) -> String? {
        guard let id = bundleIdentifier?.lowercased(), !id.isEmpty else { return nil }
        return "https://\(id)/"
    }

    /// The spoken name of the web view: the title, else the caption, else "YouTube content".
    static func frameTitle(title: String?, caption: String?, provider: String, labels: MonarchEmbedBlockLabels)
        -> String
    {
        if let title, !title.isEmpty { return title }
        if let caption, !caption.isEmpty { return caption }
        return labels.frameTitle?(provider) ?? MonarchEmbedBlockStrings.frameTitle(provider)
    }

    /// The title of the load button.
    static func loadTitle(provider: String, labels: MonarchEmbedBlockLabels) -> String {
        labels.load?(provider) ?? MonarchEmbedBlockStrings.load(provider)
    }
}
