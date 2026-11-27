import SwiftUI

/// The round, icon-only look of one call control, for a `Button` or the
/// trigger of a `Menu`. The label should be a `Label` with the icon-only
/// label style, so VoiceOver reads its title.
struct MonarchCallControlButtonStyle: ButtonStyle {
    let tone: MonarchCallControlsTokens.Tone
    var isWide = false

    func makeBody(configuration: Configuration) -> some View {
        MonarchCallControlSurface(configuration: configuration, tone: tone, isWide: isWide)
    }
}

/// The toggles of the call controls: a round button whose tone follows the
/// state, given to VoiceOver as a standard `Toggle`.
struct MonarchCallControlToggleStyle: ToggleStyle {
    let control: MonarchCallControlsTokens.Control

    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            configuration.label
        }
        .buttonStyle(
            MonarchCallControlButtonStyle(tone: MonarchCallControlsTokens.tone(control, isOn: configuration.isOn))
        )
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}

/// Reads the environment, which a style value cannot do itself.
private struct MonarchCallControlSurface: View {
    let configuration: ButtonStyleConfiguration
    let tone: MonarchCallControlsTokens.Tone
    let isWide: Bool

    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric private var diameter: CGFloat = MonarchCallControlsTokens.diameter

    var body: some View {
        let appearance = MonarchCallControlsTokens.appearance(
            tone: tone,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        configuration.label
            .labelStyle(.iconOnly)
            .monarchTextStyle(.heading)
            .padding(.horizontal, isWide ? MonarchCallControlsTokens.wideHorizontalPadding : 0)
            .frame(minWidth: diameter, minHeight: diameter)
            .monarchSurface(appearance, in: Capsule())
            .contentShape(Capsule())
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}
