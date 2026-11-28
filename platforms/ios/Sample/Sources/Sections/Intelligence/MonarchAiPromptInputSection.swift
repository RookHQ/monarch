import MonarchUI
import SwiftUI

struct MonarchAiPromptInputSection: CataloguePage {
    static let title = "AI prompt input"

    @Environment(\.monarchTheme) private var theme
    @State private var prompt = ""
    @State private var isBusy = false
    @State private var files: [MonarchAiAttachmentTrayFile] = []
    @State private var sent = "Nothing yet"
    @State private var commandPrompt = ""
    @State private var searchesWeb = false

    private let commands = [
        MonarchSlashMenuItem(id: "summarize", label: "summarize", description: "Summarize the conversation"),
        MonarchSlashMenuItem(id: "translate", label: "translate", description: "Translate the last answer"),
    ]

    var body: some View {
        CatalogueCaption("Send, then Stop while the answer runs")
        MonarchAiPromptInput(
            "Message the assistant",
            text: $prompt,
            attachments: files,
            maxLength: 200,
            isBusy: isBusy,
            countTokens: { $0.count / 4 },
            onStop: { isBusy = false },
            onAttach: {
                files.append(MonarchAiAttachmentTrayFile(id: UUID().uuidString, name: "ledger.pdf", size: 182_000))
            },
            onReceiveFiles: { providers in
                for provider in providers {
                    files.append(
                        MonarchAiAttachmentTrayFile(
                            id: UUID().uuidString,
                            name: provider.suggestedName ?? "Pasted image"
                        )
                    )
                }
            },
            onRemoveAttachment: { id in files.removeAll { $0.id == id } },
            onSubmit: { text in
                sent = text.isEmpty ? "\(files.count) files" : text
                files = []
                isBusy = true
            }
        )
        Text("Sent: \(sent)")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Slash commands, a toolbar and a footer")
        MonarchAiPromptInput(
            "Message the assistant",
            text: $commandPrompt,
            triggerMenu: {
                MonarchAiSlashCommands(text: $commandPrompt, commands: commands) { command in
                    "/\(command.label) "
                }
            },
            toolbar: {
                MonarchAiPromptToolbar {
                    Toggle("Search the web", systemImage: "globe", isOn: $searchesWeb)
                }
            },
            footer: {
                Text("Answers can be wrong. Check important facts.")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            },
            onSubmit: { _ in }
        )

        CatalogueCaption("Disabled, with the reason")
        MonarchAiPromptInput(
            "Message the assistant",
            text: .constant(""),
            disabledReason: "Connect a provider to start."
        ) { _ in }
        .disabled(true)
    }
}
