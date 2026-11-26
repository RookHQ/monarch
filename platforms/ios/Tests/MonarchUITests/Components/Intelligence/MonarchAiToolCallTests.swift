import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiToolCallFixtures: ComponentFixtures {
    static let id = "ai-tool-call"

    static let search = MonarchAiToolCallPart(
        id: "call-1",
        name: "search_documents",
        input: ["query": "Q3 plan", "limit": 5],
        state: .succeeded,
        output: ["results": [["title": "Q3 plan.pdf", "score": 0.92]]],
        startedAt: "2026-03-02T09:00:00Z",
        endedAt: "2026-03-02T09:00:01.400Z"
    )

    static let fixtures: [Fixture] =
        [
            Fixture("open with input and output") {
                MonarchAiToolCall(search, isExpanded: .constant(true)) { part in
                    part.input["query"]?.stringValue.map { "Searched documents for “\($0)”" }
                }
            },
            Fixture("failed with error") {
                MonarchAiToolCall(
                    MonarchAiToolCallPart(
                        id: "call-2",
                        name: "sendEmail",
                        input: ["to": "ada@example.com"],
                        state: .failed,
                        error: "The mail server did not answer in 30 seconds."
                    )
                )
            },
            Fixture("open, empty") {
                MonarchAiToolCall(
                    MonarchAiToolCallPart(id: "call-3", name: "fetch-weather", state: .running),
                    isExpanded: .constant(true)
                )
            },
            Fixture("truncated output") {
                MonarchAiToolCall(
                    MonarchAiToolCallPart(
                        id: "call-4",
                        name: "read_file",
                        input: ["path": "/logs/today.txt"],
                        state: .succeeded,
                        output: .string(String(repeating: "A line of a long log file.\n", count: 40))
                    ),
                    summary: "Read today's log",
                    systemImage: "doc.text",
                    isExpanded: .constant(true),
                    maxValueLength: 200
                )
            },
            Fixture("long title") {
                MonarchAiToolCall(
                    MonarchAiToolCallPart(id: "call-5", name: "x", state: .awaitingResult),
                    summary: String(repeating: "A summary that wraps across several lines. ", count: 4)
                )
            },
        ]
        + MonarchAiToolCallState.allCases.map { state in
            Fixture("state \(state.rawValue)") {
                MonarchAiToolCall(MonarchAiToolCallPart(id: state.rawValue, name: "create_invoice", state: state))
            }
        }
}

@Suite("MonarchAiToolCall")
struct MonarchAiToolCallTests {
    @Test("Tool names become words")
    func humanizedName() {
        #expect(MonarchAiToolCallTokens.humanizedName("search_documents") == "Search documents")
        #expect(MonarchAiToolCallTokens.humanizedName("searchDocuments") == "Search documents")
        #expect(MonarchAiToolCallTokens.humanizedName("search-documents") == "Search documents")
        #expect(MonarchAiToolCallTokens.humanizedName("crm.contacts/find") == "Crm contacts find")
        #expect(MonarchAiToolCallTokens.humanizedName("get2FA") == "Get2 fa")
        #expect(MonarchAiToolCallTokens.humanizedName("___") == "___")
    }

    @Test("The title is the summary, then the description, then the tool's label")
    func title() {
        let labels = ["search_documents": "Search the library"]
        #expect(MonarchAiToolCallTokens.toolLabel("search_documents", toolLabels: labels) == "Search the library")
        #expect(MonarchAiToolCallTokens.toolLabel("send_email", toolLabels: labels) == "Send email")
        #expect(MonarchAiToolCallTokens.title(summary: "S", described: "D", label: "L") == "S")
        #expect(MonarchAiToolCallTokens.title(summary: nil, described: "D", label: "L") == "D")
        #expect(MonarchAiToolCallTokens.title(summary: "", described: nil, label: "L") == "L")
    }

    @Test("Values are pretty JSON, and plain strings stay as they are")
    func formatted() {
        let object = MonarchAiToolCallTokens.formatted(["b": 1, "a": "x"])
        #expect(object.isJSON)
        #expect(object.text.contains("\"a\": \"x\""))
        #expect(object.text.contains("\n"))
        let keyA = object.text.range(of: "\"a\"")?.lowerBound
        let keyB = object.text.range(of: "\"b\"")?.lowerBound
        #expect(keyA != nil && keyB != nil && keyA! < keyB!)

        let embedded = MonarchAiToolCallTokens.formatted(.string("{\"q\":\"plan\"}"))
        #expect(embedded.isJSON)
        #expect(embedded.text.contains("\"q\": \"plan\""))

        let plain = MonarchAiToolCallTokens.formatted(.string("Hello"))
        #expect(!plain.isJSON)
        #expect(plain.text == "Hello")

        let broken = MonarchAiToolCallTokens.formatted(.string("{not json"))
        #expect(!broken.isJSON)
        #expect(broken.text == "{not json")
    }

    @Test("Long text is cut at a line end near the limit")
    func truncated() {
        let short = MonarchAiToolCallTokens.truncated("short", max: 10)
        #expect(short.text == "short" && !short.isTruncated)
        let unlimited = MonarchAiToolCallTokens.truncated("anything", max: 0)
        #expect(unlimited.text == "anything" && !unlimited.isTruncated)
        let lines = String(repeating: "123456789\n", count: 5)
        let cut = MonarchAiToolCallTokens.truncated(lines, max: 23)
        #expect(cut.isTruncated)
        #expect(cut.text == "123456789\n123456789")
        let noBreak = MonarchAiToolCallTokens.truncated(String(repeating: "a", count: 50), max: 20)
        #expect(noBreak.text.count == 20)
    }

    @Test("Streamed text stands in for an input that is still empty")
    func inputValue() {
        let streaming = MonarchAiToolCallPart(id: "1", name: "t", input: [:], state: .running, inputText: "{\"q\":")
        #expect(MonarchAiToolCallTokens.inputValue(streaming) == .string("{\"q\":"))
        let none = MonarchAiToolCallPart(id: "1", name: "t", state: .running)
        #expect(MonarchAiToolCallTokens.inputValue(none) == nil)
        let parsed = MonarchAiToolCallPart(id: "1", name: "t", input: ["q": "a"], state: .running, inputText: "x")
        #expect(MonarchAiToolCallTokens.inputValue(parsed) == ["q": "a"])
    }

    @Test("The duration comes from the ISO times and is written like the web")
    func duration() {
        let us = Locale(identifier: "en_US")
        let labels = MonarchAiToolCallLabels()
        let measured = MonarchAiToolCallTokens.duration(MonarchAiToolCallFixtures.search)
        #expect(measured.map { abs($0 - 1.4) < 0.001 } == true)
        let farCut = MonarchAiToolCallTokens.truncated(String(repeating: "123456789\n", count: 5), max: 25)
        #expect(farCut.text.count == 25)
        let open = MonarchAiToolCallPart(id: "1", name: "t", state: .running, startedAt: "2026-03-02T09:00:00Z")
        #expect(MonarchAiToolCallTokens.duration(open) == nil)
        let backwards = MonarchAiToolCallPart(
            id: "1",
            name: "t",
            state: .succeeded,
            startedAt: "2026-03-02T09:00:05Z",
            endedAt: "2026-03-02T09:00:00Z"
        )
        #expect(MonarchAiToolCallTokens.duration(backwards) == nil)
        #expect(MonarchAiToolCallTokens.durationText(0.4, locale: us, labels: labels) == "0.4s")
        #expect(MonarchAiToolCallTokens.durationText(0.01, locale: us, labels: labels) == "0.1s")
        #expect(MonarchAiToolCallTokens.durationText(12.3, locale: us, labels: labels) == "12s")
        #expect(MonarchAiToolCallTokens.durationText(125, locale: us, labels: labels) == "2m 05s")
        #expect(
            MonarchAiToolCallTokens.durationText(0.4, locale: Locale(identifier: "de_DE"), labels: labels) == "0,4s"
        )
        let custom = MonarchAiToolCallLabels(formatDuration: { "\(Int($0)) sec" })
        #expect(MonarchAiToolCallTokens.durationText(3, locale: us, labels: custom) == "3 sec")
    }

    @Test("Every state has a word, a tone and a distinct symbol")
    func states() {
        let symbols = Set(MonarchAiToolCallState.allCases.map { MonarchAiToolCallTokens.stateLook($0).symbol })
        #expect(symbols.count == MonarchAiToolCallState.allCases.count)
        #expect(MonarchAiToolCallTokens.stateLook(.failed).tone == .danger)
        #expect(MonarchAiToolCallTokens.stateLook(.succeeded).tone == .success)
        #expect(MonarchAiToolCallTokens.stateLook(.pendingApproval).tone == .warning)
        #expect(MonarchAiToolCallTokens.stateName(.succeeded, labels: MonarchAiToolCallLabels()) == "Done")
        #expect(
            MonarchAiToolCallTokens.stateName(.pendingApproval, labels: MonarchAiToolCallLabels())
                == "Waiting for approval"
        )
        let custom = MonarchAiToolCallLabels(state: { $0 == .failed ? "Broke" : "Other" })
        #expect(MonarchAiToolCallTokens.stateName(.failed, labels: custom) == "Broke")
        for state in MonarchAiToolCallState.allCases {
            #expect(MonarchAiToolCallStrings.state(state) != "monarch.aiToolCall.state.\(state.rawValue)")
        }
    }

    @Test("VoiceOver reads the row, the announcement and the notes")
    func accessibility() {
        let us = Locale(identifier: "en_US")
        let labels = MonarchAiToolCallLabels()
        #expect(
            MonarchAiToolCallTokens.rowLabel(title: "Search documents", state: "Done", duration: "1s")
                == "Search documents, Done, 1s"
        )
        #expect(MonarchAiToolCallTokens.rowLabel(title: "Search", state: "Running", duration: nil) == "Search, Running")
        #expect(
            MonarchAiToolCallTokens.announcement(label: "Search documents", state: "Done", labels: labels)
                == "Search documents: Done"
        )
        #expect(
            MonarchAiToolCallTokens.truncatedNote(shown: 4000, total: 52113, locale: us, labels: labels)
                == "Showing 4,000 of 52,113 characters"
        )
        #expect(MonarchAiToolCallStrings.errorLine("Error", "Timed out") == "Error: Timed out")
        #expect(MonarchAiToolCallStrings.toolLine("Tool", "send_email") == "Tool: send_email")
    }

    @Test("The card uses the subtle fill and the AI tool role")
    func colours() {
        #expect(MonarchAiToolCallTokens.fill == \MonarchColorTokens.bgSubtle)
        #expect(MonarchAiToolCallTokens.border == \MonarchColorTokens.borderSubtle)
        #expect(MonarchAiToolCallTokens.toolIcon == \MonarchColorTokens.aiTool)
        #expect(MonarchAiToolCallTokens.errorText == \MonarchColorTokens.dangerStrong)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiToolCallStrings.input == "Input")
        #expect(MonarchAiToolCallStrings.output == "Output")
        #expect(MonarchAiToolCallStrings.error == "Error")
        #expect(MonarchAiToolCallStrings.tool == "Tool")
        #expect(MonarchAiToolCallStrings.empty == "No details yet.")
        #expect(MonarchAiToolCallStrings.showMore == "Show all")
    }

    @Test("An open card is taller than a closed one")
    @MainActor
    func layout() {
        let closed = Host.layout(MonarchAiToolCall(MonarchAiToolCallFixtures.search))
        let open = Host.layout(MonarchAiToolCall(MonarchAiToolCallFixtures.search, isExpanded: .constant(true)))
        #expect(open.height > closed.height)
        #expect(closed.height >= MonarchAccessibility.minimumHitTarget)
    }
}
