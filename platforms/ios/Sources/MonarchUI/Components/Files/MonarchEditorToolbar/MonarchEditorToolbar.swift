import MonarchTokens
import SwiftUI

/// A formatting toolbar for a text editor: undo and redo, a text style menu,
/// inline styles, lists, quotes, code and links, plus product controls.
///
/// ```swift
/// TextEditor(text: $text)
///     .toolbar {
///         ToolbarItemGroup(placement: .keyboard) {
///             MonarchEditorToolbar(
///                 active: editor.activeFormats,
///                 disabled: editor.canUndo ? [] : [.undo],
///                 onLink: { url in editor.setLink(url) },
///                 action: { tool in editor.apply(tool) }
///             )
///         }
///     }
/// ```
///
/// The toolbar draws the controls and reports what was pressed; the app's
/// editor applies it and passes back which formats are on at the caret in
/// `active`. Place it above the keyboard with
/// `ToolbarItemGroup(placement: .keyboard)`, or above the editor. It scrolls
/// sideways when the controls do not fit.
///
/// With `onLink`, the link control opens a form for the address and returns
/// a safe URL (http, https, mailto, tel, a relative path, or a domain made
/// https), or `nil` to remove the link. Without it, the link control is
/// passed to `action` like the others.
///
/// Accessibility: the toolbar is a container named "Formatting". Inline
/// styles, lists and blocks are standard toggles with the Monarch toggle
/// button style, so VoiceOver reads their name and on or off state, and the
/// on state is also a filled surface. Undo, redo and product controls are
/// buttons. Body text and headings are a standard menu with a picker, whose
/// value is the current style. Every control has a 44-point touch target and
/// an icon with a spoken name.
public struct MonarchEditorToolbar: View {
    private let groups: [[MonarchEditorTool]]
    private let active: Set<MonarchEditorTool>
    private let disabled: Set<MonarchEditorTool>
    private let labels: MonarchEditorToolbarLabels
    private let onLink: (@MainActor (URL?) -> Void)?
    private let action: @MainActor (MonarchEditorTool) -> Void

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var separatorHeight = MonarchEditorToolbarTokens.separatorHeight
    @State private var isEditingLink = false
    @State private var linkText = ""

    /// An editor toolbar.
    ///
    /// - Parameters:
    ///   - groups: The tools in groups, separated by dividers. Neighbouring text style tools become one menu.
    ///   - active: The formats that are on at the caret or selection, and the current text style.
    ///   - disabled: The tools that cannot be used now, such as undo with nothing to undo.
    ///   - labels: Replacements for the built-in strings.
    ///   - onLink: Handles links with the built-in form: a URL to apply, or `nil` to remove the link.
    ///   - action: Runs when a tool is pressed or a text style is chosen.
    public init(
        groups: [[MonarchEditorTool]] = MonarchEditorTool.defaultGroups,
        active: Set<MonarchEditorTool> = [],
        disabled: Set<MonarchEditorTool> = [],
        labels: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
        onLink: (@MainActor (URL?) -> Void)? = nil,
        action: @escaping @MainActor (MonarchEditorTool) -> Void
    ) {
        self.groups = groups
        self.active = active
        self.disabled = disabled
        self.labels = labels
        self.onLink = onLink
        self.action = action
    }

    /// The content of the view.
    public var body: some View {
        let groups = MonarchEditorToolbarTokens.nonEmpty(groups)
        // One row, as on the web. When the tools are wider than the width the row wraps onto a second line, as the
        // web's does, so no tool is cut off; each tool keeps its 44 point touch area. A group ends with a separator.
        ViewThatFits(in: .horizontal) {
            toolRow(groups)
            wrappedRows(groups)
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.toolbar ?? MonarchEditorToolbarStrings.toolbar)
        .alert(labels.linkTitle ?? MonarchEditorToolbarStrings.linkTitle, isPresented: $isEditingLink) {
            linkAlert
        } message: {
            Text(labels.linkHelp ?? MonarchEditorToolbarStrings.linkHelp)
        }
    }

    private func toolRow(_ groups: [[MonarchEditorTool]]) -> some View {
        HStack(spacing: MonarchEditorToolbarTokens.toolSpacing) {
            ForEach(Array(groups.enumerated()), id: \.offset) { index, group in
                HStack(spacing: MonarchEditorToolbarTokens.toolSpacing) {
                    ForEach(Array(MonarchEditorToolbarTokens.segments(group).enumerated()), id: \.offset) {
                        _,
                        segment in
                        switch segment {
                        case .tool(let tool): control(tool)
                        case .textStyle(let tools):
                            ForEach(tools, id: \.self) { tool in control(tool) }
                        }
                    }
                    if index < groups.count - 1 {
                        Divider()
                            .frame(height: separatorHeight)
                            .overlay(theme.colors.borderSubtle)
                            .padding(.horizontal, MonarchEditorToolbarTokens.separatorPadding)
                            .accessibilityHidden(true)
                    }
                }
            }
        }
    }

    private func wrappedRows(_ groups: [[MonarchEditorTool]]) -> some View {
        MonarchFlowLayout(spacing: MonarchEditorToolbarTokens.toolSpacing, alignment: .center) {
            ForEach(Array(groups.enumerated()), id: \.offset) { index, group in
                ForEach(group, id: \.self) { tool in control(tool) }
                if index < groups.count - 1 {
                    Rectangle()
                        .fill(theme.colors.borderSubtle)
                        .frame(width: MonarchTokens.Space.px, height: separatorHeight)
                        .padding(.horizontal, MonarchEditorToolbarTokens.separatorPadding)
                        .accessibilityHidden(true)
                }
            }
        }
    }

    @ViewBuilder private var linkAlert: some View {
        TextField(labels.linkAddress ?? MonarchEditorToolbarStrings.linkAddress, text: $linkText)
            .autocorrectionDisabled()
            #if os(iOS)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
            #endif
        Button(labels.linkApply ?? MonarchEditorToolbarStrings.linkApply) {
            onLink?(MonarchEditorToolbarTokens.linkURL(from: linkText))
        }
        .disabled(MonarchEditorToolbarTokens.linkURL(from: linkText) == nil)
        if active.contains(.link) {
            Button(labels.linkRemove ?? MonarchEditorToolbarStrings.linkRemove, role: .destructive) {
                onLink?(nil)
            }
        }
        Button(labels.linkCancel ?? MonarchEditorToolbarStrings.linkCancel, role: .cancel) {}
    }

    @ViewBuilder private func control(_ tool: MonarchEditorTool) -> some View {
        let title = MonarchEditorToolbarTokens.title(tool, labels: labels)
        let symbol = MonarchEditorToolbarTokens.symbol(tool)
        switch MonarchEditorToolbarTokens.kind(tool) {
        case .button:
            Button {
                action(tool)
            } label: {
                toolLabel(tool, title: title, symbol: symbol)
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .disabled(disabled.contains(tool))
        case .toggle, .textStyle:
            Toggle(
                isOn: Binding(
                    get: { active.contains(tool) },
                    set: { _ in press(tool) }
                )
            ) {
                toolLabel(tool, title: title, symbol: symbol)
            }
            .toggleStyle(.monarchButton(.ghost, size: .sm))
            .disabled(disabled.contains(tool))
        }
    }

    /// The icon of a tool. The web draws the text styles as letters: T for the paragraph, H1 to H3 for headings.
    @ViewBuilder private func toolLabel(_ tool: MonarchEditorTool, title: String, symbol: String) -> some View {
        switch tool {
        case .paragraph:
            Label {
                Text(title)
            } icon: {
                Text(verbatim: "T").font(.system(size: 15, weight: .semibold))
            }
            .labelStyle(.iconOnly)
        case .heading(let level):
            Label {
                Text(title)
            } icon: {
                Text(verbatim: "H\(min(max(level, 1), 6))").font(.system(size: 13, weight: .semibold))
            }
            .labelStyle(.iconOnly)
        default:
            Label(title, systemImage: symbol).labelStyle(.iconOnly)
        }
    }

    private func press(_ tool: MonarchEditorTool) {
        if tool == .link, onLink != nil {
            linkText = ""
            isEditingLink = true
        } else {
            action(tool)
        }
    }
}

#if DEBUG
    #Preview("MonarchEditorToolbar") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchEditorToolbar(active: [.bold, .heading(2)], disabled: [.redo]) { _ in }
                MonarchEditorToolbar(groups: MonarchEditorTool.inlineGroups, active: [.italic]) { _ in }
            }
        }
    }
#endif
