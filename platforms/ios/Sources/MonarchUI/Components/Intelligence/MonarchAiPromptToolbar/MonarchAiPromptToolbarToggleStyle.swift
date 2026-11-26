import SwiftUI

/// The style ``MonarchAiPromptToolbar`` gives every `Toggle` inside it: a
/// compact button that takes the assistant fill and border when on.
///
/// Internal: apps get it by placing a `Toggle` in the toolbar. A candidate
/// for an `ai` case of `MonarchToggleButtonVariant` at merge.
struct MonarchAiPromptToolbarToggleStyle: ToggleStyle {
    func makeBody(configuration: Configuration) -> some View {
        Button {
            configuration.isOn.toggle()
        } label: {
            configuration.label
        }
        .buttonStyle(MonarchAiPromptToolbarToggleSurfaceStyle(isOn: configuration.isOn))
        .accessibilityRepresentation {
            Toggle(isOn: configuration.$isOn) { configuration.label }
        }
    }
}

private struct MonarchAiPromptToolbarToggleSurfaceStyle: ButtonStyle {
    let isOn: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchAiPromptToolbarToggleSurface(configuration: configuration, isOn: isOn)
    }
}

private struct MonarchAiPromptToolbarToggleSurface: View {
    let configuration: ButtonStyleConfiguration
    let isOn: Bool

    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.accessibilityDifferentiateWithoutColor) private var differentiateWithoutColor

    var body: some View {
        let size = MonarchAiPromptToolbarTokens.controlSize
        let appearance = MonarchAiPromptToolbarTokens.toggleAppearance(
            isOn: isOn,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        HStack(spacing: MonarchButtonTokens.iconSpacing) {
            if MonarchAiPromptToolbarTokens.showsCheckmark(
                isOn: isOn,
                differentiateWithoutColor: differentiateWithoutColor
            ) {
                MonarchIcon(systemName: MonarchAiPromptToolbarTokens.checkedSymbol, size: .sm)
            }
            configuration.label
        }
        .monarchTextStyle(MonarchButtonTokens.textStyle(size), weight: MonarchButtonTokens.fontWeight)
        .multilineTextAlignment(.center)
        .padding(.horizontal, MonarchButtonTokens.horizontalPadding(size))
        .padding(.vertical, MonarchButtonTokens.verticalPadding)
        .frame(minHeight: MonarchButtonTokens.minHeight(size))
        .monarchSurface(appearance, in: MonarchShapes.control)
        .monarchMinimumHitTarget()
        .monarchAnimation(.fast, value: isOn)
        .monarchAnimation(.fast, value: configuration.isPressed)
    }
}
