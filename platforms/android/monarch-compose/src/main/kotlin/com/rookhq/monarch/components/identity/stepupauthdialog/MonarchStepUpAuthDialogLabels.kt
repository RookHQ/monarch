package com.rookhq.monarch.components.identity.stepupauthdialog

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchStepUpAuthDialog`. Every `null` uses the built-in string
 * named after it in `strings_stepupauthdialog.xml`.
 *
 * @property title the title: "Confirm it’s you". The `title` parameter wins.
 * @property description the line under the title. The `description` parameter wins.
 * @property methodPicker the TalkBack name of the method tabs: "Verification method".
 * @property methodName the name of a method on its tab: "Password", "Authenticator app", "Passkey", "This device".
 * @property passwordLabel the label of the password field.
 * @property passwordRequired the error of an empty password: "Enter your password.".
 * @property codeLabel the label of the code field: "Authentication code".
 * @property codeHelper the help under the code field.
 * @property codeIncomplete the error of an incomplete code: "Enter all 6 digits.".
 * @property passkeyDescription the line of the passkey method.
 * @property passkeyButton the primary button of the passkey method: "Use passkey".
 * @property deviceOwnerDescription the line of the device method.
 * @property deviceOwnerButton the primary button of the device method: "Use screen lock".
 * @property submit the primary button of the password and code methods: "Confirm".
 * @property cancel the button that calls `onCancel`: "Cancel".
 * @property forgot the button that calls `onForgot`: "Forgot your password?".
 * @property genericError the failure shown when `errorMessage` gives none: "That did not work. Try again.".
 */
@Immutable
public class MonarchStepUpAuthDialogLabels(
    public val title: String? = null,
    public val description: String? = null,
    public val methodPicker: String? = null,
    public val methodName: ((MonarchStepUpMethod) -> String)? = null,
    public val passwordLabel: String? = null,
    public val passwordRequired: String? = null,
    public val codeLabel: String? = null,
    public val codeHelper: String? = null,
    public val codeIncomplete: String? = null,
    public val passkeyDescription: String? = null,
    public val passkeyButton: String? = null,
    public val deviceOwnerDescription: String? = null,
    public val deviceOwnerButton: String? = null,
    public val submit: String? = null,
    public val cancel: String? = null,
    public val forgot: String? = null,
    public val genericError: String? = null,
)
