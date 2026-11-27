/// The lifecycle of a tool call.
public enum MonarchAiToolCallState: String, Sendable, Hashable, Codable, CaseIterable {
    /// Waits for a person to approve or deny it.
    case pendingApproval = "pending-approval"
    /// Waits for the client to run the tool and send its result back.
    case awaitingResult = "awaiting-result"
    /// The tool is running.
    case running
    /// The tool finished.
    case succeeded
    /// The tool failed.
    case failed
    /// A person or a policy denied the call.
    case denied
    /// The reply ended before the tool finished.
    case cancelled
}
