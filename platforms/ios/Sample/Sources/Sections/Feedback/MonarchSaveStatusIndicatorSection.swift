import MonarchUI
import SwiftUI

struct MonarchSaveStatusIndicatorSection: CataloguePage {
    static let title = "Save status indicator"

    @State private var text = "Quarterly plan"
    @State private var status = MonarchSaveStatus.idle
    @State private var savedAt: Date?
    @State private var failsNext = false

    var body: some View {
        TextField("Title", text: $text)
            .textFieldStyle(.roundedBorder)
            .onChange(of: text) { status = .unsaved }
        Toggle("Fail the next save", isOn: $failsNext)
            .monarchTextStyle(.body)
        MonarchButton("Save", variant: .primary) { save() }
        MonarchSaveStatusIndicator(status, savedAt: savedAt) { save() }
        CatalogueCaption("Every status")
        ForEach(MonarchSaveStatus.allCases, id: \.self) { status in
            MonarchSaveStatusIndicator(status, savedAt: .now.addingTimeInterval(-120))
        }
    }

    private func save() {
        status = .saving
        Task {
            try? await Task.sleep(for: .seconds(1))
            if failsNext {
                status = .error
                failsNext = false
            } else {
                savedAt = .now
                status = .saved
            }
        }
    }
}
