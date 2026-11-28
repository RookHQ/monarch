import Foundation

/// Replacements for the built-in strings of ``MonarchAiTraceView``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiTraceViewLabels: Sendable {
    /// The heading and spoken name of the trace. Built in: "Run trace".
    public var label: String?
    /// The spoken name of the filter. Built in: "Show".
    public var filterLabel: String?
    /// The name of a filter. Built in: "All", "Errors only", "Slowest first".
    public var filter: (@Sendable (MonarchAiTraceFilter) -> String)?
    /// The name of a kind. Built in: "Model", "Tool", "Retrieval", "Agent", "Guardrail", "Other".
    public var kind: (@Sendable (MonarchAiTraceSpanKind) -> String)?
    /// The name of a status. Built in: "OK", "Error", "Cancelled", "Running".
    public var status: (@Sendable (MonarchAiTraceSpanStatus) -> String)?
    /// The total wall-clock time. Built in: "Total time".
    public var totalDuration: String?
    /// The total tokens. Built in: "Tokens".
    public var totalTokens: String?
    /// The total cost. Built in: "Cost".
    public var totalCost: String?
    /// The number of failed spans. Built in: "Errors".
    public var errors: String?
    /// Shown for a value that is not known. Built in: "–".
    public var notAvailable: String?
    /// Shown when there are no spans. Built in: "No spans recorded".
    public var empty: String?
    /// Shown when Errors only finds nothing. Built in: "No errors in this run".
    public var noErrors: String?
    /// Formats a duration in seconds. Built in: "850 ms", "4.2 s", "2 min 5 s".
    public var formatDuration: (@Sendable (TimeInterval) -> String)?
    /// Reads a span's place on the time scale, from its start offset and its duration (`nil` while it runs). Built in: "Starts at +1.2 s, takes 850 ms".
    public var timing: (@Sendable (String, String?) -> String)?
    /// The button that closes the details. Built in: "Close".
    public var closeDetails: String?
    /// The button that shows the span's conversation part. Built in: "Show in conversation".
    public var openPart: String?
    /// The start in the details. Built in: "Start".
    public var start: String?
    /// The end in the details. Built in: "End".
    public var end: String?
    /// The duration in the details. Built in: "Duration".
    public var duration: String?
    /// Input tokens in the details. Built in: "Input tokens".
    public var inputTokens: String?
    /// Output tokens in the details. Built in: "Output tokens".
    public var outputTokens: String?
    /// The model in the details. Built in: "Model".
    public var model: String?
    /// The status in the details. Built in: "Status".
    public var statusName: String?
    /// The kind in the details. Built in: "Kind".
    public var kindName: String?
    /// The error heading in the details. Built in: "Error".
    public var error: String?
    /// The attributes heading in the details. Built in: "Attributes".
    public var attributes: String?
    /// The input heading in the details. Built in: "Input".
    public var input: String?
    /// The output heading in the details. Built in: "Output".
    public var output: String?
    /// The spoken name of the button that shows a span's children, from its name. Built in: "Show spans inside fetch".
    public var expand: (@Sendable (String) -> String)?
    /// The spoken name of the button that hides a span's children, from its name. Built in: "Hide spans inside fetch".
    public var collapse: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - label: The heading and spoken name of the trace.
    ///   - filterLabel: The spoken name of the filter.
    ///   - filter: The name of a filter.
    ///   - kind: The name of a kind.
    ///   - status: The name of a status.
    ///   - totalDuration: The total wall-clock time.
    ///   - totalTokens: The total tokens.
    ///   - totalCost: The total cost.
    ///   - errors: The number of failed spans.
    ///   - notAvailable: Shown for a value that is not known.
    ///   - empty: Shown when there are no spans.
    ///   - noErrors: Shown when Errors only finds nothing.
    ///   - formatDuration: Formats a duration in seconds.
    ///   - timing: Reads a span's place on the time scale, from its start offset and its duration (`nil` while it runs).
    ///   - closeDetails: The button that closes the details.
    ///   - openPart: The button that shows the span's conversation part.
    ///   - start: The start in the details.
    ///   - end: The end in the details.
    ///   - duration: The duration in the details.
    ///   - inputTokens: Input tokens in the details.
    ///   - outputTokens: Output tokens in the details.
    ///   - model: The model in the details.
    ///   - statusName: The status in the details.
    ///   - kindName: The kind in the details.
    ///   - error: The error heading in the details.
    ///   - attributes: The attributes heading in the details.
    ///   - input: The input heading in the details.
    ///   - output: The output heading in the details.
    ///   - expand: The spoken name of the button that shows a span's children, from its name.
    ///   - collapse: The spoken name of the button that hides a span's children, from its name.
    public init(
        label: String? = nil,
        filterLabel: String? = nil,
        filter: (@Sendable (MonarchAiTraceFilter) -> String)? = nil,
        kind: (@Sendable (MonarchAiTraceSpanKind) -> String)? = nil,
        status: (@Sendable (MonarchAiTraceSpanStatus) -> String)? = nil,
        totalDuration: String? = nil,
        totalTokens: String? = nil,
        totalCost: String? = nil,
        errors: String? = nil,
        notAvailable: String? = nil,
        empty: String? = nil,
        noErrors: String? = nil,
        formatDuration: (@Sendable (TimeInterval) -> String)? = nil,
        timing: (@Sendable (String, String?) -> String)? = nil,
        closeDetails: String? = nil,
        openPart: String? = nil,
        start: String? = nil,
        end: String? = nil,
        duration: String? = nil,
        inputTokens: String? = nil,
        outputTokens: String? = nil,
        model: String? = nil,
        statusName: String? = nil,
        kindName: String? = nil,
        error: String? = nil,
        attributes: String? = nil,
        input: String? = nil,
        output: String? = nil,
        expand: (@Sendable (String) -> String)? = nil,
        collapse: (@Sendable (String) -> String)? = nil
    ) {
        self.label = label
        self.filterLabel = filterLabel
        self.filter = filter
        self.kind = kind
        self.status = status
        self.totalDuration = totalDuration
        self.totalTokens = totalTokens
        self.totalCost = totalCost
        self.errors = errors
        self.notAvailable = notAvailable
        self.empty = empty
        self.noErrors = noErrors
        self.formatDuration = formatDuration
        self.timing = timing
        self.closeDetails = closeDetails
        self.openPart = openPart
        self.start = start
        self.end = end
        self.duration = duration
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.model = model
        self.statusName = statusName
        self.kindName = kindName
        self.error = error
        self.attributes = attributes
        self.input = input
        self.output = output
        self.expand = expand
        self.collapse = collapse
    }
}
