package com.rookhq.monarch.components.technical.installverify

/** Whether the installed code has reported in. */
public enum class MonarchInstallVerifyStatus {
    /** Waiting for the first report. */
    Waiting,

    /** The code reported in from at least one site. */
    Connected,

    /** The product cannot check the install live. */
    Unavailable,
}
