import SwiftUI

/// Text cut after a few lines, with a button that shows the rest.
///
/// ```swift
/// MonarchTruncatedText(note.body)
/// MonarchTruncatedText(note.body, lineLimit: 2, isExpanded: $showsWholeNote)
/// ```
///
/// The button appears only when the text is longer than the limit. Without a
/// binding the component keeps the expanded state itself.
///
/// Accessibility: VoiceOver reads the whole text whether or not it is cut,
/// because the text is one `Text` with a line limit. The button is a
/// standard button whose title says what it does, with a touch target at
/// least 44 points high. The change is animated unless Reduce Motion is on.
public struct MonarchTruncatedText: View {
    private let text: String
    private let lineLimit: Int
    private let externalIsExpanded: Binding<Bool>?
    private let isExpandable: Bool
    private let showMoreLabel: String?
    private let showLessLabel: String?

    @Environment(\.monarchTheme) private var theme
    @State private var internalIsExpanded = false
    @State private var limitedHeight: CGFloat = 0
    @State private var fullHeight: CGFloat = 0

    /// Truncated text that keeps its own expanded state.
    ///
    /// - Parameters:
    ///   - text: The text, already localised.
    ///   - lineLimit: How many lines are shown before the text is cut. At least 1.
    ///   - isExpandable: Whether a button shows the rest. Turn it off only
    ///     where the whole text is one tap away, such as a row that opens a record.
    ///   - showMoreLabel: Replaces the built-in "Show more" title.
    ///   - showLessLabel: Replaces the built-in "Show less" title.
    public init(
        _ text: String,
        lineLimit: Int = 3,
        isExpandable: Bool = true,
        showMoreLabel: String? = nil,
        showLessLabel: String? = nil
    ) {
        self.text = text
        self.lineLimit = MonarchTruncatedTextTokens.lineLimit(lineLimit)
        self.externalIsExpanded = nil
        self.isExpandable = isExpandable
        self.showMoreLabel = showMoreLabel
        self.showLessLabel = showLessLabel
    }

    /// Truncated text whose expanded state the app holds.
    ///
    /// - Parameters:
    ///   - text: The text, already localised.
    ///   - lineLimit: How many lines are shown before the text is cut. At least 1.
    ///   - isExpanded: Whether the whole text is shown.
    ///   - isExpandable: Whether a button shows the rest.
    ///   - showMoreLabel: Replaces the built-in "Show more" title.
    ///   - showLessLabel: Replaces the built-in "Show less" title.
    public init(
        _ text: String,
        lineLimit: Int = 3,
        isExpanded: Binding<Bool>,
        isExpandable: Bool = true,
        showMoreLabel: String? = nil,
        showLessLabel: String? = nil
    ) {
        self.text = text
        self.lineLimit = MonarchTruncatedTextTokens.lineLimit(lineLimit)
        self.externalIsExpanded = isExpanded
        self.isExpandable = isExpandable
        self.showMoreLabel = showMoreLabel
        self.showLessLabel = showLessLabel
    }

    private var isExpanded: Bool { externalIsExpanded?.wrappedValue ?? internalIsExpanded }

    /// The content of the view.
    public var body: some View {
        let isTruncated = MonarchTruncatedTextTokens.isTruncated(
            fullHeight: fullHeight,
            limitedHeight: limitedHeight
        )
        VStack(alignment: .leading, spacing: MonarchTruncatedTextTokens.spacing) {
            Text(text)
                .lineLimit(isExpanded ? nil : lineLimit)
                .monarchTextStyle(MonarchTruncatedTextTokens.textStyle)
                .foregroundStyle(theme.colors[role: MonarchTruncatedTextTokens.text])
                .fixedSize(horizontal: false, vertical: true)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background { measurement }
            if MonarchTruncatedTextTokens.showsButton(
                isExpandable: isExpandable,
                isTruncated: isTruncated,
                isExpanded: isExpanded
            ) {
                Button(action: toggle) {
                    Text(
                        MonarchTruncatedTextTokens.buttonTitle(
                            isExpanded: isExpanded,
                            showMoreLabel: showMoreLabel,
                            showLessLabel: showLessLabel
                        )
                    )
                    .monarchTextStyle(MonarchTruncatedTextTokens.buttonTextStyle)
                    .foregroundStyle(theme.colors[role: MonarchTruncatedTextTokens.button])
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .monarchAnimation(.base, value: isExpanded)
    }

    /// Two hidden copies of the text at the width of the visible one: the
    /// limited lines and the whole text. Their heights say whether the text
    /// is cut. SwiftUI has no direct way to ask.
    private var measurement: some View {
        ZStack {
            Text(text)
                .lineLimit(lineLimit)
                .monarchTextStyle(MonarchTruncatedTextTokens.textStyle)
                .fixedSize(horizontal: false, vertical: true)
                .background { MonarchTruncatedTextHeightReader(height: $limitedHeight) }
            Text(text)
                .monarchTextStyle(MonarchTruncatedTextTokens.textStyle)
                .fixedSize(horizontal: false, vertical: true)
                .background { MonarchTruncatedTextHeightReader(height: $fullHeight) }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .hidden()
        .accessibilityHidden(true)
    }

    private func toggle() {
        if let externalIsExpanded {
            externalIsExpanded.wrappedValue.toggle()
        } else {
            internalIsExpanded.toggle()
        }
    }
}

/// Writes the height of the view it is the background of into a binding.
private struct MonarchTruncatedTextHeightReader: View {
    @Binding var height: CGFloat

    var body: some View {
        GeometryReader { proxy in
            Color.clear
                .onAppear { height = proxy.size.height }
                .onChange(of: proxy.size.height) { _, newHeight in height = newHeight }
        }
    }
}

#if DEBUG
    #Preview("MonarchTruncatedText") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTruncatedText(
                    "A long note that runs over the limit so that the button appears below it. "
                        + "It keeps going for a while, with more detail than fits in two lines of body text.",
                    lineLimit: 2
                )
                MonarchTruncatedText("A short note.")
                MonarchTruncatedText(
                    "A long title cut to one line, with no button because the row opens the record.",
                    lineLimit: 1,
                    isExpandable: false
                )
            }
        }
    }
#endif
