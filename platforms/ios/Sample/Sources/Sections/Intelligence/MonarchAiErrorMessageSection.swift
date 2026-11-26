import MonarchUI
import SwiftUI

struct MonarchAiErrorMessageSection: CataloguePage {
    static let title = "AI error message"

    @State private var lastAction = "No action yet."
    @State private var isRetrying = false

    var body: some View {
        Text(lastAction)
            .monarchTextStyle(.body)

        CatalogueCaption("Rate limit, counting down from 12 seconds")
        MonarchAiErrorMessage(
            .rateLimit,
            details: "request_id: req_8f3a2c\nretry-after: 12",
            retryAfter: 12,
            announces: false,
            onRetry: { lastAction = "Try again" },
            onSwitchModel: { lastAction = "Switch model" }
        )

        CatalogueCaption("Conversation too long")
        MonarchAiErrorMessage(
            .contextOverflow,
            announces: false,
            onNewConversation: { lastAction = "New conversation" },
            onSummarize: { lastAction = "Summarize and continue" }
        )

        CatalogueCaption("Safety block")
        MonarchAiErrorMessage(.safetyBlock, announces: false, onEditPrompt: { lastAction = "Edit message" })

        CatalogueCaption("Connection lost, with a retry that takes a moment")
        MonarchAiErrorMessage(
            MonarchAiErrorKind(code: "ETIMEDOUT"),
            isRetrying: isRetrying,
            announces: false,
            onRetry: {
                isRetrying = true
                lastAction = "Retrying"
                Task {
                    try? await Task.sleep(for: .seconds(2))
                    isRetrying = false
                    lastAction = "Retried"
                }
            }
        )

        CatalogueCaption("Tool failure and unknown")
        MonarchAiErrorMessage(.toolFailure, announces: false, onRetry: { lastAction = "Try again" })
        MonarchAiErrorMessage(.unknown, announces: false)
    }
}
