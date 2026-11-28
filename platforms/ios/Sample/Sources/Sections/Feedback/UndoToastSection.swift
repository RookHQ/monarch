import MonarchUI
import SwiftUI

/// Recipe: a toast with an Undo action and a deferred commit.
struct UndoToastSection: CataloguePage {
    static let title = "Undo toast (recipe)"

    @State private var toasts = MonarchToastCenter()
    @State private var notes = ["Groceries", "Trip plan", "Reading list"]
    @State private var committed: [String] = []

    var body: some View {
        VStack(alignment: .leading) {
            ForEach(notes, id: \.self) { note in
                HStack {
                    Text(note).monarchTextStyle(.body)
                    Spacer()
                    MonarchButton("Delete", systemImage: "trash", variant: .ghost, size: .sm) {
                        delete(note)
                    }
                }
            }
            if !committed.isEmpty {
                CatalogueCaption("Deleted for good: \(committed.joined(separator: ", "))")
            }
        }
        .frame(minHeight: 280, alignment: .top)
        .monarchToastHost(toasts)
    }

    private func delete(_ note: String) {
        guard let index = notes.firstIndex(of: note) else { return }
        notes.remove(at: index)
        let window: TimeInterval = 8
        let commit = Task {
            try? await Task.sleep(for: .seconds(window * toasts.durationScale))
            guard !Task.isCancelled else { return }
            committed.append(note)
        }
        toasts.show("Note deleted", message: note, actionTitle: "Undo", duration: window) {
            commit.cancel()
            notes.insert(note, at: min(index, notes.count))
            toasts.show("Restored", tone: .success, duration: 4)
        }
    }
}
