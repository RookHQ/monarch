import SwiftUI

/// Shows a video, design or demo from an allow-listed site in a web view,
/// and any other web link as a link card. Only a provider's own embed
/// address is ever loaded; the pasted URL itself never is.
///
/// ```swift
/// MonarchEmbedBlock(url: "https://youtu.be/aqz-KE-bpKQ", caption: "Launch walkthrough")
/// MonarchEmbedBlock(url: "https://example.com/notes")   // a link card
/// ```
///
/// The web view keeps no website data between launches, opens no windows
/// from script, waits for a tap before playing media, stays on the
/// embed's host and opens other links the person taps in the browser.
/// With `requiresTapToLoad`, nothing is loaded from the site until the
/// person taps the load button.
///
/// Accessibility: the web view is a container named by `title`, the
/// caption, or "YouTube content"; the site's own page supplies what is
/// inside. The link card and the message for an unusable link are read as
/// in ``MonarchLinkCard`` and ``MonarchInlineNotice``.
public struct MonarchEmbedBlock: View {
    private let url: String
    private let caption: String?
    private let title: String?
    private let providers: [MonarchEmbedProvider]
    private let requiresTapToLoad: Bool
    private let labels: MonarchEmbedBlockLabels

    @State private var isLoadRequested = false
    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL

    /// An embed.
    ///
    /// - Parameters:
    ///   - url: The URL someone pasted: a share link, not an embed code.
    ///   - caption: Shown under the content and used as the spoken name of the web view.
    ///   - title: The spoken name of the web view when it differs from the caption.
    ///   - providers: The sites that may be shown in a web view.
    ///   - requiresTapToLoad: Whether the web view waits for a tap before loading anything from the site.
    ///   - labels: Replacements for the built-in strings.
    public init(
        url: String,
        caption: String? = nil,
        title: String? = nil,
        providers: [MonarchEmbedProvider] = MonarchEmbedProvider.defaults,
        requiresTapToLoad: Bool = false,
        labels: MonarchEmbedBlockLabels = MonarchEmbedBlockLabels()
    ) {
        self.url = url
        self.caption = caption
        self.title = title
        self.providers = providers
        self.requiresTapToLoad = requiresTapToLoad
        self.labels = labels
    }

    /// Decides how a pasted URL is shown: a web view when it is an https URL
    /// with no port on an allow-listed host and the provider gives an https
    /// embed address, a link card for any other http or https URL, and
    /// nothing for anything else.
    ///
    /// - Parameters:
    ///   - url: The pasted URL. Spaces around it are ignored.
    ///   - providers: The sites that may be shown in a web view.
    /// - Returns: How the URL is shown.
    public static func resolve(
        _ url: String,
        providers: [MonarchEmbedProvider] = MonarchEmbedProvider.defaults
    ) -> MonarchEmbedResolution {
        MonarchEmbedBlockTokens.resolve(url, providers: providers)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchEmbedBlockTokens.spacing) {
            switch MonarchEmbedBlockTokens.resolve(url, providers: providers) {
            case .frame(_, let embedURL, let provider):
                frame(embedURL: embedURL, provider: provider)
            case .link(let link, let host):
                MonarchLinkCard(
                    caption ?? host,
                    url: link,
                    description: link.absoluteString,
                    siteName: host,
                    image: nil,
                    size: .sm
                )
            case .invalid:
                MonarchInlineNotice(labels.invalid ?? MonarchEmbedBlockStrings.invalid, tone: .warning)
            }
            if let caption, !caption.isEmpty {
                Text(caption)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchEmbedBlockTokens.caption])
            }
        }
    }

    private func frame(embedURL: URL, provider: MonarchEmbedProvider) -> some View {
        let name = MonarchEmbedBlockTokens.frameTitle(
            title: title,
            caption: caption,
            provider: provider.name,
            labels: labels
        )
        return ZStack {
            theme.colors[role: MonarchEmbedBlockTokens.frameBackground]
            if requiresTapToLoad && !isLoadRequested {
                placeholder(provider: provider)
            } else {
                MonarchEmbedBlockWebView(embedURL: embedURL, openURL: openURL)
                    .accessibilityElement(children: .contain)
                    .accessibilityLabel(name)
            }
        }
        .aspectRatio(provider.aspectRatio, contentMode: .fit)
        .frame(maxWidth: .infinity)
        .clipShape(MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchEmbedBlockTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
    }

    private func placeholder(provider: MonarchEmbedProvider) -> some View {
        VStack(spacing: MonarchEmbedBlockTokens.placeholderSpacing) {
            MonarchIcon(systemName: MonarchEmbedBlockTokens.placeholderSymbol, size: .lg)
                .foregroundStyle(theme.colors[role: MonarchEmbedBlockTokens.placeholderSymbolColor])
            MonarchButton(
                MonarchEmbedBlockTokens.loadTitle(provider: provider.name, labels: labels),
                systemImage: MonarchEmbedBlockTokens.loadSymbol,
                size: .sm
            ) {
                isLoadRequested = true
            }
        }
        .padding(MonarchEmbedBlockTokens.placeholderPadding)
    }
}

#if DEBUG
    #Preview("MonarchEmbedBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchEmbedBlock(
                    url: "https://youtu.be/aqz-KE-bpKQ",
                    caption: "Launch walkthrough",
                    requiresTapToLoad: true
                )
                MonarchEmbedBlock(url: "https://example.com/notes")
                MonarchEmbedBlock(url: "javascript:alert(1)")
            }
        }
    }
#endif
