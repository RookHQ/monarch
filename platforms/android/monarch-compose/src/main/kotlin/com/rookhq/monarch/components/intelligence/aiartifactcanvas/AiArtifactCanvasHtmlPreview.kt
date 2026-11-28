package com.rookhq.monarch.components.intelligence.aiartifactcanvas

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A web page artifact in an `android.webkit.WebView`, the View the plan
 * names for HTML (Compose has no HTML renderer). It is a sandbox: JavaScript
 * stays off, network loads, file and content access, storage, location and
 * new windows are blocked, every link is refused so the page cannot navigate,
 * and the document carries a Content Security Policy with no network and no
 * scripts.
 */
@Composable
internal fun AiArtifactCanvasHtmlPreview(html: String, name: String, modifier: Modifier) {
    val document = MonarchAiArtifactCanvasTokens.sandboxedDocument(html)
    AndroidView(
        factory = ::aiArtifactCanvasWebView,
        modifier = modifier.semantics { contentDescription = name },
        update = { view ->
            if (view.tag != document) {
                view.tag = document
                view.loadDataWithBaseURL(null, document, "text/html", "utf-8", null)
            }
        },
        onRelease = { view ->
            view.stopLoading()
            view.destroy()
        },
    )
}

/**
 * The sandboxed web view of the preview. Everything a page could use to run
 * code, reach the network, the files or the device, or open a window is off:
 * the Content Security Policy of the document is the second wall, not the only one.
 */
internal fun aiArtifactCanvasWebView(context: Context): WebView =
    WebView(context).apply {
        settings.javaScriptEnabled = false
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.setSupportMultipleWindows(false)
        settings.blockNetworkLoads = true
        settings.blockNetworkImage = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.domStorageEnabled = false
        settings.setGeolocationEnabled(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webViewClient = AiArtifactCanvasNoNavigation()
    }

/** Refuses every navigation, so a link in the preview cannot open another page. */
private class AiArtifactCanvasNoNavigation : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
}
