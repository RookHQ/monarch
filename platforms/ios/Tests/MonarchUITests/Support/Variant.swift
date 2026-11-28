import MonarchUI
import SwiftUI
import Testing

/// One of the four themes, by the two fixed inputs that select it.
struct ThemeCase: Sendable, CustomTestStringConvertible {
    let scheme: MonarchSchemePreference
    let contrast: MonarchContrastPreference
    let id: String

    var testDescription: String { id }

    static let all: [ThemeCase] = [
        ThemeCase(scheme: .light, contrast: .standard, id: "light"),
        ThemeCase(scheme: .dark, contrast: .standard, id: "dark"),
        ThemeCase(scheme: .light, contrast: .more, id: "hc-light"),
        ThemeCase(scheme: .dark, contrast: .more, id: "hc-dark"),
    ]
}

/// The environments every component is rendered in.
enum RenderCondition: String, CaseIterable, Sendable, CustomTestStringConvertible {
    case standard, largestText, rightToLeft, largestTextRightToLeft

    var testDescription: String { rawValue }

    var dynamicTypeSize: DynamicTypeSize {
        switch self {
        case .standard, .rightToLeft: .large
        case .largestText, .largestTextRightToLeft: .accessibility5
        }
    }

    var layoutDirection: LayoutDirection {
        switch self {
        case .standard, .largestText: .leftToRight
        case .rightToLeft, .largestTextRightToLeft: .rightToLeft
        }
    }
}

extension View {
    /// Applies a theme and a render condition, on the theme's canvas colour.
    func rendered(in theme: ThemeCase, _ condition: RenderCondition) -> some View {
        self
            .padding()
            .background(ThemedCanvas())
            .monarchTheme(scheme: theme.scheme, contrast: theme.contrast)
            .environment(\.dynamicTypeSize, condition.dynamicTypeSize)
            .environment(\.layoutDirection, condition.layoutDirection)
    }
}

private struct ThemedCanvas: View {
    @Environment(\.monarchTheme) private var theme
    var body: some View { theme.colors.bgCanvas }
}
