import MonarchUI
import SwiftUI

struct MonarchReactionBarSection: CataloguePage {
    static let title = "Reaction bar"

    @State private var reactions = [
        MonarchReactionBarItem("👍", count: 3, isReacted: true, reactedBy: "You, Ana and Ben"),
        MonarchReactionBarItem("🎉", count: 1, reactedBy: "Chen"),
        MonarchReactionBarItem("❤️", count: 12),
    ]

    var body: some View {
        Text("Shipped the new onboarding today.").monarchTextStyle(.body)
        MonarchReactionBar(reactions, quickReactions: ["👍", "❤️", "🎉", "😂", "👀"]) { emoji, isReacted in
            toggle(emoji, isReacted)
        }

        CatalogueCaption("Under a post")
        MonarchReactionBar(reactions, size: .md, maxReactions: 4) { emoji, isReacted in
            toggle(emoji, isReacted)
        }

        CatalogueCaption("Read-only")
        MonarchReactionBar(reactions)
    }

    private func toggle(_ emoji: String, _ isReacted: Bool) {
        let delta = isReacted ? 1 : -1
        if let index = reactions.firstIndex(where: { $0.emoji == emoji }) {
            let old = reactions[index]
            reactions[index] = MonarchReactionBarItem(
                emoji,
                count: old.count + delta,
                isReacted: isReacted,
                label: old.label,
                reactedBy: old.reactedBy
            )
            if reactions[index].count == 0 { reactions.remove(at: index) }
        } else if isReacted {
            reactions.append(MonarchReactionBarItem(emoji, count: 1, isReacted: true))
        }
    }
}
