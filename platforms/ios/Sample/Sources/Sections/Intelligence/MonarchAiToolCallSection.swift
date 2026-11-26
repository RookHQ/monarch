import MonarchUI
import SwiftUI

struct MonarchAiToolCallSection: CataloguePage {
    static let title = "AI tool call"

    @State private var state = MonarchAiToolCallState.running

    private var search: MonarchAiToolCallPart {
        MonarchAiToolCallPart(
            id: "call-1",
            name: "search_documents",
            input: ["query": "Q3 plan", "limit": 5],
            state: state,
            output: state == .succeeded ? ["results": [["title": "Q3 plan.pdf", "score": 0.92]]] : nil,
            error: state == .failed ? "The search service did not answer." : nil,
            startedAt: "2026-03-02T09:00:00Z",
            endedAt: state == .running ? nil : "2026-03-02T09:00:01.400Z"
        )
    }

    var body: some View {
        CatalogueCaption("Humanized summary with details")
        Picker("State", selection: $state) {
            ForEach(MonarchAiToolCallState.allCases, id: \.self) { state in
                Text(state.rawValue).tag(state)
            }
        }
        MonarchAiToolCall(search) { part in
            part.input["query"]?.stringValue.map { "Searched documents for “\($0)”" }
        }

        CatalogueCaption("Every state")
        ForEach(MonarchAiToolCallState.allCases, id: \.self) { state in
            MonarchAiToolCall(
                MonarchAiToolCallPart(
                    id: state.rawValue,
                    name: "send_email",
                    input: ["to": "ada@example.com"],
                    state: state,
                    error: state == .failed ? "The mail server refused the message." : nil
                )
            )
        }
    }
}
