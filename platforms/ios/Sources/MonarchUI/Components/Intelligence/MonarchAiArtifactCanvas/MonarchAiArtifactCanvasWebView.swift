import SwiftUI
// WebKit: the deployment target is iOS 17, and SwiftUI's own WebView needs
// iOS 26, so the HTML preview is a WKWebView in a representable.
import WebKit

/// The sandboxed HTML preview of ``MonarchAiArtifactCanvas``: the inert
/// policy of `MonarchSandboxedWebView` (JavaScript off, no stored website
/// data, a rule list that blocks every network load), HTML stripped of
/// scripts and handlers, a Content Security Policy, and every navigation away
/// from the document cancelled, so untrusted model output can neither run
/// code nor reach the network.
struct MonarchAiArtifactCanvasWebView {
    let html: String

    @MainActor func makeWebView(coordinator: Coordinator) -> WKWebView {
        let webView = MonarchSandboxedWebView.makeWebView(.inert, delegate: coordinator)
        load(webView, coordinator: coordinator)
        return webView
    }

    @MainActor func update(_ webView: WKWebView, coordinator: Coordinator) {
        if coordinator.loadedHTML != html { load(webView, coordinator: coordinator) }
    }

    /// Loads the document once the network-blocking rule list is installed,
    /// so nothing is requested before the block exists.
    @MainActor private func load(_ webView: WKWebView, coordinator: Coordinator) {
        coordinator.loadedHTML = html
        let document = MonarchAiArtifactCanvasTokens.document(html)
        let wanted = html
        Task { @MainActor in
            if !coordinator.hasNetworkBlock {
                coordinator.hasNetworkBlock = await MonarchSandboxedWebView.installNetworkBlock(on: webView)
            }
            // The content changed while the rules compiled: the newer load wins.
            guard coordinator.loadedHTML == wanted else { return }
            webView.loadHTMLString(document, baseURL: nil)
        }
    }

    @MainActor func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    /// Cancels every navigation but the preview's own document and refuses new windows.
    @MainActor final class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        var loadedHTML: String?
        var hasNetworkBlock = false

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping @MainActor @Sendable (WKNavigationActionPolicy) -> Void
        ) {
            let allowed = MonarchAiArtifactCanvasTokens.allowsNavigation(to: navigationAction.request.url)
            decisionHandler(allowed ? .allow : .cancel)
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? {
            nil
        }
    }
}

#if canImport(UIKit)
    extension MonarchAiArtifactCanvasWebView: UIViewRepresentable {
        func makeUIView(context: Context) -> WKWebView {
            let webView = makeWebView(coordinator: context.coordinator)
            webView.isOpaque = false
            return webView
        }

        func updateUIView(_ webView: WKWebView, context: Context) {
            update(webView, coordinator: context.coordinator)
        }
    }
#elseif canImport(AppKit)
    extension MonarchAiArtifactCanvasWebView: NSViewRepresentable {
        func makeNSView(context: Context) -> WKWebView {
            makeWebView(coordinator: context.coordinator)
        }

        func updateNSView(_ webView: WKWebView, context: Context) {
            update(webView, coordinator: context.coordinator)
        }
    }
#endif
