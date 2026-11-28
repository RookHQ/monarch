import MonarchUI
import SwiftUI

struct MonarchDurationInputSection: CataloguePage {
    static let title = "Duration input"

    @State private var estimate: Int? = 90
    @State private var buffer: Int? = 10
    @State private var target: Int? = 1_680
    @State private var logged: Int?

    var body: some View {
        MonarchDurationInput(
            "Estimate",
            minutes: $estimate,
            helper: "Type 1h 30m, 90m, 1.5h or 1:30, then press Return."
        )
        Text("Saved: \(estimate.map { "\($0) minutes" } ?? "none")")
            .monarchTextStyle(.caption)

        MonarchDurationInput(
            "Buffer",
            minutes: $buffer,
            minimum: 5,
            maximum: 120,
            step: 5,
            helper: "5 minutes to 2 hours. Type 3h to see the range message."
        )

        MonarchDurationInput(
            "Response target",
            minutes: $target,
            largestUnit: .days,
            minutesPerDay: 480,
            helper: "Work days of 8 hours: 1d is 480 minutes."
        )

        MonarchDurationInput("Time logged", minutes: $logged, showsStepper: false)

        MonarchDurationInput("Disabled", minutes: .constant(45))
            .disabled(true)
    }
}
