import SwiftUI

/// A placeholder for content that is loading: skeleton rows, cards or
/// paragraphs, or a labelled spinner.
///
/// ```swift
/// if model.tickets == nil {
///     MonarchLoadingState("Loading tickets", variant: .rows)
/// }
/// ```
///
/// The skeleton is the system placeholder redaction, so it follows the
/// theme's text colours and does not move. Replace the whole view with the
/// content when it arrives.
///
/// Accessibility: the view is one element read as the label, with the
/// placeholders hidden, and the label is announced once when the view
/// appears. The spinner variant shows the label. Placeholder lines scale
/// with Dynamic Type.
public struct MonarchLoadingState: View {
    private let label: String
    private let variant: MonarchLoadingStateVariant
    private let rows: Int

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var avatarSize = MonarchLoadingStateTokens.avatarSize
    @ScaledMetric(relativeTo: .body) private var cardHeight = MonarchLoadingStateTokens.cardHeight

    /// A loading state.
    ///
    /// - Parameters:
    ///   - label: What is loading, such as "Loading tickets". Already localised.
    ///   - variant: The placeholder layout.
    ///   - rows: The rows or cards drawn by the list, rows and cards variants.
    public init(
        _ label: String,
        variant: MonarchLoadingStateVariant = .list,
        rows: Int = 4
    ) {
        self.label = label
        self.variant = variant
        self.rows = rows
    }

    /// The content of the view.
    public var body: some View {
        placeholder
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .onAppear {
                AccessibilityNotification.Announcement(label).post()
            }
    }

    @ViewBuilder private var placeholder: some View {
        let count = MonarchLoadingStateTokens.rowCount(rows)
        switch variant {
        case .spinner:
            VStack(spacing: MonarchLoadingStateTokens.spinnerSpacing) {
                ProgressView()
                    .progressViewStyle(.circular)
                Text(label)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: MonarchLoadingStateTokens.label])
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(MonarchLoadingStateTokens.spinnerPadding)
        case .detail:
            VStack(alignment: .leading, spacing: MonarchLoadingStateTokens.lineSpacing) {
                ForEach(Array(MonarchLoadingStateTokens.detailWidths.enumerated()), id: \.offset) { index, width in
                    bar(
                        width,
                        height: index == 0
                            ? MonarchLoadingStateTokens.headingBarHeight : MonarchLoadingStateTokens.barHeight
                    )
                }
            }
        case .cards:
            LazyVGrid(
                columns: [
                    GridItem(
                        .adaptive(minimum: MonarchLoadingStateTokens.cardMinimumWidth),
                        spacing: MonarchLoadingStateTokens.rowSpacing
                    )
                ],
                spacing: MonarchLoadingStateTokens.rowSpacing
            ) {
                ForEach(0..<count, id: \.self) { _ in
                    MonarchShapes.container
                        .fill(theme.colors[role: MonarchLoadingStateTokens.blockFill])
                        .frame(height: cardHeight)
                }
            }
        case .rows:
            // As on the web: bordered rows with a long line and a short value of the same width at the end.
            VStack(alignment: .leading, spacing: 0) {
                ForEach(0..<count, id: \.self) { index in
                    HStack(spacing: MonarchLoadingStateTokens.rowGap) {
                        bar(
                            MonarchLoadingStateTokens.lineWidths(index).first,
                            height: MonarchLoadingStateTokens.barHeight
                        )
                        bar(100, height: MonarchLoadingStateTokens.barHeight)
                            .frame(width: MonarchLoadingStateTokens.valueWidth)
                    }
                    .frame(minHeight: MonarchLoadingStateTokens.rowMinHeight)
                    .padding(.vertical, MonarchLoadingStateTokens.rowPadding)
                    .overlay(alignment: .bottom) {
                        Rectangle()
                            .fill(theme.colors.borderSubtle)
                            .frame(height: MonarchShapes.borderWidth)
                    }
                }
            }
        case .list:
            VStack(alignment: .leading, spacing: MonarchLoadingStateTokens.rowSpacing) {
                ForEach(0..<count, id: \.self) { index in
                    HStack(spacing: MonarchLoadingStateTokens.rowGap) {
                        Circle()
                            .fill(theme.colors[role: MonarchLoadingStateTokens.blockFill])
                            .frame(width: avatarSize, height: avatarSize)
                        VStack(alignment: .leading, spacing: MonarchLoadingStateTokens.lineSpacing) {
                            bar(
                                MonarchLoadingStateTokens.lineWidths(index).first,
                                height: MonarchLoadingStateTokens.barHeight
                            )
                            bar(
                                MonarchLoadingStateTokens.lineWidths(index).second,
                                height: MonarchLoadingStateTokens.barHeight
                            )
                        }
                    }
                }
            }
        }
    }

    /// A skeleton bar `percent` of the available width.
    private func bar(_ percent: Int, height: CGFloat) -> some View {
        GeometryReader { proxy in
            RoundedRectangle(cornerRadius: MonarchLoadingStateTokens.barRadius, style: .continuous)
                .fill(theme.colors[role: MonarchLoadingStateTokens.blockFill])
                .frame(width: proxy.size.width * CGFloat(percent) / 100, height: height)
        }
        .frame(height: height)
    }
}

#if DEBUG
    #Preview("MonarchLoadingState") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchLoadingState("Loading contacts", rows: 2)
                MonarchLoadingState("Loading tickets", variant: .rows, rows: 2)
                MonarchLoadingState("Loading projects", variant: .cards, rows: 2)
                MonarchLoadingState("Loading the article", variant: .detail)
                MonarchLoadingState("Loading", variant: .spinner)
            }
        }
    }
#endif
