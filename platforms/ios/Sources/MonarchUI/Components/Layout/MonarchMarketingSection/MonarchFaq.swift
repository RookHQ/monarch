import SwiftUI

/// Frequently asked questions: each question is a standard `DisclosureGroup` with the Monarch style, and its
/// answer opens under it.
///
/// ```swift
/// MonarchFaq(items: [MonarchFaqItem("Which formats can I export?", answer: "HTML, Markdown and PDF.")])
/// ```
///
/// Accessibility: each question is a disclosure button read with "Expanded" or "Collapsed"; the answer is read
/// after it when open. Any number of questions can be open at once.
public struct MonarchFaq: View {
    private let items: [MonarchFaqItem]
    @State private var expanded: Set<Int>

    @Environment(\.monarchTheme) private var theme

    /// A list of questions.
    ///
    /// - Parameters:
    ///   - items: The questions, in reading order.
    ///   - expandedIndices: The questions that start open, by position.
    public init(items: [MonarchFaqItem], expandedIndices: Set<Int> = []) {
        self.items = items
        self._expanded = State(initialValue: expandedIndices)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                DisclosureGroup(
                    isExpanded: Binding(
                        get: { expanded.contains(index) },
                        set: { isOn in
                            if isOn { expanded.insert(index) } else { expanded.remove(index) }
                        }
                    )
                ) {
                    Text(item.answer)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)
                } label: {
                    Text(item.question)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.titleColor])
                }
                .disclosureGroupStyle(.monarch)
                if index < items.count - 1 { MonarchDivider() }
            }
        }
    }
}
