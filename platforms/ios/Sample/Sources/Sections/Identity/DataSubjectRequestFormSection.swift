import MonarchUI
import SwiftUI

/// Recipe: a privacy request (access, deletion, correction) from the person the data is about.
struct DataSubjectRequestFormSection: CataloguePage {
    static let title = "Data subject request form (recipe)"

    @Environment(\.monarchTheme) private var theme
    @State private var type: String?
    @State private var isAgent = false
    @State private var name = ""
    @State private var email = ""
    @State private var agentName = ""
    @State private var country: String?
    @State private var details = ""
    @State private var isAccurate = false
    @State private var showsErrors = false
    @State private var isSending = false
    @State private var reference: String?

    private var emailError: String? {
        guard showsErrors else { return nil }
        if email.isEmpty { return "Enter an email address." }
        return email.contains("@") ? nil : "Enter an email address like name@example.com."
    }

    private var isComplete: Bool {
        type != nil && emailError == nil && !email.isEmpty && country != nil && isAccurate
            && (!isAgent || !agentName.isEmpty) && (type != "correct" || !details.isEmpty)
    }

    var body: some View {
        if let reference {
            MonarchBanner(
                "Request received",
                message: "Your reference is \(reference). Keep it in case you need to ask about the request.",
                tone: .success
            )
        } else {
            form
        }
    }

    private var form: some View {
        VStack(alignment: .leading, spacing: theme.spacing.space4) {
            MonarchRadioCardGroup(
                "What would you like to do?",
                selection: $type,
                options: [
                    MonarchRadioCardOption(
                        "access",
                        title: "Get a copy of my data",
                        description: "Receive a copy of the personal information held about you."
                    ),
                    MonarchRadioCardOption(
                        "delete",
                        title: "Delete my data",
                        description: "Ask for your personal information to be deleted."
                    ),
                    MonarchRadioCardOption(
                        "correct",
                        title: "Correct my data",
                        description: "Fix personal information that is wrong or incomplete."
                    ),
                ]
            )
            if showsErrors, type == nil {
                Label("Choose what you would like to do.", systemImage: "exclamationmark.circle")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.dangerStrong)
            }
            MonarchFormField("Who is this request for?") {
                Picker("Who is this request for?", selection: $isAgent) {
                    Text("Myself").tag(false)
                    Text("Someone I act for").tag(true)
                }
                .pickerStyle(.segmented)
            }
            MonarchTextField(isAgent ? "Their full name" : "Full name", text: $name)
            MonarchTextField(
                isAgent ? "Their email" : "Email",
                text: $email,
                helper: "Used to identify the account and to reply about this request.",
                error: emailError
            )
            if isAgent {
                MonarchTextField(
                    "Your full name",
                    text: $agentName,
                    helper: "You may be asked for proof that you can act for this person.",
                    error: showsErrors && agentName.isEmpty ? "Enter your full name." : nil
                )
            }
            MonarchCountrySelect(
                "Country or region where the person lives",
                selection: $country,
                error: showsErrors && country == nil ? "Choose a country or region." : nil,
                isRequired: true
            )
            MonarchFormField(
                "Details",
                helper: "Anything that helps find the information or explains the request.",
                error: showsErrors && type == "correct" && details.isEmpty
                    ? "Describe what the request is about." : nil,
                isRequired: type == "correct"
            ) {
                TextField("Details", text: $details, axis: .vertical).lineLimit(3...6)
            }
            Toggle("The information I gave is accurate", isOn: $isAccurate)
                .toggleStyle(.monarchCheckbox)
            MonarchButton("Send request", variant: .primary, isLoading: isSending, fullWidth: true) { submit() }
        }
    }

    private func submit() {
        showsErrors = true
        guard isComplete else {
            AccessibilityNotification.Announcement("Some answers need attention.").post()
            return
        }
        isSending = true
        Task {
            // The app sends the request to its privacy service here.
            try? await Task.sleep(for: .milliseconds(600))
            isSending = false
            reference = "DSR-1042"
        }
    }
}
