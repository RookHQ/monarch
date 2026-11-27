/// Whether the installed code has reported in, for ``MonarchInstallVerify``.
@_spi(Experimental) public enum MonarchInstallVerifyStatus: String, Hashable, Sendable, CaseIterable {
    /// Waiting for the first report. Shows a progress indicator.
    case waiting
    /// The code reported from a site.
    case connected
    /// The product cannot check, so the person checks by hand.
    case unavailable
}
