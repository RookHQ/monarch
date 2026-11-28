import SwiftUI

/// Side-by-side columns inside a document, such as a comparison or an
/// image beside its notes. Each child is one column.
///
/// ```swift
/// MonarchColumnsBlock(ratio: 0.4) {
///     Image("chart")
///     Text("Sign-ups doubled after the launch.")
/// }
/// ```
///
/// Two columns share the width by `ratio` (the first column's share, kept
/// between 0.2 and 0.8); three or more are equal. The columns stack in one
/// column when the block is narrower than 480 points and at the
/// accessibility text sizes. Columns are top-aligned and placed from the
/// leading edge, so the first column is on the right in right-to-left
/// languages.
///
/// Accessibility: the block is a container; VoiceOver reads the first
/// column, then the next, in order.
public struct MonarchColumnsBlock<Content: View>: View {
    private let ratio: Double
    private let content: Content

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A block of columns.
    ///
    /// - Parameters:
    ///   - ratio: The share of the width for the first of two columns, 0.2
    ///     to 0.8. Ignored for three or more columns.
    ///   - content: The columns, one view each. Wrap a column of several
    ///     views in a `VStack`.
    public init(ratio: Double = 0.5, @ViewBuilder content: () -> Content) {
        self.ratio = ratio
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        MonarchColumnsBlockLayout(
            ratio: MonarchColumnsBlockTokens.clampRatio(ratio),
            forcesStack: dynamicTypeSize.isAccessibilitySize
        ) {
            content
        }
        .accessibilityElement(children: .contain)
    }
}

#if DEBUG
    #Preview("MonarchColumnsBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchColumnsBlock(ratio: 0.35) {
                    Text("Before").monarchTextStyle(.bodyStrong)
                    Text("Sign-ups doubled after the launch, mostly from the new referral page.")
                        .monarchTextStyle(.body)
                }
                MonarchColumnsBlock {
                    Text("Plan").monarchTextStyle(.body)
                    Text("Build").monarchTextStyle(.body)
                    Text("Ship").monarchTextStyle(.body)
                }
            }
        }
    }
#endif
