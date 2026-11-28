import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiComputerViewSection: CataloguePage {
    static let title = "AI computer view"

    private static let steps: [(MonarchAiComputerActionKind, String, MonarchAiComputerPoint?, String?)] = [
        (.navigate, "Opened the sign-in page", nil, "https://example.com/login"),
        (.click, "Clicked the email field", MonarchAiComputerPoint(x: 640, y: 300), nil),
        (.type, "Typed the email address", MonarchAiComputerPoint(x: 640, y: 300), "sam@example.com"),
        (.click, "Clicked Continue", MonarchAiComputerPoint(x: 900, y: 420), nil),
        (.read, "Read the account summary", nil, nil),
    ]

    @State private var status = MonarchAiComputerViewStatus.working
    @State private var actions: [MonarchAiComputerAction] = []
    @State private var selection: String?

    private var frame: MonarchAiComputerFrame {
        MonarchAiComputerFrame(
            image: Image(systemName: "rectangle.on.rectangle"),
            width: 1280,
            height: 800,
            capturedAt: actions.last?.date ?? .now,
            alt: "The example sign-in page"
        )
    }

    var body: some View {
        CatalogueCaption("Add actions, pause, take over and stop")
        MonarchAiComputerView(
            status: status,
            frame: frame,
            actions: actions,
            currentURL: "https://example.com/login",
            windowTitle: "Sign in",
            pointer: actions.last?.point,
            waitingReason: "Sign in to continue",
            selection: $selection,
            onPauseChange: { paused in status = paused ? .paused : .working },
            onTakeOver: { status = .takenOver },
            onHandBack: { status = .working },
            onStop: { status = .stopped }
        )
        MonarchButton("Next action", systemImage: "plus", size: .sm) {
            let step = Self.steps[actions.count % Self.steps.count]
            actions.append(
                MonarchAiComputerAction(
                    id: UUID().uuidString,
                    kind: step.0,
                    label: step.1,
                    date: .now,
                    point: step.2,
                    detail: step.3
                )
            )
        }
        Picker("Status", selection: $status) {
            ForEach(MonarchAiComputerViewStatus.allCases, id: \.self) { status in
                Text(status.rawValue).tag(status)
            }
        }

        CatalogueCaption("Compact, after a private action")
        MonarchAiComputerView(
            status: .done,
            frame: frame,
            actions: [
                MonarchAiComputerAction(
                    id: "password",
                    kind: .type,
                    label: "Typed the password",
                    date: .now,
                    isSensitive: true
                )
            ],
            surface: .computer,
            isCompact: true
        )
    }
}
