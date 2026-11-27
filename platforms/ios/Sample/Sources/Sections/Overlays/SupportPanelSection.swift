import MonarchUI
import SwiftUI

/// Recipe: help and support in a sheet, with a Chat tab and a Help tab of
/// articles, contact options and the status page.
struct SupportPanelSection: CataloguePage {
    static let title = "Support panel (recipe)"

    /// One message of the support conversation.
    struct SupportMessage: Identifiable {
        let id: Int
        let author: String
        let text: String
        let time: Date
    }

    /// The tabs of the panel.
    enum Tab: Hashable { case chat, help }

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @State private var tab = Tab.chat
    @State private var draft = ""
    @State private var query = ""
    @State private var opened = "Nothing opened"
    @State private var messages = [
        SupportMessage(
            id: 1,
            author: "Rook Support",
            text: "Hi! How can we help today?",
            time: Date().addingTimeInterval(-300)
        )
    ]

    private let articles = [
        "Reset your password",
        "Invite people to your workspace",
        "Export your data",
    ]

    var body: some View {
        CatalogueCaption("Opened in a sheet from a Help button")
        MonarchButton("Help & support", systemImage: "questionmark.circle") { isPresented = true }
            .sheet(isPresented: $isPresented) {
                panel
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
        Text(opened)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private var panel: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Section", selection: $tab) {
                    Text("Chat").tag(Tab.chat)
                    Text("Help").tag(Tab.help)
                }
                .pickerStyle(.segmented)
                .padding(theme.spacing.space4)
                if tab == .chat { chat } else { help }
            }
            .navigationTitle("Help & support")
            #if os(iOS)
                .navigationBarTitleDisplayMode(.inline)
            #endif
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { isPresented = false }
                }
            }
        }
    }

    private var chat: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: theme.spacing.space3) {
                ForEach(messages) { message in
                    MonarchMessageRow(MonarchMessageRowAuthor(message.author), time: message.time) {
                        Text(message.text)
                    }
                }
            }
            .padding(.horizontal, theme.spacing.space4)
        }
        .safeAreaInset(edge: .bottom) {
            MonarchMessageComposer("Message support", text: $draft, showsEmojiButton: false) { message in
                messages.append(
                    SupportMessage(id: messages.count + 1, author: "You", text: message.text, time: Date())
                )
                draft = ""
            }
            .padding(theme.spacing.space4)
        }
    }

    private var help: some View {
        List {
            Section("Popular articles") {
                ForEach(articles.filter { query.isEmpty || $0.localizedCaseInsensitiveContains(query) }, id: \.self) {
                    article in
                    Button(article) { opened = "Opened “\(article)”" }
                        .monarchTextStyle(.body)
                }
            }
            Section("Contact us") {
                if let email = URL(string: "mailto:support@example.com") {
                    Link(destination: email) {
                        Label("support@example.com", systemImage: "envelope")
                    }
                }
                LabeledContent("Phone", value: "Mon–Fri, 9–6")
            }
            Section("Status") {
                HStack {
                    MonarchStatusDot("All systems normal", tone: .success)
                    Spacer()
                    if let status = URL(string: "https://status.example.com") {
                        Link("System status", destination: status)
                            .buttonStyle(.monarchLink(.standalone, isExternal: true))
                    }
                }
                if let center = URL(string: "https://help.example.com") {
                    Link("Help center", destination: center)
                        .buttonStyle(.monarchLink(.standalone, isExternal: true))
                }
            }
        }
        .searchable(text: $query, prompt: "Search help")
    }
}
