import SwiftUI

/// Its content in one row when it fits the width, else stacked: a title
/// with its actions, a label with its value. At the accessibility text
/// sizes it always stacks, so nothing is squeezed or cut.
///
/// It is `ViewThatFits` over an `HStack` and a `VStack` of the same
/// content, the standard way to choose a layout by the space there is.
struct MonarchRowOrStack<Content: View>: View {
    private let rowAlignment: VerticalAlignment
    private let stackAlignment: HorizontalAlignment
    private let spacing: CGFloat?
    private let content: Content

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        rowAlignment: VerticalAlignment = .center,
        stackAlignment: HorizontalAlignment = .leading,
        spacing: CGFloat? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.rowAlignment = rowAlignment
        self.stackAlignment = stackAlignment
        self.spacing = spacing
        self.content = content()
    }

    var body: some View {
        if Self.alwaysStacks(isAccessibilitySize: dynamicTypeSize.isAccessibilitySize) {
            stack
        } else {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: rowAlignment, spacing: spacing) { content }
                stack
            }
        }
    }

    private var stack: some View {
        VStack(alignment: stackAlignment, spacing: spacing) { content }
    }

    /// Whether the content stacks whatever the width: at the accessibility sizes.
    nonisolated static func alwaysStacks(isAccessibilitySize: Bool) -> Bool {
        isAccessibilitySize
    }
}
