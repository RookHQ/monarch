import SwiftUI

/// The pressed and disabled look of the notifications button.
struct MonarchNotificationsBellButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchNotificationsBellButtonStyleBody(configuration: configuration)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchNotificationsBellButtonStyleBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchNotificationsBellTokens.appearance(
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        configuration.label
            .monarchSurface(appearance, in: MonarchNotificationsBellTokens.shape)
            .monarchMinimumHitTarget()
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}
