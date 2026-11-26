import MonarchUI
import SwiftUI

struct MonarchAiTaskProgressSection: CataloguePage {
    static let title = "AI task progress"

    private static let labels = ["Read the documents", "Group them by topic", "Write the summary"]

    @State private var status: MonarchAiTaskStatus = .running
    @State private var steps = Self.start()
    @State private var startedAt = Date()
    @State private var notify = false
    @State private var run: Task<Void, Never>?

    var body: some View {
        CatalogueCaption("Run the task; cancel or ask to be notified")
        MonarchAiTaskProgress(
            "Summarising 40 documents",
            status: status,
            startedAt: startedAt,
            steps: steps,
            eta: status == .running ? Double(steps.filter { $0.state != .done }.count * 60) : nil,
            isNotifying: $notify,
            onCancel: {
                run?.cancel()
                status = .cancelled
            }
        )
        MonarchButton("Start", variant: .primary, size: .sm) { start() }

        CatalogueCaption("Failed")
        MonarchAiTaskProgress(
            "Export the report",
            status: .failed,
            startedAt: Date(timeIntervalSinceNow: -120),
            steps: [
                MonarchAiPlanStep(id: "a", label: "Collect the rows", state: .done),
                MonarchAiPlanStep(id: "b", label: "Write the file", state: .failed),
            ]
        )
    }

    private static func start() -> [MonarchAiPlanStep] {
        labels.enumerated().map { MonarchAiPlanStep(id: "\($0.offset)", label: $0.element) }
    }

    private func start() {
        run?.cancel()
        steps = Self.start()
        startedAt = Date()
        status = .running
        run = Task {
            for index in steps.indices {
                steps[index].state = .active
                try? await Task.sleep(for: .seconds(2))
                if Task.isCancelled { return }
                steps[index].state = .done
            }
            status = .done
        }
    }
}
