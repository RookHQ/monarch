import MonarchUI
import SwiftUI

struct MonarchMentionPickerSection: CataloguePage {
    static let title = "Mention picker"

    @State private var text = "Thanks @an"
    @State private var query: MonarchMentionQuery?
    @State private var results: [MonarchMentionItem] = []
    @State private var status = MonarchMentionPickerStatus.ready
    @State private var failsNextSearch = false
    @State private var mentioned: [String] = []

    private let directory = [
        MonarchMentionItem(id: "u1", label: "Ana Lopez", detail: "Design"),
        MonarchMentionItem(id: "u2", label: "Andre Kim", detail: "No access to this conversation", isDisabled: true),
        MonarchMentionItem(id: "u3", label: "Ben Okafor", detail: "Engineering"),
        MonarchMentionItem(id: "c1", label: "announcements", kind: .channel, detail: "214 members"),
        MonarchMentionItem(id: "c2", label: "general", kind: .channel, detail: "1,024 members"),
    ]

    var body: some View {
        CatalogueCaption("Type @ or # in the field; suggestions show above it")
        VStack(spacing: 0) {
            MonarchMentionPicker(
                query,
                items: results,
                status: status,
                onRetry: { search() },
                onSelect: { item in
                    guard let query else { return }
                    text = query.replacing(in: text, with: "\(query.trigger)\(item.label)")
                    mentioned.append(item.label)
                    self.query = nil
                }
            )
            TextField("Message", text: $text)
                .textFieldStyle(.roundedBorder)
        }
        .onChange(of: text, initial: true) { _, new in
            query = MonarchMentionQuery.find(in: new, triggers: ["@", "#"])
        }
        .task(id: query) { search() }
        Toggle("Fail the next search", isOn: $failsNextSearch)
        if !mentioned.isEmpty {
            Text("Will notify: \(mentioned.joined(separator: ", "))").monarchTextStyle(.caption)
        }
    }

    private func search() {
        guard let query else { return }
        if failsNextSearch {
            failsNextSearch = false
            status = .failed
            return
        }
        let kind: MonarchMentionKind = query.trigger == "#" ? .channel : .person
        results = directory.filter {
            $0.kind == kind && (query.text.isEmpty || $0.label.localizedCaseInsensitiveContains(query.text))
        }
        status = .ready
    }
}
