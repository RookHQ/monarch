import MonarchTokens
import SwiftUI

/// The switch of a settings row, drawn as the web draws it: a narrow track with a round thumb, the label at the
/// leading edge and the switch at the trailing edge. VoiceOver is given a standard `Toggle`.
struct MonarchSwitchStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchSwitchBody(configuration: configuration)
    }
}

extension ToggleStyle where Self == MonarchSwitchStyle {
    static var monarchSwitch: MonarchSwitchStyle { MonarchSwitchStyle() }
}

/// Reads the environment, which a `ToggleStyle` value cannot do itself.
private struct MonarchSwitchBody: View {
    let configuration: ToggleStyleConfiguration

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @ScaledMetric(relativeTo: .body) private var trackHeight = MonarchSwitchStyleTokens.trackHeight

    var body: some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            HStack(alignment: .center, spacing: MonarchSwitchStyleTokens.labelSpacing) {
                configuration.label
                    .frame(maxWidth: .infinity, alignment: .leading)
                track
            }
            .frame(minHeight: MonarchAccessibility.minimumHitTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .sensoryFeedback(.selection, trigger: configuration.isOn)
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }

    private var track: some View {
        let thumb = trackHeight - 2 * MonarchSwitchStyleTokens.thumbInset
        let travel = trackHeight * MonarchSwitchStyleTokens.widthRatio - 2 * MonarchSwitchStyleTokens.thumbInset - thumb
        return Capsule()
            .fill(theme.colors[role: MonarchSwitchStyleTokens.track(isOn: configuration.isOn, isEnabled: isEnabled)])
            .frame(width: trackHeight * MonarchSwitchStyleTokens.widthRatio, height: trackHeight)
            .overlay(alignment: .leading) {
                Circle()
                    .fill(theme.colors[role: MonarchSwitchStyleTokens.thumb(isEnabled: isEnabled)])
                    .frame(width: thumb, height: thumb)
                    .offset(x: MonarchSwitchStyleTokens.thumbInset + (configuration.isOn ? travel : 0))
            }
            .monarchAnimation(.fast, value: configuration.isOn)
    }
}

/// Component tokens of the settings switch.
enum MonarchSwitchStyleTokens {
    /// The height of the track before Dynamic Type.
    static let trackHeight: CGFloat = MonarchTokens.Space.space5
    /// The width of the track as a multiple of its height.
    static let widthRatio: CGFloat = 1.75
    /// The gap between the track's edge and the thumb.
    static let thumbInset: CGFloat = MonarchTokens.Space.space0_5
    /// The gap between the label and the track.
    static let labelSpacing: CGFloat = MonarchTokens.Space.space3

    /// The fill of the track: the accent when on, the strong neutral when off, muted when disabled.
    static func track(isOn: Bool, isEnabled: Bool) -> MonarchColorRole {
        guard isEnabled else { return \.bgMuted }
        return isOn ? \.accentSolid : \.bgEmphasis
    }

    /// The fill of the thumb.
    static func thumb(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.accentOnSolid : \.textDisabled
    }
}
