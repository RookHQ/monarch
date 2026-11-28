import Foundation
import Testing
import WebKit

@_spi(Experimental) @testable import MonarchUI

/// The sandbox of the artifact canvas web view (C-024): every setting of the
/// inert policy, the Content Security Policy, the rule list, and the HTML
/// stripping, with the cases of Android's `MonarchAiArtifactCanvasTest`.
@Suite("MonarchAiArtifactCanvas sandbox")
@MainActor
struct MonarchAiArtifactCanvasSandboxTests {
    private typealias Tokens = MonarchAiArtifactCanvasTokens

    @Test("The inert configuration: no script, no stored data, no windows, media waits for a tap")
    func inertConfiguration() {
        let configuration = MonarchSandboxedWebView.configuration(.inert)
        #expect(!configuration.defaultWebpagePreferences.allowsContentJavaScript)
        #expect(!configuration.websiteDataStore.isPersistent)
        #expect(!configuration.preferences.javaScriptCanOpenWindowsAutomatically)
        #expect(configuration.mediaTypesRequiringUserActionForPlayback == .all)
        #if os(iOS)
            #expect(!configuration.allowsInlineMediaPlayback)
        #endif
    }

    @Test("The embed configuration differs only in script and inline media")
    func embedConfiguration() {
        let configuration = MonarchSandboxedWebView.configuration(.embed)
        #expect(configuration.defaultWebpagePreferences.allowsContentJavaScript)
        #expect(!configuration.websiteDataStore.isPersistent)
        #expect(!configuration.preferences.javaScriptCanOpenWindowsAutomatically)
        #expect(configuration.mediaTypesRequiringUserActionForPlayback == .all)
        #if os(iOS)
            #expect(configuration.allowsInlineMediaPlayback)
        #endif
    }

    @Test("The canvas web view is built from the inert policy, with the delegate and no link previews")
    func webView() {
        let coordinator = MonarchAiArtifactCanvasWebView.Coordinator()
        let webView = MonarchSandboxedWebView.makeWebView(.inert, delegate: coordinator)
        #expect(webView.navigationDelegate === coordinator)
        #expect(webView.uiDelegate === coordinator)
        #expect(!webView.allowsLinkPreview)
        #expect(!webView.configuration.defaultWebpagePreferences.allowsContentJavaScript)
        #expect(!webView.configuration.websiteDataStore.isPersistent)
    }

    @Test("The Content Security Policy refuses everything but inline styles and data images and fonts")
    func contentSecurityPolicy() {
        #expect(
            Tokens.contentSecurityPolicy
                == "default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:; "
                + "form-action 'none'; base-uri 'none'; frame-src 'none'; object-src 'none'; script-src 'none'"
        )
        let document = Tokens.document("<p>Hi</p><script>alert(1)</script>")
        #expect(document.contains("Content-Security-Policy"))
        #expect(document.contains("script-src 'none'"))
        #expect(!document.contains("<script>"))
    }

    @Test("The rule list blocks http, https and file loads and nothing else")
    func ruleList() throws {
        let data = try #require(MonarchSandboxedWebView.blockingRules.data(using: .utf8))
        let rules = try #require(try JSONSerialization.jsonObject(with: data) as? [[String: Any]])
        let filters = rules.compactMap { ($0["trigger"] as? [String: Any])?["url-filter"] as? String }
        #expect(filters == ["^https?://", "^file:"])
        #expect(rules.allSatisfy { ($0["action"] as? [String: Any])?["type"] as? String == "block" })
        #expect(MonarchSandboxedWebView.blockingRuleListIdentifier == "monarch.sandbox.block-network")
    }

    @Test("Inert HTML drops scripts, frames, base, meta refresh and event handlers (Android's case)")
    func inertHtml() {
        let html =
            "<p onclick=\"steal()\">Hi</p><script>alert(1)</script><iframe src=\"https://x.test\"></iframe>"
            + "<meta http-equiv=\"refresh\" content=\"0;url=https://x.test\"><base href=\"https://x.test\">"
        #expect(Tokens.inertHtml(html) == "<p>Hi</p>")
    }

    @Test("Inert HTML handles every element and attribute form of the list")
    func inertHtmlForms() {
        #expect(Tokens.inertHtml("<SCRIPT type=x>a</SCRIPT>text") == "text")
        #expect(Tokens.inertHtml("<object data=x>fallback</object>ok") == "ok")
        #expect(Tokens.inertHtml("<embed src=x>ok") == "ok")
        #expect(Tokens.inertHtml("<link rel=preconnect href=https://x.test>ok") == "ok")
        #expect(Tokens.inertHtml("<template><b>x</b></template>ok") == "ok")
        #expect(Tokens.inertHtml("<frameset cols=1>ok") == "ok")
        #expect(
            Tokens.inertHtml("<img src=\"data:image/png;base64,AA\" onerror='x()'>")
                == "<img src=\"data:image/png;base64,AA\">"
        )
        #expect(Tokens.inertHtml("<a onmouseover=go()>l</a>") == "<a>l</a>")
        #expect(
            Tokens.inertHtml("<p style=\"color:red\">kept</p><b>bold</b>")
                == "<p style=\"color:red\">kept</p><b>bold</b>"
        )
        // A tag split around another one does not reassemble.
        #expect(!Tokens.inertHtml("<scr<script></script>ipt>alert(1)</script>").lowercased().contains("<script"))
    }

    @Test("Only the preview's own document may navigate")
    func navigation() {
        #expect(Tokens.allowsNavigation(to: URL(string: "about:blank")))
        #expect(!Tokens.allowsNavigation(to: URL(string: "https://example.com")))
        #expect(!Tokens.allowsNavigation(to: URL(string: "file:///etc/hosts")))
        #expect(!Tokens.allowsNavigation(to: URL(string: "javascript:alert(1)")))
    }
}
