import SwiftUI

/// The `/` command menu of an assistant prompt: type a slash and a list of
/// commands such as /summarize or /translate appears above the prompt,
/// filtered as you type.
///
/// ```swift
/// .safeAreaInset(edge: .bottom) {
///     VStack(spacing: 0) {
///         MonarchAiSlashCommands(text: $prompt, commands: commands) { command in
///             "/\(command.label) "
///         }
///         MonarchMessageComposer("Message the assistant", text: $prompt) { send($0) }
///     }
/// }
/// ```
///
/// It is the slash command preset of ``MonarchSlashMenu`` for prompts: it
/// reads the command being typed from `text` itself (a slash at the start of
/// any word, followed by letters, digits or hyphens), filters `commands` by
/// name (names that start with the query first, then names that contain it,
/// then descriptions and keywords), and shows each name with the slash. A
/// command's `label` is its name, with or without the slash. When a command
/// is chosen, `onSelect` returns the text that replaces the typed `/query`,
/// such as `"/summarize "`; return `nil` to remove it and run the command at
/// once. Disabled commands are shown but cannot be chosen. It draws nothing
/// while no command is being typed. Put it directly above the prompt, in the
/// bottom safe-area inset, where it stays above the keyboard.
///
/// Accessibility: the list is a container named "Commands"; each command is
/// a button read as its name and description. The number of commands, or
/// "No commands match “/xyz”", is announced as it changes, so focus stays in
/// the prompt. Rows are at least 44 points high.
public struct MonarchAiSlashCommands: View {
    @Binding private var text: String
    private let commands: [MonarchSlashMenuItem]
    private let trigger: Character
    private let groupOrder: [String]
    private let labels: MonarchSlashMenuLabels
    private let onSelect: @MainActor (MonarchSlashMenuItem) -> String?

    /// A slash command menu for a prompt.
    ///
    /// - Parameters:
    ///   - text: The prompt's text. The menu reads the command being typed from it and writes the result back.
    ///   - commands: Every command. `label` is the name typed after the slash.
    ///   - trigger: The character that opens the menu.
    ///   - groupOrder: Group headings in display order, for commands with a `group`.
    ///   - labels: Replacements for the list name, the empty text and the count.
    ///   - onSelect: Receives the chosen command and returns the text that
    ///     replaces the typed command, or `nil` to remove it.
    public init(
        text: Binding<String>,
        commands: [MonarchSlashMenuItem],
        trigger: Character = "/",
        groupOrder: [String] = [],
        labels: MonarchSlashMenuLabels = MonarchSlashMenuLabels(),
        onSelect: @escaping @MainActor (MonarchSlashMenuItem) -> String?
    ) {
        _text = text
        self.commands = commands
        self.trigger = trigger
        self.groupOrder = groupOrder
        self.labels = labels
        self.onSelect = onSelect
    }

    private typealias Tokens = MonarchAiSlashCommandsTokens

    /// The content of the view.
    public var body: some View {
        let query = Tokens.query(in: text, trigger: trigger)
        let shown = query.map { Tokens.shown(commands, query: $0.text, trigger: trigger) } ?? []
        // The commands are already filtered and labelled, so the menu gets an
        // empty query that keeps them in this order.
        MonarchSlashMenu(
            query.map { MonarchSlashMenuQuery(trigger: $0.trigger, text: "", range: $0.range) },
            items: shown,
            groupOrder: groupOrder,
            labels: Tokens.menuLabels(labels, typed: query.map { "\($0.trigger)\($0.text)" } ?? "")
        ) { item in
            guard let query, let command = commands.first(where: { $0.id == item.id }) else { return }
            text = Tokens.text(after: query, in: text, replacement: onSelect(command))
        }
        .environment(\.monarchAiTone, true)
    }
}

#if DEBUG
    #Preview("MonarchAiSlashCommands") {
        let commands = [
            MonarchSlashMenuItem(
                id: "summarize",
                label: "summarize",
                description: "Summarize the open document",
                systemImage: "list.bullet",
                keywords: ["recap"]
            ),
            MonarchSlashMenuItem(id: "rewrite", label: "rewrite", description: "Rewrite in another tone"),
            MonarchSlashMenuItem(id: "schedule", label: "schedule", description: "Needs a calendar", isDisabled: true),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchAiSlashCommands(text: .constant("/"), commands: commands) { "/\($0.label) " }
                MonarchAiSlashCommands(text: .constant("Hi /zz"), commands: commands) { _ in nil }
            }
        }
    }
#endif
