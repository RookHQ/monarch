import MonarchUI
import SwiftUI

struct MonarchStageJourneySection: CataloguePage {
    static let title = "Stage journey"

    private static let stages = [
        MonarchStageJourneyStage(id: "draft", label: "Draft", group: "Preparation"),
        MonarchStageJourneyStage(id: "filed", label: "Filed", group: "Preparation"),
        MonarchStageJourneyStage(
            id: "exam",
            label: "Examination",
            group: "Prosecution",
            description: "The office reviews the claims."
        ),
        MonarchStageJourneyStage(id: "allowed", label: "Allowed", group: "Prosecution"),
        MonarchStageJourneyStage(id: "granted", label: "Granted", group: "Outcome"),
        MonarchStageJourneyStage(id: "withdrawn", label: "Withdrawn", group: "Outcome"),
    ]

    @State private var current = "filed"
    @State private var previous: String? = "draft"
    @State private var enteredAt = Date.now.addingTimeInterval(-3 * 86400)
    @State private var history: [MonarchStageHistoryEntry] = [
        MonarchStageHistoryEntry(id: "0", to: "Draft", at: .now.addingTimeInterval(-9 * 86400), actor: "Alex"),
        MonarchStageHistoryEntry(
            id: "1",
            from: "Draft",
            to: "Filed",
            at: .now.addingTimeInterval(-3 * 86400),
            actor: "Alex",
            meta: "2 deadlines added"
        ),
    ]
    @State private var failsNext = false

    private struct SampleFailure: LocalizedError {
        var errorDescription: String? { "Answer the office action first." }
    }

    private var terminal: MonarchStageJourneyTerminal? {
        switch current {
        case "granted": .success
        case "withdrawn": .closed
        default: nil
        }
    }

    private var moves: [MonarchStageJourneyTransition] {
        guard let index = Self.stages.firstIndex(where: { $0.id == current }) else { return [] }
        var moves: [MonarchStageJourneyTransition] = []
        if terminal == nil, index + 1 < Self.stages.count - 1 {
            let next = Self.stages[index + 1]
            moves.append(
                MonarchStageJourneyTransition(id: next.id, label: next.label, needsInput: next.id == "allowed")
            )
            moves.append(MonarchStageJourneyTransition(id: "withdrawn", label: "Withdrawn", kind: .exit))
        }
        if let previous, let stage = Self.stages.first(where: { $0.id == previous }) {
            moves.append(MonarchStageJourneyTransition(id: stage.id, label: stage.label, kind: .back))
        }
        return moves
    }

    var body: some View {
        MonarchStageJourney(
            Self.stages,
            currentID: current,
            terminal: terminal,
            enteredAt: enteredAt,
            currentMeta: current == "exam" ? "Office action due" : nil,
            transitions: moves,
            onTransition: { move in
                try await Task.sleep(for: .milliseconds(600))
                if failsNext {
                    failsNext = false
                    throw SampleFailure()
                }
                let from = Self.stages.first { $0.id == current }?.label
                history.insert(
                    MonarchStageHistoryEntry(
                        id: UUID().uuidString,
                        from: from,
                        to: move.label,
                        at: .now,
                        actor: "You",
                        isBackward: move.kind == .back
                    ),
                    at: 0
                )
                previous = move.kind == .back ? nil : current
                current = move.id
                enteredAt = .now
            }
        )
        Toggle("Fail the next move", isOn: $failsNext)
            .monarchTextStyle(.body)

        CatalogueCaption("Strip only")
        MonarchStageJourney(Self.stages, currentID: current, terminal: terminal, variant: .strip)

        CatalogueCaption("History, newest first")
        MonarchStageHistory(history)
    }
}
