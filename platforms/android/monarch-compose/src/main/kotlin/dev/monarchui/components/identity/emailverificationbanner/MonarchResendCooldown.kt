package dev.monarchui.components.identity.emailverificationbanner

import androidx.compose.runtime.Immutable

/**
 * A resend with a cooldown: the send in flight, the seconds left before
 * another send, success and failure. Pure, so it is tested without a screen.
 * Internal to MonarchEmailVerificationBanner; a candidate for `internal/` when
 * another resend control needs it.
 *
 * @property sending a send is in flight.
 * @property remaining whole seconds before another send is allowed; 0 when allowed.
 * @property sent the last send succeeded. Cleared by the next attempt.
 * @property failed the last send failed. Cleared by the next attempt.
 */
@Immutable
internal data class MonarchResendCooldown(
    val sending: Boolean = false,
    val remaining: Int = 0,
    val sent: Boolean = false,
    val failed: Boolean = false,
) {
    /** Whether a send is allowed now. */
    val canSend: Boolean get() = !sending && remaining == 0

    /** Starts a send, or `null` while sending or cooling down. */
    fun begin(): MonarchResendCooldown? = if (canSend) copy(sending = true, sent = false, failed = false) else null

    /** The send succeeded: the cooldown starts. */
    fun succeed(cooldown: Int): MonarchResendCooldown =
        copy(sending = false, sent = true, remaining = maxOf(0, cooldown))

    /** The send failed: no cooldown starts. */
    fun fail(): MonarchResendCooldown = copy(sending = false, failed = true)

    /** One second passed. */
    fun tick(): MonarchResendCooldown = copy(remaining = maxOf(0, remaining - 1))

    companion object {
        /** The state when the banner appears, with [initial] seconds of cooldown already running. */
        fun start(initial: Int): MonarchResendCooldown = MonarchResendCooldown(remaining = maxOf(0, initial))
    }
}
