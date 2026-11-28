package dev.monarchui.components.identity.usernameavailabilityinput

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The state of a check: the status and the check's own message, if it gave one. */
@Immutable
internal data class MonarchUsernameAvailabilityCheck(
    val status: MonarchUsernameAvailabilityStatus,
    val message: String? = null,
)

/** Component tokens and the pure logic of MonarchUsernameAvailabilityInput. */
internal object MonarchUsernameAvailabilityInputTokens {
    /** The default time typing must pause before the check runs, as on the web. */
    const val DEFAULT_DEBOUNCE_MILLIS: Long = 400L

    val SpinnerSize: Dp = MonarchTokens.Space.space5.dp
    val SpinnerStrokeWidth: Dp = MonarchTokens.Space.space0_5.dp
    val MessageGap: Dp = MonarchTokens.Space.space2.dp
    val MessageTopPadding: Dp = MonarchTokens.Space.space1_5.dp

    /** The state after the check ended: its result, or the error status when it threw. */
    fun checkOf(outcome: Result<MonarchUsernameAvailabilityResult?>): MonarchUsernameAvailabilityCheck {
        if (outcome.isFailure) return MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Error)
        val result = outcome.getOrNull()
            ?: return MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Idle)
        val status = when (result.valid) {
            true -> MonarchUsernameAvailabilityStatus.Valid
            false -> MonarchUsernameAvailabilityStatus.Invalid
        }
        return MonarchUsernameAvailabilityCheck(status, result.message)
    }

    /** Whether the status is a settled result that is announced. "Checking" on every pause would be noise. */
    fun announces(status: MonarchUsernameAvailabilityStatus): Boolean =
        status == MonarchUsernameAvailabilityStatus.Valid ||
            status == MonarchUsernameAvailabilityStatus.Invalid ||
            status == MonarchUsernameAvailabilityStatus.Error

    /** The colour of the status message. */
    fun messageColor(status: MonarchUsernameAvailabilityStatus, colors: MonarchColors): Color = when (status) {
        MonarchUsernameAvailabilityStatus.Idle, MonarchUsernameAvailabilityStatus.Checking -> colors.textMuted
        MonarchUsernameAvailabilityStatus.Valid -> colors.successStrong
        MonarchUsernameAvailabilityStatus.Invalid -> colors.dangerStrong
        MonarchUsernameAvailabilityStatus.Error -> colors.warningStrong
    }
}
