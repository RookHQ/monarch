package com.rookhq.monarch.components.feedback.spinner

import androidx.compose.ui.unit.Dp
import com.rookhq.monarch.foundation.MonarchControlMetrics

/** Component tokens of MonarchSpinner. */
internal object MonarchSpinnerTokens {
    /** One turn takes 0.7 seconds, like the web. */
    const val TurnMillis: Int = 700

    fun diameter(size: MonarchSpinnerSize): Dp = when (size) {
        MonarchSpinnerSize.Sm -> MonarchControlMetrics.SpinnerSmall
        MonarchSpinnerSize.Md -> MonarchControlMetrics.SpinnerMedium
        MonarchSpinnerSize.Lg -> MonarchControlMetrics.SpinnerLarge
    }
}
