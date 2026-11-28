@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiBackgroundTasksSection: CataloguePage {
    static let title = "AI background tasks"

    @State private var showsTasks = false
    @State private var tasks: [MonarchAiBackgroundTask] = MonarchAiBackgroundTasksSection.initialTasks

    private static var initialTasks: [MonarchAiBackgroundTask] {
        let now = Date.now
        return [
            MonarchAiBackgroundTask(
                "Summarize 40 contracts",
                id: "contracts",
                status: .running,
                agent: "Research agent",
                progress: 0.3,
                step: "Reading contract 12 of 40",
                startedAt: now.addingTimeInterval(-192)
            ),
            MonarchAiBackgroundTask("Translate the help center", id: "translate", status: .queued),
            MonarchAiBackgroundTask(
                "Draft replies to 12 tickets",
                id: "replies",
                status: .needsInput,
                step: "Which tone should the replies use?",
                startedAt: now.addingTimeInterval(-900)
            ),
            MonarchAiBackgroundTask(
                "Clean up the CRM",
                id: "crm",
                status: .failed,
                endedAt: now.addingTimeInterval(-300),
                error: "The CRM did not answer in time."
            ),
            MonarchAiBackgroundTask(
                "Weekly report",
                id: "report",
                status: .succeeded,
                endedAt: now.addingTimeInterval(-3_600),
                result: "Report saved to Documents."
            ),
        ]
    }

    var body: some View {
        CatalogueCaption("The button shows the counts and opens the tray in a sheet.")
        MonarchAiBackgroundTasksButton(tasks: tasks) { showsTasks = true }
            .sheet(isPresented: $showsTasks) {
                NavigationStack {
                    MonarchAiBackgroundTasks(
                        tasks: tasks,
                        onOpen: { _ in showsTasks = false },
                        onCancel: { task in
                            update(task.id) {
                                $0.status = .cancelled
                                $0.endedAt = .now
                            }
                        },
                        onRetry: { task in
                            update(task.id) {
                                $0.status = .running
                                $0.startedAt = .now
                                $0.error = nil
                            }
                        },
                        onRespond: { task in update(task.id) { $0.status = .running } },
                        onDismiss: { task in tasks.removeAll { $0.id == task.id } },
                        onClearFinished: { finished in
                            let ids = Set(finished.map(\.id))
                            tasks.removeAll { ids.contains($0.id) }
                        }
                    )
                }
                .presentationDetents([.medium, .large])
            }
        MonarchButton("Finish the contracts task", variant: .ghost, size: .sm) {
            update("contracts") {
                $0.status = .succeeded
                $0.endedAt = .now
                $0.result = "40 summaries ready."
            }
        }
        MonarchButton("Reset", variant: .ghost, size: .sm) {
            tasks = Self.initialTasks
        }
    }

    private func update(_ id: String, _ change: (inout MonarchAiBackgroundTask) -> Void) {
        guard let index = tasks.firstIndex(where: { $0.id == id }) else { return }
        change(&tasks[index])
    }
}
