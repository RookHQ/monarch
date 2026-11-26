import SwiftUI
// WebKit: the deployment target is iOS 17, and SwiftUI's own WebView needs
// iOS 26, so the embed is a WKWebView in a representable.
import WebKit

/// The web view of ``MonarchEmbedBlock``: loads one allow-listed embed
/// address with no stored website data, keeps the main frame on the embed's
/// host and sends taps on other links to the system browser.
struct MonarchEmbedBlockWebView {
    let embedURL: URL
    let openURL: OpenURLAction

    /// The request for the embed, with the app's origin as the Referer.
    static func request(for url: URL, bundleIdentifier: String?) -> URLRequest {
        var request = URLRequest(url: url)
        if let referer = MonarchEmbedBlockTokens.referer(bundleIdentifier: bundleIdentifier) {
            request.setValue(referer, forHTTPHeaderField: "Referer")
        }
        return request
    }

    @MainActor func makeWebView(coordinator: Coordinator) -> WKWebView {
        let webView = MonarchSandboxedWebView.makeWebView(.embed, delegate: coordinator)
        webView.load(Self.request(for: embedURL, bundleIdentifier: Bundle.main.bundleIdentifier))
        coordinator.loadedURL = embedURL
        return webView
    }

    @MainActor func update(_ webView: WKWebView, coordinator: Coordinator) {
        coordinator.openURL = openURL
        coordinator.embedHost = embedURL.host
        if coordinator.loadedURL != embedURL {
            coordinator.loadedURL = embedURL
            webView.load(Self.request(for: embedURL, bundleIdentifier: Bundle.main.bundleIdentifier))
        }
    }

    @MainActor func makeCoordinator() -> Coordinator {
        Coordinator(embedHost: embedURL.host, openURL: openURL)
    }

    /// Applies ``MonarchEmbedBlockTokens/policy(for:isMainFrame:isLinkActivation:embedHost:)`` to every navigation.
    @MainActor final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var embedHost: String?
        var openURL: OpenURLAction
        var loadedURL: URL?

        init(embedHost: String?, openURL: OpenURLAction) {
            self.embedHost = embedHost
            self.openURL = openURL
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping @MainActor @Sendable (WKNavigationActionPolicy) -> Void
        ) {
            let url = navigationAction.request.url
            let policy = MonarchEmbedBlockTokens.policy(
                for: url,
                isMainFrame: navigationAction.targetFrame?.isMainFrame ?? true,
                isLinkActivation: navigationAction.navigationType == .linkActivated,
                embedHost: embedHost
            )
            switch policy {
            case .allow:
                decisionHandler(.allow)
            case .openExternally:
                if let url { openURL(url) }
                decisionHandler(.cancel)
            case .cancel:
                decisionHandler(.cancel)
            }
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            // A new window (target="_blank") opens in the browser, never in the app.
            if let url = navigationAction.request.url, ["http", "https"].contains(url.scheme?.lowercased() ?? "") {
                openURL(url)
            }
            return nil
        }
    }
}

#if canImport(UIKit)
    extension MonarchEmbedBlockWebView: UIViewRepresentable {
        func makeUIView(context: Context) -> WKWebView {
            let webView = makeWebView(coordinator: context.coordinator)
            webView.isOpaque = false
            webView.scrollView.isScrollEnabled = false
            return webView
        }

        func updateUIView(_ webView: WKWebView, context: Context) {
            update(webView, coordinator: context.coordinator)
        }
    }
#elseif canImport(AppKit)
    extension MonarchEmbedBlockWebView: NSViewRepresentable {
        func makeNSView(context: Context) -> WKWebView {
            makeWebView(coordinator: context.coordinator)
        }

        func updateNSView(_ webView: WKWebView, context: Context) {
            update(webView, coordinator: context.coordinator)
        }
    }
#endif
