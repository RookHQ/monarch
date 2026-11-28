package dev.monarchui.components.billing.receiptpreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.monarchui.R
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.files.filepreview.MonarchFilePreview
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The preview of a receipt: a `Dialog` the width of the screen, as tall as its content up to
 * [MonarchReceiptPreviewTokens.ScreenMaxHeight], with a heading, a Close button and the `MonarchFilePreview`.
 * Back closes it.
 */
@Composable
internal fun ReceiptScreen(
    receipt: MonarchReceipt,
    onClose: () -> Unit,
    onDownload: ((MonarchReceipt) -> Unit)?,
    labels: MonarchReceiptPreviewLabels,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            modifier = Modifier.heightIn(max = MonarchReceiptPreviewTokens.ScreenMaxHeight).fillMaxWidth(),
            color = MonarchReceiptPreviewTokens.screenColor(MonarchTheme.colors),
            contentColor = MonarchTheme.colors.textDefault,
        ) {
            Column(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(MonarchReceiptPreviewTokens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(MonarchReceiptPreviewTokens.Gap),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MonarchText(
                        text = monarchString(labels.title, R.string.monarch_receiptpreview_title),
                        modifier = Modifier.weight(1f),
                        style = MonarchTextStyle.Heading,
                    )
                    MonarchIconButton(
                        label = monarchString(labels.close, R.string.monarch_receiptpreview_close),
                        onClick = onClose,
                    ) {
                        MonarchDefaultIcons.Close()
                    }
                }
                MonarchFilePreview(
                    name = receipt.name,
                    modifier = Modifier.fillMaxWidth(),
                    contentType = receipt.contentType,
                    image = receipt.image,
                    pdf = receipt.pdf,
                    onDownload = onDownload?.let { download -> { download(receipt) } },
                    alwaysShowDownload = true,
                )
            }
        }
    }
}
