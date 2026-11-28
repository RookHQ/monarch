package dev.monarchui.components.technical.installverify

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchInstallVerify`. Every
 * `null` keeps the built-in string `monarch_installverify_<key>`.
 *
 * @property installHeading the heading of the install half, "Install".
 * @property verifyHeading the heading of the verify half, "Verify".
 * @property platforms the spoken name of the snippet tabs, "Platform".
 * @property copyInstructions the button that copies the developer handoff, "Copy instructions for your developer".
 * @property instructionsCopied the copy button after a copy, "Instructions copied".
 * @property shareInstructions the button that opens the share sheet with the handoff, "Share with your developer".
 * @property waitingTitle "Waiting for your site…".
 * @property waitingBody the text under it, given the site names.
 * @property connectedTitle "Connected on example.com", given the hosts it was seen on.
 * @property connectedBody the text under it.
 * @property unavailableTitle "Live verification isn't available".
 * @property unavailableBody the text under it.
 * @property targetsLabel the spoken name of the list of sites, "Websites".
 * @property seen when a site was last seen, "seen 5 minutes ago", given the relative time.
 * @property notSeen a site not seen yet, "not seen yet".
 * @property openTest the button that opens a test page, "Open a test page".
 */
@Immutable
public class MonarchInstallVerifyLabels(
    public val installHeading: String? = null,
    public val verifyHeading: String? = null,
    public val platforms: String? = null,
    public val copyInstructions: String? = null,
    public val instructionsCopied: String? = null,
    public val shareInstructions: String? = null,
    public val waitingTitle: String? = null,
    public val waitingBody: ((targets: List<String>) -> String)? = null,
    public val connectedTitle: ((hosts: List<String>) -> String)? = null,
    public val connectedBody: String? = null,
    public val unavailableTitle: String? = null,
    public val unavailableBody: String? = null,
    public val targetsLabel: String? = null,
    public val seen: ((time: String) -> String)? = null,
    public val notSeen: String? = null,
    public val openTest: String? = null,
)
