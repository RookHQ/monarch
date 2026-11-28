#if DEBUG
    import SwiftUI

    /// Shows preview content in the light theme, the dark theme and at the
    /// largest Dynamic Type size. Used only by `#Preview` blocks.
    struct MonarchPreviewMatrix<Content: View>: View {
        @ViewBuilder let content: Content

        var body: some View {
            ScrollView {
                VStack(spacing: 0) {
                    section(scheme: .light, size: .large)
                    section(scheme: .dark, size: .large)
                    section(scheme: .light, size: .accessibility5)
                }
            }
        }

        private func section(scheme: MonarchSchemePreference, size: DynamicTypeSize) -> some View {
            MonarchPreviewSection { content }
                .dynamicTypeSize(size)
                .monarchTheme(scheme: scheme)
        }
    }

    /// One themed block of a preview, on the theme's canvas colour.
    private struct MonarchPreviewSection<Content: View>: View {
        @ViewBuilder let content: Content

        @Environment(\.monarchTheme) private var theme

        var body: some View {
            content
                .padding(theme.spacing.space4)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(theme.colors.bgCanvas)
        }
    }
#endif
