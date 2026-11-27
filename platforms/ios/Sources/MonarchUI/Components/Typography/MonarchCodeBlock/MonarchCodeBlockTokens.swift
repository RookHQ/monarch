import MonarchTokens
import SwiftUI

/// Component tokens and logic for ``MonarchCodeBlock``.
enum MonarchCodeBlockTokens {
    typealias Kind = MonarchCodeBlockTokenizer.Kind
    typealias DiffKind = MonarchCodeBlockLine.DiffKind

    /// The corner radius of the block.
    static let cornerRadius: CGFloat = MonarchTokens.Radius.md
    /// The width of the border and of the line under the header.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// Padding before and after the header title.
    static let headerLeadingPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding after the header actions.
    static let headerTrailingPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the header.
    static let headerVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// The gap between the header title and its actions, and between actions.
    static let headerSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after each line of code.
    static let lineHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// Padding above the first line and below the last.
    static let codeVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// The gap between a line number and the code.
    static let numberSpacing: CGFloat = MonarchTokens.Space.space3
    /// The fill of the block.
    static var background: MonarchColorRole { \.bgSubtle }
    /// The fill of the header.
    static var headerBackground: MonarchColorRole { \.bgMuted }
    /// The border of the block and the line under the header.
    static var border: MonarchColorRole { \.borderSubtle }
    /// The colour of the header title.
    static var title: MonarchColorRole { \.textMuted }
    /// The colour of line numbers.
    static var number: MonarchColorRole { \.textMuted }
    /// How many spaces stand for a tab, as on the web.
    static let tabWidth = 4
    /// The fewest digits a line number column holds, as on the web.
    static let minimumNumberDigits = 3

    private static let languageNames: [String: String] = [
        "js": "JavaScript", "javascript": "JavaScript", "mjs": "JavaScript", "cjs": "JavaScript", "jsx": "JSX",
        "ts": "TypeScript", "typescript": "TypeScript", "tsx": "TSX", "json": "JSON", "jsonc": "JSON",
        "py": "Python", "python": "Python", "sh": "Shell", "bash": "Bash", "zsh": "Shell", "shell": "Shell",
        "console": "Shell", "css": "CSS", "scss": "SCSS", "html": "HTML", "xml": "XML", "svg": "SVG",
        "sql": "SQL", "go": "Go", "golang": "Go", "rust": "Rust", "rs": "Rust", "diff": "Diff", "yaml": "YAML",
        "yml": "YAML", "md": "Markdown", "markdown": "Markdown",
    ]

    /// A display name for a language id ("ts" gives "TypeScript"). Unknown
    /// ids are returned as given; an empty id gives `nil`.
    static func languageName(_ language: String?) -> String? {
        let id = (language ?? "").trimmingCharacters(in: .whitespaces)
        guard !id.isEmpty else { return nil }
        return languageNames[id.lowercased()] ?? id
    }

    /// Whether the block is drawn as a diff: as asked, or else when the
    /// language is "diff" or "patch".
    static func isDiff(_ explicit: Bool?, language: String?) -> Bool {
        if let explicit { return explicit }
        let id = (language ?? "").trimmingCharacters(in: .whitespaces).lowercased()
        return id == "diff" || id == "patch"
    }

    /// The header text: the title, else the language name.
    static func headerTitle(title: String?, language: String?) -> String? {
        if let title, !title.isEmpty { return title }
        return languageName(language)
    }

    /// The name of the code: the spoken name of the region and the target
    /// of the copy button. The title, else "<Language> code", else "Code".
    static func name(title: String?, language: String?, labels: MonarchCodeBlockLabels) -> String {
        if let title, !title.isEmpty { return title }
        return labels.resolvedCodeName(languageName(language))
    }

    /// The colour of a token kind, or `nil` for the line's colour. The
    /// status "strong" roles keep text contrast on the muted fill.
    static func role(for kind: Kind) -> MonarchColorRole? {
        switch kind {
        case .plain: nil
        case .keyword: \.infoStrong
        case .string: \.successStrong
        case .number: \.warningStrong
        case .comment: \.textMuted
        case .tag: \.dangerStrong
        case .attr, .property: \.aiStrong
        }
    }

    /// Whether a token kind is drawn in italics.
    static func isItalic(_ kind: Kind) -> Bool {
        kind == .comment
    }

    /// The colour of a whole line.
    static func lineForeground(_ diff: DiffKind?) -> MonarchColorRole {
        switch diff {
        case .hunk: \.infoStrong
        case .meta: \.textMuted
        default: \.textDefault
        }
    }

    /// The fill behind a line, or `nil` for none.
    static func lineBackground(_ diff: DiffKind?) -> MonarchColorRole? {
        switch diff {
        case .add: \.aiDiffAdd
        case .remove: \.aiDiffRemove
        default: nil
        }
    }

    /// Whether a line is drawn in a heavier weight (file headers).
    static func isLineEmphasized(_ diff: DiffKind?) -> Bool {
        diff == .meta
    }

    /// The visible marker of a diff line, so a change never depends on colour.
    static func marker(_ diff: DiffKind?) -> String {
        switch diff {
        case .add: "+"
        case .remove: "\u{2212}"
        default: " "
        }
    }

    /// The colour of a diff marker.
    static func markerRole(_ diff: DiffKind?) -> MonarchColorRole {
        switch diff {
        case .add: \.aiDiffAddStrong
        case .remove: \.aiDiffRemoveStrong
        default: \.textMuted
        }
    }

    /// A line number padded to the width of the largest, so the column lines
    /// up in a monospaced font. `index` starts at zero.
    static func lineNumber(_ index: Int, of count: Int) -> String {
        let digits = max(minimumNumberDigits, String(max(count, 1)).count)
        let number = String(index + 1)
        return String(repeating: " ", count: max(0, digits - number.count)) + number
    }

    /// Text as it is drawn: tabs become spaces. The copied code keeps its tabs.
    static func displayText(_ text: String) -> String {
        text.replacingOccurrences(of: "\t", with: String(repeating: " ", count: tabWidth))
    }

    /// What VoiceOver reads for a line: its text, after "Added" or "Removed"
    /// for a changed line of a diff. Line numbers are not read.
    static func spokenLine(_ line: MonarchCodeBlockLine, labels: MonarchCodeBlockLabels) -> String {
        switch line.diff {
        case .add: MonarchCodeBlockStrings.diffLine(labels.resolvedAdded, line.text)
        case .remove: MonarchCodeBlockStrings.diffLine(labels.resolvedRemoved, line.text)
        default: line.text
        }
    }

    /// Whether VoiceOver visits a line: a changed line of a diff always, any
    /// other line only when it has more than white space.
    static func isSpoken(_ line: MonarchCodeBlockLine) -> Bool {
        if line.diff == .add || line.diff == .remove { return true }
        return !line.text.allSatisfy(\.isWhitespace)
    }
}
