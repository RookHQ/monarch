import MonarchUI
import SwiftUI

struct MonarchRunningTimerSection: CataloguePage {
    static let title = "Running timer"

    @State private var startedAt: Date?
    @State private var project = "Atlas"
    @State private var log: [String] = []
    @State private var failsNext = false

    @Environment(\.monarchTheme) private var theme

    private struct SampleFailure: LocalizedError {
        var errorDescription: String? { "The server did not answer. Try again." }
    }

    var body: some View {
        CatalogueCaption("Panel")
        MonarchRunningTimer(
            startedAt: startedAt,
            label: "ENG-142 Fix the export",
            isStartDisabled: project.isEmpty,
            onStart: {
                try await pretendToSave()
                startedAt = .now
            },
            onStop: {
                try await pretendToSave()
                record(until: .now)
            },
            onStopAt: { date in
                try await pretendToSave()
                record(until: date)
            },
            onDiscard: {
                try await pretendToSave()
                startedAt = nil
                log.append("Discarded")
            },
            fields: {
                Picker("Project", selection: $project) {
                    Text("Atlas").tag("Atlas")
                    Text("Pulse").tag("Pulse")
                }
                .monarchTextStyle(.body)
            }
        )
        Toggle("Fail the next action", isOn: $failsNext)
            .monarchTextStyle(.body)

        CatalogueCaption("Indicator, for a toolbar")
        MonarchRunningTimer(
            startedAt: startedAt,
            label: "ENG-142",
            variant: .indicator,
            showsWhenStopped: true,
            onStop: {
                try await pretendToSave()
                record(until: .now)
            },
            onDiscard: {
                startedAt = nil
                log.append("Discarded")
            },
            onOpen: { log.append("Opened ENG-142") }
        )

        CatalogueCaption("Running for more than 12 hours")
        MonarchRunningTimer(startedAt: Date.now.addingTimeInterval(-13 * 3600), label: "Forgotten timer", onStop: {})

        CatalogueCaption("Log")
        ForEach(Array(log.enumerated()), id: \.offset) { _, line in
            Text(line)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func pretendToSave() async throws {
        try await Task.sleep(for: .milliseconds(600))
        if failsNext {
            failsNext = false
            throw SampleFailure()
        }
    }

    private func record(until end: Date) {
        guard let startedAt else { return }
        let minutes = Int(end.timeIntervalSince(startedAt) / 60)
        log.append("Logged \(minutes) min on \(project)")
        self.startedAt = nil
    }
}
