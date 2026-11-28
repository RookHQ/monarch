import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct WeatherProps: Decodable, Sendable {
    let city: String
    let temperature: Double?
    let link: String?
}

private struct BrokenError: Error {}

/// A view builder that always fails, to test the render error.
private func brokenView() throws -> Text {
    throw BrokenError()
}

/// Collects the action types a test channel receives.
@MainActor private final class ReceivedTypes {
    var types: [String] = []
}

@MainActor private func sampleRegistry() -> MonarchAiUiRegistry {
    var registry = MonarchAiUiRegistry()
    registry.register(
        "Weather",
        props: WeatherProps.self,
        description: "Current weather for a city.",
        validate: { !$0.city.isEmpty },
        content: { props, emit in
            VStack(alignment: .leading) {
                Text(props.city).monarchTextStyle(.subheading)
                Text(props.temperature.map { "\(Int($0))°" } ?? "…").monarchTextStyle(.body)
                if emit.isConnected {
                    MonarchButton("Refresh", size: .sm) { emit("refresh") }
                }
            }
        }
    )
    registry.register("Chart", props: WeatherProps.self, streaming: .onComplete) { props, _ in
        Text(props.city)
    }
    registry.register("Broken", props: WeatherProps.self) { _, _ in
        try brokenView()
    }
    return registry
}

enum MonarchAiGenerativeUiFixtures: ComponentFixtures {
    static let id = "ai-generative-ui"
    static let fixtures = [
        Fixture("rendered, with actions") {
            MonarchAiGenerativeUi(
                MonarchAiUiPart(component: "Weather", props: ["city": "Lisbon", "temperature": 21]),
                registry: sampleRegistry(),
                onAction: { _ in }
            )
        },
        Fixture("unknown component") {
            MonarchAiGenerativeUi(MonarchAiUiPart(component: "Script", props: [:]), registry: sampleRegistry())
        },
        Fixture("invalid props") {
            MonarchAiGenerativeUi(MonarchAiUiPart(component: "Weather", props: ["city": 3]), registry: sampleRegistry())
        },
        Fixture("render error, custom label") {
            MonarchAiGenerativeUi(
                MonarchAiUiPart(component: "Broken", props: ["city": "Oslo"]),
                registry: sampleRegistry(),
                labels: MonarchAiGenerativeUiLabels(failed: "The chart could not be drawn.")
            )
        },
        Fixture("streaming, partial props render") {
            MonarchAiGenerativeUi(
                MonarchAiUiPart(component: "Weather", props: "{\"city\": \"Lis", state: .streaming),
                registry: sampleRegistry()
            )
        },
        Fixture("streaming, waiting for the complete part") {
            MonarchAiGenerativeUi(
                MonarchAiUiPart(component: "Chart", props: "{\"city\": \"Oslo\"}", state: .streaming),
                registry: sampleRegistry()
            )
        },
    ]
}

@Suite("MonarchAiGenerativeUi")
@MainActor struct MonarchAiGenerativeUiTests {
    private func outcome(_ part: MonarchAiUiPart, emit: MonarchAiUiEmit = MonarchAiUiEmit()) -> String {
        switch MonarchAiGenerativeUiTokens.outcome(part: part, registry: sampleRegistry(), emit: emit) {
        case .rendered: "rendered"
        case .pending: "pending"
        case .rejected(let reason): reason.rawValue
        }
    }

    @Test("Only registered names render, matched exactly")
    func allowList() {
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: ["city": "Lisbon"])) == "rendered")
        #expect(outcome(MonarchAiUiPart(component: "weather", props: ["city": "Lisbon"])) == "unknownComponent")
        #expect(outcome(MonarchAiUiPart(component: "", props: [:])) == "unknownComponent")
        #expect(outcome(MonarchAiUiPart(component: "constructor", props: [:])) == "unknownComponent")
        let registry = sampleRegistry()
        #expect(registry.names == ["Weather", "Chart", "Broken"])
        #expect(registry.contains("Chart"))
        #expect(!registry.contains("Script"))
    }

    @Test("Props that do not decode or fail validation are refused")
    func invalidProps() {
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: ["city": 3])) == "invalidProps")
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: ["temperature": 20])) == "invalidProps")
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: ["city": ""])) == "invalidProps")
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: ["a", "b"])) == "invalidProps")
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: "{\"city\":\"Oslo\"}")) == "invalidProps")
    }

    @Test("A view builder that throws shows the failure notice")
    func renderError() {
        #expect(outcome(MonarchAiUiPart(component: "Broken", props: ["city": "Oslo"])) == "renderError")
    }

    @Test("Streaming parts render as soon as they decode, and are never refused for their props")
    func streaming() {
        #expect(
            outcome(MonarchAiUiPart(component: "Weather", props: "{\"city\": \"Lis", state: .streaming)) == "rendered"
        )
        #expect(outcome(MonarchAiUiPart(component: "Weather", props: "{\"temp", state: .streaming)) == "pending")
        #expect(
            outcome(MonarchAiUiPart(component: "Chart", props: "{\"city\": \"Oslo\"}", state: .streaming)) == "pending"
        )
        #expect(outcome(MonarchAiUiPart(component: "Chart", props: ["city": "Oslo"], state: .done)) == "rendered")
        #expect(outcome(MonarchAiUiPart(component: "Nope", props: "{", state: .streaming)) == "unknownComponent")
    }

    @Test("Unsafe URLs are dropped by key and by shape, at any depth")
    func urls() {
        let props: MonarchAiJSON = [
            "href": "javascript:alert(1)",
            "src": "https://example.com/a.png",
            "url": "/relative/path",
            "note": "Meeting at 12:30: bring notes",
            "hidden": ["deep": ["target": "data:text/html,<script>"]],
            "mail": "mailto:team@example.com",
            "phone": "tel:123",
            "spaced": "java\nscript:alert(1)",
        ]
        let clean = MonarchAiUiSanitizer.sanitize(props)
        #expect(clean?["href"] == MonarchAiJSON?.none)
        #expect(clean?["src"] == "https://example.com/a.png")
        #expect(clean?["url"] == "/relative/path")
        #expect(clean?["note"] == "Meeting at 12:30: bring notes")
        #expect(clean?["hidden"]?["deep"] == .object([:]))
        #expect(clean?["mail"] == "mailto:team@example.com")
        #expect(clean?["phone"] == MonarchAiJSON?.none)
        #expect(clean?["spaced"] == MonarchAiJSON?.none)
    }

    @Test("Sizes, depth and numbers are limited")
    func limits() {
        let long = String(repeating: "a", count: MonarchAiUiSanitizer.maxString + 10)
        #expect(
            MonarchAiUiSanitizer.clean(.string(long), key: "x", depth: 0)?.stringValue?.count
                == MonarchAiUiSanitizer.maxString
        )
        let many = MonarchAiJSON.array(Array(repeating: 1, count: 600))
        if case .array(let items) = MonarchAiUiSanitizer.clean(many, key: "x", depth: 0) {
            #expect(items.count == MonarchAiUiSanitizer.maxItems)
        } else {
            Issue.record("Expected an array")
        }
        #expect(MonarchAiUiSanitizer.clean(.number(.infinity), key: "x", depth: 0) == MonarchAiJSON?.none)
        #expect(MonarchAiUiSanitizer.clean(.number(.nan), key: "x", depth: 0) == MonarchAiJSON?.none)
        var nested: MonarchAiJSON = "leaf"
        for _ in 0..<10 { nested = ["child": nested] }
        let cleaned = MonarchAiUiSanitizer.sanitize(nested)
        var depth = 0
        var cursor = cleaned
        while let next = cursor?["child"] {
            depth += 1
            cursor = next
        }
        #expect(depth < 10)
        #expect(MonarchAiUiSanitizer.sanitize(.null) == .object([:]))
        #expect(MonarchAiUiSanitizer.sanitize("text") == MonarchAiJSON?.none)
    }

    @Test("Partial JSON is closed as far as it can be read")
    func partial() {
        #expect(MonarchAiPartialJSON.parse("{\"a\": 1}") == ["a": 1])
        #expect(MonarchAiPartialJSON.parse("{\"a\": \"hel") == ["a": "hel"])
        #expect(MonarchAiPartialJSON.parse("{\"a\": 1, \"b") == ["a": 1, "b": nil])
        #expect(MonarchAiPartialJSON.parse("{\"a\": 1,") == ["a": 1])
        #expect(MonarchAiPartialJSON.parse("{\"a\":") == ["a": nil])
        #expect(MonarchAiPartialJSON.parse("{\"a\": [1, 2") == ["a": [1, 2]])
        #expect(MonarchAiPartialJSON.parse("{\"a\": tru") == [:])
        #expect(MonarchAiPartialJSON.parse("{\"a\": 1, \"b\": fal") == ["a": 1])
        #expect(MonarchAiPartialJSON.parse("{\"a\": \"x\\") == ["a": "x"])
        #expect(MonarchAiPartialJSON.parse("{\"a\": {\"b\": \"c") == ["a": ["b": "c"]])
        #expect(MonarchAiPartialJSON.parse("") == MonarchAiJSON?.none)
        #expect(MonarchAiPartialJSON.parse("   ") == MonarchAiJSON?.none)
    }

    @Test("Actions name the part and carry a sanitised payload")
    func actions() {
        let part = MonarchAiUiPart(component: "Weather", props: [:], id: "p1", toolCallId: "call_1")
        let action = MonarchAiGenerativeUiTokens.action(
            type: "submit",
            payload: ["slot": "10:00", "href": "javascript:x"],
            part: part
        )
        #expect(action.type == "submit")
        #expect(action.component == "Weather")
        #expect(action.partId == "p1")
        #expect(action.toolCallId == "call_1")
        #expect(action.payload == ["slot": "10:00"])
    }

    @Test("The action channel sends only when connected and ignores an empty type")
    func emit() {
        let received = ReceivedTypes()
        let connected = MonarchAiUiEmit { type, _ in received.types.append(type) }
        #expect(connected.isConnected)
        connected("select")
        connected("")
        #expect(received.types == ["select"])
        let silent = MonarchAiUiEmit()
        #expect(!silent.isConnected)
        silent("select")
    }

    @Test("The manifest lists each name with its description")
    func manifest() {
        #expect(sampleRegistry().manifest() == "- Weather: Current weather for a city.\n- Chart\n- Broken")
    }

    @Test("Notices resolve and can be replaced")
    func notices() {
        let labels = MonarchAiGenerativeUiLabels()
        let texts = MonarchAiUiRejection.allCases.map { MonarchAiGenerativeUiTokens.notice($0, labels: labels) }
        #expect(Set(texts).count == 3)
        #expect(texts.allSatisfy { !$0.hasPrefix("monarch.") })
        let custom = MonarchAiGenerativeUiLabels(unknown: "Not available.")
        #expect(MonarchAiGenerativeUiTokens.notice(.unknownComponent, labels: custom) == "Not available.")
        #expect(MonarchAiGenerativeUiStrings.loading == "Loading")
    }
}
