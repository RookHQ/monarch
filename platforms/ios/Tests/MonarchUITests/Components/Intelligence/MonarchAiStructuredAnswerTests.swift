import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private enum AnswerSamples {
    static let schema = [
        MonarchAiStructuredField("candidate", label: "Candidate"),
        MonarchAiStructuredField("role", label: "Role"),
        MonarchAiStructuredField("stage", label: "Stage", type: .badge, tone: .info),
        MonarchAiStructuredField("score", label: "Overall score", type: .number),
        MonarchAiStructuredField("strengths", label: "Strengths", type: .list),
        MonarchAiStructuredField("risks", label: "Open questions", type: .list),
        MonarchAiStructuredField("summary", label: "Summary", type: .markdown),
    ]
    static let brief: [String: MonarchAiJSON] = [
        "candidate": "Alex Morgan",
        "role": "Senior engineer",
        "stage": "Final interview",
        "score": 4.2,
        "strengths": ["Clear system design reasoning", "Strong written communication"],
        "risks": ["Limited on-call experience"],
        "summary": "Interviewers agree on **technical depth**. Confirm the [start date](https://example.com).",
    ]
    static let healthSchema = [
        MonarchAiStructuredField(
            "health",
            label: "Health",
            type: .badge,
            toneForValue: { $0 == "At risk" ? .warning : .success }
        ),
        MonarchAiStructuredField("usage", label: "Seat usage", type: .number, format: .percent),
        MonarchAiStructuredField("renewal", label: "Renewal"),
        MonarchAiStructuredField("notes", label: "Notes", type: .markdown),
    ]
    static let en = Locale(identifier: "en_US")
}

enum MonarchAiStructuredAnswerFixtures: ComponentFixtures {
    static let id = "ai-structured-answer"
    static let fixtures = [
        Fixture("list layout, every field type") {
            MonarchAiStructuredAnswer(AnswerSamples.schema, data: AnswerSamples.brief, title: "Interview brief")
        },
        Fixture("streaming, fields still to come") {
            MonarchAiStructuredAnswer(
                AnswerSamples.schema,
                data: ["candidate": "Alex Morgan", "stage": "Final interview"],
                title: "Interview brief, second round",
                isStreaming: true
            )
        },
        Fixture("complete with empty fields") {
            MonarchAiStructuredAnswer(AnswerSamples.schema, data: ["candidate": "Alex Morgan", "risks": []])
        },
        Fixture("grid layout with a wide field and actions") {
            MonarchAiStructuredAnswer(
                AnswerSamples.healthSchema,
                data: [
                    "health": "At risk", "usage": 0.62, "renewal": "In 45 days", "notes": "Usage fell in **March**.",
                ],
                title: "Account health",
                layout: .grid
            ) {
                MonarchButton("Copy", variant: .ghost, size: .sm) {}
            }
        },
        Fixture("grid layout streaming") {
            MonarchAiStructuredAnswer(AnswerSamples.healthSchema, data: [:], layout: .grid, isStreaming: true)
        },
        Fixture("long values and custom labels") {
            MonarchAiStructuredAnswer(
                AnswerSamples.schema,
                data: [
                    "candidate": .string(String(repeating: "A very long candidate name ", count: 6)),
                    "stage": ["Screen", "Technical", "Final interview", "Offer", "Background check"],
                ],
                title: String(repeating: "A long title that wraps ", count: 4),
                labels: MonarchAiStructuredAnswerLabels(aiAnswer: "Generated", empty: "Missing")
            )
        },
    ]
}

@Suite("MonarchAiStructuredAnswer")
struct MonarchAiStructuredAnswerTests {
    private typealias Tokens = MonarchAiStructuredAnswerTokens

    @Test("Missing values: absent, null, empty text and empty arrays")
    func missing() {
        #expect(Tokens.isMissing(nil))
        #expect(Tokens.isMissing(.null))
        #expect(Tokens.isMissing(""))
        #expect(Tokens.isMissing([]))
        #expect(!Tokens.isMissing(" "))
        #expect(!Tokens.isMissing(0))
        #expect(!Tokens.isMissing(false))
    }

    @Test("A field is pending while streaming, empty once complete, and a value when present")
    func states() {
        #expect(Tokens.state(nil, isStreaming: true) == .pending)
        #expect(Tokens.state(nil, isStreaming: false) == .empty)
        #expect(Tokens.state("Alex", isStreaming: true) == .value)
        #expect(Tokens.state([], isStreaming: false) == .empty)
    }

    @Test("List and badge items skip missing elements and turn values into text")
    func items() {
        #expect(Tokens.items(["One", nil, "", 2, true]) == ["One", "2", "true"])
        #expect(Tokens.items("Single") == ["Single"])
        #expect(Tokens.items(nil).isEmpty)
        #expect(Tokens.items(["a": 1]) == [#"{"a":1}"#])
    }

    @Test("Numbers are written in the locale with the field's format; anything else stays text")
    func numbers() {
        #expect(Tokens.number(1200, format: .number, locale: AnswerSamples.en) == "1,200")
        #expect(Tokens.number("1,200.5", format: .number, locale: AnswerSamples.en) == "1,200.5")
        #expect(Tokens.number(0.62, format: .percent, locale: AnswerSamples.en) == "62%")
        #expect(Tokens.number(4.2, format: .number, locale: Locale(identifier: "de_DE")) == "4,2")
        #expect(Tokens.number(12.5, format: .currency(code: "USD"), locale: AnswerSamples.en) == "$12.50")
        #expect(Tokens.number("n/a", format: .number, locale: AnswerSamples.en) == "n/a")
        #expect(Tokens.number("12%", format: .number, locale: AnswerSamples.en) == "12%")
    }

    @Test("Badges take the field's tone, or the tone of their value")
    func tones() {
        let fixed = MonarchAiStructuredField("stage", label: "Stage", type: .badge, tone: .info)
        #expect(Tokens.tone("Anything", field: fixed) == .info)
        let byValue = AnswerSamples.healthSchema[0]
        #expect(Tokens.tone("At risk", field: byValue) == .warning)
        #expect(Tokens.tone("Healthy", field: byValue) == .success)
        #expect(Tokens.tone("x", field: MonarchAiStructuredField("k", label: "K", type: .badge)) == .neutral)
    }

    @Test("List and Markdown fields are wide unless the field says otherwise")
    func wide() {
        #expect(Tokens.isWide(MonarchAiStructuredField("a", label: "A", type: .list)))
        #expect(Tokens.isWide(MonarchAiStructuredField("a", label: "A", type: .markdown)))
        #expect(!Tokens.isWide(MonarchAiStructuredField("a", label: "A", type: .text)))
        #expect(Tokens.isWide(MonarchAiStructuredField("a", label: "A", isWide: true)))
        #expect(!Tokens.isWide(MonarchAiStructuredField("a", label: "A", type: .list, isWide: false)))
    }

    @Test("Grid rows pair narrow fields and give wide fields a row of their own")
    func gridRows() {
        #expect(Tokens.gridRows(AnswerSamples.schema, columns: 2) == [[0, 1], [2, 3], [4], [5], [6]])
        #expect(Tokens.gridRows(AnswerSamples.healthSchema, columns: 2) == [[0, 1], [2], [3]])
        #expect(Tokens.gridRows(AnswerSamples.healthSchema, columns: 1) == [[0], [1], [2], [3]])
        #expect(Tokens.gridRows([], columns: 2).isEmpty)
    }

    @Test("VoiceOver reads each field's value, Still generating, or Not provided")
    func spokenValues() {
        func spoken(_ value: MonarchAiJSON?, _ field: MonarchAiStructuredField, streaming: Bool = false) -> String {
            Tokens.spokenValue(
                value,
                field: field,
                isStreaming: streaming,
                locale: AnswerSamples.en,
                pending: "Still generating",
                empty: "Not provided"
            )
        }
        let schema = AnswerSamples.schema
        #expect(spoken("Alex Morgan", schema[0]) == "Alex Morgan")
        #expect(spoken(nil, schema[0], streaming: true) == "Still generating")
        #expect(spoken(nil, schema[0]) == "Not provided")
        #expect(spoken(1234.5, schema[3]) == "1,234.5")
        #expect(spoken(["One", "Two"], schema[4]) == "One, Two")
        #expect(spoken("Final interview", schema[2]) == "Final interview")
        #expect(spoken("**Bold**", schema[6]) == "**Bold**")
    }

    @Test("Model HTML stays text")
    func htmlStaysText() {
        #expect(Tokens.text("<b>bold</b>") == "<b>bold</b>")
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiStructuredAnswerStrings.aiAnswer == "AI answer")
        #expect(MonarchAiStructuredAnswerStrings.pending == "Still generating")
        #expect(MonarchAiStructuredAnswerStrings.empty == "Not provided")
        #expect(MonarchAiStructuredAnswerStrings.complete == "Response complete.")
    }
}
