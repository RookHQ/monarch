package dev.monarchui.components.identity.emailverificationbanner

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEmailVerificationBanner`.
 * Every `null` keeps the built-in string `monarch_emailverificationbanner_<key>`.
 *
 * @property title the bold first line, "Verify your email address".
 * @property message the message, from the address: "We sent a verification link to ana@example.com. …".
 * @property resend the resend button, "Resend email".
 * @property resendIn the resend button during the cooldown, from the seconds left: "Resend in 42s".
 * @property sent shown and announced after a successful send, "Verification email sent".
 * @property changeEmail the change button, "Change email".
 * @property dismiss the dismiss button, "Dismiss".
 * @property error shown and announced when sending failed, "Couldn’t send the email. Try again."
 */
@Immutable
public class MonarchEmailVerificationBannerLabels(
    public val title: String? = null,
    public val message: ((email: String) -> String)? = null,
    public val resend: String? = null,
    public val resendIn: ((seconds: Int) -> String)? = null,
    public val sent: String? = null,
    public val changeEmail: String? = null,
    public val dismiss: String? = null,
    public val error: String? = null,
)
