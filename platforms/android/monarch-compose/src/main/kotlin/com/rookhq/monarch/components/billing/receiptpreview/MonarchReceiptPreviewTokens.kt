package com.rookhq.monarch.components.billing.receiptpreview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure rules of MonarchReceiptPreview. */
internal object MonarchReceiptPreviewTokens {
    /** The space between the button and the error under it. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The padding of the full-screen preview's top bar and content. */
    val ScreenPadding: Dp = MonarchTokens.Space.space4.dp

    /** The tallest the preview gets, so on a tall window it is a panel and not a blank screen. */
    val ScreenMaxHeight: Dp = (MonarchTokens.Space.space24 * 8).dp

    /** The colour of the "No receipt" text. */
    fun noneColor(colors: MonarchColors): Color = colors.textMuted

    /** The background of the full-screen preview. */
    fun screenColor(colors: MonarchColors): Color = colors.bgCanvas
}

/** Where a receipt preview is: idle, loading, failed or open on a loaded receipt. */
internal sealed interface ReceiptPreviewPhase {
    /** The button, nothing loaded. */
    data object Idle : ReceiptPreviewPhase

    /** The loader runs; the button shows its loading state. */
    data object Loading : ReceiptPreviewPhase

    /** The loader failed; the error and Try again are shown under the button. */
    data object Failed : ReceiptPreviewPhase

    /** The full-screen preview shows [receipt]. */
    data class Open(val receipt: MonarchReceipt) : ReceiptPreviewPhase
}
