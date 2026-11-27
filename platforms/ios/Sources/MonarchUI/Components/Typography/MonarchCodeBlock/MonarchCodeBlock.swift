import SwiftUI

/// A code sample for documentation, settings and messages: a header with
/// the file name or language, copy, a wrap toggle, optional line numbers,
/// light syntax colouring and a diff mode.
///
/// ```swift
/// MonarchCodeBlock(
///     "curl https://api.example.com/v1/items \\\n  -H \"Authorization: Bearer $TOKEN\"",
///     language: "bash",
///     title: "Create an item"
/// )
/// ```
///
/// The code is monospaced text built from an `AttributedString`. Long lines
/// scroll sideways inside the block, so the screen never does, until the
/// wrap toggle is on. The copy button is ``MonarchCopyButton`` and copies
/// the code exactly as given. The highlighter knows JavaScript and
/// TypeScript, JSON, Python, shell, CSS, HTML and XML, SQL, Go and Rust;
/// other languages are plain text.
///
/// Accessibility: the code is a container named after the title, else the
/// language ("TypeScript code"), else "Code". Each line is one element read
/// as its text; added and removed lines of a diff start with "Added" or
/// "Removed"; line numbers and markers are not read, and nothing depends on
/// colour alone. The wrap toggle is a standard toggle and the copy button
/// announces the copy. The code reads left to right in every layout.
public struct MonarchCodeBlock: View {
    private let code: String
    private let language: String?
    private let title: String?
    private let isDiffOverride: Bool?
    private let isHighlighted: Bool
    private let showsLineNumbers: Bool
    private let showsWrapToggle: Bool
    private let isCopyable: Bool
    private let externalIsWrapped: Binding<Bool>?
    private let labels: MonarchCodeBlockLabels
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var internalIsWrapped = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var viewportWidth: CGFloat = 0

    /// A code block.
    ///
    /// - Parameters:
    ///   - code: The code, exactly as it should be copied.
    ///   - language: A language id ("ts", "python", "bash", "diff"), which
    ///     picks the highlighter and the header text. Unknown languages are
    ///     plain text.
    ///   - title: The header text, such as a file name, already localised.
    ///     It also names the code. Defaults to the language name.
    ///   - isDiff: Whether `+` and `-` lines are drawn as a diff. Defaults to
    ///     true when `language` is "diff" or "patch".
    ///   - isHighlighted: Whether syntax colouring is on.
    ///   - showsLineNumbers: Whether line numbers are shown before the lines.
    ///   - showsWrapToggle: Whether the wrap toggle is shown.
    ///   - isCopyable: Whether the copy button is shown.
    ///   - isWrapped: Whether long lines wrap instead of scrolling sideways.
    ///     Without it the component keeps the state itself, starting unwrapped.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied code after each copy.
    public init(
        _ code: String,
        language: String? = nil,
        title: String? = nil,
        isDiff: Bool? = nil,
        isHighlighted: Bool = true,
        showsLineNumbers: Bool = false,
        showsWrapToggle: Bool = true,
        isCopyable: Bool = true,
        isWrapped: Binding<Bool>? = nil,
        labels: MonarchCodeBlockLabels = MonarchCodeBlockLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.code = code
        self.language = language
        self.title = title
        self.isDiffOverride = isDiff
        self.isHighlighted = isHighlighted
        self.showsLineNumbers = showsLineNumbers
        self.showsWrapToggle = showsWrapToggle
        self.isCopyable = isCopyable
        self.externalIsWrapped = isWrapped
        self.labels = labels
        self.onCopy = onCopy
    }

    private var isWrapped: Binding<Bool> {
        externalIsWrapped ?? $internalIsWrapped
    }

    private var isDiff: Bool {
        MonarchCodeBlockTokens.isDiff(isDiffOverride, language: language)
    }

    private var name: String {
        MonarchCodeBlockTokens.name(title: title, language: language, labels: labels)
    }

    /// The content of the view.
    public var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchCodeBlockTokens.cornerRadius, style: .continuous)
        let lines = MonarchCodeBlockLine.lines(
            code: code,
            language: language,
            isDiff: isDiff,
            highlight: isHighlighted
        )
        VStack(alignment: .leading, spacing: 0) {
            if hasHeader {
                header
                Rectangle()
                    .fill(theme.colors[role: MonarchCodeBlockTokens.border])
                    .frame(height: MonarchCodeBlockTokens.borderWidth)
                    .accessibilityHidden(true)
            }
            codeRegion(lines)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors[role: MonarchCodeBlockTokens.background])
        .clipShape(shape)
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchCodeBlockTokens.border],
                lineWidth: MonarchCodeBlockTokens.borderWidth
            )
        )
    }

    private var headerTitle: String? {
        MonarchCodeBlockTokens.headerTitle(title: title, language: language)
    }

    private var hasHeader: Bool {
        headerTitle != nil || showsWrapToggle || isCopyable
    }

    private var header: some View {
        // One row like the web header: the name on the left (it truncates), the actions on the right. Only the
        // accessibility text sizes stack them.
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: MonarchCodeBlockTokens.headerSpacing) {
                    headerText
                    actions
                }
            } else {
                HStack(spacing: MonarchCodeBlockTokens.headerSpacing) {
                    headerText
                    Spacer(minLength: 0)
                    HStack(spacing: 0) { actionButtons }
                        .fixedSize()
                        .layoutPriority(1)
                }
            }
        }
        .padding(.leading, MonarchCodeBlockTokens.headerLeadingPadding)
        .padding(.trailing, MonarchCodeBlockTokens.headerTrailingPadding)
        .padding(.vertical, MonarchCodeBlockTokens.headerVerticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(theme.colors[role: MonarchCodeBlockTokens.headerBackground])
    }

    @ViewBuilder private var headerText: some View {
        if let headerTitle {
            Text(headerTitle)
                .monarchTextStyle(.code)
                .foregroundStyle(theme.colors[role: MonarchCodeBlockTokens.title])
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }

    private var actions: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchCodeBlockTokens.headerSpacing) { actionButtons }
            VStack(alignment: .leading, spacing: MonarchCodeBlockTokens.headerSpacing) { actionButtons }
        }
    }

    @ViewBuilder private var actionButtons: some View {
        if showsWrapToggle {
            Toggle(labels.resolvedWrap, isOn: isWrapped)
                .toggleStyle(.monarchButton(.ghost, size: .sm))
                .fixedSize()
        }
        if isCopyable {
            MonarchCopyButton(
                labels.copy,
                value: code,
                target: name,
                variant: .ghost,
                size: .sm,
                copiedTitle: labels.copied,
                copiedAnnouncement: labels.copiedAnnouncement,
                onCopy: onCopy
            )
            .fixedSize()
        }
    }

    @ViewBuilder private func codeRegion(_ lines: [MonarchCodeBlockLine]) -> some View {
        let content = VStack(alignment: .leading, spacing: 0) {
            ForEach(Array(lines.enumerated()), id: \.offset) { index, line in
                lineView(line, index: index, count: lines.count)
            }
        }
        .padding(.vertical, MonarchCodeBlockTokens.codeVerticalPadding)
        .monarchTextStyle(.code)
        Group {
            if isWrapped.wrappedValue {
                content
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                ScrollView(.horizontal) {
                    content
                        .frame(minWidth: viewportWidth, alignment: .leading)
                }
                .onGeometryChange(for: CGFloat.self, of: { $0.size.width }, action: { viewportWidth = $0 })
            }
        }
        // Code reads left to right in every layout.
        .environment(\.layoutDirection, .leftToRight)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(name)
    }

    private func lineView(_ line: MonarchCodeBlockLine, index: Int, count: Int) -> some View {
        let isWrapped = isWrapped.wrappedValue
        return HStack(alignment: .firstTextBaseline, spacing: 0) {
            if showsLineNumbers {
                Text(MonarchCodeBlockTokens.lineNumber(index, of: count))
                    .foregroundStyle(theme.colors[role: MonarchCodeBlockTokens.number])
                    .padding(.trailing, MonarchCodeBlockTokens.numberSpacing)
                    .accessibilityHidden(true)
            }
            if isDiff {
                Text(MonarchCodeBlockTokens.marker(line.diff))
                    .bold()
                    .foregroundStyle(theme.colors[role: MonarchCodeBlockTokens.markerRole(line.diff)])
                    .accessibilityHidden(true)
            }
            Text(attributed(line))
                .fontWeight(MonarchCodeBlockTokens.isLineEmphasized(line.diff) ? .semibold : nil)
                .foregroundStyle(theme.colors[role: MonarchCodeBlockTokens.lineForeground(line.diff)])
                .fixedSize(horizontal: !isWrapped, vertical: true)
                .frame(maxWidth: isWrapped ? .infinity : nil, alignment: .leading)
        }
        .padding(.horizontal, MonarchCodeBlockTokens.lineHorizontalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(lineBackground(line.diff))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchCodeBlockTokens.spokenLine(line, labels: labels))
        .accessibilityHidden(!MonarchCodeBlockTokens.isSpoken(line))
    }

    @ViewBuilder private func lineBackground(_ diff: MonarchCodeBlockLine.DiffKind?) -> some View {
        if let role = MonarchCodeBlockTokens.lineBackground(diff) {
            Rectangle().fill(theme.colors[role: role])
        }
    }

    /// The tokens of a line as styled text. An empty line keeps its height.
    private func attributed(_ line: MonarchCodeBlockLine) -> AttributedString {
        guard !line.tokens.isEmpty else { return AttributedString(" ") }
        var result = AttributedString()
        for token in line.tokens {
            var part = AttributedString(MonarchCodeBlockTokens.displayText(token.text))
            if let role = MonarchCodeBlockTokens.role(for: token.kind) {
                part.foregroundColor = theme.colors[role: role]
            }
            if MonarchCodeBlockTokens.isItalic(token.kind) {
                part.inlinePresentationIntent = .emphasized
            }
            result += part
        }
        return result
    }
}

#if DEBUG
    #Preview("MonarchCodeBlock") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchCodeBlock(
                    "const answer = 42; // the answer\nexport function greet(name: string) {\n  return `Hi ${name}`;\n}",
                    language: "ts",
                    showsLineNumbers: true
                )
                MonarchCodeBlock(
                    "@@ -1,2 +1,2 @@\n-const port = 3000;\n+const port = 8080;\n export default port;",
                    language: "diff",
                    title: "server.ts"
                )
                MonarchCodeBlock("{\"id\": 1, \"active\": true}", language: "json", isWrapped: .constant(true))
            }
        }
    }
#endif
