import MonarchUI
import SwiftUI

struct MonarchAiStructuredAnswerSection: CataloguePage {
    static let title = "AI structured answer"

    @State private var count = 0
    @State private var isStreaming = false
    @State private var copied = false

    private let schema = [
        MonarchAiStructuredField("candidate", label: "Candidate"),
        MonarchAiStructuredField("role", label: "Role"),
        MonarchAiStructuredField("stage", label: "Stage", type: .badge, tone: .info),
        MonarchAiStructuredField("score", label: "Overall score", type: .number),
        MonarchAiStructuredField("strengths", label: "Strengths", type: .list),
        MonarchAiStructuredField("risks", label: "Open questions", type: .list),
        MonarchAiStructuredField("summary", label: "Summary", type: .markdown),
    ]

    private let brief: [String: MonarchAiJSON] = [
        "candidate": "Alex Morgan",
        "role": "Senior engineer",
        "stage": "Final interview",
        "score": 4.2,
        "strengths": ["Clear system design reasoning", "Strong written communication"],
        "risks": ["Limited on-call experience"],
        "summary": "Interviewers agree on **technical depth**. Confirm availability for the start date.",
    ]

    private var streamed: [String: MonarchAiJSON] {
        var data: [String: MonarchAiJSON] = [:]
        for field in schema.prefix(count) {
            data[field.key] = brief[field.key]
        }
        return data
    }

    var body: some View {
        CatalogueCaption("List layout with a copy action")
        MonarchAiStructuredAnswer(schema, data: brief, title: "Interview brief") {
            MonarchButton(copied ? "Copied" : "Copy", variant: .ghost, size: .sm) { copied = true }
        }

        CatalogueCaption("Streaming in")
        MonarchButton("Replay", variant: .primary) { replay() }
            .disabled(isStreaming)
        MonarchAiStructuredAnswer(
            schema,
            data: streamed,
            title: "Interview brief, second round",
            isStreaming: isStreaming
        )

        CatalogueCaption("Grid layout")
        MonarchAiStructuredAnswer(
            [
                MonarchAiStructuredField(
                    "health",
                    label: "Health",
                    type: .badge,
                    toneForValue: { $0 == "At risk" ? .warning : .success }
                ),
                MonarchAiStructuredField("usage", label: "Seat usage", type: .number, format: .percent),
                MonarchAiStructuredField("renewal", label: "Renewal"),
                MonarchAiStructuredField("owner", label: "Owner"),
            ],
            data: ["health": "At risk", "usage": 0.62, "renewal": "In 45 days"],
            title: "Account health",
            layout: .grid
        )
    }

    private func replay() {
        count = 0
        isStreaming = true
        Task {
            for _ in schema {
                try? await Task.sleep(for: .milliseconds(450))
                count += 1
            }
            isStreaming = false
        }
    }
}
