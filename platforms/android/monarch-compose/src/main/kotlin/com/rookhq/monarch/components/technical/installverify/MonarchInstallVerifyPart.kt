package com.rookhq.monarch.components.technical.installverify

/** Which halves of a `MonarchInstallVerify` are shown. */
public enum class MonarchInstallVerifyPart {
    /** Install and Verify, each under its heading. */
    All,

    /** Only the snippets and the developer handoff, for a step of a wizard. */
    Install,

    /** Only the status, the sites and the test page, for a step of a wizard. */
    Verify,
}
