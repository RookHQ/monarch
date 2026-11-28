import MonarchUI
import SwiftUI

struct MonarchAiConversationListSection: CataloguePage {
    static let title = "AI conversation list"

    @State private var history: [MonarchAiConversationSummary] = {
        let now = Date()
        let day: TimeInterval = 86_400
        return [
            MonarchAiConversationSummary(id: "1", title: "Quarterly plan", updatedAt: now - day * 20, isPinned: true),
            MonarchAiConversationSummary(
                id: "2",
                title: "Summarise the support tickets",
                updatedAt: now - 600,
                preview: "Here are the five themes from this week."
            ),
            MonarchAiConversationSummary(id: "3", title: "", updatedAt: now - 3_600),
            MonarchAiConversationSummary(id: "4", title: "Draft the launch email", updatedAt: now - day * 2),
            MonarchAiConversationSummary(id: "5", title: "Trip ideas for Porto", updatedAt: now - day * 30),
        ]
    }()
    @State private var activeID: String? = "2"

    var body: some View {
        CatalogueCaption("Swipe a row, or long-press it, for Rename, Pin and Delete")
        NavigationStack {
            MonarchAiConversationList(
                history,
                activeID: activeID,
                onNew: {
                    let id = UUID().uuidString
                    history.append(MonarchAiConversationSummary(id: id, title: "", updatedAt: Date()))
                    activeID = id
                },
                onRename: { id, title in update(id) { $0.title = title } },
                onPin: { id, isPinned in update(id) { $0.isPinned = isPinned } },
                onDelete: { id in history.removeAll { $0.id == id } },
                onSelect: { id in activeID = id }
            )
            .navigationTitle("History")
        }
        .frame(height: 560)
    }

    private func update(_ id: String, _ change: (inout MonarchAiConversationSummary) -> Void) {
        guard let index = history.firstIndex(where: { $0.id == id }) else { return }
        change(&history[index])
    }
}
