import MonarchUI
import SwiftUI

struct MonarchAutosaveIndicatorSection: CataloguePage {
    static let title = "Autosave indicator"

    private struct Offline: Error {}

    @State private var draft = "Meeting notes"
    @State private var failures: MonarchAutosaveSampleFailures
    @State private var autosave: MonarchAutosave<String>

    init() {
        let failures = MonarchAutosaveSampleFailures()
        _failures = State(initialValue: failures)
        _autosave = State(
            initialValue: MonarchAutosave("Meeting notes", retries: 1, retryDelay: .seconds(1)) { _ in
                try await Task.sleep(for: .milliseconds(600))
                if failures.left > 0 {
                    failures.left -= 1
                    throw Offline()
                }
            }
        )
    }

    var body: some View {
        CatalogueCaption("Type: it saves one second after you stop")
        TextField("Notes", text: $draft, axis: .vertical)
            .textFieldStyle(.roundedBorder)
            .onChange(of: draft) { autosave.update(draft) }
        MonarchAutosaveIndicator(autosave, errorMessage: { _ in "You are offline" })

        CatalogueCaption("Controls")
        Button("Fail the next two saves") { failures.left = 2 }
            .buttonStyle(.monarch(.secondary))
        Toggle("Save automatically", isOn: Binding(get: { autosave.isEnabled }, set: { autosave.isEnabled = $0 }))
        Button("Save now") {
            Task { await autosave.flush() }
        }
        .buttonStyle(.monarch(.secondary))
        Text(autosave.isDirty ? "Some changes are not saved." : "Everything is saved.")
            .monarchTextStyle(.caption)
    }
}

/// Lets the page make saves fail on demand.
@MainActor
private final class MonarchAutosaveSampleFailures {
    var left = 0
}
