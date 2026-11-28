import SwiftUI

/// The look of one button inside an attached ``MonarchButtonGroup``.
///
/// It draws only the label. The fill, the border and the lines between the
/// buttons are drawn by the group, which is the only place that knows where
/// every button is; the button reports its visible frame and its fill.
struct MonarchButtonGroupSegmentStyle: ButtonStyle {
    let variant: MonarchButtonVariant
    let size: MonarchButtonSize
    let axis: Axis
    let fullWidth: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchButtonGroupSegmentBody(
            configuration: configuration,
            variant: variant,
            size: size,
            axis: axis,
            fullWidth: fullWidth
        )
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchButtonGroupSegmentBody: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchButtonVariant
    let size: MonarchButtonSize
    let axis: Axis
    let fullWidth: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchButtonAppearance.resolve(
            variant: variant,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let foreground = theme.colors[role: appearance.foreground]
        // In a column every button is as wide as the widest one.
        let stretches = fullWidth || axis == .vertical

        configuration.label
            .monarchTextStyle(MonarchButtonTokens.textStyle(size), weight: MonarchButtonTokens.fontWeight)
            .multilineTextAlignment(.center)
            .foregroundStyle(foreground)
            .tint(foreground)
            .padding(.horizontal, MonarchButtonTokens.horizontalPadding(size))
            .padding(.vertical, MonarchButtonTokens.verticalPadding)
            .frame(
                minWidth: MonarchButtonGroupTokens.minSegmentWidth,
                maxWidth: stretches ? .infinity : nil,
                minHeight: MonarchButtonGroupTokens.minSegmentHeight(size, axis: axis)
            )
            .anchorPreference(key: MonarchButtonGroupSegmentsKey.self, value: .bounds) { bounds in
                [MonarchButtonGroupSegment(bounds: bounds, fill: appearance.background)]
            }
            .monarchMinimumHitTarget()
    }
}
