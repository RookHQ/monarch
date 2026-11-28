package dev.monarchui.components.identity.stepupauthdialog

/** What a `MonarchStepUpAuthDialog` hands to `onVerify` to check with the server. Same names as iOS. */
public sealed interface MonarchStepUpCredential {
    /** The method that produced the credential. */
    public val method: MonarchStepUpMethod

    /**
     * The password the person typed.
     *
     * @property password the password, as typed.
     */
    public data class Password(public val password: String) : MonarchStepUpCredential {
        override val method: MonarchStepUpMethod get() = MonarchStepUpMethod.Password

        /** Never prints the password. */
        override fun toString(): String = "Password(***)"
    }

    /**
     * The code from the authenticator app.
     *
     * @property code the 6 digits.
     */
    public data class Code(public val code: String) : MonarchStepUpCredential {
        override val method: MonarchStepUpMethod get() = MonarchStepUpMethod.Code
    }

    /** The person chose their passkey: run the Credential Manager assertion in `onVerify`. */
    public data object Passkey : MonarchStepUpCredential {
        override val method: MonarchStepUpMethod get() = MonarchStepUpMethod.Passkey
    }

    /** The device check in `onRequestBiometric` has passed. */
    public data object DeviceOwner : MonarchStepUpCredential {
        override val method: MonarchStepUpMethod get() = MonarchStepUpMethod.DeviceOwner
    }
}
