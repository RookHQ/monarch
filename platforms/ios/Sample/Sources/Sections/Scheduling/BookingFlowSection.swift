import MonarchUI
import SwiftUI

/// Recipe: book an appointment step by step.
struct BookingFlowSection: CataloguePage {
    static let title = "Booking flow (recipe)"

    enum BookingStep: Hashable {
        case time, details, review
    }

    private static let start = Date(timeIntervalSince1970: 1_793_750_400)
    private let steps = [
        MonarchStepperStep(id: "service", label: "Service"),
        MonarchStepperStep(id: "time", label: "Time"),
        MonarchStepperStep(id: "details", label: "Details"),
        MonarchStepperStep(id: "review", label: "Review"),
    ]
    private let services = ["Intro call · 15 min", "Consultation · 45 min"]

    @Environment(\.monarchTheme) private var theme
    @State private var path: [BookingStep] = []
    @State private var service: String?
    @State private var slot: Date?
    @State private var day: Date? = start
    @State private var zone: String? = "Europe/London"
    @State private var name = ""
    @State private var email = ""
    @State private var isBooking = false
    @State private var isBooked = false

    private var slots: [MonarchSlotPickerSlot] {
        [9, 10, 13.5, 15].map { MonarchSlotPickerSlot(start: Self.start.addingTimeInterval($0 * 3_600)) }
    }

    private func progress(_ index: Int) -> some View {
        MonarchStepper(steps, current: index, compact: .always)
    }

    private func continueButton(_ next: BookingStep, isEnabled: Bool) -> some View {
        MonarchButton("Continue", variant: .primary, fullWidth: true) { path.append(next) }
            .disabled(!isEnabled)
            .padding(theme.spacing.space4)
    }

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if isBooked {
                    MonarchEmptyState(
                        "You're booked",
                        message: "We sent the details to \(email).",
                        systemImage: "checkmark.circle",
                        kind: .success
                    )
                } else {
                    List {
                        Section { progress(0) }
                        Section("Choose a service") {
                            ForEach(services, id: \.self) { item in
                                Button {
                                    service = item
                                    path.append(.time)
                                } label: {
                                    LabeledContent(item) {
                                        Image(systemName: "chevron.forward").accessibilityHidden(true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Book a session")
            .navigationDestination(for: BookingStep.self) { step in
                switch step {
                case .time:
                    ScrollView {
                        VStack(alignment: .leading, spacing: theme.spacing.space4) {
                            progress(1)
                            MonarchTimezoneSelect("Time zone", selection: $zone)
                            MonarchSlotPicker(selection: $slot, day: $day, slots: slots, timeZone: zone ?? "UTC")
                        }
                        .padding(theme.spacing.space4)
                    }
                    .safeAreaInset(edge: .bottom) { continueButton(.details, isEnabled: slot != nil) }
                    .navigationTitle("Pick a time")
                case .details:
                    ScrollView {
                        VStack(alignment: .leading, spacing: theme.spacing.space4) {
                            progress(2)
                            MonarchTextField("Name", text: $name)
                            MonarchTextField("Email", text: $email, prompt: "name@example.com")
                        }
                        .padding(theme.spacing.space4)
                    }
                    .safeAreaInset(edge: .bottom) {
                        continueButton(.review, isEnabled: !name.isEmpty && email.contains("@"))
                    }
                    .navigationTitle("Your details")
                case .review:
                    List {
                        Section { progress(3) }
                        Section("Summary") {
                            LabeledContent("Service", value: service ?? "")
                            LabeledContent("Time", value: slot?.formatted(date: .abbreviated, time: .shortened) ?? "")
                            LabeledContent("Name", value: name)
                            LabeledContent("Email", value: email)
                        }
                    }
                    .safeAreaInset(edge: .bottom) {
                        MonarchButton(
                            "Book",
                            variant: .primary,
                            isLoading: isBooking,
                            loadingLabel: "Booking…",
                            fullWidth: true
                        ) {
                            isBooking = true
                            Task {
                                // Send the booking; keep the steps if it fails.
                                isBooking = false
                                isBooked = true
                                path = []
                            }
                        }
                        .padding(theme.spacing.space4)
                    }
                    .navigationTitle("Review")
                }
            }
        }
        .frame(minHeight: theme.spacing.space24 * 7)
    }
}
