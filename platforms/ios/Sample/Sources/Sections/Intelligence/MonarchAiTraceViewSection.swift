@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiTraceViewSection: CataloguePage {
    static let title = "AI trace view"

    @State private var last = "Select a span to see its details."

    private let spans = [
        MonarchAiTraceSpan(id: "run", kind: .agent, name: "Answer question", status: .ok, start: 0, end: 4.2),
        MonarchAiTraceSpan(
            id: "plan",
            parentId: "run",
            kind: .model,
            name: "Plan",
            status: .ok,
            start: 0.1,
            end: 1.3,
            inputTokens: 1_200,
            outputTokens: 180,
            costMicros: 4_200,
            model: "Large model",
            attributes: ["temperature": "0.2"],
            input: "Plan how to answer: what changed in Q3?",
            output: "1. Search the Q3 plan. 2. Summarise the changes.",
            partId: "msg-1"
        ),
        MonarchAiTraceSpan(
            id: "search",
            parentId: "run",
            kind: .tool,
            name: "search_documents",
            status: .error,
            start: 1.4,
            end: 2.0,
            error: "The search service timed out after 600 ms."
        ),
        MonarchAiTraceSpan(
            id: "retry",
            parentId: "run",
            kind: .retrieval,
            name: "Retry search",
            status: .ok,
            start: 2.1,
            end: 3.0
        ),
        MonarchAiTraceSpan(
            id: "fetch",
            parentId: "retry",
            kind: .tool,
            name: "fetch",
            status: .ok,
            start: 2.2,
            end: 2.9,
            costMicros: 12_000
        ),
        MonarchAiTraceSpan(
            id: "answer",
            parentId: "run",
            kind: .model,
            name: "Write answer",
            status: .ok,
            start: 3.0,
            end: 4.1,
            inputTokens: 2_400,
            outputTokens: 420,
            costMicros: 9_100
        ),
    ]

    var body: some View {
        MonarchAiTraceView(spans) { span in
            last = "Show \(span.partId ?? span.name) in the conversation"
        }
        Text(last).monarchTextStyle(.caption)
    }
}
