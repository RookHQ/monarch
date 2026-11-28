package dev.monarchui.components.files.embedblock

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import dev.monarchui.R
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Shows a video, design or demo from an allow-listed site in a web view, and
 * any other web link as a link card. Only a provider's own embed address is
 * ever loaded; the pasted URL itself never is.
 *
 * Built on `android.webkit.WebView` through `AndroidView`, because Compose
 * has no web view. The URL is decided by [MonarchEmbedResolution.resolve]:
 * an https share link on a host of [providers] becomes the provider's embed
 * address in a frame of its aspect ratio; any other http or https link is a
 * `MonarchLinkCard`; anything else is a short message. The web view runs the
 * provider's scripts (players need them) but has no file or content access,
 * no geolocation, no new windows, no mixed content and no JavaScript bridge,
 * and media start only on a tap. A link inside the frame that leaves the
 * provider's host opens outside the app, through [onOpenLink] or
 * `LocalUriHandler`.
 *
 * @param url the URL someone pasted: a share link, not an embed code.
 * @param modifier the modifier of the block.
 * @param caption text under the frame or the card. Also the TalkBack name of the frame.
 * @param title the TalkBack name of the frame when there is no caption. `null` uses "YouTube content".
 * @param providers the sites that may be framed.
 * @param onOpenLink called with a link to open outside the app: the link card, or a link that leaves the frame.
 * `null` opens it with `LocalUriHandler`.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchEmbedBlock(
    url: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    title: String? = null,
    providers: List<MonarchEmbedProvider> = MonarchEmbedProvider.Defaults,
    onOpenLink: ((String) -> Unit)? = null,
    labels: MonarchEmbedBlockLabels = MonarchEmbedBlockLabels(),
) {
    val tokens = MonarchEmbedBlockTokens
    val colors = MonarchTheme.colors
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = onOpenLink ?: { uriHandler.openUri(it) }
    val resolution = MonarchEmbedResolution.resolve(url, providers)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        when (resolution) {
            is MonarchEmbedResolution.Frame -> {
                val frameTitle = caption ?: title
                    ?: labels.frameTitle?.invoke(resolution.provider.name)
                    ?: stringResource(R.string.monarch_embedblock_frame_title, resolution.provider.name)
                EmbedBlockFrame(resolution, frameTitle, open)
            }
            is MonarchEmbedResolution.Link -> EmbedBlockLinkCard(resolution, open)
            is MonarchEmbedResolution.Invalid -> Text(
                text = monarchString(labels.invalid, R.string.monarch_embedblock_invalid),
                style = MonarchTheme.typography.body,
                color = tokens.mutedColor(colors),
            )
        }
        if (caption != null) {
            Text(
                text = caption,
                modifier = Modifier.fillMaxWidth(),
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(colors),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The card of a link that cannot be framed: the host in bold and the address in the link colour, as on the web. */
@Composable
private fun EmbedBlockLinkCard(link: MonarchEmbedResolution.Link, open: (String) -> Unit) {
    MonarchCard(
        onClick = { open(link.url) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MonarchTheme.spacing.space3),
    ) {
        Text(text = link.host, style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault)
        Text(text = link.url, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textLink)
    }
}

@Composable
private fun EmbedBlockFrame(frame: MonarchEmbedResolution.Frame, frameTitle: String, open: (String) -> Unit) {
    val background = MonarchEmbedBlockTokens.frameColor(MonarchTheme.colors)
    val currentOpen by rememberUpdatedState(open)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(frame.provider.aspectRatio)
            .clip(MonarchShapeDefaults.Container)
            .background(background)
            .border(
                MonarchShapeDefaults.BorderWidth,
                MonarchTheme.colors.borderDefault,
                MonarchShapeDefaults.Container,
            )
            .semantics { contentDescription = frameTitle },
    ) {
        // Previews cannot run a web view; the tinted frame keeps its place.
        if (!LocalInspectionMode.current) {
            AndroidView(
                factory = { context -> embedBlockWebView(context, frame.src) { currentOpen(it) } },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.setBackgroundColor(background.toArgb())
                    if (view.tag != frame.src) {
                        view.tag = frame.src
                        view.loadUrl(frame.src)
                    }
                },
                onRelease = { view ->
                    view.stopLoading()
                    view.destroy()
                },
            )
        }
    }
}

/**
 * A web view locked down for one provider's embed address. Scripts run,
 * because players need them; everything a page could use to reach the app
 * or the device is off.
 */
@SuppressLint("SetJavaScriptEnabled")
internal fun embedBlockWebView(context: Context, src: String, open: (String) -> Unit): WebView =
    WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setGeolocationEnabled(false)
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.mediaPlaybackRequiresUserGesture = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (MonarchEmbedBlockTokens.staysInFrame(src, request.url)) return false
                val scheme = request.url.scheme
                if (request.hasGesture() && (scheme == "https" || scheme == "http")) open(request.url.toString())
                return true
            }
        }
    }
