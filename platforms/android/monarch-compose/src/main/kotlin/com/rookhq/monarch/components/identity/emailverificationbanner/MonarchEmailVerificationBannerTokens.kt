package com.rookhq.monarch.components.identity.emailverificationbanner

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchEmailVerificationBanner. */
internal object MonarchEmailVerificationBannerTokens {
    /** The default seconds before another send, as on the web. */
    const val DEFAULT_COOLDOWN_SECONDS: Int = 60

    /** One second of the countdown. */
    const val TICK_MILLIS: Long = 1000L

    /** The gap between the buttons. */
    val ButtonGap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between the buttons and the status line. */
    val StatusGap: Dp = MonarchTokens.Space.space1.dp

    /** The tone drawn: warning when features are locked, info when nothing is blocked. Others draw as warning. */
    fun tone(tone: MonarchTone): MonarchTone = if (tone == MonarchTone.Info) MonarchTone.Info else MonarchTone.Warning

    /** The colour of the status line: success after a send, danger after a failure. */
    fun statusColor(failed: Boolean, colors: MonarchColors): Color =
        if (failed) colors.dangerStrong else colors.successStrong
}
