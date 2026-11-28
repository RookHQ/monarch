package dev.monarchui.components.files.fileupload

import android.content.res.Resources
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchSurfaceColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchFormatBytes
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * Chooses files with the system picker, checks their type, size and count,
 * explains refusals and hands the accepted files to the app, which uploads them.
 *
 * Built by Monarch around the system pickers from `androidx.activity`:
 * `OpenMultipleDocuments` (or `OpenDocument` for one file) for
 * [MonarchFileUploadPicker.Documents], and `PickMultipleVisualMedia` (or
 * `PickVisualMedia`) for [MonarchFileUploadPicker.Media], which needs no
 * storage permission. The pickers are the system's; Monarch adds the zone,
 * the limits as a hint, the checks, the refusal messages in a
 * `MonarchInlineNotice` and a polite "2 files added." announcement. Pair it
 * with `MonarchUploadQueue` to show the uploads.
 *
 * The name, type and size of each file are read from its provider on the
 * main thread when the picker returns; the content is never read.
 *
 * @param onFiles called after every pick with the accepted files and the refused ones, with their messages.
 * @param modifier the modifier of the component.
 * @param variant a bordered zone, or one compact line.
 * @param picker the document picker or the photo picker.
 * @param accept allowed types: MIME types (`application/pdf`), wildcards (`image/` and a star) and extensions
 * (`.csv`). Empty allows every file. Also filters the picker where the system can.
 * @param maxSize the largest size per file, in bytes.
 * @param maxFiles the most files in total, counting [currentCount].
 * @param currentCount files already attached, which count towards [maxFiles]. At [maxFiles] the button is disabled.
 * @param multiple lets the picker return several files. By default unless [maxFiles] is 1.
 * @param enabled whether the user can choose files.
 * @param invalid draws the zone with the danger border and sets an error on the button, for example when a
 * required file is missing.
 * @param showErrors shows the refusal messages under the zone. `false` leaves them to the app, from [onFiles].
 * @param icon an icon at the start of the zone. Decorative; Monarch bundles no icons.
 * @param hint the text under the label. `null` writes the limits: "Up to 10 MB each · 5 files max".
 * @param errorText the error set on the button while [invalid], for TalkBack. Shown by the app's form field.
 * @param validate the app's own check: return a message to refuse a file, or `null` to accept it.
 * @param labels replacements for the built-in strings.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchFileUpload(
    onFiles: (accepted: List<MonarchPickedFile>, rejected: List<MonarchFileRejection>) -> Unit,
    modifier: Modifier = Modifier,
    variant: MonarchFileUploadVariant = MonarchFileUploadVariant.Zone,
    picker: MonarchFileUploadPicker = MonarchFileUploadPicker.Documents,
    accept: List<String> = emptyList(),
    maxSize: Long? = null,
    maxFiles: Int? = null,
    currentCount: Int = 0,
    multiple: Boolean = maxFiles != 1,
    enabled: Boolean = true,
    invalid: Boolean = false,
    showErrors: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    hint: String? = null,
    errorText: String? = null,
    validate: ((MonarchPickedFile) -> String?)? = null,
    labels: MonarchFileUploadLabels = MonarchFileUploadLabels(),
) {
    val tokens = MonarchFileUploadTokens
    val context = LocalContext.current
    val resources = LocalResources.current
    val full = tokens.full(maxFiles, currentCount)
    val blocked = !enabled || full
    var rejections by rememberSaveable { mutableStateOf(listOf<String>()) }
    var status by rememberSaveable { mutableStateOf("") }
    val formatSize: (Long) -> String = labels.formatSize ?: { monarchFormatBytes(it) }
    val take: (List<Uri>) -> Unit = { uris ->
        if (!blocked && uris.isNotEmpty()) {
            val files = uris.map { MonarchPickedFile.of(context.contentResolver, it) }
            val result = MonarchFileUploadValidator.validate(
                context = context,
                files = files,
                accept = accept,
                maxSize = maxSize,
                maxFiles = maxFiles,
                currentCount = currentCount,
                multiple = multiple,
                labels = labels,
                validate = validate,
            )
            rejections = result.rejected.map { it.message }
            status = FileUploadMessages(resources, labels, formatSize).added(result.accepted.size)
            onFiles(result.accepted, result.rejected)
        }
    }
    val browse = rememberFileUploadBrowse(picker, accept, multiple, take)
    val hintText = fileUploadHint(full, maxFiles, hint, maxSize?.let(formatSize), labels)
    val buttonError = MonarchFileUploadTokens.buttonError(invalid, errorText, rejections)
    val button: @Composable () -> Unit = {
        MonarchButton(
            text = monarchString(labels.browse, R.string.monarch_fileupload_browse),
            onClick = browse,
            modifier = Modifier.semantics { if (buttonError != null) error(buttonError) },
            size = MonarchButtonSize.Sm,
            enabled = !blocked,
        )
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        when (variant) {
            MonarchFileUploadVariant.Zone -> FileUploadZone(
                label = monarchString(labels.label, R.string.monarch_fileupload_label),
                hint = hintText,
                enabled = !blocked,
                invalid = invalid || rejections.isNotEmpty(),
                icon = icon,
                button = button,
            )
            MonarchFileUploadVariant.Compact -> Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                button()
                if (hintText.isNotEmpty()) FileUploadHint(hintText, !blocked, Modifier.weight(1f, fill = false))
            }
        }
        if (showErrors && rejections.isNotEmpty()) {
            MonarchInlineNotice(
                message = rejections.joinToString("\n"),
                tone = MonarchTone.Danger,
                variant = MonarchInlineNoticeVariant.Plain,
                announcement = MonarchBannerAnnouncement.Polite,
            )
        }
        // Always present, so each pick is announced as a change of this live region.
        MonarchLiveStatus(text = status.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** Registers the four system pickers and returns the action that opens the one asked for. */
@Composable
private fun rememberFileUploadBrowse(
    picker: MonarchFileUploadPicker,
    accept: List<String>,
    multiple: Boolean,
    take: (List<Uri>) -> Unit,
): () -> Unit {
    val tokens = MonarchFileUploadTokens
    val openDocuments = rememberLauncherForActivityResult(OpenMultipleDocuments()) { take(it) }
    val openDocument = rememberLauncherForActivityResult(OpenDocument()) { take(listOfNotNull(it)) }
    val pickMedia = rememberLauncherForActivityResult(PickMultipleVisualMedia()) { take(it) }
    val pickMedium = rememberLauncherForActivityResult(PickVisualMedia()) { take(listOfNotNull(it)) }
    return {
        when (picker) {
            MonarchFileUploadPicker.Documents -> {
                val types = tokens.documentTypes(accept) { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
                if (multiple) openDocuments.launch(types) else openDocument.launch(types)
            }
            MonarchFileUploadPicker.Media -> {
                val request = PickVisualMediaRequest(tokens.mediaType(accept))
                if (multiple) pickMedia.launch(request) else pickMedium.launch(request)
            }
        }
    }
}

/** The hint: the limit reached, the app's hint, or the limits. */
@Composable
private fun fileUploadHint(
    full: Boolean,
    maxFiles: Int?,
    hint: String?,
    maxSize: String?,
    labels: MonarchFileUploadLabels,
): String = when {
    full && maxFiles != null -> labels.limitReached?.invoke(maxFiles)
        ?: pluralStringResource(R.plurals.monarch_fileupload_limit_reached, maxFiles, maxFiles)
    hint != null -> hint
    else -> limitsHint(maxSize, maxFiles, labels)
}

@Composable
private fun FileUploadZone(
    label: String,
    hint: String,
    enabled: Boolean,
    invalid: Boolean,
    icon: (@Composable () -> Unit)?,
    button: @Composable () -> Unit,
) {
    val tokens = MonarchFileUploadTokens
    val colors = MonarchTheme.colors
    val zone = tokens.zoneColors(invalid, enabled, colors)
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .monarchSurface(MonarchSurfaceColors(zone.container, zone.content, null), shape)
            .drawBehind {
                val width = MonarchTokens.Space.px.dp.toPx()
                drawRoundRect(
                    color = zone.border ?: Color.Transparent,
                    topLeft = Offset(width / 2, width / 2),
                    size = Size(size.width - width, size.height - width),
                    cornerRadius = CornerRadius(shape.topStart.toPx(size, this)),
                    style = Stroke(
                        width = width,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(MonarchTokens.Space.space1_5.dp.toPx(), MonarchTokens.Space.space1.dp.toPx()),
                        ),
                    ),
                )
            }
            .padding(tokens.ZonePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Box(
            modifier = Modifier
                .size(MonarchTokens.Space.space10.dp)
                .background(colors.bgSurface, CircleShape)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides tokens.labelColor(enabled, colors)) {
                (icon ?: MonarchDefaultIcons.Upload)()
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.TextGap),
        ) {
            Text(
                text = label,
                style = MonarchTheme.typography.bodyStrong,
                color = tokens.labelColor(enabled, colors),
                textAlign = TextAlign.Center,
            )
            if (hint.isNotEmpty()) FileUploadHint(hint, enabled)
        }
        button()
    }
}

@Composable
private fun FileUploadHint(text: String, enabled: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MonarchTheme.typography.caption,
        color = MonarchFileUploadTokens.hintColor(enabled, MonarchTheme.colors),
    )
}

/** The hint written from the limits, or an empty string without limits. */
@Composable
private fun limitsHint(maxSize: String?, maxFiles: Int?, labels: MonarchFileUploadLabels): String {
    labels.limits?.let { return it(maxSize, maxFiles) }
    val parts = listOfNotNull(
        maxSize?.let { stringResource(R.string.monarch_fileupload_limit_size, it) },
        maxFiles?.let { pluralStringResource(R.plurals.monarch_fileupload_limit_count, it, it) },
    )
    return parts.joinToString(stringResource(R.string.monarch_fileupload_limit_separator))
}

/** The refusal messages and the announcement, from the labels or the built-in strings. */
internal class FileUploadMessages(
    private val resources: Resources,
    private val labels: MonarchFileUploadLabels,
    private val formatSize: (Long) -> String,
) {
    /** The message for a refused file; [maxFiles] is the count limit that applies. */
    fun refusal(check: FileUploadCheck, maxSize: Long, maxFiles: Int): String {
        val name = check.file.name
        return when (check.code) {
            MonarchFileRejectionCode.InvalidType -> labels.invalidType?.invoke(name)
                ?: resources.getString(R.string.monarch_fileupload_invalid_type, name)
            MonarchFileRejectionCode.TooLarge -> formatSize(maxSize).let { max ->
                labels.tooLarge?.invoke(name, max)
                    ?: resources.getString(R.string.monarch_fileupload_too_large, name, max)
            }
            MonarchFileRejectionCode.TooMany -> labels.tooMany?.invoke(name, maxFiles)
                ?: resources.getQuantityString(R.plurals.monarch_fileupload_too_many, maxFiles, name, maxFiles)
            MonarchFileRejectionCode.Custom, null -> check.custom.orEmpty()
        }
    }

    /** The refused files of [checks], with their messages; [maxFiles] is the count limit that applies. */
    fun rejections(checks: List<FileUploadCheck>, maxSize: Long?, maxFiles: Int?): List<MonarchFileRejection> =
        checks.mapNotNull { check ->
            check.code?.let { MonarchFileRejection(check.file, it, refusal(check, maxSize ?: 0L, maxFiles ?: 0)) }
        }

    /** The announcement after [count] files are accepted, or an empty string for none. */
    fun added(count: Int): String = when {
        count == 0 -> ""
        else -> labels.added?.invoke(count)
            ?: resources.getQuantityString(R.plurals.monarch_fileupload_added, count, count)
    }
}
