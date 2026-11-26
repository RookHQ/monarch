import SwiftUI

/// The content of one block of ``MonarchBlockEditor``: an editable field in
/// the block's style, or the block's text when it cannot be edited. Custom
/// blocks are drawn by their own Monarch components.
struct MonarchBlockEditorRow: View {
    @Binding var block: MonarchBlock
    let fieldLabel: String
    let marker: String?
    let prompt: String?
    let isEditable: Bool
    let focus: FocusState<String?>.Binding
    let embedProviders: [MonarchEmbedProvider]
    let labels: MonarchBlockEditorLabels
    let onDeleteTable: @MainActor () -> Void
    let onTone: @MainActor (MonarchTone) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric(relativeTo: .body) private var markerWidth = MonarchBlockEditorTokens.markerWidth

    private typealias Tokens = MonarchBlockEditorTokens

    var body: some View {
        switch block.kind {
        case .paragraph, .heading:
            textField(block.kind)
        case .bulletList, .orderedList:
            HStack(alignment: .firstTextBaseline, spacing: Tokens.rowSpacing) {
                Text(marker ?? "")
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.markerColor])
                    .frame(minWidth: markerWidth, alignment: .trailing)
                    .accessibilityHidden(true)
                textField(block.kind)
            }
        case .taskList:
            HStack(alignment: .firstTextBaseline, spacing: Tokens.rowSpacing) {
                Toggle(isOn: $block.isChecked) {
                    Text(Tokens.checkboxLabel(block, labels: labels))
                }
                .toggleStyle(.monarchCheckbox)
                .labelsHidden()
                .disabled(!isEditable)
                .accessibilityLabel(Tokens.checkboxLabel(block, labels: labels))
                textField(block.kind)
                    .strikethrough(block.isChecked)
            }
        case .blockquote:
            HStack(spacing: Tokens.rowSpacing) {
                Rectangle()
                    .fill(theme.colors[role: Tokens.quoteBarColor])
                    .frame(width: Tokens.quoteBarWidth)
                    .accessibilityHidden(true)
                textField(block.kind).italic()
            }
            .fixedSize(horizontal: false, vertical: true)
        case .codeBlock:
            textField(block.kind)
                .padding(Tokens.codePadding)
                .background(
                    RoundedRectangle(cornerRadius: Tokens.codeCornerRadius, style: .continuous)
                        .fill(theme.colors[role: Tokens.codeBackground])
                )
                // Code reads left to right in every language.
                .environment(\.layoutDirection, .leftToRight)
        case .callout(let tone):
            VStack(alignment: .trailing, spacing: Tokens.rowSpacing) {
                if isEditable { toneSwitcher(current: tone) }
                MonarchCalloutBlock(tone: tone) { textField(block.kind) }
            }
        case .toggle:
            toggleBlock
        case .table:
            tableBlock
        case .embed:
            embedBlock
        }
    }

    // MARK: Text

    @ViewBuilder private func textField(_ kind: MonarchBlockKind) -> some View {
        let color =
            kind == .taskList
            ? Tokens.taskTextColor(isChecked: block.isChecked, isEnabled: isEnabled)
            : Tokens.textColor(isEnabled: isEnabled)
        let isHeading: Bool = if case .heading = kind { true } else { false }
        Group {
            if isEditable {
                TextField(
                    fieldLabel,
                    text: $block.text,
                    prompt: prompt.map { Text($0).foregroundStyle(theme.colors[role: Tokens.promptColor]) },
                    axis: .vertical
                )
                .textFieldStyle(.plain)
                .focused(focus, equals: block.id)
                .autocorrectionDisabled(kind == .codeBlock)
                .accessibilityLabel(fieldLabel)
            } else {
                Text(block.text)
                    .accessibilityLabel(fieldLabel)
                    .accessibilityValue(block.text)
            }
        }
        .monarchTextStyle(Tokens.textStyle(kind))
        .foregroundStyle(theme.colors[role: color])
        .frame(maxWidth: .infinity, minHeight: Tokens.minRowHeight, alignment: .leading)
        .accessibilityAddTraits(isHeading ? .isHeader : [])
    }

    // MARK: Callout tone

    /// The tones of a callout as the web's switcher above the block shows them: one small button each, the
    /// current tone raised.
    private func toneSwitcher(current: MonarchTone) -> some View {
        HStack(spacing: 0) {
            ForEach([MonarchTone.neutral, .info, .success, .warning, .danger], id: \.self) { tone in
                Button {
                    onTone(tone)
                } label: {
                    Group {
                        if tone == .neutral {
                            Circle().frame(width: 6, height: 6)
                        } else {
                            MonarchIcon(
                                systemName: MonarchCalloutBlockTokens.symbol(tone: tone, override: nil),
                                size: .sm
                            )
                        }
                    }
                    .foregroundStyle(theme.colors[role: \.textMuted])
                    .frame(width: Tokens.toneButtonSize, height: Tokens.toneButtonSize)
                    .background {
                        if tone == current {
                            MonarchShapes.control.fill(theme.colors[role: \.bgSurface])
                        }
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(Tokens.toneName(tone, labels: labels))
                .accessibilityAddTraits(tone == current ? .isSelected : [])
            }
        }
        .padding(Tokens.toneSwitcherPadding)
        .background(MonarchShapes.control.fill(theme.colors[role: \.bgMuted]))
    }

    // MARK: Custom blocks

    @ViewBuilder private var toggleBlock: some View {
        let summaryPrompt = labels.summaryPrompt ?? MonarchBlockEditorStrings.summaryPrompt
        let summary = block.text.isEmpty ? summaryPrompt : block.text
        MonarchToggleBlock(summary, isExpanded: $block.isExpanded) {
            if isEditable {
                VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
                    TextField(summaryPrompt, text: $block.text)
                        .textFieldStyle(.plain)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
                        .focused(focus, equals: block.id)
                        .frame(minHeight: Tokens.minRowHeight)
                    TextField(
                        labels.detailPrompt ?? MonarchBlockEditorStrings.detailPrompt,
                        text: $block.detail,
                        axis: .vertical
                    )
                    .textFieldStyle(.plain)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
                    .frame(minHeight: Tokens.minRowHeight)
                }
            } else if !block.detail.isEmpty {
                Text(block.detail)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
            }
        }
    }

    @ViewBuilder private var tableBlock: some View {
        let name = Tokens.name(.table, labels: labels)
        if isEditable {
            MonarchTableBlock(
                data: Binding(
                    get: { block.table ?? MonarchTableBlockData() },
                    set: { block.table = $0 }
                ),
                label: name,
                labels: labels.table,
                onDelete: onDeleteTable
            )
        } else {
            MonarchTableBlock(data: block.table ?? MonarchTableBlockData(), label: name)
        }
    }

    @ViewBuilder private var embedBlock: some View {
        let link = block.text.trimmingCharacters(in: .whitespacesAndNewlines)
        VStack(alignment: .leading, spacing: Tokens.innerSpacing) {
            if isEditable {
                TextField(labels.embedPrompt ?? MonarchBlockEditorStrings.embedPrompt, text: $block.text)
                    .textFieldStyle(.plain)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.textColor(isEnabled: isEnabled)])
                    .autocorrectionDisabled()
                    #if os(iOS)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                    #endif
                    .focused(focus, equals: block.id)
                    .frame(minHeight: Tokens.minRowHeight)
                    .accessibilityLabel(fieldLabel)
            }
            if !link.isEmpty {
                MonarchEmbedBlock(
                    url: link,
                    caption: block.detail.isEmpty ? nil : block.detail,
                    providers: embedProviders,
                    requiresTapToLoad: true,
                    labels: labels.embed
                )
            }
        }
    }
}
