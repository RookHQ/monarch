/// Replacements for the built-in strings of ``MonarchInstallVerify``.
/// A property that is `nil` keeps the built-in English string.
@_spi(Experimental) public struct MonarchInstallVerifyLabels: Sendable {
    /// The heading over the snippets. Built in: "Install".
    public var installHeading: String?
    /// The heading over the status. Built in: "Verify".
    public var verifyHeading: String?
    /// The spoken name of the snippet tabs. Built in: "Platform".
    public var platforms: String?
    /// The button that copies the instructions. Built in: "Copy instructions for your developer".
    public var shareInstructions: String?
    /// The link that opens an email with the instructions. Built in: "Email them".
    public var emailDeveloper: String?
    /// The subject of the shared instructions. Built in: "Install instructions".
    public var shareSubject: String?
    /// The status title while waiting. Built in: "Waiting for your site…".
    public var waitingTitle: String?
    /// The status text while waiting, from the site names. Built in: "Add the snippet and open any
    /// page of example.com. This updates by itself."
    public var waitingBody: (@Sendable ([String]) -> String)?
    /// The status title once connected, from the site names. Built in: "Connected on example.com".
    public var connectedTitle: (@Sendable ([String]) -> String)?
    /// The status text once connected. Built in: "The snippet loaded there. Try it out to see it
    /// working end to end."
    public var connectedBody: String?
    /// The status title when the product cannot check. Built in: "Live verification isn’t available".
    public var unavailableTitle: String?
    /// The status text when the product cannot check. Built in: "Open a page where you added the
    /// snippet and check that it appears."
    public var unavailableBody: String?
    /// The spoken name of the list of sites. Built in: "Websites".
    public var targets: String?
    /// When a site last reported, from a relative time. Built in: "seen 5 minutes ago".
    public var seen: (@Sendable (String) -> String)?
    /// A site that has not reported. Built in: "not seen yet".
    public var notSeen: String?
    /// The test page button. Built in: "Open a test page".
    public var openTest: String?
    /// The spoken hint of the test page button. Built in: "Opens in Safari."
    public var openTestHint: String?

    /// Replacements for built-in strings. Every parameter is optional; each is described on its property.
    public init(
        installHeading: String? = nil,
        verifyHeading: String? = nil,
        platforms: String? = nil,
        shareInstructions: String? = nil,
        emailDeveloper: String? = nil,
        shareSubject: String? = nil,
        waitingTitle: String? = nil,
        waitingBody: (@Sendable ([String]) -> String)? = nil,
        connectedTitle: (@Sendable ([String]) -> String)? = nil,
        connectedBody: String? = nil,
        unavailableTitle: String? = nil,
        unavailableBody: String? = nil,
        targets: String? = nil,
        seen: (@Sendable (String) -> String)? = nil,
        notSeen: String? = nil,
        openTest: String? = nil,
        openTestHint: String? = nil
    ) {
        self.installHeading = installHeading
        self.verifyHeading = verifyHeading
        self.platforms = platforms
        self.shareInstructions = shareInstructions
        self.emailDeveloper = emailDeveloper
        self.shareSubject = shareSubject
        self.waitingTitle = waitingTitle
        self.waitingBody = waitingBody
        self.connectedTitle = connectedTitle
        self.connectedBody = connectedBody
        self.unavailableTitle = unavailableTitle
        self.unavailableBody = unavailableBody
        self.targets = targets
        self.seen = seen
        self.notSeen = notSeen
        self.openTest = openTest
        self.openTestHint = openTestHint
    }
}
