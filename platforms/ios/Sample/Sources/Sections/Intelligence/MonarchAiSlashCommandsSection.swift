import MonarchUI
import SwiftUI

struct MonarchAiSlashCommandsSection: CataloguePage {
    static let title = "AI slash commands"

    @Environment(\.monarchTheme) private var theme
    @State private var prompt = "/"
    @State private var ran = "Nothing yet"

    private let commands = [
        MonarchSlashMenuItem(
            id: "summarize",
            label: "summarize",
            description: "Summarize the open document",
            systemImage: "list.bullet",
            keywords: ["recap", "shorten"]
        ),
        MonarchSlashMenuItem(
            id: "rewrite",
            label: "rewrite",
            description: "Rewrite the selection in a different tone",
            systemImage: "pencil"
        ),
        MonarchSlashMenuItem(
            id: "search",
            label: "search",
            description: "Search connected documents",
            systemImage: "magnifyingglass"
        ),
        MonarchSlashMenuItem(
            id: "schedule",
            label: "schedule",
            description: "Needs a calendar connection",
            systemImage: "calendar",
            isDisabled: true
        ),
    ]

    var body: some View {
        CatalogueCaption("Type / in the prompt")
        VStack(spacing: 0) {
            MonarchAiSlashCommands(text: $prompt, commands: commands) { command in
                if command.id == "search" {
                    ran = "Ran /search at once"
                    return nil
                }
                return "/\(command.label) "
            }
            TextField("Message the assistant", text: $prompt, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .monarchTextStyle(.body)
        }
        Text(ran)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
