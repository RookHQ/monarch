import MonarchUI
import SwiftUI

/// Recipe: a satisfaction survey that saves the rating at once and the comment separately.
struct CsatSurveySection: CataloguePage {
    static let title = "CSAT survey (recipe)"

    private enum Phase: String, CaseIterable, Identifiable {
        case survey = "Survey"
        case loading = "Loading"
        case failed = "Load failed"
        case locked = "Locked"
        case submitted = "Submitted"
        var id: Self { self }
    }

    private struct SaveFailure: LocalizedError {
        var errorDescription: String? { "That didn’t save. Try again." }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var phase = Phase.survey
    @State private var usesFivePoints = false
    @State private var failsNextSave = false
    @State private var rating: Int?
    @State private var comment = ""
    @State private var status = MonarchSaveStatus.idle
    @State private var savedAt: Date?
    @State private var errorMessage: String?

    var body: some View {
        Picker("State", selection: $phase) {
            ForEach(Phase.allCases) { Text($0.rawValue).tag($0) }
        }
        .pickerStyle(.menu)
        Toggle("Five-point scale", isOn: $usesFivePoints)
            .monarchTextStyle(.body)
        Toggle("Fail the next save", isOn: $failsNextSave)
            .monarchTextStyle(.body)
        Group {
            switch phase {
            case .loading:
                MonarchLoadingState("Loading survey", variant: .spinner)
            case .failed:
                MonarchErrorState(
                    "We couldn’t load this survey",
                    message: "The link has expired. Ask for a new one.",
                    kind: .notFound
                )
            case .submitted:
                MonarchBanner("Thanks for your feedback.", message: "Your answer was recorded.", tone: .success)
            case .survey, .locked:
                survey(isLocked: phase == .locked)
            }
        }
        .padding(theme.spacing.space4)
        .background(theme.colors.bgSurface, in: RoundedRectangle(cornerRadius: theme.radius.lg))
    }

    private func survey(isLocked: Bool) -> some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            Text("Request #1042 · Billing question")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
            Text("How would you rate the support you received?")
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if usesFivePoints {
                MonarchRatingPicker(
                    "Your rating",
                    value: ratingBinding,
                    options: [
                        MonarchRatingOption(1, label: "Very poor"),
                        MonarchRatingOption(2, label: "Poor"),
                        MonarchRatingOption(3, label: "Okay"),
                        MonarchRatingOption(4, label: "Good"),
                        MonarchRatingOption(5, label: "Excellent"),
                    ],
                    isReadOnly: isLocked
                )
            } else {
                HStack(spacing: theme.spacing.space3) {
                    Toggle("Good", systemImage: "hand.thumbsup", isOn: choice(2))
                    Toggle("Bad", systemImage: "hand.thumbsdown", isOn: choice(1))
                }
                .toggleStyle(.monarchButton(.outline, size: .lg))
                .disabled(isLocked)
            }
            if isLocked {
                MonarchInlineNotice("Thanks for your feedback. This answer can no longer be changed.")
            } else if rating == nil {
                Text("Choose an answer above. You can add a comment afterwards.")
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                MonarchFormField("Anything you’d like to add?", helper: "\(500 - comment.count) characters left") {
                    TextField("Comment", text: $comment, axis: .vertical)
                        .lineLimit(3...6)
                        .monarchTextStyle(.body)
                        .textFieldStyle(.roundedBorder)
                        .onChange(of: comment) { _, newValue in
                            if newValue.count > 500 { comment = String(newValue.prefix(500)) }
                        }
                }
                HStack {
                    MonarchButton("Save comment", variant: .secondary, isLoading: status == .saving) { save() }
                        .disabled(comment.isEmpty)
                    MonarchSaveStatusIndicator(status, savedAt: savedAt, errorMessage: errorMessage) { save() }
                }
            }
        }
    }

    private var ratingBinding: Binding<Int?> {
        Binding(
            get: { rating },
            set: { newValue in
                rating = newValue
                save()
            }
        )
    }

    private func choice(_ value: Int) -> Binding<Bool> {
        Binding(
            get: { rating == value },
            set: { isOn in
                rating = isOn ? value : nil
                if isOn { save() }
            }
        )
    }

    private func save() {
        status = .saving
        errorMessage = nil
        Task {
            try? await Task.sleep(for: .milliseconds(600))
            if failsNextSave {
                failsNextSave = false
                status = .error
                errorMessage = SaveFailure().errorDescription
            } else {
                status = .saved
                savedAt = .now
            }
        }
    }
}
