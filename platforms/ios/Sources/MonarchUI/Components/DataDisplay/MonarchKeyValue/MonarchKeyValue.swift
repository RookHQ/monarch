import SwiftUI

/// Compact metadata as label and value pairs, such as
/// "Region eu-west · Plan Team · ID 4f2a…".
///
/// ```swift
/// MonarchKeyValue([
///     MonarchKeyValueItem("Region", value: "eu-west"),
///     MonarchKeyValueItem("Plan", value: "Team"),
///     MonarchKeyValueItem("ID", value: "4f2a…", copyValue: workspace.id, isMonospaced: true),
/// ])
/// ```
///
/// In the wrap layout the pairs flow onto more lines when they do not fit,
/// with a separator before every pair but the first. In the stack layout
/// each pair has its own line. For a grid of facts on a detail page, use a
/// `Form` or `LabeledContent`.
///
/// Accessibility: each pair is one element whose label is the key and whose
/// value is the value, so the two stay together. An empty value is drawn as
/// a dash and read as "Not set". Separators are decorative. A copy button is
/// a separate ``MonarchCopyButton`` named "Copy" and the key, with a 44-point
/// touch target. Text scales with Dynamic Type and long values wrap.
public struct MonarchKeyValue: View {
    private let items: [MonarchKeyValueItem]
    private let separator: MonarchKeyValueSeparator
    private let size: MonarchKeyValueSize
    private let layout: MonarchKeyValueLayout
    private let emptyLabel: String?
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .caption) private var dotSize = MonarchKeyValueTokens.dotSize
    @ScaledMetric(relativeTo: .caption) private var dividerHeight = MonarchKeyValueTokens.dividerHeight

    /// A key-value list.
    ///
    /// - Parameters:
    ///   - items: The pairs, in reading order.
    ///   - separator: What sits between pairs in the wrap layout.
    ///   - size: Caption text or body text.
    ///   - layout: Pairs that wrap, or one pair per line.
    ///   - emptyLabel: Replaces the built-in "Not set" spoken for an empty value.
    ///   - onCopy: Runs with the copied text after a copy button is used.
    public init(
        _ items: [MonarchKeyValueItem],
        separator: MonarchKeyValueSeparator = .dot,
        size: MonarchKeyValueSize = .sm,
        layout: MonarchKeyValueLayout = .wrap,
        emptyLabel: String? = nil,
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.items = items
        self.separator = separator
        self.size = size
        self.layout = layout
        self.emptyLabel = emptyLabel
        self.onCopy = onCopy
    }

    /// The content of the view.
    public var body: some View {
        switch layout {
        case .wrap:
            MonarchFlowLayout(
                spacing: MonarchKeyValueTokens.separatorSpacing,
                rowSpacing: MonarchKeyValueTokens.rowSpacing,
                alignment: .center
            ) {
                pairs
            }
        case .stack:
            VStack(alignment: .leading, spacing: MonarchKeyValueTokens.rowSpacing) {
                pairs
            }
        }
    }

    private var pairs: some View {
        ForEach(Array(items.enumerated()), id: \.offset) { index, item in
            HStack(spacing: MonarchKeyValueTokens.separatorSpacing) {
                if MonarchKeyValueTokens.showsSeparator(at: index, separator: separator, layout: layout) {
                    separatorMark
                }
                pair(item)
            }
        }
    }

    @ViewBuilder private var separatorMark: some View {
        if let role = MonarchKeyValueTokens.separator(separator) {
            Group {
                switch separator {
                case .dot:
                    Circle().fill(theme.colors[role: role]).frame(width: dotSize, height: dotSize)
                case .divider:
                    Rectangle()
                        .fill(theme.colors[role: role])
                        .frame(width: MonarchKeyValueTokens.dividerWidth, height: dividerHeight)
                case .none:
                    EmptyView()
                }
            }
            .accessibilityHidden(true)
        }
    }

    private func pair(_ item: MonarchKeyValueItem) -> some View {
        let style = MonarchKeyValueTokens.textStyle(size)
        let isEmpty = MonarchKeyValueTokens.isEmpty(item.value)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchKeyValueTokens.pairSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchKeyValueTokens.pairSpacing) {
                Text(item.label)
                    .monarchTextStyle(style)
                    .foregroundStyle(theme.colors[role: MonarchKeyValueTokens.label])
                Text(isEmpty ? MonarchKeyValueTokens.emptyGlyph : (item.value ?? ""))
                    .monarchTextStyle(MonarchKeyValueTokens.valueStyle(size, isMonospaced: item.isMonospaced))
                    .foregroundStyle(
                        theme.colors[role: isEmpty ? MonarchKeyValueTokens.empty : MonarchKeyValueTokens.value]
                    )
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(item.label)
            .accessibilityValue(MonarchKeyValueTokens.accessibilityValue(item.value, emptyLabel: emptyLabel))
            if let text = MonarchKeyValueTokens.copyText(item) {
                MonarchCopyButton(
                    value: text,
                    target: item.label,
                    variant: .ghost,
                    size: .sm,
                    isIconOnly: true,
                    onCopy: onCopy
                )
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchKeyValue") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchKeyValueSeparator.allCases, id: \.self) { separator in
                    MonarchKeyValue(
                        [
                            MonarchKeyValueItem("Region", value: "eu-west"),
                            MonarchKeyValueItem("Plan", value: "Team"),
                            MonarchKeyValueItem("Owner", value: nil),
                        ],
                        separator: separator
                    )
                }
                MonarchKeyValue(
                    [
                        MonarchKeyValueItem("ID", value: "4f2a91c0", isCopyable: true, isMonospaced: true),
                        MonarchKeyValueItem("Created", value: "12 March"),
                    ],
                    size: .md,
                    layout: .stack
                )
            }
        }
    }
#endif
