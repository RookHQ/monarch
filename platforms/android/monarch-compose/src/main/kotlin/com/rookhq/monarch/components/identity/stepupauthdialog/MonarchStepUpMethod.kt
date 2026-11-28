package com.rookhq.monarch.components.identity.stepupauthdialog

/** A way to confirm it is still the signed-in person in a `MonarchStepUpAuthDialog`. Same names as iOS. */
public enum class MonarchStepUpMethod {
    /** The account password. */
    Password,

    /** A 6-digit code from an authenticator app (the web's `totp`). */
    Code,

    /** A passkey, run by the app with Credential Manager in `onVerify`. */
    Passkey,

    /** The device's biometrics or screen lock, run by the app with `BiometricPrompt` in `onRequestBiometric`. */
    DeviceOwner,
}
