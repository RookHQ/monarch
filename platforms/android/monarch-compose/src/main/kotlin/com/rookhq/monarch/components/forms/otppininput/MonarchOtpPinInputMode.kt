package com.rookhq.monarch.components.forms.otppininput

/** Which characters a `MonarchOtpPinInput` accepts. */
public enum class MonarchOtpPinInputMode {
    /** Digits 0 to 9, with the number keyboard. */
    Numeric,

    /** Letters A to Z, upper-cased, and digits 0 to 9. */
    Alphanumeric,
}
