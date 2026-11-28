package com.rookhq.monarch.components.intelligence.aiconversation

/** The lifecycle of a tool call. In the web's JSON the names are kebab-case, such as `pending-approval`. */
public enum class MonarchAiToolCallState {
    /** Waits for a person to approve or deny it. */
    PendingApproval,

    /** Waits for the client to run the tool and send its result back. */
    AwaitingResult,

    /** The tool is running. */
    Running,

    /** The tool finished. */
    Succeeded,

    /** The tool failed. */
    Failed,

    /** A person or a policy denied the call. */
    Denied,

    /** The reply ended before the tool finished. */
    Cancelled,
}
