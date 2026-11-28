package dev.monarchui.components.billing.receiptpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A "View receipt" button that opens a receipt the app already has in a
 * full-screen preview.
 *
 * A `MonarchButton` that opens a full-screen `Dialog` with a
 * `MonarchFilePreview` of the receipt: the picture, or the pages of a PDF.
 * Without a receipt, the muted text "No receipt" stands in place of the button,
 * as plain text rather than a disabled button. For a private file that should
 * be fetched only when someone opens it, use the overload with `load`.
 *
 * @param receipt the loaded receipt, or `null` when the expense has none.
 * @param modifier the modifier of the button, or of the "No receipt" text.
 * @param variant the visual weight of the button.
 * @param size the visible height of the button. The touch target is at least 48dp at every size.
 * @param enabled whether the user can open the receipt.
 * @param icon an icon before the label, for example a paperclip `MonarchIcon` with a `null` content
 * description. Monarch bundles no icons.
 * @param onDownload shows a Download button in the preview; called with the receipt so the app saves it.
 * @param labels replacements for the built-in strings.
 * @param initiallyOpen whether the preview starts open, for a docs preview or a restored screen.
 */
@Composable
public fun MonarchReceiptPreview(
    receipt: MonarchReceipt?,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Ghost,
    size: MonarchButtonSize = MonarchButtonSize.Sm,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onDownload: ((MonarchReceipt) -> Unit)? = null,
    labels: MonarchReceiptPreviewLabels = MonarchReceiptPreviewLabels(),
    initiallyOpen: Boolean = false,
) {
    if (receipt == null) {
        MonarchText(
            text = monarchString(labels.none, R.string.monarch_receiptpreview_none),
            modifier = modifier,
            style = MonarchTextStyle.Caption,
            color = MonarchReceiptPreviewTokens.noneColor(MonarchTheme.colors),
        )
        return
    }
    var open by remember { mutableStateOf(initiallyOpen) }
    ViewButton(onClick = { open = true }, modifier, variant, size, enabled, loading = false, icon, labels)
    if (open) {
        ReceiptScreen(receipt = receipt, onClose = { open = false }, onDownload = onDownload, labels = labels)
    }
}

/**
 * A "View receipt" button that fetches a private receipt only when pressed,
 * then shows it full screen.
 *
 * Pressing the button runs [load], the app's code that fetches the file with
 * its own authorisation and returns it; nothing is loaded before. While it
 * runs, the button shows its loading state and TalkBack reads "Loading
 * receipt". When it fails, "The receipt could not be loaded." and Try again
 * appear under the button and are announced. Leaving the screen cancels the
 * load.
 *
 * @param load fetches the receipt. Run on each press and on Try again. Throw to report a failure.
 * @param modifier the modifier of the button and the error under it.
 * @param variant the visual weight of the button.
 * @param size the visible height of the button. The touch target is at least 48dp at every size.
 * @param enabled whether the user can open the receipt.
 * @param icon an icon before the label, for example a paperclip `MonarchIcon` with a `null` content
 * description.
 * @param onDownload shows a Download button in the preview; called with the receipt so the app saves it.
 * @param onLoadError called with the failure of [load], for logging. The error is shown either way.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchReceiptPreview(
    load: suspend () -> MonarchReceipt,
    modifier: Modifier = Modifier,
    variant: MonarchButtonVariant = MonarchButtonVariant.Ghost,
    size: MonarchButtonSize = MonarchButtonSize.Sm,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    onDownload: ((MonarchReceipt) -> Unit)? = null,
    onLoadError: ((Throwable) -> Unit)? = null,
    labels: MonarchReceiptPreviewLabels = MonarchReceiptPreviewLabels(),
) {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf<ReceiptPreviewPhase>(ReceiptPreviewPhase.Idle) }
    var job by remember { mutableStateOf<Job?>(null) }
    val start = {
        job?.cancel()
        job = scope.launch {
            phase = ReceiptPreviewPhase.Loading
            phase = try {
                ReceiptPreviewPhase.Open(load())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
                onLoadError?.invoke(failure)
                ReceiptPreviewPhase.Failed
            }
        }
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MonarchReceiptPreviewTokens.Gap),
    ) {
        ViewButton(
            onClick = start,
            modifier = Modifier,
            variant = variant,
            size = size,
            enabled = enabled,
            loading = phase == ReceiptPreviewPhase.Loading,
            icon = icon,
            labels = labels,
        )
        if (phase == ReceiptPreviewPhase.Failed) {
            MonarchInlineErrorRetry(
                message = monarchString(labels.loadError, R.string.monarch_receiptpreview_load_error),
                onRetry = start,
                retryLabel = labels.retry,
            )
        }
    }
    val current = phase
    if (current is ReceiptPreviewPhase.Open) {
        ReceiptScreen(
            receipt = current.receipt,
            onClose = { phase = ReceiptPreviewPhase.Idle },
            onDownload = onDownload,
            labels = labels,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun ViewButton(
    onClick: () -> Unit,
    modifier: Modifier,
    variant: MonarchButtonVariant,
    size: MonarchButtonSize,
    enabled: Boolean,
    loading: Boolean,
    icon: (@Composable () -> Unit)?,
    labels: MonarchReceiptPreviewLabels,
) {
    MonarchButton(
        text = monarchString(labels.view, R.string.monarch_receiptpreview_view),
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        loading = loading,
        leadingIcon = icon,
        loadingLabel = monarchString(labels.loading, R.string.monarch_receiptpreview_loading),
    )
}
