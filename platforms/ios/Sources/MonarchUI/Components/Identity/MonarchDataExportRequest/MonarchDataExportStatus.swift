/// Where a data export is, as the app reports it to ``MonarchDataExportRequest``.
@_spi(Experimental) public enum MonarchDataExportStatus: String, Sendable, Hashable, CaseIterable {
    /// Nothing requested yet; the request form is shown.
    case idle
    /// The request was received and is queued.
    case requested
    /// The file is being built.
    case preparing
    /// The file can be downloaded until it expires.
    case ready
    /// The download is no longer available; the person can request again.
    case expired
    /// The export could not be built; the person can request again.
    case failed
}
