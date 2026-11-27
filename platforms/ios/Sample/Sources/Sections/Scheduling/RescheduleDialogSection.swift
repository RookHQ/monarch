import MonarchUI
import SwiftUI

/// Recipe: move a booking to a new time in a sheet.
struct RescheduleDialogSection: CataloguePage {
    static let title = "Reschedule dialog (recipe)"

    private static let current = Date(timeIntervalSince1970: 1_793_786_400)
    private static let day = Date(timeIntervalSince1970: 1_793_836_800)

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @State private var slot: Date?
    @State private var selectedDay: Date? = day
    @State private var isOutsideHours = false
    @State private var typedTime = day.addingTimeInterval(20 * 3_600)
    @State private var reason = ""
    @State private var isSaving = false
    @State private var error: String?

    private var slots: [MonarchSlotPickerSlot] {
        [9, 11, 14].map { MonarchSlotPickerSlot(start: Self.day.addingTimeInterval($0 * 3_600)) }
    }

    private var newTime: Date? { isOutsideHours ? typedTime : slot }

    private func save() {
        guard newTime != nil else { return }
        isSaving = true
        error = nil
        Task {
            // Save the new time; on failure set error and keep every choice.
            isSaving = false
            isPresented = false
        }
    }

    var body: some View {
        MonarchButton("Reschedule") { isPresented = true }
            .sheet(isPresented: $isPresented) {
                NavigationStack {
                    ScrollView {
                        VStack(alignment: .leading, spacing: theme.spacing.space4) {
                            LabeledContent("Consultation with Ana Silva") {
                                Text(Self.current.formatted(date: .abbreviated, time: .shortened))
                            }
                            .labeledContentStyle(.monarch)
                            Toggle("Choose a time outside opening hours", isOn: $isOutsideHours)
                            if isOutsideHours {
                                DatePicker("New time", selection: $typedTime)
                            } else {
                                MonarchSlotPicker(
                                    selection: $slot,
                                    day: $selectedDay,
                                    slots: slots,
                                    timeZone: "Europe/London"
                                )
                            }
                            MonarchTextField(
                                "Reason (optional)",
                                text: $reason,
                                helper: "Kept in the booking's history."
                            )
                            if let error {
                                MonarchInlineErrorRetry(error) { save() }
                            }
                        }
                        .padding(theme.spacing.space4)
                    }
                    .navigationTitle("Reschedule")
                    #if os(iOS)
                        .navigationBarTitleDisplayMode(.inline)
                    #endif
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") { isPresented = false }
                                .disabled(isSaving)
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            if isSaving {
                                ProgressView().accessibilityLabel("Saving")
                            } else {
                                Button("Save") { save() }
                                    .disabled(newTime == nil)
                            }
                        }
                    }
                }
                .presentationDetents([.large])
                .interactiveDismissDisabled(isSaving)
            }
    }
}
