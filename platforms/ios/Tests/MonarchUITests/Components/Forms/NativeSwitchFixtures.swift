import SwiftUI

@testable import MonarchUI

enum SwitchFixtures: ComponentFixtures {
    static let id = "switch"
    static let fixtures = [
        Fixture("on, off and disabled") {
            VStack {
                Toggle("Notifications", isOn: .constant(true))
                Toggle("Marketing email", isOn: .constant(false))
                Toggle("Managed by your admin", isOn: .constant(true)).disabled(true)
            }
            .padding()
        }
    ]
}
