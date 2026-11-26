import SwiftUI

/// An ordered, unordered or task list of ``MonarchAiMarkdown``. Numbers are
/// read before each item; bullets are decoration; a task's check mark is
/// read as a checkbox with its state.
struct MonarchAiMarkdownListView: View {
    let ordered: Bool
    let start: Int
    let isLoose: Bool
    let items: [MonarchAiMarkdownListItem]
    let context: MonarchAiMarkdownContext
    let depth: Int
    let isMuted: Bool

    @Environment(\.monarchTheme) private var theme

    private typealias Tokens = MonarchAiMarkdownTokens

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.itemGap(isLoose: isLoose)) {
            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                HStack(alignment: .firstTextBaseline, spacing: Tokens.markerGap) {
                    marker(index: index, item: item)
                        .frame(minWidth: Tokens.markerWidth, alignment: .leading)
                    MonarchAiMarkdownBlocks(
                        blocks: item.children,
                        context: context,
                        depth: depth + 1,
                        isTight: !isLoose,
                        isMuted: isMuted
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private func marker(index: Int, item: MonarchAiMarkdownListItem) -> some View {
        if let checked = item.checked {
            Text(Image(systemName: checked ? Tokens.checkedTaskSymbol : Tokens.openTaskSymbol))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: checked ? Tokens.Role.checkedTask : Tokens.Role.openTask])
                .accessibilityRepresentation {
                    Toggle(context.labels.task, isOn: .constant(checked))
                        .disabled(true)
                }
        } else if ordered {
            Text(Tokens.number(start: start, index: index))
                .monarchTextStyle(.body)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: Tokens.Role.marker])
        } else {
            Text(Tokens.bullet(depth: depth))
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors[role: Tokens.Role.marker])
                .accessibilityHidden(true)
        }
    }
}
