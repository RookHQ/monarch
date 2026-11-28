import Foundation

/// Pure logic of ``MonarchAiSlashCommands``.
enum MonarchAiSlashCommandsTokens {
    /// Where the trigger may be typed in a prompt: at the start of any word.
    static let position: MonarchSlashMenuTriggerPosition = .wordStart

    /// A command name without leading triggers: "/summarize" and "summarize" both give "summarize".
    static func bare(_ name: String, trigger: Character) -> String {
        String(name.drop { $0 == trigger })
    }

    /// The command being typed at the end of `text`, or `nil`.
    static func query(in text: String, trigger: Character) -> MonarchSlashMenuQuery? {
        MonarchSlashMenuQuery.find(in: text, trigger: trigger, position: position)
    }

    /// The commands that match the query, best first, filtered by their bare
    /// names, then shown with the trigger before the name ("/summarize").
    static func shown(_ commands: [MonarchSlashMenuItem], query: String, trigger: Character) -> [MonarchSlashMenuItem] {
        let bareCommands = commands.map { relabeled($0, label: bare($0.label, trigger: trigger)) }
        return MonarchSlashMenuTokens.filter(bareCommands, query: query).map {
            relabeled($0, label: "\(trigger)\($0.label)")
        }
    }

    /// The labels the slash menu uses: the app's, else the built-in command
    /// strings. The empty text always names what was typed.
    static func menuLabels(_ labels: MonarchSlashMenuLabels, typed: String) -> MonarchSlashMenuLabels {
        let empty = labels.empty?(typed) ?? MonarchAiSlashCommandsStrings.empty(typed)
        return MonarchSlashMenuLabels(
            label: labels.label ?? MonarchAiSlashCommandsStrings.label,
            empty: { _ in empty },
            count: labels.count ?? { MonarchAiSlashCommandsStrings.count($0) }
        )
    }

    /// The text after a command is chosen: the typed trigger and query are
    /// replaced by what `onSelect` returned, or removed when it returned `nil`.
    static func text(after query: MonarchSlashMenuQuery, in text: String, replacement: String?) -> String {
        query.replacing(in: text, with: replacement ?? "")
    }

    private static func relabeled(_ item: MonarchSlashMenuItem, label: String) -> MonarchSlashMenuItem {
        MonarchSlashMenuItem(
            id: item.id,
            label: label,
            description: item.description,
            systemImage: item.systemImage,
            group: item.group,
            keywords: item.keywords,
            isDisabled: item.isDisabled
        )
    }
}
