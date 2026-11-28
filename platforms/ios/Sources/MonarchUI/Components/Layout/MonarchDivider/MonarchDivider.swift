import SwiftUI

/// A separator in the theme's border colour.
///
/// It is the standard `Divider` recoloured, so it is horizontal in a vertical
/// stack and vertical in a horizontal stack, like the system one.
///
/// ```swift
/// VStack {
///     Text("Above")
///     MonarchDivider()
///     Text("Below")
/// }
/// ```
///
/// Accessibility: decorative, hidden from VoiceOver.
public struct MonarchDivider: View {
    @Environment(\.monarchTheme) private var theme

    /// A divider.
    public init() {}

    /// The content of the view.
    public var body: some View {
        Divider()
            .overlay(theme.colors.borderSubtle)
            .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchDivider") {
        MonarchPreviewMatrix {
            VStack {
                Text("Above")
                MonarchDivider()
                Text("Below")
            }
        }
    }
#endif
