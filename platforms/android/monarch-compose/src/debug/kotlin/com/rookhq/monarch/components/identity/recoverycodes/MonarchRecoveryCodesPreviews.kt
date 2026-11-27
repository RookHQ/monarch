package com.rookhq.monarch.components.identity.recoverycodes

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRecoveryCodesPreview() {
    val codes = listOf(
        MonarchRecoveryCode("4f7k-2m9q"),
        MonarchRecoveryCode("8d3r-6t1w", used = true),
        MonarchRecoveryCode("7n4h-3j8k"),
        MonarchRecoveryCode("2p6s-5l0z"),
    )
    PreviewFrame {
        MonarchRecoveryCodes(codes = codes, onRegenerate = {}, onAcknowledge = {})
        MonarchRecoveryCodes(codes = codes, initiallyRevealed = false)
    }
}
