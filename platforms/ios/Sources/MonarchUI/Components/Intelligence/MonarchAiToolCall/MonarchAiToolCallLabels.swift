import Foundation

/// Replacements for the built-in strings of ``MonarchAiToolCall``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchAiToolCallLabels: Sendable {
    /// The name of a state. Built in: "Waiting for approval", "Waiting for result",
    /// "Running", "Done", "Failed", "Denied", "Cancelled".
    public var state: (@Sendable (MonarchAiToolCallState) -> String)?
    /// Formats how long the call took, in seconds. Built in: "0.4s", "12s", "2m 05s".
    public var formatDuration: (@Sendable (TimeInterval) -> String)?
    /// The heading above the arguments. Built in: "Input".
    public var input: String?
    /// The heading above the result. Built in: "Output".
    public var output: String?
    /// Read before the error text. Built in: "Error".
    public var error: String?
    /// Names the tool in the details. Built in: "Tool".
    public var tool: String?
    /// Shown when the call has no input and no output yet. Built in: "No details yet."
    public var empty: String?
    /// Announced when the state changes, from the tool's label and the state's
    /// name. Built in: "Search documents: Done".
    public var stateChanged: (@Sendable (String, String) -> String)?
    /// The button that shows the rest of a long value. Built in: "Show all".
    public var showMore: String?
    /// Says how much is hidden, from the shown and total characters.
    /// Built in: "Showing 4,000 of 52,113 characters".
    public var truncated: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - state: The name of a state.
    ///   - formatDuration: Formats how long the call took, in seconds.
    ///   - input: The heading above the arguments.
    ///   - output: The heading above the result.
    ///   - error: Read before the error text.
    ///   - tool: Names the tool in the details.
    ///   - empty: Shown when the call has no input and no output yet.
    ///   - stateChanged: Announced when the state changes.
    ///   - showMore: The button that shows the rest of a long value.
    ///   - truncated: Says how much of a long value is hidden.
    public init(
        state: (@Sendable (MonarchAiToolCallState) -> String)? = nil,
        formatDuration: (@Sendable (TimeInterval) -> String)? = nil,
        input: String? = nil,
        output: String? = nil,
        error: String? = nil,
        tool: String? = nil,
        empty: String? = nil,
        stateChanged: (@Sendable (String, String) -> String)? = nil,
        showMore: String? = nil,
        truncated: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.state = state
        self.formatDuration = formatDuration
        self.input = input
        self.output = output
        self.error = error
        self.tool = tool
        self.empty = empty
        self.stateChanged = stateChanged
        self.showMore = showMore
        self.truncated = truncated
    }
}
