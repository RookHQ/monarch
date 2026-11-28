import SwiftUI

/// The push-to-talk button: listens while touched and held.
///
/// It looks like the outline toggle button. A system `Button` cannot report
/// touch down and touch up separately, so this is a styled label with a
/// press gesture; for VoiceOver, Switch Control and Full Keyboard Access,
/// which cannot hold, its default action starts and stops instead.
struct MonarchAiVoiceInputHoldButton: View {
    let title: String
    let systemImage: String
    let hint: String
    let size: MonarchButtonSize
    let isActive: Bool
    let onPress: @MainActor () -> Void
    let onRelease: @MainActor () -> Void
    let onToggle: @MainActor () -> Void

    @State private var isPressed = false
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchToggleButtonTokens.appearance(
            variant: .outline,
            isOn: isActive,
            isPressed: isPressed,
            isEnabled: isEnabled
        )
        Label(title, systemImage: systemImage)
            .monarchTextStyle(MonarchButtonTokens.textStyle(size), weight: MonarchButtonTokens.fontWeight)
            .padding(.horizontal, MonarchButtonTokens.horizontalPadding(size))
            .padding(.vertical, MonarchButtonTokens.verticalPadding)
            .frame(minHeight: MonarchButtonTokens.minHeight(size))
            .monarchSurface(appearance, in: MonarchShapes.control)
            .monarchMinimumHitTarget()
            .monarchAnimation(.fast, value: isActive)
            .monarchAnimation(.fast, value: isPressed)
            .onLongPressGesture(
                minimumDuration: 0,
                maximumDistance: .infinity,
                perform: {},
                onPressingChanged: { pressing in
                    guard isEnabled else { return }
                    isPressed = pressing
                    if pressing { onPress() } else { onRelease() }
                }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(title)
            .accessibilityHint(hint)
            .accessibilityAddTraits(isActive ? [.isButton, .isSelected] : .isButton)
            .accessibilityAction { onToggle() }
    }
}
