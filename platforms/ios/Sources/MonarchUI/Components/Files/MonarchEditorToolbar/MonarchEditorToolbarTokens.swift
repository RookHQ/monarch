import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchEditorToolbar``.
enum MonarchEditorToolbarTokens {
    /// The gap between tools.
    static let toolSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The space on each side of a group separator.
    static let separatorPadding: CGFloat = MonarchTokens.Space.space1
    /// The height of a group separator, before Dynamic Type.
    static let separatorHeight: CGFloat = MonarchTokens.Control.sm
    /// The SF Symbol of the text style menu.
    static let textStyleSymbol = "textformat.size"
    /// The URL schemes a link may use.
    static let allowedSchemes: Set<String> = ["http", "https", "mailto", "tel"]

    /// How a tool is drawn.
    enum Kind: Equatable {
        /// A button that runs once.
        case button
        /// A toggle button that shows whether the format is on.
        case toggle
        /// An entry of the text style menu.
        case textStyle
    }

    /// How a tool is drawn.
    static func kind(_ tool: MonarchEditorTool) -> Kind {
        switch tool {
        case .undo, .redo, .custom: .button
        case .paragraph, .heading: .textStyle
        default: .toggle
        }
    }

    /// One part of a group: a tool, or the text style menu made of neighbouring text style tools.
    enum Segment: Equatable {
        /// A single tool.
        case tool(MonarchEditorTool)
        /// The text style menu with its entries.
        case textStyle([MonarchEditorTool])
    }

    /// The parts of a group: neighbouring text style tools become one menu.
    static func segments(_ group: [MonarchEditorTool]) -> [Segment] {
        var segments: [Segment] = []
        for tool in group {
            if kind(tool) == .textStyle {
                if case .textStyle(let tools) = segments.last {
                    segments[segments.count - 1] = .textStyle(tools + [tool])
                } else {
                    segments.append(.textStyle([tool]))
                }
            } else {
                segments.append(.tool(tool))
            }
        }
        return segments
    }

    /// The groups without empty ones, so no two separators meet.
    static func nonEmpty(_ groups: [[MonarchEditorTool]]) -> [[MonarchEditorTool]] {
        groups.filter { !$0.isEmpty }
    }

    /// The text style that is on: the first active entry, else body text when offered.
    static func currentTextStyle(_ tools: [MonarchEditorTool], active: Set<MonarchEditorTool>) -> MonarchEditorTool? {
        tools.first { active.contains($0) } ?? (tools.contains(.paragraph) ? .paragraph : nil)
    }

    /// The name of a tool: the app's or the built-in one.
    static func title(_ tool: MonarchEditorTool, labels: MonarchEditorToolbarLabels) -> String {
        labels.tools[tool] ?? MonarchEditorToolbarStrings.title(tool)
    }

    /// The SF Symbol of a tool. Undo and redo use the backward and forward names, so they follow the
    /// layout direction.
    static func symbol(_ tool: MonarchEditorTool) -> String {
        switch tool {
        case .undo: "arrow.uturn.backward"
        case .redo: "arrow.uturn.forward"
        case .paragraph: "text.alignleft"
        case .heading(let level): "\(min(max(level, 1), 6)).square"
        case .bold: "bold"
        case .italic: "italic"
        case .strikethrough: "strikethrough"
        case .code: "chevron.left.forwardslash.chevron.right"
        case .link: "link"
        case .bulletList: "list.bullet"
        case .orderedList: "list.number"
        case .taskList: "checklist"
        case .blockquote: "text.quote"
        case .codeBlock: "curlybraces"
        case .custom(_, _, let systemImage): systemImage
        }
    }

    /// The URL for what was typed in the link form, or `nil` when it is not a
    /// safe link: an http, https, mailto or tel URL, an email address (made a
    /// mailto URL), a relative path, or a domain (made an https URL).
    static func linkURL(from input: String) -> URL? {
        let value = input.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty, !value.contains(where: \.isWhitespace) else { return nil }
        if let colon = value.firstIndex(of: ":"),
            value[..<colon].range(of: "^[A-Za-z][A-Za-z0-9+.-]*$", options: .regularExpression) != nil,
            !value[..<colon].contains(".")
        {
            let scheme = value[..<colon].lowercased()
            return allowedSchemes.contains(scheme) ? URL(string: value) : nil
        }
        if value.range(of: "^[^@/]+@[^@/]+\\.[^@/]+$", options: .regularExpression) != nil {
            return URL(string: "mailto:\(value)")
        }
        if value.hasPrefix("/") || value.hasPrefix("#") || value.hasPrefix("?") || value.hasPrefix("./")
            || value.hasPrefix("../")
        {
            return URL(string: value)
        }
        if value.range(of: "^[^/]+\\.[^/]+", options: .regularExpression) != nil {
            return URL(string: "https://\(value)")
        }
        return nil
    }
}
