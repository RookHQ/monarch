import MonarchUI
import SwiftUI

/// Native adoption: the standard `Toggle`, tinted by the theme.
struct SwitchSection: CataloguePage {
    static let title = "Switch (standard Toggle)"

    @State private var notifications = true

    var body: some View {
        Toggle("Notifications", isOn: $notifications)
            .monarchTextStyle(.body)
        Toggle("Disabled", isOn: .constant(true))
            .monarchTextStyle(.body)
            .disabled(true)
    }
}
