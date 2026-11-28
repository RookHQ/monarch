import Foundation
// WebKit: the deployment target is iOS 17, and SwiftUI's own WebView needs
// iOS 26, so sandboxed web content is a WKWebView in a representable.
import WebKit

/// The shared sandbox of Monarch's web views (the AI artifact canvas and the
/// embed block): one place that decides what web content may do.
///
/// Every sandbox has no stored website data, no windows opened by script,
/// media that waits for a tap, and no link previews. Then each use picks a
/// policy:
///
/// - ``Policy/inert``: untrusted HTML (model output). JavaScript off, and a
///   content rule list blocks every `http`, `https` and `file` load below the
///   Content Security Policy, as defence in depth.
/// - ``Policy/embed``: a trusted, allow-listed embed. JavaScript on; the
///   component keeps the main frame on the embed's host itself.
///
/// The components keep their own representable and navigation delegate;
/// they build the configuration and the web view here.
enum MonarchSandboxedWebView {
    /// What the web content may do.
    enum Policy: Equatable, Sendable {
        /// Untrusted HTML: no script, no network.
        case inert
        /// An allow-listed embed: script on, media inline.
        case embed
    }

    /// The identifier of the content rule list that blocks network loads.
    static let blockingRuleListIdentifier = "monarch.sandbox.block-network"

    /// The content rule list, as WebKit's JSON: block every load whose URL
    /// starts with `http:`, `https:` or `file:`. `about:blank` and `data:`
    /// (inline images in a document) stay allowed.
    static let blockingRules = """
        [{"trigger":{"url-filter":"^https?://"},"action":{"type":"block"}},\
        {"trigger":{"url-filter":"^file:"},"action":{"type":"block"}}]
        """

    /// The configuration for a policy.
    @MainActor
    static func configuration(_ policy: Policy) -> WKWebViewConfiguration {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .nonPersistent()
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.mediaTypesRequiringUserActionForPlayback = .all
        switch policy {
        case .inert:
            configuration.defaultWebpagePreferences.allowsContentJavaScript = false
        case .embed:
            configuration.defaultWebpagePreferences.allowsContentJavaScript = true
            #if os(iOS)
                configuration.allowsInlineMediaPlayback = true
            #endif
        }
        return configuration
    }

    /// A web view for a policy, with the delegates set. Link previews are off.
    @MainActor
    static func makeWebView(
        _ policy: Policy,
        delegate: (any WKNavigationDelegate & WKUIDelegate)?
    ) -> WKWebView {
        let webView = WKWebView(frame: .zero, configuration: configuration(policy))
        webView.navigationDelegate = delegate
        webView.uiDelegate = delegate
        webView.allowsLinkPreview = false
        return webView
    }

    /// Compiles the network-blocking rule list and adds it to `webView`.
    /// For the inert policy; call it before loading content. A failure to
    /// compile leaves the Content Security Policy as the only guard and is
    /// reported to the caller.
    @MainActor
    @discardableResult
    static func installNetworkBlock(on webView: WKWebView) async -> Bool {
        guard let store = WKContentRuleListStore.default(),
            let list = try? await store.compileContentRuleList(
                forIdentifier: blockingRuleListIdentifier,
                encodedContentRuleList: blockingRules
            )
        else { return false }
        webView.configuration.userContentController.add(list)
        return true
    }
}
