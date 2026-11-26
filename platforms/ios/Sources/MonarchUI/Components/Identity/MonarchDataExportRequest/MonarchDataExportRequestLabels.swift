/// Replacements for the built-in strings of ``MonarchDataExportRequest``.
/// A property that is `nil` keeps the built-in English string. Dates are
/// passed to the closures already formatted, or `nil` when unknown.
@_spi(Experimental) public struct MonarchDataExportRequestLabels: Sendable {
    /// The card's title. Built in: "Download your data".
    public var title: String?
    /// The line under the title. Built in: "Get a copy of the data in your account."
    public var description: String?
    /// The badge text of each status. Built in: "Not requested", "Requested", "Preparing", "Ready", "Expired", "Failed".
    public var statuses: [MonarchDataExportStatus: String]
    /// The button that requests the first export. Built in: "Request export".
    public var request: String?
    /// The button that requests again after an expired or failed export. Built in: "Request a new export".
    public var requestAgain: String?
    /// The download button. Built in: "Download".
    public var download: String?
    /// The label of the scope checklist. Built in: "Include".
    public var scopeLegend: String?
    /// Shown when no kind of data is chosen. Built in: "Choose at least one kind of data."
    public var scopeRequired: String?
    /// The label of the format menu. Built in: "Format".
    public var formatLabel: String?
    /// The status line while requested, from the request date. Built in: "We received your request on …".
    public var requested: (@Sendable (String?) -> String)?
    /// The status line while preparing, from the request date. Built in: "Your export is being prepared. Requested on …".
    public var preparing: (@Sendable (String?) -> String)?
    /// The spoken name of the progress bar while preparing. Built in: "Preparing your export".
    public var preparingProgress: String?
    /// The status line when ready, from the ready and expiry dates. Built in: "Your export is ready. Prepared on …. You can download it until …."
    public var ready: (@Sendable (String?, String?) -> String)?
    /// The status line when expired, from the expiry date. Built in: "The download expired on …. Request a new export to get your data."
    public var expired: (@Sendable (String?) -> String)?
    /// The status line when failed. Built in: "Your export could not be prepared. Request it again."
    public var failed: String?
    /// Shown when the request throws an error with no description. Built in: "The request did not go through. Try again."
    public var requestFailed: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        title: String? = nil,
        description: String? = nil,
        statuses: [MonarchDataExportStatus: String] = [:],
        request: String? = nil,
        requestAgain: String? = nil,
        download: String? = nil,
        scopeLegend: String? = nil,
        scopeRequired: String? = nil,
        formatLabel: String? = nil,
        requested: (@Sendable (String?) -> String)? = nil,
        preparing: (@Sendable (String?) -> String)? = nil,
        preparingProgress: String? = nil,
        ready: (@Sendable (String?, String?) -> String)? = nil,
        expired: (@Sendable (String?) -> String)? = nil,
        failed: String? = nil,
        requestFailed: String? = nil
    ) {
        self.title = title
        self.description = description
        self.statuses = statuses
        self.request = request
        self.requestAgain = requestAgain
        self.download = download
        self.scopeLegend = scopeLegend
        self.scopeRequired = scopeRequired
        self.formatLabel = formatLabel
        self.requested = requested
        self.preparing = preparing
        self.preparingProgress = preparingProgress
        self.ready = ready
        self.expired = expired
        self.failed = failed
        self.requestFailed = requestFailed
    }
}
