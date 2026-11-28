import SwiftUI

/// One step of the list: the marker and, beside it, the name, the
/// description and the optional note. A step that can be selected is a
/// button.
struct MonarchStepperItem: View {
    let step: MonarchStepperStep
    let index: Int
    let total: Int
    let status: MonarchStepperStatus
    let isSelectable: Bool
    let isUnavailable: Bool
    let isBusy: Bool
    let labels: MonarchStepperLabels
    let onSelect: (@MainActor (MonarchStepperStep, Int) -> Void)?
    /// The name under the marker, for a row that is too narrow for names beside their markers.
    var isStacked = false

    /// This step with its name under the marker.
    func stacked() -> MonarchStepperItem {
        var copy = self
        copy.isStacked = true
        return copy
    }

    @Environment(\.monarchTheme) private var theme
    @FocusState private var isFocused: Bool

    var body: some View {
        let label = MonarchStepperTokens.stepLabel(position: index + 1, total: total, label: step.label, labels: labels)
        let value = MonarchStepperTokens.stepValue(status: status, step: step, isBusy: isBusy, labels: labels)
        if isSelectable, let onSelect {
            Button {
                onSelect(step, index)
            } label: {
                content
            }
            .buttonStyle(MonarchStepperItemButtonStyle())
            .focused($isFocused)
            .monarchFocusRing(isFocused, in: MonarchShapes.control)
            .accessibilityLabel(label)
            .accessibilityValue(value)
        } else {
            content
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityValue(value)
        }
    }

    /// Every step has the height of a touch target and is aligned to its
    /// top, so that markers line up whether or not a step is a button.
    private var content: some View {
        let layout =
            isStacked
            ? AnyLayout(VStackLayout(alignment: .center, spacing: MonarchStepperTokens.textSpacing))
            : AnyLayout(HStackLayout(alignment: .top, spacing: MonarchStepperTokens.itemSpacing))
        return layout {
            MonarchStepperMarker(status: status, position: index + 1, isBusy: isBusy)
            VStack(alignment: isStacked ? .center : .leading, spacing: MonarchStepperTokens.textSpacing) {
                Text(step.label)
                    .monarchTextStyle(
                        MonarchStepperTokens.labelStyle,
                        weight: MonarchStepperTokens.labelWeight(for: status)
                    )
                    .foregroundStyle(
                        theme.colors[role: MonarchStepperTokens.labelColor(for: status, isUnavailable: isUnavailable)]
                    )
                if let description = step.description {
                    Text(description)
                        .monarchTextStyle(MonarchStepperTokens.detailStyle)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if step.isOptional {
                    Text(labels.optional ?? MonarchStepperStrings.optional)
                        .monarchTextStyle(MonarchStepperTokens.detailStyle)
                        .italic()
                        // A custom family has no italic face, so the slant is drawn.
                        .transformEffect(
                            theme.typography.face(for: .regular) == nil
                                ? .identity : CGAffineTransform(a: 1, b: 0, c: -0.2, d: 1, tx: 1.2, ty: 0)
                        )
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .multilineTextAlignment(isStacked ? .center : .leading)
            .fixedSize(horizontal: isStacked, vertical: false)
            .padding(.top, isStacked ? 0 : MonarchStepperTokens.textSpacing)
        }
        .padding(MonarchStepperTokens.itemPadding)
        .frame(
            minWidth: MonarchAccessibility.minimumHitTarget,
            minHeight: MonarchAccessibility.minimumHitTarget,
            alignment: .topLeading
        )
        .contentShape(Rectangle())
    }
}

/// Draws a muted surface behind a step while it is pressed.
private struct MonarchStepperItemButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        MonarchStepperItemButtonBody(configuration: configuration)
    }
}

/// Reads the theme, which a `ButtonStyle` value cannot do itself.
private struct MonarchStepperItemButtonBody: View {
    let configuration: ButtonStyleConfiguration

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        configuration.label
            .background {
                if configuration.isPressed {
                    MonarchShapes.control.fill(theme.colors.bgMuted)
                }
            }
    }
}
