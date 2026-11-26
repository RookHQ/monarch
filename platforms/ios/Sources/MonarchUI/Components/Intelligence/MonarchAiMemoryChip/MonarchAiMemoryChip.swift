import SwiftUI

/// One thing the assistant remembers, shown as "Remembered: …", with a menu
/// to edit or forget it.
///
/// ```swift
/// MonarchAiMemoryChip(
///     MonarchAiMemoryPart(id: "m1", action: .write, text: "Prefers summaries under 200 words", scope: "Ada"),
///     onEdit: { id, text in memories.update(id, text: text) },
///     onForget: { id in memories.forget(id) }
/// )
/// ```
///
/// It shows a ``MonarchAiMemoryPart`` from the conversation model: the
/// prefix follows the part's action ("Used memory:", "Remembered:",
/// "Forgotten:"). `chip` is an inline pill in the AI colours for a thread;
/// `row` is a full-width row without the prefix for a list of memories.
/// With `onEdit` or `onForget` a menu offers Edit and Forget. Edit opens a
/// dialog with a text field; `onEdit` receives the trimmed text and is never
/// called with empty or unchanged text. Forget runs straight away; pair it
/// with an undo toast if people need a way back. A forgotten memory offers
/// no commands. The app stores and deletes memories.
///
/// Accessibility: the prefix, the text and the scope are one element, which
/// also carries Edit and Forget as actions. The menu button is a separate
/// element named "Options for memory:" and a shortened copy of the text, so
/// each name is unique in a list, with a 44-point touch target. The glyph is
/// decorative.
public struct MonarchAiMemoryChip: View {
    private let memory: MonarchAiMemoryPart
    private let variant: MonarchAiMemoryChipVariant
    private let showsScope: Bool?
    private let labels: MonarchAiMemoryChipLabels
    private let onEdit: (@MainActor (_ id: String, _ text: String) -> Void)?
    private let onForget: (@MainActor (_ id: String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isEditing = false
    @State private var draft = ""

    /// A memory chip.
    ///
    /// - Parameters:
    ///   - memory: The memory, from the conversation model.
    ///   - variant: An inline chip or a full-width row.
    ///   - showsScope: Whether the scope follows the text. By default on the chip and not on the row.
    ///   - labels: Replaces the built-in strings.
    ///   - onEdit: Adds Edit. Receives the id and the new, trimmed text.
    ///   - onForget: Adds Forget. Receives the id; the chip does not ask for confirmation.
    public init(
        _ memory: MonarchAiMemoryPart,
        variant: MonarchAiMemoryChipVariant = .chip,
        showsScope: Bool? = nil,
        labels: MonarchAiMemoryChipLabels = MonarchAiMemoryChipLabels(),
        onEdit: (@MainActor (_ id: String, _ text: String) -> Void)? = nil,
        onForget: (@MainActor (_ id: String) -> Void)? = nil
    ) {
        self.memory = memory
        self.variant = variant
        self.showsScope = showsScope
        self.labels = labels
        self.onEdit = onEdit
        self.onForget = onForget
    }

    private typealias Tokens = MonarchAiMemoryChipTokens

    private var offersCommands: Bool {
        Tokens.offersCommands(action: memory.action, hasEdit: onEdit != nil, hasForget: onForget != nil)
    }

    /// The content of the view.
    public var body: some View {
        let appearance = Tokens.appearance(variant: variant, action: memory.action)
        let shape = Capsule()
        HStack(alignment: .center, spacing: Tokens.spacing) {
            content(appearance)
            if offersCommands {
                commands.foregroundStyle(
                    variant == .row ? theme.colors.textDefault : theme.colors[role: appearance.glyph]
                )
            }
        }
        .padding(.horizontal, variant == .chip ? Tokens.horizontalPadding : 0)
        .padding(.vertical, variant == .chip ? Tokens.verticalPadding : 0)
        .frame(maxWidth: variant == .row ? .infinity : nil, alignment: .leading)
        .background {
            if let background = appearance.background {
                shape.fill(theme.colors[role: background])
            }
        }
        .overlay {
            if let border = appearance.border {
                shape.strokeBorder(theme.colors[role: border], lineWidth: Tokens.borderWidth)
                    .allowsHitTesting(false)
            }
        }
        .alert(labels.editTitle, isPresented: $isEditing) {
            TextField(labels.editField, text: $draft)
            Button(labels.save) { save() }
                .disabled(Tokens.edited(draft, original: memory.text) == nil)
            Button(labels.cancel, role: .cancel) {}
        }
    }

    private func content(_ appearance: Tokens.Appearance) -> some View {
        let prefix = variant == .chip ? labels.prefix(memory.action) : nil
        let scope = Tokens.showsScope(showsScope, variant: variant, scope: memory.scope) ? memory.scope : nil
        return HStack(alignment: .firstTextBaseline, spacing: Tokens.spacing) {
            if variant == .chip {
                MonarchAiBotGlyph(size: .sm)
                    .foregroundStyle(theme.colors[role: appearance.glyph])
            }
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                Group {
                    if let prefix {
                        Text(prefix).foregroundStyle(theme.colors[role: appearance.prefix]).bold()
                            + Text(" " + memory.text).foregroundStyle(theme.colors[role: appearance.text])
                    } else {
                        Text(memory.text).foregroundStyle(theme.colors[role: appearance.text])
                    }
                }
                .monarchTextStyle(variant == .chip ? .label : .body)
                .strikethrough(memory.action == .forget)
                .fixedSize(horizontal: false, vertical: true)
                if let scope {
                    Text(scope)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .frame(maxWidth: variant == .row ? .infinity : nil, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.accessibilityLabel(prefix: prefix, text: memory.text, scope: scope))
        .accessibilityActions {
            if offersCommands, onEdit != nil {
                Button(labels.edit) { startEditing() }
            }
            if offersCommands, let onForget {
                Button(labels.forget) { onForget(memory.id) }
            }
        }
    }

    /// A row shows its commands as two icon buttons, edit and forget, as the web; a chip keeps the menu.
    @ViewBuilder private var commands: some View {
        if variant == .row {
            HStack(spacing: 0) {
                if onEdit != nil {
                    Button {
                        startEditing()
                    } label: {
                        MonarchIcon(systemName: Tokens.editSymbol, label: labels.edit)
                            .monarchMinimumHitTarget()
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                if let onForget {
                    Button {
                        onForget(memory.id)
                    } label: {
                        MonarchIcon(systemName: "xmark", label: labels.forget)
                            .monarchMinimumHitTarget()
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
        } else {
            menu
        }
    }

    private var menu: some View {
        Menu {
            if onEdit != nil {
                Button(labels.edit, systemImage: Tokens.editSymbol) { startEditing() }
            }
            if let onForget {
                Button(labels.forget, systemImage: Tokens.forgetSymbol, role: .destructive) {
                    onForget(memory.id)
                }
            }
        } label: {
            MonarchIcon(systemName: Tokens.menuSymbol, label: labels.options(Tokens.shortText(memory.text)))
                .monarchMinimumHitTarget()
                .contentShape(Rectangle())
        }
        .menuIndicator(.hidden)
        .buttonStyle(.plain)
    }

    private func startEditing() {
        draft = memory.text
        isEditing = true
    }

    private func save() {
        if let text = Tokens.edited(draft, original: memory.text) {
            onEdit?(memory.id, text)
        }
    }
}

#if DEBUG
    #Preview("MonarchAiMemoryChip") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiMemoryChip(
                    MonarchAiMemoryPart(
                        id: "m1",
                        action: .write,
                        text: "Prefers summaries under 200 words",
                        scope: "Ada"
                    ),
                    onEdit: { _, _ in },
                    onForget: { _ in }
                )
                MonarchAiMemoryChip(MonarchAiMemoryPart(id: "m2", action: .read, text: "Works in Lisbon"))
                MonarchAiMemoryChip(MonarchAiMemoryPart(id: "m3", action: .forget, text: "Has a dog called Pixel"))
                MonarchAiMemoryChip(
                    MonarchAiMemoryPart(
                        id: "m4",
                        action: .write,
                        text: "Writes in British English",
                        scope: "Workspace"
                    ),
                    variant: .row,
                    onForget: { _ in }
                )
            }
        }
    }
#endif
