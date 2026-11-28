import SwiftUI

/// A placeholder shaped like the answer that is on its way: lines of text,
/// a set of cards or a table. Show it between sending a prompt and the first
/// streamed token, and replace it in place with the answer.
///
/// ```swift
/// if conversation.status == .submitted {
///     MonarchAiResponseSkeleton(shape: .table, lines: 3, columns: 4)
/// }
/// ```
///
/// The bars pulse gently with the assistant tint. Under Reduce Motion they
/// stay still.
///
/// Accessibility: one element read as "Generating response…" (or `label`),
/// with the updates-frequently trait; the bars are hidden. It posts no
/// announcement: the conversation announces its own progress.
public struct MonarchAiResponseSkeleton: View {
    private let shape: MonarchAiResponseSkeletonShape
    private let lines: Int?
    private let columns: Int
    private let label: String?

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// A response placeholder.
    ///
    /// - Parameters:
    ///   - shape: The kind of answer expected, to avoid a jump when it arrives.
    ///   - lines: The number of text lines, cards or table body rows. 3 for
    ///     text and cards, 4 for a table when `nil`.
    ///   - columns: The number of table columns.
    ///   - label: Replaces the built-in spoken text, "Generating response…".
    public init(
        shape: MonarchAiResponseSkeletonShape = .text,
        lines: Int? = nil,
        columns: Int = 3,
        label: String? = nil
    ) {
        self.shape = shape
        self.lines = lines
        self.columns = columns
        self.label = label
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if reduceMotion {
                MonarchAiResponseSkeletonShapes(shape: shape, count: count, columns: columnCount, pulse: 0)
            } else {
                TimelineView(.animation) { context in
                    MonarchAiResponseSkeletonShapes(
                        shape: shape,
                        count: count,
                        columns: columnCount,
                        pulse: MonarchPulse.value(at: context.date.timeIntervalSinceReferenceDate)
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label ?? MonarchAiResponseSkeletonStrings.label)
        .accessibilityAddTraits(.updatesFrequently)
    }

    private var count: Int { MonarchAiResponseSkeletonTokens.count(lines, shape: shape) }
    private var columnCount: Int { MonarchAiResponseSkeletonTokens.columns(columns) }
}

/// The bars of one frame.
private struct MonarchAiResponseSkeletonShapes: View {
    let shape: MonarchAiResponseSkeletonShape
    let count: Int
    let columns: Int
    let pulse: Double

    @Environment(\.monarchTheme) private var theme

    typealias Tokens = MonarchAiResponseSkeletonTokens

    var body: some View {
        switch shape {
        case .text:
            VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
                ForEach(0..<count, id: \.self) { index in
                    MonarchAiSkeletonBar(width: Tokens.lineWidth(index: index, count: count), pulse: pulse)
                }
            }
        case .cards:
            VStack(alignment: .leading, spacing: Tokens.groupSpacing) {
                ForEach(0..<count, id: \.self) { _ in
                    VStack(alignment: .leading, spacing: Tokens.cardSpacing) {
                        MonarchAiSkeletonBar(width: Tokens.cardTitleWidth, isStrong: true, pulse: pulse)
                        MonarchAiSkeletonBar(width: 1, pulse: pulse)
                        MonarchAiSkeletonBar(width: Tokens.cardLineWidth, pulse: pulse)
                    }
                    .padding(Tokens.cardPadding)
                    .background(MonarchShapes.control.fill(theme.colors[role: Tokens.surface]))
                    .overlay(
                        MonarchShapes.control.strokeBorder(
                            theme.colors[role: Tokens.border],
                            lineWidth: MonarchShapes.borderWidth
                        )
                    )
                }
            }
        case .table:
            VStack(spacing: 0) {
                row(index: -1)
                ForEach(0..<count, id: \.self) { index in
                    Divider().overlay(theme.colors[role: Tokens.border])
                    row(index: index)
                }
            }
            .background(MonarchShapes.control.fill(theme.colors[role: Tokens.surface]))
            .clipShape(MonarchShapes.control)
            .overlay(
                MonarchShapes.control.strokeBorder(
                    theme.colors[role: Tokens.border],
                    lineWidth: MonarchShapes.borderWidth
                )
            )
        }
    }

    private func row(index: Int) -> some View {
        let isHeader = index < 0
        return HStack(spacing: Tokens.cellSpacing) {
            ForEach(0..<columns, id: \.self) { column in
                MonarchAiSkeletonBar(
                    width: isHeader ? Tokens.headerWidth : Tokens.cellWidth(row: index, column: column),
                    isStrong: isHeader,
                    pulse: pulse
                )
            }
        }
        .padding(Tokens.cardPadding)
        .background(theme.colors[role: isHeader ? Tokens.headerSurface : Tokens.surface])
    }
}

/// One bar: a capsule a fraction of the available width, starting at the leading edge.
private struct MonarchAiSkeletonBar: View {
    let width: Double
    var isStrong = false
    let pulse: Double

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var height = MonarchAiResponseSkeletonTokens.barHeight

    var body: some View {
        GeometryReader { proxy in
            Capsule()
                .fill(
                    theme.colors[
                        role: isStrong ? MonarchAiResponseSkeletonTokens.strongBar : MonarchAiResponseSkeletonTokens.bar
                    ]
                )
                .overlay(Capsule().fill(theme.colors[role: MonarchAiResponseSkeletonTokens.shimmer]).opacity(pulse))
                .frame(width: proxy.size.width * width)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .frame(height: height)
    }
}

#if DEBUG
    #Preview("MonarchAiResponseSkeleton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiResponseSkeleton()
                MonarchAiResponseSkeleton(shape: .cards, lines: 2)
                MonarchAiResponseSkeleton(shape: .table, lines: 3, columns: 4)
            }
        }
    }
#endif
