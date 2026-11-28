import MonarchUI
import SwiftUI

/// Recipe: a multi-step message sequence with waits, channels, variables and a sample preview.
struct SequenceBuilderSection: CataloguePage {
    static let title = "Sequence builder (recipe)"

    struct Step: Identifiable, Equatable {
        let id: UUID
        var channel: String
        var waitMinutes: Int?
        var subject: String
        var body: String
    }

    @Environment(\.monarchTheme) private var theme
    @State private var steps = [
        Step(
            id: UUID(),
            channel: "email",
            waitMinutes: 0,
            subject: "Welcome, {{firstName}}",
            body: "Hi {{firstName}}, thanks for trying {{product}}."
        ),
        Step(
            id: UUID(),
            channel: "sms",
            waitMinutes: 2_880,
            subject: "",
            body: "Need a hand with {{product}}? Reply to this message."
        ),
    ]

    private let channels = ["email": "Email", "sms": "SMS"]
    private let variables = [("firstName", "First name", "Alex"), ("product", "Product", "Rook CRM")]

    var body: some View {
        ForEach(Array(steps.enumerated()), id: \.element.id) { index, step in
            stepCard(index: index, step: step)
        }
        HStack {
            MonarchButton("Add email step", systemImage: "plus", size: .sm) { add("email") }
            MonarchButton("Add SMS step", systemImage: "plus", size: .sm) { add("sms") }
        }
        .disabled(steps.count >= 20)
    }

    private func stepCard(index: Int, step: Step) -> some View {
        let binding = $steps[index]
        let channel = channels[step.channel] ?? step.channel
        return MonarchCard {
            VStack(alignment: .leading, spacing: theme.spacing.space3) {
                HStack {
                    Text("Step \(index + 1), \(channel)")
                        .monarchTextStyle(.subheading)
                        .accessibilityAddTraits(.isHeader)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Menu {
                        Button("Move up", systemImage: "arrow.up") { move(index, by: -1) }.disabled(index == 0)
                        Button("Move down", systemImage: "arrow.down") { move(index, by: 1) }
                            .disabled(index == steps.count - 1)
                    } label: {
                        Label("Reorder step \(index + 1)", systemImage: "arrow.up.arrow.down").labelStyle(.iconOnly)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    Button {
                        steps.remove(at: index)
                    } label: {
                        Label("Remove step \(index + 1)", systemImage: "xmark").labelStyle(.iconOnly)
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                }
                MonarchDurationInput(
                    "Wait before sending",
                    minutes: binding.waitMinutes,
                    helper: (step.waitMinutes ?? 0) == 0 ? "Sent straight away" : "Sent after the previous step"
                )
                if step.channel == "email" {
                    MonarchTextField(
                        "Subject",
                        text: binding.subject,
                        error: step.subject.trimmingCharacters(in: .whitespaces).isEmpty ? "Write a subject line." : nil
                    )
                }
                MonarchFormField("Message", error: step.body.isEmpty ? "Write the message." : nil) {
                    TextField("Message", text: binding.body, axis: .vertical)
                        .lineLimit(3...8)
                        .monarchTextStyle(.body)
                }
                Menu("Insert a variable", systemImage: "curlybraces") {
                    ForEach(variables, id: \.0) { variable in
                        Button("Insert \(variable.1)") { steps[index].body += "{{\(variable.0)}}" }
                    }
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
                DisclosureGroup("Preview with sample data") {
                    VStack(alignment: .leading, spacing: theme.spacing.space1) {
                        if step.channel == "email" {
                            Text(render(step.subject)).monarchTextStyle(.bodyStrong)
                        }
                        Text(render(step.body)).monarchTextStyle(.body)
                    }
                }
                .disclosureGroupStyle(.monarch)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Step \(index + 1), \(channel)")
        .accessibilityActions {
            if index > 0 { Button("Move up") { move(index, by: -1) } }
            if index < steps.count - 1 { Button("Move down") { move(index, by: 1) } }
        }
    }

    private func render(_ template: String) -> String {
        variables.reduce(template) { text, variable in
            text.replacingOccurrences(of: "{{\(variable.0)}}", with: variable.2)
        }
    }

    private func add(_ channel: String) {
        steps.append(Step(id: UUID(), channel: channel, waitMinutes: 1_440, subject: "", body: ""))
    }

    private func move(_ index: Int, by offset: Int) {
        let target = index + offset
        guard steps.indices.contains(target) else { return }
        steps.swapAt(index, target)
        AccessibilityNotification.Announcement("Moved to position \(target + 1).").post()
    }
}
