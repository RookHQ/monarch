package com.rookhq.monarch.components.files.embedblock

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and rules of MonarchEmbedBlock, built from primitives and roles only. */
internal object MonarchEmbedBlockTokens {
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /**
     * Whether the web view may navigate to [target] itself: only https pages
     * on the host of the embed address. Anything else leaves the frame.
     */
    fun staysInFrame(src: String, target: Uri): Boolean {
        val home = src.toUri().host?.lowercase(Locale.ROOT) ?: return false
        return target.scheme == "https" && target.host?.lowercase(Locale.ROOT) == home
    }

    /** The background of the frame while the page loads. */
    fun frameColor(colors: MonarchColors): Color = colors.bgMuted

    /** The colour of the caption and of the message for a URL that cannot be shown. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted
}
