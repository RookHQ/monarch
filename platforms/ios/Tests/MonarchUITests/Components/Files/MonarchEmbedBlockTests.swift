import Foundation
import MonarchTokens
import SwiftUI
import Testing
import WebKit

@testable import MonarchUI

enum MonarchEmbedBlockFixtures: ComponentFixtures {
    static let id = "embed-block"
    static let fixtures = [
        // The web view itself is not hosted in fixtures: it would reach the network.
        Fixture("video waiting for a tap, with a caption") {
            MonarchEmbedBlock(
                url: "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                caption: "Launch walkthrough",
                requiresTapToLoad: true
            )
        },
        Fixture("design, 4 by 3, waiting for a tap") {
            MonarchEmbedBlock(
                url: "https://www.figma.com/design/abc123/Onboarding",
                requiresTapToLoad: true,
                labels: MonarchEmbedBlockLabels(load: { "Open the \($0) file here" })
            )
        },
        Fixture("other site as a link card") {
            MonarchEmbedBlock(url: "https://www.example.com/notes/2026/a-long-path-to-a-page", caption: "Meeting notes")
        },
        Fixture("unusable link") {
            MonarchEmbedBlock(url: "javascript:alert(1)")
        },
    ]
}

@Suite("MonarchEmbedBlock")
@MainActor
struct MonarchEmbedBlockTests {
    private func embed(_ raw: String) -> String? {
        if case .frame(_, let embedURL, _) = MonarchEmbedBlock.resolve(raw) { return embedURL.absoluteString }
        return nil
    }

    @Test("YouTube share links become youtube-nocookie embeds")
    func youTube() {
        let expected = "https://www.youtube-nocookie.com/embed/aqz-KE-bpKQ"
        #expect(embed("https://www.youtube.com/watch?v=aqz-KE-bpKQ") == expected)
        #expect(embed("https://youtu.be/aqz-KE-bpKQ") == expected)
        #expect(embed("  https://m.youtube.com/shorts/aqz-KE-bpKQ  ") == expected)
        #expect(embed("https://youtube.com/live/aqz-KE-bpKQ") == expected)
        #expect(embed("https://www.youtube.com/watch?v=short") == nil)
        #expect(embed("https://www.youtube.com/watch?v=aqz-KE-bp%3Cs") == nil)
    }

    @Test("Vimeo, Loom, Figma and CodePen links become their embeds")
    func providers() {
        #expect(embed("https://vimeo.com/76979871") == "https://player.vimeo.com/video/76979871")
        #expect(
            embed("https://www.loom.com/share/0123456789abcdef0123")
                == "https://www.loom.com/embed/0123456789abcdef0123"
        )
        #expect(embed("https://www.loom.com/share/not-hex") == nil)
        #expect(
            embed("https://www.figma.com/design/abc/Name")
                == "https://www.figma.com/embed?embed_host=monarch&url=https%3A%2F%2Fwww.figma.com%2Fdesign%2Fabc%2FName"
        )
        #expect(
            embed("https://codepen.io/some-user/pen/abc123")
                == "https://codepen.io/some-user/embed/abc123?default-tab=result"
        )
        #expect(embed("https://codepen.io/some-user/details/abc123") == nil)
    }

    @Test("Other web links are link cards; anything else is not shown")
    func kinds() {
        #expect(MonarchEmbedBlock.resolve("https://www.example.com/a").kind == .link)
        if case .link(_, let host) = MonarchEmbedBlock.resolve("https://www.Example.com/a") {
            #expect(host == "example.com")
        }
        // http, a port, or an allow-listed host with no embeddable form: a link, never a frame.
        #expect(MonarchEmbedBlock.resolve("http://youtu.be/aqz-KE-bpKQ").kind == .link)
        #expect(MonarchEmbedBlock.resolve("https://youtu.be:8443/aqz-KE-bpKQ").kind == .link)
        #expect(MonarchEmbedBlock.resolve("https://www.youtube.com/feed").kind == .link)
        for raw in [
            "javascript:alert(1)", "data:text/html,hi", "/relative/path", "not a url", "",
            "https://user:pw@example.com",
        ] {
            #expect(MonarchEmbedBlock.resolve(raw).kind == .invalid, "\(raw)")
        }
    }

    @Test("A custom provider list replaces the defaults")
    func customProviders() {
        let only = [
            MonarchEmbedProvider(id: "docs", name: "Docs", hosts: ["docs.example.com"]) { url in
                URL(string: "https://docs.example.com/embed\(url.path)")
            }
        ]
        if case .frame(_, let embedURL, let provider) = MonarchEmbedBlock.resolve(
            "https://docs.example.com/a",
            providers: only
        ) {
            #expect(embedURL.absoluteString == "https://docs.example.com/embed/a")
            #expect(provider.id == "docs")
            #expect(provider.aspectRatio == MonarchEmbedProvider.videoAspectRatio)
        } else {
            Issue.record("expected a frame")
        }
        #expect(MonarchEmbedBlock.resolve("https://youtu.be/aqz-KE-bpKQ", providers: only).kind == .link)
        let insecure = [
            MonarchEmbedProvider(id: "x", name: "X", hosts: ["x.com"]) { _ in URL(string: "http://x.com/e") }
        ]
        #expect(MonarchEmbedBlock.resolve("https://x.com/a", providers: insecure).kind == .link)
    }

    @Test("The web view stays on the embed host and sends tapped links to the browser")
    func policy() {
        let host = "www.youtube-nocookie.com"
        let embed = URL(string: "https://www.youtube-nocookie.com/embed/x")
        let other = URL(string: "https://www.youtube.com/watch?v=x")
        #expect(
            MonarchEmbedBlockTokens.policy(for: embed, isMainFrame: true, isLinkActivation: false, embedHost: host)
                == .allow
        )
        #expect(
            MonarchEmbedBlockTokens.policy(for: other, isMainFrame: true, isLinkActivation: true, embedHost: host)
                == .openExternally
        )
        #expect(
            MonarchEmbedBlockTokens.policy(for: other, isMainFrame: true, isLinkActivation: false, embedHost: host)
                == .cancel
        )
        #expect(
            MonarchEmbedBlockTokens.policy(for: embed, isMainFrame: true, isLinkActivation: true, embedHost: host)
                == .openExternally
        )
        #expect(
            MonarchEmbedBlockTokens.policy(for: other, isMainFrame: false, isLinkActivation: false, embedHost: host)
                == .allow
        )
        #expect(
            MonarchEmbedBlockTokens.policy(
                for: URL(string: "http://ads.example.com"),
                isMainFrame: false,
                isLinkActivation: false,
                embedHost: host
            ) == .cancel
        )
        #expect(
            MonarchEmbedBlockTokens.policy(
                for: URL(string: "about:blank"),
                isMainFrame: false,
                isLinkActivation: false,
                embedHost: host
            ) == .allow
        )
        #expect(
            MonarchEmbedBlockTokens.policy(
                for: URL(string: "tel:123"),
                isMainFrame: true,
                isLinkActivation: true,
                embedHost: host
            ) == .cancel
        )
        #expect(
            MonarchEmbedBlockTokens.policy(for: nil, isMainFrame: true, isLinkActivation: false, embedHost: host)
                == .cancel
        )
    }

    @Test("The web view keeps no website data and opens no windows from script")
    func configuration() {
        let configuration = MonarchSandboxedWebView.configuration(.embed)
        #expect(!configuration.websiteDataStore.isPersistent)
        #expect(!configuration.preferences.javaScriptCanOpenWindowsAutomatically)
        #expect(configuration.mediaTypesRequiringUserActionForPlayback == .all)
        #expect(configuration.defaultWebpagePreferences.allowsContentJavaScript)
        let request = MonarchEmbedBlockWebView.request(
            for: URL(string: "https://www.youtube-nocookie.com/embed/x")!,
            bundleIdentifier: "com.Example.App"
        )
        #expect(request.value(forHTTPHeaderField: "Referer") == "https://com.example.app/")
        #expect(MonarchEmbedBlockTokens.referer(bundleIdentifier: nil) == nil)
    }

    @Test("The web view is named by its title, caption or provider")
    func names() {
        let labels = MonarchEmbedBlockLabels()
        #expect(
            MonarchEmbedBlockTokens.frameTitle(title: "T", caption: "C", provider: "YouTube", labels: labels) == "T"
        )
        #expect(
            MonarchEmbedBlockTokens.frameTitle(title: nil, caption: "C", provider: "YouTube", labels: labels) == "C"
        )
        #expect(
            MonarchEmbedBlockTokens.frameTitle(title: nil, caption: nil, provider: "YouTube", labels: labels)
                == "YouTube content"
        )
        #expect(MonarchEmbedBlockTokens.loadTitle(provider: "Vimeo", labels: labels) == "Load Vimeo content")
        let custom = MonarchEmbedBlockLabels(frameTitle: { "Video from \($0)" })
        #expect(
            MonarchEmbedBlockTokens.frameTitle(title: "", caption: nil, provider: "Loom", labels: custom)
                == "Video from Loom"
        )
        #expect(MonarchEmbedBlockStrings.invalid == "This link can’t be shown.")
    }

    @Test("Colours come from the theme roles")
    func colours() {
        #expect(MonarchEmbedBlockTokens.frameBackground == \MonarchColorTokens.bgSubtle)
        #expect(MonarchEmbedBlockTokens.border == \MonarchColorTokens.borderSubtle)
        #expect(MonarchEmbedBlockTokens.caption == \MonarchColorTokens.textMuted)
    }
}
