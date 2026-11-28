package dev.monarchui.components.files.filepreview

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIcon
import dev.monarchui.components.files.filetypeicon.MonarchFileTypeIconSize
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Shows a file's content in place: a picture, the pages of a PDF, text, or
 * the app's video or audio player. Anything else, or anything that failed to
 * load, falls back to the file type icon, the name and a download action.
 *
 * Built by Monarch: Android has no single preview view, so each kind has its
 * own renderer. Pictures follow the image convention (the app loads a
 * [Painter]); PDFs are rendered page by page with the platform `PdfRenderer`
 * from a `file:` or `content:` URI; text is shown in a scrolling, selectable
 * block; video and audio take the app's player as a slot, because Monarch
 * does not depend on Media3. Monarch never downloads or opens a file itself:
 * [onDownload] is the app's.
 *
 * @param name the file name, for the type, the labels and the fallback.
 * @param modifier the modifier of the preview. It fills the width it is given.
 * @param contentType the MIME type. Decides the kind with the name.
 * @param kind forces a kind instead of detecting it. [MonarchFilePreviewKind.None] shows the fallback.
 * @param image the picture, for an image file, or `null` while there is none.
 * @param text the content of a text or code file.
 * @param pdf a `file:` or `content:` URI of a PDF the app can read.
 * @param onDownload shows a Download button that calls this; the app saves or opens the file.
 * @param failed whether loading the content failed: the fallback says so.
 * @param alwaysShowDownload shows the Download button under every preview, not only in the fallback.
 * @param imageDescription what the picture shows, for TalkBack. `null` uses the file name; an empty string
 * hides the picture when a caption next to it says the same.
 * @param labels replacements for the built-in strings.
 * @param player the video or audio player, for example a Media3 `PlayerView` in an `AndroidView`.
 * @param fallback replaces the default fallback (icon, name, message and download).
 */
@Composable
public fun MonarchFilePreview(
    name: String,
    modifier: Modifier = Modifier,
    contentType: String? = null,
    kind: MonarchFilePreviewKind? = null,
    image: Painter? = null,
    text: String? = null,
    pdf: Uri? = null,
    onDownload: (() -> Unit)? = null,
    failed: Boolean = false,
    alwaysShowDownload: Boolean = false,
    imageDescription: String? = null,
    labels: MonarchFilePreviewLabels = MonarchFilePreviewLabels(),
    player: (@Composable () -> Unit)? = null,
    fallback: (@Composable () -> Unit)? = null,
) {
    val colors = MonarchFilePreviewTokens.colors(MonarchTheme.colors)
    val detected = kind ?: remember(name, contentType) { MonarchFilePreviewKind.of(name, contentType) }
    var pdfFailed by remember(pdf) { mutableStateOf(false) }
    val hasContent = MonarchFilePreviewTokens.hasContent(
        kind = detected,
        image = image != null,
        pdf = pdf != null,
        text = text != null,
        player = player != null,
    )
    val resolution = MonarchFilePreviewTokens.resolve(detected, hasContent, failed || pdfFailed)
    val label = previewLabel(name, labels)
    val maxHeight = previewMaxHeight()
    val shape = MonarchShapeDefaults.Container
    val resources = LocalResources.current
    val download: (@Composable () -> Unit)? = onDownload?.let { action ->
        { DownloadButton(name, labels, action) }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .clip(shape)
            .border(MonarchShapeDefaults.BorderWidth, colors.border, shape)
            .background(colors.container),
    ) {
        when (resolution.kind) {
            MonarchFilePreviewKind.Image -> {
                PictureContent(checkNotNull(image), (imageDescription ?: name).ifEmpty { null }, maxHeight)
            }
            MonarchFilePreviewKind.Pdf -> MonarchFilePreviewPdf(
                uri = checkNotNull(pdf),
                label = label,
                pageLabel = { number, count ->
                    labels.page?.invoke(number, count)
                        ?: resources.getString(R.string.monarch_filepreview_page, number, count)
                },
                maxHeight = maxHeight,
                surface = colors.surface,
                onFailed = { pdfFailed = true },
            )
            MonarchFilePreviewKind.Text -> TextContent(checkNotNull(text), label, maxHeight, colors)
            MonarchFilePreviewKind.Video, MonarchFilePreviewKind.Audio ->
                MediaContent(resolution.kind == MonarchFilePreviewKind.Video, label, colors, checkNotNull(player))
            MonarchFilePreviewKind.None -> if (fallback != null) {
                fallback()
            } else {
                Fallback(name, contentType, fallbackMessage(resolution.message, labels), colors, download)
            }
        }
        if (download != null && MonarchFilePreviewTokens.showsFooter(alwaysShowDownload, resolution.kind)) {
            Footer(colors, download)
        }
    }
}

/** A picture fills the width at its own proportions, even a small one (the painter alone keeps its height). */
@Composable
private fun PictureContent(picture: Painter, description: String?, maxHeight: Dp) {
    val size = picture.intrinsicSize
    val ratio = if (size.isSpecified && size.width > 0f && size.height > 0f) size.width / size.height else null
    Image(
        painter = picture,
        contentDescription = description,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier)
            .heightIn(max = maxHeight),
        contentScale = ContentScale.Fit,
    )
}

/** At most 70% of the window height, as the web's 70vh. */
@Composable
private fun previewMaxHeight(): Dp {
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    return MonarchFilePreviewTokens.maxHeight(windowHeight)
}

@Composable
private fun previewLabel(name: String, labels: MonarchFilePreviewLabels): String =
    labels.previewOf?.invoke(name) ?: stringResource(R.string.monarch_filepreview_preview_of, name)

@Composable
private fun fallbackMessage(message: MonarchFilePreviewTokens.Message?, labels: MonarchFilePreviewLabels): String? =
    when (message) {
        MonarchFilePreviewTokens.Message.Failed -> monarchString(labels.failed, R.string.monarch_filepreview_failed)
        MonarchFilePreviewTokens.Message.Unavailable ->
            monarchString(labels.unavailable, R.string.monarch_filepreview_unavailable)
        null -> null
    }

@Composable
private fun MediaContent(
    video: Boolean,
    label: String,
    colors: MonarchFilePreviewTokens.Colors,
    player: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (video) {
                    Modifier.background(colors.media)
                } else {
                    Modifier.padding(MonarchFilePreviewTokens.TextPadding)
                },
            )
            .semantics {
                contentDescription = label
                isTraversalGroup = true
            },
        contentAlignment = Alignment.Center,
    ) { player() }
}

@Composable
private fun TextContent(text: String, label: String, maxHeight: Dp, colors: MonarchFilePreviewTokens.Colors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .background(colors.surface)
            .semantics {
                contentDescription = label
                isTraversalGroup = true
            }
            .verticalScroll(rememberScrollState())
            .padding(MonarchFilePreviewTokens.TextPadding),
    ) {
        SelectionContainer {
            Text(text = text, style = MonarchTheme.typography.code, color = colors.name)
        }
    }
}

@Composable
private fun Fallback(
    name: String,
    contentType: String?,
    message: String?,
    colors: MonarchFilePreviewTokens.Colors,
    download: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MonarchFilePreviewTokens.FallbackHorizontalPadding,
                vertical = MonarchFilePreviewTokens.FallbackVerticalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MonarchFilePreviewTokens.FallbackGap),
    ) {
        MonarchFileTypeIcon(fileName = name, contentType = contentType, size = MonarchFileTypeIconSize.Lg)
        Text(text = name, style = MonarchTheme.typography.bodyStrong, color = colors.name, textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                text = message,
                style = MonarchTheme.typography.body,
                color = colors.message,
                textAlign = TextAlign.Center,
            )
        }
        download?.invoke()
    }
}

@Composable
private fun Footer(colors: MonarchFilePreviewTokens.Colors, download: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .drawBehind {
                val line = MonarchShapeDefaults.BorderWidth.toPx()
                drawLine(colors.divider, Offset(0f, line / 2), Offset(size.width, line / 2), line)
            }
            .padding(
                horizontal = MonarchFilePreviewTokens.FooterHorizontalPadding,
                vertical = MonarchFilePreviewTokens.FooterVerticalPadding,
            ),
        horizontalArrangement = Arrangement.End,
    ) {
        download()
    }
}

@Composable
private fun DownloadButton(name: String, labels: MonarchFilePreviewLabels, onDownload: () -> Unit) {
    val spoken = labels.downloadName?.invoke(name) ?: stringResource(R.string.monarch_filepreview_download_name, name)
    MonarchButton(
        text = monarchString(labels.download, R.string.monarch_filepreview_download),
        onClick = onDownload,
        modifier = Modifier.semantics { contentDescription = spoken },
        size = MonarchButtonSize.Sm,
        leadingIcon = MonarchDefaultIcons.Download,
    )
}
