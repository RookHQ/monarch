import MonarchUI
import SwiftUI

/// Recipe: a review queue for AI outputs as a `NavigationSplitView` (a
/// navigation stack on iPhone), with MonarchRatingPicker per criterion,
/// notes, a segmented Pass / Fail picker and Previous, Next and Submit.
struct AiEvalReviewSection: CataloguePage {
    static let title = "AI eval review (split view recipe)"

    private struct EvalItem: Identifiable {
        let id: String
        let title: String
        let prompt: String
        let output: String
        var reference: String?
    }

    private struct Criterion: Identifiable {
        let id: String
        let label: String
        var detail: String?
        var scale = 5
    }

    @Environment(\.monarchTheme) private var theme
    @State private var isOpen = false
    @State private var scores: [String: [String: Int]] = [:]
    @State private var notes: [String: String] = [:]
    @State private var verdicts: [String: String] = [:]
    @State private var current: String? = "q1"
    @State private var isSubmitted = false

    private let items = [
        EvalItem(
            id: "q1",
            title: "Summarize a meeting",
            prompt: "Summarize the attached meeting notes in three bullet points.",
            output: "- The launch moves to **next Tuesday**.\n- Design owns the final copy review.\n"
                + "- Support needs the FAQ by Friday.",
            reference: "Launch moved to Tuesday; design reviews copy; support needs the FAQ by Friday."
        ),
        EvalItem(
            id: "q2",
            title: "Answer a policy question",
            prompt: "Can I expense a train ticket for a client visit?",
            output: "Yes. Travel for client visits is reimbursable with a receipt, up to the standard fare."
        ),
        EvalItem(
            id: "q3",
            title: "Draft a reply",
            prompt: "Reply to the customer asking for a refund on order 1042.",
            output: "Thanks for reaching out. I have started the refund for order 1042; it reaches you in 5 days."
        ),
    ]

    private let rubric = [
        Criterion(id: "accuracy", label: "Accuracy", detail: "Facts match the source or reference."),
        Criterion(id: "completeness", label: "Completeness", detail: "Covers everything asked."),
        Criterion(id: "tone", label: "Tone", scale: 3),
    ]

    var body: some View {
        CatalogueCaption("The review queue, opened in a sheet")
        Button("Review AI outputs") { isOpen = true }
            .buttonStyle(.monarch(.secondary))
            .sheet(isPresented: $isOpen) { queue }
        Text(isSubmitted ? "Reviews submitted" : "\(verdicts.count) of \(items.count) reviewed")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private var queue: some View {
        NavigationSplitView(
            sidebar: {
                List(selection: $current) {
                    ProgressView(
                        "\(verdicts.count) of \(items.count) reviewed",
                        value: Double(verdicts.count),
                        total: Double(items.count)
                    )
                    .progressViewStyle(.monarchLinear)
                    ForEach(items) { item in
                        VStack(alignment: .leading) {
                            Text(item.title).monarchTextStyle(.body)
                            Text(verdictText(item.id))
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                        .accessibilityElement(children: .combine)
                        .tag(item.id)
                    }
                }
                .navigationTitle("Review AI outputs")
            },
            detail: {
                if let index = items.firstIndex(where: { $0.id == current }) {
                    detail(index)
                } else {
                    MonarchEmptyState("Nothing to review", message: "Outputs to review appear here.")
                }
            }
        )
    }

    private func detail(_ index: Int) -> some View {
        let item = items[index]
        let total = rubric.compactMap { scores[item.id]?[$0.id] }.reduce(0, +)
        let maximum = rubric.map(\.scale).reduce(0, +)
        return ScrollView {
            VStack(alignment: .leading, spacing: theme.spacing.space4) {
                Text("\(index + 1) of \(items.count)")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                heading("Prompt")
                Text(item.prompt).monarchTextStyle(.body)
                HStack(spacing: theme.spacing.space2) {
                    heading("Output")
                    MonarchAiGeneratedBadge(size: .sm)
                }
                MonarchAiMarkdown(item.output)
                if let reference = item.reference {
                    heading("Expected")
                    Text(reference).monarchTextStyle(.body)
                }
                heading("Rubric")
                Text("Score \(total) / \(maximum)").monarchTextStyle(.bodyStrong)
                ForEach(rubric) { criterion in
                    if let detail = criterion.detail {
                        Text(detail).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
                    }
                    MonarchRatingPicker(
                        criterion.label,
                        value: score(item.id, criterion.id),
                        in: 1...criterion.scale,
                        isClearable: true
                    )
                }
                MonarchFormField("Notes") {
                    TextField("Notes", text: note(item.id), axis: .vertical)
                        .lineLimit(3...8)
                }
                heading("Verdict")
                Picker("Verdict", selection: verdict(item.id)) {
                    Text("Pass").tag("pass" as String?)
                    Text("Fail").tag("fail" as String?)
                }
                .pickerStyle(.segmented)
                HStack(spacing: theme.spacing.space2) {
                    MonarchButton("Previous", systemImage: "chevron.backward", size: .sm) {
                        current = items[index - 1].id
                    }
                    .disabled(index == 0)
                    MonarchButton("Next", trailingSystemImage: "chevron.forward", size: .sm) {
                        current = items[index + 1].id
                    }
                    .disabled(index == items.count - 1)
                }
                MonarchButton("Submit reviews", variant: .primary, fullWidth: true) {
                    isSubmitted = true
                    isOpen = false
                }
                .disabled(verdicts.count < items.count)
            }
            .padding(theme.spacing.space4)
        }
        .navigationTitle(item.title)
    }

    private func heading(_ text: String) -> some View {
        Text(text).monarchTextStyle(.subheading).accessibilityAddTraits(.isHeader)
    }

    private func score(_ item: String, _ criterion: String) -> Binding<Int?> {
        Binding(get: { scores[item]?[criterion] }, set: { scores[item, default: [:]][criterion] = $0 })
    }

    private func note(_ item: String) -> Binding<String> {
        Binding(get: { notes[item] ?? "" }, set: { notes[item] = $0 })
    }

    private func verdict(_ item: String) -> Binding<String?> {
        Binding(get: { verdicts[item] }, set: { verdicts[item] = $0 })
    }

    private func verdictText(_ item: String) -> String {
        switch verdicts[item] {
        case "pass": "Pass"
        case "fail": "Fail"
        default: "Not reviewed"
        }
    }
}
