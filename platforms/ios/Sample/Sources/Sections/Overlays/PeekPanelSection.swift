import MonarchUI
import SwiftUI

/// Native adoption: the inspector, a trailing column at regular width and a sheet on a phone.
struct PeekPanelSection: CataloguePage {
    static let title = "Peek panel (inspector)"

    @Environment(\.monarchTheme) private var theme
    @State private var selected: Int?

    private let invoices = ["Invoice 1041", "Invoice 1042", "Invoice 1043", "Invoice 1044"]

    private var isShown: Binding<Bool> {
        Binding(get: { selected != nil }, set: { if !$0 { selected = nil } })
    }

    var body: some View {
        CatalogueCaption("Choose a record to preview it beside the list")
        ForEach(invoices.indices, id: \.self) { index in
            MonarchButton(invoices[index], variant: selected == index ? .primary : .secondary, fullWidth: true) {
                selected = index
            }
        }
        // The modifier sits on an empty view so the page stays a plain column.
        theme.colors.bgCanvas
            .frame(height: 0)
            .accessibilityHidden(true)
            .inspector(isPresented: isShown) {
                if let index = selected {
                    preview(index)
                        .presentationDetents([.medium, .large])
                        .inspectorColumnWidth(min: 280, ideal: 360, max: 480)
                }
            }
    }

    private func preview(_ index: Int) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text(invoices[index])
                .monarchTextStyle(.heading)
                .accessibilityAddTraits(.isHeader)
            Text("\(index + 1) of \(invoices.count)")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            HStack(spacing: theme.spacing.space2) {
                MonarchButton("Previous", systemImage: "chevron.backward") { selected = index - 1 }
                    .disabled(index == 0)
                MonarchButton("Next", trailingSystemImage: "chevron.forward") { selected = index + 1 }
                    .disabled(index == invoices.count - 1)
            }
            MonarchButton("Close preview", variant: .ghost) { selected = nil }
        }
        .foregroundStyle(theme.colors.textDefault)
        .padding(theme.spacing.space4)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
