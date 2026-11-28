import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A transport that echoes the prompt and says how many tools the unsaved agent has.
private struct BuilderTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        let prompt = request.messages.last?.text ?? ""
        return MonarchAiEventStream { continuation in
            let task = Task {
                try? await Task.sleep(for: .milliseconds(500))
                continuation.yield(.textDelta("The agent received: \(prompt)"))
                continuation.yield(.done())
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

/// A send the conversation refused, so the prompt input gives the text back.
private struct NotSent: Error {}

/// Recipe: configure an agent in a Form and test it; a segmented control on
/// iPhone, side by side on iPad.
struct AiAgentBuilderSection: CataloguePage {
    static let title = "AiAgentBuilder (recipe)"

    private enum Pane: Hashable { case configure, test }

    @Environment(\.monarchTheme) private var theme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private let maxInstructions = 8000
    private let sources = [
        MonarchAiKnowledgeSource(
            id: "handbook",
            label: "Team handbook",
            kind: .space,
            children: [MonarchAiKnowledgeSource(id: "onboarding", label: "Onboarding guide", kind: .document)]
        ),
        MonarchAiKnowledgeSource(id: "tickets", label: "Support tickets", kind: .app),
    ]

    @State private var name = "Onboarding guide"
    @State private var role = "Helps new teammates find answers"
    @State private var instructions = "Answer from the team handbook. Keep answers short and name the source."
    @State private var knowledge = ["handbook"]
    @State private var connectors = [
        MonarchAiConnector(
            id: "docs",
            name: "Documents",
            status: .connected,
            isEnabled: true,
            tools: [
                MonarchAiConnectorTool(id: "search", name: "Search documents", risk: .read, permission: .allow),
                MonarchAiConnectorTool(id: "tasks", name: "Create tasks", risk: .write, permission: .ask),
            ]
        )
    ]
    @State private var triedSave = false
    @State private var saved = false
    @State private var pane = Pane.configure
    @State private var showsTools = false
    @State private var prompt = ""
    @State private var conversation = MonarchAiConversation(transport: BuilderTransport())

    private var agent: MonarchAiAgentRef {
        MonarchAiAgentRef(name: name.isEmpty ? "New agent" : name, role: role.isEmpty ? nil : role)
    }

    private var nameError: String? {
        triedSave && name.trimmingCharacters(in: .whitespaces).isEmpty ? "Give the agent a name." : nil
    }

    private var toolIDs: [String] {
        connectors.filter(\.isEnabled).flatMap { connector in
            connector.tools.filter { $0.permission != .never }.map(\.id)
        }
    }

    private var definition: MonarchAiJSON {
        .object([
            "name": .string(name),
            "role": .string(role),
            "instructions": .string(instructions),
            "tools": .array(toolIDs.map { .string($0) }),
            "knowledge": .array(knowledge.map { .string($0) }),
        ])
    }

    var body: some View {
        CatalogueCaption("Configure and Test; side by side on iPad")
        NavigationStack {
            Group {
                if horizontalSizeClass == .regular {
                    HStack(spacing: 0) {
                        form
                        Divider()
                        test
                    }
                } else {
                    VStack(spacing: 0) {
                        Picker("View", selection: $pane) {
                            Text("Configure").tag(Pane.configure)
                            Text("Test").tag(Pane.test)
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                        .padding(.horizontal, theme.spacing.space4)
                        .padding(.vertical, theme.spacing.space2)
                        if pane == .configure { form } else { test }
                    }
                }
            }
            .navigationTitle(agent.name)
            .navigationBarTitleDisplayMode(.inline)
        }
        .frame(height: MonarchTokens.Space.space24 * 6)
    }

    private var form: some View {
        Form {
            Section("Persona") {
                MonarchAiAgentAvatar(agent, size: .lg, isDecorative: true)
                MonarchTextField("Name", text: $name, error: nameError)
                MonarchTextField(
                    "Role",
                    text: $role,
                    helper: "One line shown under the name, such as Support assistant."
                )
            }
            Section {
                TextField("Instructions", text: $instructions, axis: .vertical)
                    .lineLimit(4...12)
                    .monarchTextStyle(.body)
            } header: {
                Text("Instructions")
            } footer: {
                Text("\(instructions.count) / \(maxInstructions) characters")
            }
            Section("Tools") {
                Button {
                    showsTools = true
                } label: {
                    LabeledContent("Tools", value: "\(toolIDs.count) on")
                }
            }
            Section("Knowledge") {
                MonarchAiKnowledgeScope(sources, selection: $knowledge, startsExpanded: ["handbook"])
            }
            Section {
                Button("Save agent") {
                    let isNamed = !name.trimmingCharacters(in: .whitespaces).isEmpty
                    triedSave = true
                    saved = isNamed
                    if isNamed { AccessibilityNotification.Announcement("Agent saved.").post() }
                }
            } footer: {
                if saved { Text("Agent saved.") }
            }
        }
        .onChange(of: instructions) { _, text in
            if text.count > maxInstructions { instructions = String(text.prefix(maxInstructions)) }
        }
        .onChange(of: definition) { saved = false }
        .sheet(isPresented: $showsTools) {
            MonarchAiConnectorPicker("Tools", connectors: $connectors, isSearchable: false)
        }
    }

    private var test: some View {
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: theme.spacing.space2) {
                HStack(spacing: theme.spacing.space2) {
                    MonarchAiAgentAvatar(
                        agent,
                        state: conversation.isBusy ? .working : .idle,
                        size: .sm,
                        isDecorative: true
                    )
                    Text("Test")
                        .monarchTextStyle(.subheading)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button("Restart test") {
                        conversation.stop()
                        conversation.reset()
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .disabled(conversation.messages.isEmpty)
                }
                Text("Try the agent with the current settings. Nothing here is saved.")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            .padding(theme.spacing.space4)
            MonarchAiThread(
                conversation.messages,
                status: conversation.status,
                error: conversation.error,
                showsDaySeparators: false,
                onStop: { conversation.stop() },
                empty: {
                    Text("Send a message to try the agent.")
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                }
            )
            .safeAreaInset(edge: .bottom) {
                MonarchAiPromptInput(
                    "Message the agent",
                    text: $prompt,
                    isBusy: conversation.isBusy,
                    onStop: { conversation.stop() },
                    onSubmit: { text in
                        guard conversation.send(text, body: ["agent": definition]) else { throw NotSent() }
                    }
                )
                .padding(theme.spacing.space4)
            }
        }
    }
}
