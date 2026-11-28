package dev.monarchui.components.intelligence.aiartifactcanvas

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.intelligence.aiartifactcard.IconTile
import dev.monarchui.components.intelligence.aicodeblock.MonarchAiCodeBlock
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
import dev.monarchui.components.intelligence.aimarkdown.MonarchAiMarkdown
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * The working surface for an artifact a reply produced: the title and
 * version, Copy, Download and Save, an optional version stepper, and Preview
 * and Code tabs.
 *
 * A product pattern composed from Monarch components and Material 3: on a phone
 * (and with [MonarchAiArtifactCanvasPlacement.Sheet]) a full-height
 * `MonarchBottomSheet`, so the system back gesture and the drag handle close
 * it; on a wide window a pane where the app places it, closed by its close
 * button (the app's navigation keeps the back gesture). The tabs are
 * `MonarchTabs`.
 * Documents and tables preview through `MonarchAiMarkdown`, code shows in
 * `MonarchAiCodeBlock` with line numbers, an image shows the picture the app
 * loaded, and a web page renders only inside a sandboxed `WebView` with
 * JavaScript and network loads off, so model output never runs. Code and
 * charts have no built-in preview and open on the Code tab. The title is a
 * heading and the sheet is announced by its pane title.
 *
 * @param artifact the artifact version shown.
 * @param open whether the canvas is shown.
 * @param onDismissRequest called by the close button and, on a sheet, by the back gesture, a swipe down or a tap
 * outside.
 * @param modifier the modifier of the canvas.
 * @param placement a sheet on phones and a pane on wide windows, always a pane, or always a sheet.
 * @param tab the open tab; `null` lets the canvas keep it, starting on Preview.
 * @param onTabChange called when the person picks a tab.
 * @param image the picture of an image artifact, loaded by the app from the artifact's address.
 * @param onCopy called with the artifact after Copy puts its content on the clipboard.
 * @param onDownload shows Download; the app produces the file.
 * @param onSave shows Save as the primary action; it shows progress until the call returns.
 * @param closeIcon the close glyph; without one the close button shows its label as text.
 * @param versions a version stepper under the title, usually `MonarchAiArtifactVersions`.
 * @param preview replaces the built-in preview for every kind, such as a chart drawn by the app.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchAiArtifactCanvas(
    artifact: MonarchAiCanvasArtifact,
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    placement: MonarchAiArtifactCanvasPlacement = MonarchAiArtifactCanvasPlacement.Auto,
    tab: MonarchAiArtifactCanvasTab? = null,
    onTabChange: ((MonarchAiArtifactCanvasTab) -> Unit)? = null,
    image: Painter? = null,
    onCopy: ((MonarchAiCanvasArtifact) -> Unit)? = null,
    onDownload: ((MonarchAiCanvasArtifact) -> Unit)? = null,
    onSave: (suspend (MonarchAiCanvasArtifact) -> Unit)? = null,
    closeIcon: (@Composable () -> Unit)? = null,
    versions: (@Composable () -> Unit)? = null,
    preview: (@Composable (MonarchAiCanvasArtifact) -> Unit)? = null,
    labels: MonarchAiArtifactCanvasLabels = MonarchAiArtifactCanvasLabels(),
) {
    AiArtifactCanvasImpl(
        artifact = artifact,
        open = open,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        placement = placement,
        tab = tab,
        onTabChange = onTabChange,
        image = image,
        onCopy = onCopy,
        onDownload = onDownload,
        onSave = onSave,
        closeIcon = closeIcon,
        versions = versions,
        preview = preview,
        labels = labels,
        kindIcon = null,
    )
}

/**
 * [MonarchAiArtifactCanvas] with the kind's icon in a tinted tile before the title, as the web header shows it.
 *
 * @param kindIcon the icon of the artifact's kind, for example a `MonarchIcon` of a document with a `null`
 * content description; `null` shows no tile. The other parameters are those of the first overload.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun MonarchAiArtifactCanvas(
    artifact: MonarchAiCanvasArtifact,
    open: Boolean,
    onDismissRequest: () -> Unit,
    kindIcon: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    placement: MonarchAiArtifactCanvasPlacement = MonarchAiArtifactCanvasPlacement.Auto,
    tab: MonarchAiArtifactCanvasTab? = null,
    onTabChange: ((MonarchAiArtifactCanvasTab) -> Unit)? = null,
    image: Painter? = null,
    onCopy: ((MonarchAiCanvasArtifact) -> Unit)? = null,
    onDownload: ((MonarchAiCanvasArtifact) -> Unit)? = null,
    onSave: (suspend (MonarchAiCanvasArtifact) -> Unit)? = null,
    closeIcon: (@Composable () -> Unit)? = null,
    versions: (@Composable () -> Unit)? = null,
    preview: (@Composable (MonarchAiCanvasArtifact) -> Unit)? = null,
    labels: MonarchAiArtifactCanvasLabels = MonarchAiArtifactCanvasLabels(),
) {
    AiArtifactCanvasImpl(
        kindIcon = kindIcon,
        artifact = artifact,
        open = open,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        placement = placement,
        tab = tab,
        onTabChange = onTabChange,
        image = image,
        onCopy = onCopy,
        onDownload = onDownload,
        onSave = onSave,
        closeIcon = closeIcon,
        versions = versions,
        preview = preview,
        labels = labels,
    )
}

/** The shared body of both overloads. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMonarchApi::class)
@Composable
private fun AiArtifactCanvasImpl(
    artifact: MonarchAiCanvasArtifact,
    open: Boolean,
    onDismissRequest: () -> Unit,
    kindIcon: (@Composable () -> Unit)?,
    modifier: Modifier,
    placement: MonarchAiArtifactCanvasPlacement,
    tab: MonarchAiArtifactCanvasTab?,
    onTabChange: ((MonarchAiArtifactCanvasTab) -> Unit)?,
    image: Painter?,
    onCopy: ((MonarchAiCanvasArtifact) -> Unit)?,
    onDownload: ((MonarchAiCanvasArtifact) -> Unit)?,
    onSave: (suspend (MonarchAiCanvasArtifact) -> Unit)?,
    closeIcon: (@Composable () -> Unit)?,
    versions: (@Composable () -> Unit)?,
    preview: (@Composable (MonarchAiCanvasArtifact) -> Unit)?,
    labels: MonarchAiArtifactCanvasLabels,
) {
    if (!open) return
    val name = labels.label?.invoke(artifact.title)
        ?: stringResource(R.string.monarch_aiartifactcanvas_label, artifact.title)
    val body: @Composable (Modifier) -> Unit = { bodyModifier ->
        AiArtifactCanvasBody(
            artifact = artifact,
            tab = tab,
            onTabChange = onTabChange,
            labels = labels,
            modifier = bodyModifier,
            customPreview = preview != null,
            header = { AiArtifactCanvasHeader(artifact, labels, closeIcon, kindIcon, onDismissRequest) },
            toolbar = {
                AiArtifactCanvasToolbar(artifact, labels, versions, onCopy, onDownload, onSave) {
                    if (closeIcon == null) {
                        MonarchButton(
                            text = monarchString(labels.close, R.string.monarch_aiartifactcanvas_close),
                            onClick = onDismissRequest,
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                        )
                    }
                }
            },
            preview = { AiArtifactCanvasPreview(artifact, image, preview, labels) },
        )
    }
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    if (MonarchAiArtifactCanvasTokens.asSheet(placement, windowWidth)) {
        MonarchBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            fullWidthContent = true,
        ) {
            body(Modifier.fillMaxHeight().semantics { paneTitle = name })
        }
    } else {
        val paneColors = MonarchSurfaceColors(
            container = Color.Transparent,
            content = MonarchTheme.colors.textDefault,
            border = MonarchTheme.colors.borderSubtle,
        )
        Box(
            modifier = modifier
                .semantics {
                    isTraversalGroup = true
                    paneTitle = name
                }
                .monarchSurface(paneColors, MonarchTheme.shapes.lg),
        ) {
            body(Modifier)
        }
    }
}

/** The header, the toolbar, the tabs and the open tab, shared by the sheet and the pane. */
@OptIn(ExperimentalMonarchApi::class)
@Composable
private fun AiArtifactCanvasBody(
    artifact: MonarchAiCanvasArtifact,
    tab: MonarchAiArtifactCanvasTab?,
    onTabChange: ((MonarchAiArtifactCanvasTab) -> Unit)?,
    labels: MonarchAiArtifactCanvasLabels,
    modifier: Modifier,
    customPreview: Boolean,
    header: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
    preview: @Composable () -> Unit,
) {
    val tokens = MonarchAiArtifactCanvasTokens
    val previewAvailable = customPreview || tokens.hasBuiltInPreview(artifact.kind)
    var ownTab by rememberSaveable { mutableStateOf(MonarchAiArtifactCanvasTab.Preview) }
    val shown = tokens.shownTab(tab ?: ownTab, previewAvailable)
    Column(
        modifier = modifier.padding(tokens.Padding),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        header()
        toolbar()
        if (previewAvailable) {
            val tabs = MonarchAiArtifactCanvasTab.entries
            val names = tabs.map { aiArtifactCanvasTabName(it, labels) }
            MonarchSegmentedControl(
                options = names,
                selectedIndex = tabs.indexOf(shown),
                onSelect = { index ->
                    val option = tabs[index]
                    if (tab == null) ownTab = option
                    onTabChange?.invoke(option)
                },
                modifier = Modifier.fillMaxWidth(),
                size = MonarchSegmentedControlSize.Md,
                fillWidth = true,
            )
        }
        val html = artifact.kind == MonarchAiArtifactKind.Html && !customPreview
        if (shown == MonarchAiArtifactCanvasTab.Preview && html) {
            AiArtifactCanvasHtml(artifact, labels)
        } else {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            ) {
                if (shown == MonarchAiArtifactCanvasTab.Preview) {
                    preview()
                } else {
                    MonarchAiCodeBlock(
                        code = artifact.content,
                        language = artifact.language ?: tokens.defaultLanguage(artifact.kind),
                        showLineNumbers = true,
                        copyable = false,
                    )
                }
            }
        }
    }
}

/** The title as a heading, the kind and version under it, and the close button. */
@Composable
private fun AiArtifactCanvasHeader(
    artifact: MonarchAiCanvasArtifact,
    labels: MonarchAiArtifactCanvasLabels,
    closeIcon: (@Composable () -> Unit)?,
    kindIcon: (@Composable () -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val tokens = MonarchAiArtifactCanvasTokens
    val colors = MonarchTheme.colors
    val kind = labels.kind?.invoke(artifact.kind) ?: stringResource(tokens.kindName(artifact.kind))
    val version = labels.version?.invoke(artifact.version)
        ?: stringResource(R.string.monarch_aiartifactcanvas_version, artifact.version)
    val close = monarchString(labels.close, R.string.monarch_aiartifactcanvas_close)
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap), verticalAlignment = Alignment.Top) {
        if (kindIcon != null) IconTile(kindIcon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TitleGap)) {
            Text(
                text = artifact.title,
                modifier = Modifier.semantics { heading() },
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.titleColor(colors),
            )
            Text(
                text = "$kind · $version",
                style = MonarchTheme.typography.caption,
                color = tokens.mutedColor(colors),
            )
        }
        // Without a glyph the close button is text, at the end of the toolbar, so the title keeps its width.
        if (closeIcon != null) {
            MonarchIconButton(label = close, onClick = onDismissRequest, icon = closeIcon)
        }
    }
}

/** The version stepper and Copy, Download and Save. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiArtifactCanvasToolbar(
    artifact: MonarchAiCanvasArtifact,
    labels: MonarchAiArtifactCanvasLabels,
    versions: (@Composable () -> Unit)?,
    onCopy: ((MonarchAiCanvasArtifact) -> Unit)?,
    onDownload: ((MonarchAiCanvasArtifact) -> Unit)?,
    onSave: (suspend (MonarchAiCanvasArtifact) -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    val tokens = MonarchAiArtifactCanvasTokens
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    versions?.invoke()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchCopyButton(
            value = artifact.content,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            target = artifact.title,
            onCopy = { onCopy?.invoke(artifact) },
            label = monarchString(labels.copy, R.string.monarch_aiartifactcanvas_copy),
            copiedLabel = monarchString(labels.copied, R.string.monarch_aiartifactcanvas_copied),
        )
        onDownload?.let { download ->
            MonarchButton(
                text = monarchString(labels.download, R.string.monarch_aiartifactcanvas_download),
                onClick = { download(artifact) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
        onSave?.let { save ->
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_aiartifactcanvas_save),
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            try {
                                save(artifact)
                            } finally {
                                saving = false
                            }
                        }
                    }
                },
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
                loading = saving,
            )
        }
        trailing()
    }
}

/** The web page preview: the sandboxed `WebView` fills the rest of the canvas, with the sandbox note under it. */
@Composable
private fun ColumnScope.AiArtifactCanvasHtml(artifact: MonarchAiCanvasArtifact, labels: MonarchAiArtifactCanvasLabels) {
    val tokens = MonarchAiArtifactCanvasTokens
    val title = artifact.title
    AiArtifactCanvasHtmlPreview(
        html = artifact.content,
        name = labels.frameTitle?.invoke(title)
            ?: stringResource(R.string.monarch_aiartifactcanvas_frame_title, title),
        modifier = Modifier.fillMaxWidth().weight(1f),
    )
    Text(
        text = monarchString(labels.sandboxNote, R.string.monarch_aiartifactcanvas_sandbox_note),
        style = MonarchTheme.typography.caption,
        color = tokens.mutedColor(MonarchTheme.colors),
    )
}

/** The preview of every kind but a web page. */
@Composable
private fun AiArtifactCanvasPreview(
    artifact: MonarchAiCanvasArtifact,
    image: Painter?,
    custom: (@Composable (MonarchAiCanvasArtifact) -> Unit)?,
    labels: MonarchAiArtifactCanvasLabels,
) {
    val tokens = MonarchAiArtifactCanvasTokens
    when {
        custom != null -> custom(artifact)
        artifact.kind == MonarchAiArtifactKind.Image && image != null -> Image(
            painter = image,
            contentDescription = artifact.title,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
        artifact.kind == MonarchAiArtifactKind.Image -> Text(
            text = monarchString(labels.imageUnavailable, R.string.monarch_aiartifactcanvas_image_unavailable),
            style = MonarchTheme.typography.body,
            color = tokens.warningColor(MonarchTheme.colors),
        )
        artifact.kind == MonarchAiArtifactKind.Document || artifact.kind == MonarchAiArtifactKind.Table ->
            MonarchAiMarkdown(text = artifact.content)
        else -> Text(
            text = monarchString(labels.noPreview, R.string.monarch_aiartifactcanvas_no_preview),
            style = MonarchTheme.typography.body,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
    }
}

@Composable
private fun aiArtifactCanvasTabName(tab: MonarchAiArtifactCanvasTab, labels: MonarchAiArtifactCanvasLabels): String =
    when (tab) {
        MonarchAiArtifactCanvasTab.Preview -> monarchString(labels.preview, R.string.monarch_aiartifactcanvas_preview)
        MonarchAiArtifactCanvasTab.Code -> monarchString(labels.code, R.string.monarch_aiartifactcanvas_code)
    }
