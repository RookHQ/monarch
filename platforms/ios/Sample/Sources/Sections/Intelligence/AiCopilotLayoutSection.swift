import MonarchTokens
@_spi(Experimental) import MonarchUI
import SwiftUI

/// A transport that answers every prompt after a short pause and says how
/// many selected rows came with it, so the page shows the selection arriving.
private struct CopilotTransport: MonarchAiTransport {
    func stream(_ request: MonarchAiTransportRequest) -> MonarchAiEventStream {
        let prompt = request.messages.last?.text ?? ""
        let selection: String
        if case .array(let items)? = request.body["selection"],
            case .object(let item)? = items.first,
            case .string(let text)? = item["text"]
        {
            selection = " (selection: \(text))"
        } else {
            selection = " (no selection)"
        }
        return MonarchAiEventStream { continuation in
            let task = Task {
                try? await Task.sleep(for: .milliseconds(500))
                continuation.yield(.textDelta("You asked: \(prompt)\(selection)"))
                continuation.yield(.done())
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }
}

/// Recipe: app content with the assistant docked as an inspector on iPad
/// and opened as a sheet on iPhone; the page's selection goes with each prompt.
struct AiCopilotLayoutSection: CataloguePage {
    static let title = "AiCopilotLayout (recipe)"

    @Environment(\.monarchTheme) private var theme
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    private let rows = ["Northwind renewal", "Harbor expansion", "Summit pilot"]
    private let starters = [
        MonarchAiStarterPrompt(id: "sum", title: "Summarize the selection", prompt: "Summarize the selection."),
        MonarchAiStarterPrompt(id: "risk", title: "Which one is most at risk?", prompt: "Which one is most at risk?"),
    ]

    @State private var conversation = MonarchAiConversation(transport: CopilotTransport())
    @State private var isAssistantOpen = false
    @State private var draft = ""
    @State private var selected: [String] = []

    /// The page chip always shows; the selection chip follows it while rows are selected.
    private var context: [MonarchAiContextItem] {
        var items = [MonarchAiContextItem(id: "page", kind: .page, label: "Opportunities", isRemovable: false)]
        if !selected.isEmpty {
            items.append(
                MonarchAiContextItem(
                    id: "selection",
                    kind: .selection,
                    label: selected.count == 1 ? "1 row" : "\(selected.count) rows",
                    description: selected.joined(separator: "; ")
                )
            )
        }
        return items
    }

    var body: some View {
        CatalogueCaption("Select rows, then open the assistant")
        content
            .safeAreaInset(edge: .bottom, alignment: .trailing) {
                // Hidden while the assistant is docked as an inspector; under a sheet it stays.
                if !(horizontalSizeClass == .regular && isAssistantOpen) {
                    MonarchAiLauncher(isOpen: $isAssistantOpen)
                        .padding(theme.spacing.space4)
                }
            }
            .monarchAiAssistantPanel(isPresented: $isAssistantOpen) {
                MonarchAiAssistantPanel(
                    "Assistant",
                    conversation: conversation,
                    isPresented: $isAssistantOpen,
                    prompt: $draft,
                    context: context,
                    starters: starters,
                    onRemoveContext: { id in if id == "selection" { selected = [] } },
                    headerActions: {},
                    toolbar: {},
                    footer: { MonarchAiDisclaimer() }
                )
            }
            .onChange(of: selected, initial: true) { _, selection in
                conversation.body = Self.requestBody(for: selection)
            }
            .frame(height: MonarchTokens.Space.space24 * 6)
    }

    private var content: some View {
        List {
            Section {
                ForEach(rows, id: \.self) { row in
                    Toggle(row, isOn: isSelected(row))
                        .toggleStyle(.monarchCheckbox)
                }
            } header: {
                Text("Opportunities")
            } footer: {
                Text("Select rows, then ask the assistant about them.")
            }
        }
    }

    private func isSelected(_ row: String) -> Binding<Bool> {
        Binding(
            get: { selected.contains(row) },
            set: { isOn in selected = rows.filter { $0 == row ? isOn : selected.contains($0) } }
        )
    }

    /// The selection as the web's `body.selection`: label and text objects.
    private static func requestBody(for selection: [String]) -> [String: MonarchAiJSON] {
        guard !selection.isEmpty else { return [:] }
        let item: MonarchAiJSON = .object([
            "label": .string("Selected"),
            "text": .string(selection.joined(separator: "; ")),
        ])
        return ["selection": .array([item])]
    }
}
