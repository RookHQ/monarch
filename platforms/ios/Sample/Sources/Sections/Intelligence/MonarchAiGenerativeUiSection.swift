import Foundation
import MonarchUI
import SwiftUI

private struct SlotPickerProps: Decodable, Sendable {
    let title: String
    let slots: [String]?
}

struct MonarchAiGenerativeUiSection: CataloguePage {
    static let title = "AI generative UI"

    @State private var streamed = ""
    @State private var lastAction = "No action yet."

    private static let fullProps =
        "{\"title\": \"Pick a time for the call\", \"slots\": [\"Mon 10:00\", \"Tue 14:30\", \"Wed 09:00\"]}"

    private var registry: MonarchAiUiRegistry {
        var registry = MonarchAiUiRegistry()
        registry.register(
            "SlotPicker",
            props: SlotPickerProps.self,
            description: "Lets the person pick a meeting time."
        ) {
            props,
            emit in
            MonarchCard {
                Text(props.title).monarchTextStyle(.subheading)
                ForEach(props.slots ?? [], id: \.self) { slot in
                    MonarchButton(slot, size: .sm) {
                        emit("select", payload: ["slot": .string(slot)])
                    }
                }
            }
        }
        return registry
    }

    /// While streaming, the props are JSON text; once complete, they are decoded data.
    private var part: MonarchAiUiPart {
        let isDone = streamed.count == Self.fullProps.count
        let props: MonarchAiJSON =
            isDone
            ? ((try? JSONDecoder().decode(MonarchAiJSON.self, from: Data(streamed.utf8))) ?? .null)
            : .string(streamed)
        return MonarchAiUiPart(component: "SlotPicker", props: props, id: "part-1", state: isDone ? .done : .streaming)
    }

    var body: some View {
        CatalogueCaption("Streaming props")
        MonarchAiGenerativeUi(
            part,
            registry: registry,
            onAction: { action in lastAction = "\(action.type): \(action.payload?["slot"]?.stringValue ?? "")" }
        )
        MonarchButton("Stream", variant: .ghost, size: .sm) { stream() }
        Text(lastAction).monarchTextStyle(.caption)

        CatalogueCaption("A name that is not allowed")
        MonarchAiGenerativeUi(
            MonarchAiUiPart(component: "WebView", props: ["url": "javascript:alert(1)"]),
            registry: registry
        )

        CatalogueCaption("Props that do not match")
        MonarchAiGenerativeUi(MonarchAiUiPart(component: "SlotPicker", props: ["title": 42]), registry: registry)

        CatalogueCaption("Manifest for the system prompt")
        Text(registry.manifest()).monarchTextStyle(.code)
    }

    private func stream() {
        streamed = ""
        Task {
            for character in Self.fullProps {
                streamed.append(character)
                try? await Task.sleep(for: .milliseconds(20))
            }
        }
    }
}
