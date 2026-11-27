import SwiftUI

/// The button that opens ``MonarchAiBackgroundTasks``: the title with a
/// count of running (or queued) tasks and of tasks that need the person.
///
/// ```swift
/// MonarchAiBackgroundTasksButton(tasks: tasks) { showsTasks = true }
/// ```
///
/// Accessibility: one button read as "Background tasks, 2 running, 1 needs
/// you". The counts are words, not colour alone; the "needs you" count uses
/// the warning tone.
@_spi(Experimental) public struct MonarchAiBackgroundTasksButton: View {
    private let tasks: [MonarchAiBackgroundTask]
    private let labels: MonarchAiBackgroundTasksLabels
    private let action: @MainActor () -> Void

    @Environment(\.locale) private var locale

    /// A button with the tray's counts.
    ///
    /// - Parameters:
    ///   - tasks: The same tasks the tray shows.
    ///   - labels: Replacements for the built-in strings.
    ///   - action: Opens the tray, usually in a sheet.
    public init(
        tasks: [MonarchAiBackgroundTask],
        labels: MonarchAiBackgroundTasksLabels = MonarchAiBackgroundTasksLabels(),
        action: @escaping @MainActor () -> Void
    ) {
        self.tasks = tasks
        self.labels = labels
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let counts = MonarchAiBackgroundTasksTokens.buttonCounts(tasks, locale: locale, labels: labels)
        Button(action: action) {
            HStack(spacing: MonarchAiBackgroundTasksTokens.actionSpacing) {
                MonarchIcon(systemName: MonarchAiBackgroundTasksTokens.buttonSymbol, size: .sm)
                Text(MonarchAiBackgroundTasksTokens.title(labels))
                if let activity = counts.activity {
                    MonarchBadge(
                        activity,
                        tone: tasks.contains { $0.status == .running }
                            ? MonarchAiBackgroundTasksTokens.runningTone : .neutral
                    )
                }
                if let attention = counts.needsInput {
                    MonarchBadge(attention, tone: MonarchAiBackgroundTasksTokens.needsInputTone)
                }
            }
        }
        .buttonStyle(.monarch(.secondary, size: .sm))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchAiBackgroundTasksTokens.buttonLabel(tasks, locale: locale, labels: labels))
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { action() }
    }
}

#if DEBUG
    #Preview("MonarchAiBackgroundTasksButton") {
        MonarchPreviewMatrix {
            MonarchAiBackgroundTasksButton(
                tasks: [
                    MonarchAiBackgroundTask("A", id: "1", status: .running),
                    MonarchAiBackgroundTask("B", id: "2", status: .needsInput),
                ]
            ) {}
        }
    }
#endif
