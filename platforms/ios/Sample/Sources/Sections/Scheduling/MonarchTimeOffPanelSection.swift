@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchTimeOffPanelSection: CataloguePage {
    static let title = "Time off panel"

    private enum Shown: String, CaseIterable {
        case entries, loading, failed
    }

    private struct SampleError: LocalizedError {
        var errorDescription: String? { "The server did not answer. Try again." }
    }

    @State private var shown: Shown = .entries
    @State private var failsNext = false
    @State private var entries: [MonarchTimeOffEntry] = {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        func day(_ offset: Int) -> Date { calendar.date(byAdding: .day, value: offset, to: today) ?? today }
        return [
            MonarchTimeOffEntry(
                id: "1",
                personID: "ana",
                personName: "Ana Lima",
                isAllDay: true,
                start: day(2),
                end: day(6),
                type: "Annual leave",
                reason: "Family visit",
                status: "Pending"
            ),
            MonarchTimeOffEntry(id: "2", personID: nil, isAllDay: true, start: day(20), end: day(20)),
            MonarchTimeOffEntry(
                id: "3",
                personID: "kai",
                personName: "Kai Chen",
                isAllDay: true,
                start: day(-10),
                end: day(-9),
                type: "Sick leave"
            ),
        ]
    }()

    var body: some View {
        Picker("State", selection: $shown) {
            ForEach(Shown.allCases, id: \.self) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.segmented)
        Toggle("Fail the next save", isOn: $failsNext)
        MonarchTimeOffPanel(
            entries: entries,
            people: [MonarchTimeOffOption("Ana Lima", value: "ana"), MonarchTimeOffOption("Kai Chen", value: "kai")],
            allowsClosure: true,
            types: [
                MonarchTimeOffOption("Annual leave", value: "Annual leave"),
                MonarchTimeOffOption("Training", value: "Training"),
            ],
            isLoading: shown == .loading,
            errorMessage: shown == .failed ? "Time off could not be loaded." : nil,
            onRetry: { shown = .entries },
            onAdd: { draft in
                if failsNext {
                    failsNext = false
                    throw SampleError()
                }
                let name = ["ana": "Ana Lima", "kai": "Kai Chen"][draft.personID ?? ""]
                entries.append(
                    MonarchTimeOffEntry(
                        id: UUID().uuidString,
                        personID: draft.personID,
                        personName: name,
                        isAllDay: draft.isAllDay,
                        start: draft.start,
                        end: draft.end,
                        type: draft.type,
                        reason: draft.reason
                    )
                )
            },
            onRemove: { entry in
                if failsNext {
                    failsNext = false
                    throw SampleError()
                }
                entries.removeAll { $0.id == entry.id }
            }
        )
    }
}
