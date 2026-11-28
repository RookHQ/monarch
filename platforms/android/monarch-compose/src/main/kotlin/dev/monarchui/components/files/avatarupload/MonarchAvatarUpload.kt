package dev.monarchui.components.files.avatarupload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.files.fileupload.FileUploadMessages
import dev.monarchui.components.files.fileupload.MonarchFileUploadTokens
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchFormatBytes
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Picks a profile or workspace picture with the system photo picker and
 * crops it to a circle (or a square) before handing it over.
 *
 * Built by Monarch: the picker is the system's `PickVisualMedia` (no storage
 * permission), and the crop editor is a Compose `Canvas` with
 * `Modifier.transformable`: drag to move, pinch or the Material 3 `Slider`
 * to zoom, arrow keys and plus and minus on a keyboard, and TalkBack actions
 * to move and zoom. Type and size are checked as in `MonarchFileUpload`. The
 * picked image is decoded off the main thread and scaled down to at most
 * 2048 pixels; Save draws the kept square into an [outputSize] bitmap and
 * calls [onChange], keeping Save busy until it returns. When [onChange]
 * throws, "The photo could not be saved. Try again." is shown and the
 * editor stays open.
 *
 * @param name the person's or workspace's name, for the initials and the TalkBack name of the picture.
 * @param image the current picture, or `null` for the initials.
 * @param onChange stores the cropped picture. Suspend until it is stored; throw to report a failure.
 * @param modifier the modifier of the component.
 * @param shape a circle for people, a square for workspaces and organisations. Also the crop mask.
 * @param enabled whether the user can change the picture.
 * @param accept allowed image types, as MIME types, wildcards or extensions.
 * @param maxSize the largest file accepted, in bytes.
 * @param outputSize the width and height of the cropped picture, in pixels.
 * @param onRemove removes the picture. Without it, or without [image], there is no Remove button.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchAvatarUpload(
    name: String,
    image: Painter?,
    onChange: suspend (MonarchAvatarUploadResult) -> Unit,
    modifier: Modifier = Modifier,
    shape: MonarchAvatarShape = MonarchAvatarShape.Circle,
    enabled: Boolean = true,
    accept: List<String> = listOf("image/png", "image/jpeg", "image/webp", "image/gif"),
    maxSize: Long? = null,
    outputSize: Int = MonarchAvatarUploadTokens.OUTPUT_SIZE,
    onRemove: (() -> Unit)? = null,
    labels: MonarchAvatarUploadLabels = MonarchAvatarUploadLabels(),
) {
    val resolver = LocalContext.current.contentResolver
    AvatarUploadContent(
        name = name,
        image = image,
        onChange = onChange,
        modifier = modifier,
        shape = shape,
        enabled = enabled,
        accept = accept,
        maxSize = maxSize,
        outputSize = outputSize,
        onRemove = onRemove,
        labels = labels,
        decode = { MonarchAvatarUploadDecoder.decode(resolver, it.uri, MonarchAvatarUploadTokens.MAX_DECODE_SIDE) },
        workContext = Dispatchers.Default,
    )
}

/** A picked file and its decoded image. */
internal class AvatarUploadPicked(val file: MonarchPickedFile, val image: ImageBitmap)

/**
 * The body of [MonarchAvatarUpload], with the decoder and the context of the
 * decoding and drawing work given, so tests can run them in place.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught")
@Composable
internal fun AvatarUploadContent(
    name: String,
    image: Painter?,
    onChange: suspend (MonarchAvatarUploadResult) -> Unit,
    modifier: Modifier,
    shape: MonarchAvatarShape,
    enabled: Boolean,
    accept: List<String>,
    maxSize: Long?,
    outputSize: Int,
    onRemove: (() -> Unit)?,
    labels: MonarchAvatarUploadLabels,
    decode: (MonarchPickedFile) -> ImageBitmap?,
    workContext: CoroutineContext,
) {
    val tokens = MonarchAvatarUploadTokens
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<AvatarUploadPicked?>(null) }
    var view by remember { mutableStateOf(AvatarUploadView()) }
    var error by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var returnFocus by remember { mutableStateOf(false) }
    val pickerFocus = remember { FocusRequester() }
    val editorFocus = remember { FocusRequester() }
    val saveFailed = monarchString(labels.saveFailed, R.string.monarch_avatarupload_save_failed)
    val openFailed = monarchString(labels.openFailed, R.string.monarch_avatarupload_open_failed)
    val pick: (Uri) -> Unit = { uri ->
        val file = MonarchPickedFile.of(context.contentResolver, uri)
        val check = MonarchFileUploadTokens.check(listOf(file), accept, maxSize, null, 0, false, null).single()
        if (check.code != null) {
            val formatSize = labels.validation.formatSize ?: { monarchFormatBytes(it) }
            error = FileUploadMessages(resources, labels.validation, formatSize)
                .refusal(check, maxSize ?: 0L, 1)
        } else {
            scope.launch {
                val decoded = withContext(workContext) { decode(file) }
                if (decoded == null) {
                    error = openFailed
                } else {
                    error = ""
                    view = AvatarUploadView()
                    picked = AvatarUploadPicked(file, decoded)
                }
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri -> if (uri != null) pick(uri) }
    val close = {
        picked = null
        saving = false
        returnFocus = true
    }
    val save: () -> Unit = save@{
        val current = picked ?: return@save
        if (saving || !enabled) return@save
        saving = true
        error = ""
        scope.launch {
            val crop = tokens.cropRect(current.image.width, current.image.height, view)
            try {
                val output = withContext(workContext) { avatarUploadRender(current.image, crop, outputSize) }
                onChange(MonarchAvatarUploadResult(output, current.file, crop))
                close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The app's save may fail with any exception; each one is shown as a failed save.
                saving = false
                error = saveFailed
            }
        }
    }
    LaunchedEffect(picked) {
        withFrameNanos { }
        if (picked != null) {
            editorFocus.requestFocus()
        } else if (returnFocus) {
            returnFocus = false
            pickerFocus.requestFocus()
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        val editing = picked
        if (editing != null) {
            AvatarUploadEditor(
                image = editing.image,
                view = view,
                onViewChange = { view = it },
                shape = shape,
                saving = saving,
                enabled = enabled,
                onSave = save,
                onCancel = close,
                strings = avatarUploadEditorStrings(labels),
                focusRequester = editorFocus,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchAvatar(image = image, name = name, shape = shape, size = MonarchAvatarSize.Lg)
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
                ) {
                    MonarchButton(
                        text = if (image == null) {
                            monarchString(labels.upload, R.string.monarch_avatarupload_upload)
                        } else {
                            monarchString(labels.change, R.string.monarch_avatarupload_change)
                        },
                        onClick = {
                            launcher.launch(PickVisualMediaRequest(MonarchFileUploadTokens.mediaType(accept)))
                        },
                        modifier = Modifier.focusRequester(pickerFocus),
                        size = MonarchButtonSize.Sm,
                        enabled = enabled,
                    )
                    if (image != null && onRemove != null) {
                        MonarchButton(
                            text = monarchString(labels.remove, R.string.monarch_avatarupload_remove),
                            onClick = onRemove,
                            variant = MonarchButtonVariant.Ghost,
                            size = MonarchButtonSize.Sm,
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        if (error.isNotEmpty()) {
            Text(
                text = error,
                style = MonarchTheme.typography.caption,
                color = tokens.errorColor(MonarchTheme.colors),
            )
        }
        // Always present, so each new error is announced as a change of this live region.
        MonarchLiveStatus(text = error.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun avatarUploadEditorStrings(labels: MonarchAvatarUploadLabels): AvatarUploadEditorStrings {
    val zoomFormat = stringResource(R.string.monarch_avatarupload_zoom_value)
    return AvatarUploadEditorStrings(
        cropArea = monarchString(labels.cropArea, R.string.monarch_avatarupload_crop_area),
        cropHint = monarchString(labels.cropHint, R.string.monarch_avatarupload_crop_hint),
        zoom = monarchString(labels.zoom, R.string.monarch_avatarupload_zoom),
        zoomValue = labels.zoomValue ?: { zoomFormat.format(it) },
        moveLeft = monarchString(labels.moveLeft, R.string.monarch_avatarupload_move_left),
        moveRight = monarchString(labels.moveRight, R.string.monarch_avatarupload_move_right),
        moveUp = monarchString(labels.moveUp, R.string.monarch_avatarupload_move_up),
        moveDown = monarchString(labels.moveDown, R.string.monarch_avatarupload_move_down),
        zoomIn = monarchString(labels.zoomIn, R.string.monarch_avatarupload_zoom_in),
        zoomOut = monarchString(labels.zoomOut, R.string.monarch_avatarupload_zoom_out),
        save = monarchString(labels.save, R.string.monarch_avatarupload_save),
        cancel = monarchString(labels.cancel, R.string.monarch_avatarupload_cancel),
    )
}

/** Draws the kept square of [image] into a new square bitmap of [outputSize] pixels. */
internal fun avatarUploadRender(image: ImageBitmap, crop: MonarchAvatarCrop, outputSize: Int): ImageBitmap {
    val output = ImageBitmap(outputSize, outputSize)
    val side = crop.size.roundToInt().coerceAtLeast(1)
    Canvas(output).drawImageRect(
        image = image,
        srcOffset = IntOffset(crop.x.roundToInt(), crop.y.roundToInt()),
        srcSize = IntSize(side, side),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(outputSize, outputSize),
        paint = Paint().apply { filterQuality = FilterQuality.High },
    )
    return output
}
