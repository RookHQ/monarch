/// Which halves of ``MonarchInstallVerify`` show.
@_spi(Experimental) public enum MonarchInstallVerifyPart: String, Hashable, Sendable, CaseIterable {
    /// Install and Verify, each under its heading.
    case all
    /// Only the snippets and the developer handoff, for one step of a wizard.
    case install
    /// Only the status, the sites and the test page, for one step of a wizard.
    case verify
}
