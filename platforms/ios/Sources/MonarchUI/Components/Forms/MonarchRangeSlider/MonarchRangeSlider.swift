import MonarchTokens
import SwiftUI

/// A track with two thumbs that chooses a lower and an upper value: a price
/// filter, an age band, working hours.
///
/// SwiftUI's `Slider` has one thumb; this control draws two and gives each
/// one the behaviour of a slider for VoiceOver.
///
/// ```swift
/// @State private var price: ClosedRange<Double> = 20...80
///
/// MonarchFormField("Price") {
///     MonarchRangeSlider("Price", value: $price, in: 0...200, step: 5, showsValue: true)
/// }
/// ```
///
/// The thumbs can meet but never cross; `minimumStepsBetween` keeps a gap.
/// Touching the track moves the nearer thumb there and dragging continues
/// until the finger lifts. `onEditingChanged` reports the start and end of a
/// drag, like `Slider`; save the value when it reports `false`.
///
/// Accessibility: each thumb is its own adjustable element, named by the
/// label and "Minimum" or "Maximum" ("Price, Minimum"), with the formatted
/// value as its value. Swiping up or down moves it by one step and stops at
/// the other thumb. The lower thumb is read first in both directions. The
/// shown range is hidden from VoiceOver because the thumbs say the same. The
/// touch area is 44 points high.
public struct MonarchRangeSlider: View {
    private let label: String
    @Binding private var value: ClosedRange<Double>
    private let rules: MonarchRangeSliderModel.Rules
    private let size: MonarchRangeSliderSize
    private let isInvalid: Bool
    private let labels: MonarchRangeSliderLabels
    private let valueText: ((Double) -> String)?
    private let showsValue: Bool
    private let onEditingChanged: (Bool) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.layoutDirection) private var layoutDirection
    @State private var activeThumb: MonarchRangeSliderModel.Thumb?

    /// A range slider.
    ///
    /// - Parameters:
    ///   - label: What the range is, already localised. Spoken with each thumb; not drawn.
    ///   - value: The lower and upper values.
    ///   - bounds: The lowest and highest values.
    ///   - step: The distance between allowed values. 0 allows any value.
    ///   - minimumStepsBetween: The smallest gap between the thumbs, in steps.
    ///   - size: The size of the track and thumbs.
    ///   - isInvalid: Draws the range in the danger colour.
    ///   - labels: Replacements for the built-in strings.
    ///   - valueText: Formats a value for VoiceOver and the shown range. The number in the current locale by default.
    ///   - showsValue: Shows the formatted range above the track.
    ///   - onEditingChanged: Runs with `true` when a drag starts and `false` when it ends.
    public init(
        _ label: String,
        value: Binding<ClosedRange<Double>>,
        in bounds: ClosedRange<Double> = 0...100,
        step: Double = 1,
        minimumStepsBetween: Int = 0,
        size: MonarchRangeSliderSize = .md,
        isInvalid: Bool = false,
        labels: MonarchRangeSliderLabels = MonarchRangeSliderLabels(),
        valueText: ((Double) -> String)? = nil,
        showsValue: Bool = false,
        onEditingChanged: @escaping (Bool) -> Void = { _ in }
    ) {
        self.label = label
        _value = value
        self.rules = MonarchRangeSliderModel.Rules(bounds: bounds, step: step, minimumStepsBetween: minimumStepsBetween)
        self.size = size
        self.isInvalid = isInvalid
        self.labels = labels
        self.valueText = valueText
        self.showsValue = showsValue
        self.onEditingChanged = onEditingChanged
    }

    private var current: ClosedRange<Double> { MonarchRangeSliderModel.normalized(value, rules: rules) }

    private func text(_ number: Double) -> String {
        valueText?(number) ?? MonarchRangeSliderTokens.defaultValueText(number)
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchRangeSliderTokens.appearance(isInvalid: isInvalid, isEnabled: isEnabled)
        HStack(alignment: .center, spacing: MonarchTokens.Space.space3) {
            track(appearance)
            if showsValue {
                Text(
                    MonarchRangeSliderTokens.rangeText(
                        lower: text(current.lowerBound),
                        upper: text(current.upperBound),
                        labels: labels
                    )
                )
                .monarchTextStyle(.bodySmall)
                .monospacedDigit()
                .foregroundStyle(theme.colors[role: appearance.value])
                .fixedSize()
                .accessibilityHidden(true)
            }
        }
        .accessibilityElement(children: .contain)
    }

    private func track(_ appearance: MonarchRangeSliderTokens.Appearance) -> some View {
        let diameter = MonarchRangeSliderTokens.thumbDiameter(size)
        let trackHeight = MonarchRangeSliderTokens.trackHeight(size)
        let isRightToLeft = layoutDirection == .rightToLeft
        return GeometryReader { geometry in
            let width = geometry.size.width
            let middle = geometry.size.height / 2
            let low = position(current.lowerBound, width: width, diameter: diameter, isRightToLeft: isRightToLeft)
            let high = position(current.upperBound, width: width, diameter: diameter, isRightToLeft: isRightToLeft)
            ZStack {
                Capsule()
                    .fill(theme.colors[role: appearance.track])
                    .frame(width: width, height: trackHeight)
                    .position(x: width / 2, y: middle)
                Capsule()
                    .fill(theme.colors[role: appearance.range])
                    .frame(width: abs(high - low), height: trackHeight)
                    .position(x: (low + high) / 2, y: middle)
                thumb(.upper, appearance: appearance, diameter: diameter)
                    .position(x: high, y: middle)
                thumb(.lower, appearance: appearance, diameter: diameter)
                    .position(x: low, y: middle)
            }
            .contentShape(Rectangle())
            .gesture(drag(width: width, diameter: diameter, isRightToLeft: isRightToLeft))
        }
        // Positions are computed in reading order by the model, so the drawing
        // itself is kept left to right and is not mirrored a second time.
        .environment(\.layoutDirection, .leftToRight)
        .frame(height: MonarchRangeSliderTokens.hitTarget)
        .frame(maxWidth: .infinity)
    }

    private func position(_ number: Double, width: CGFloat, diameter: CGFloat, isRightToLeft: Bool) -> CGFloat {
        MonarchRangeSliderModel.position(
            ofFraction: MonarchRangeSliderModel.fraction(of: number, rules: rules),
            width: width,
            thumbDiameter: diameter,
            isRightToLeft: isRightToLeft
        )
    }

    private func thumb(
        _ thumb: MonarchRangeSliderModel.Thumb,
        appearance: MonarchRangeSliderTokens.Appearance,
        diameter: CGFloat
    ) -> some View {
        let number = thumb == .lower ? current.lowerBound : current.upperBound
        return Circle()
            .fill(theme.colors[role: appearance.thumbFill])
            .overlay(
                Circle().strokeBorder(
                    theme.colors[role: appearance.thumbBorder],
                    lineWidth: MonarchRangeSliderTokens.thumbBorderWidth
                )
            )
            .frame(width: diameter, height: diameter)
            .frame(width: MonarchRangeSliderTokens.hitTarget, height: MonarchRangeSliderTokens.hitTarget)
            .contentShape(Rectangle())
            .accessibilityElement()
            .accessibilityLabel(MonarchRangeSliderTokens.thumbLabel(thumb, label: label, labels: labels))
            .accessibilityValue(text(number))
            .accessibilitySortPriority(thumb == .lower ? 2 : 1)
            .accessibilityAdjustableAction { direction in
                guard isEnabled else { return }
                switch direction {
                case .increment:
                    value = MonarchRangeSliderModel.adjusting(thumb, increment: true, in: current, rules: rules)
                case .decrement:
                    value = MonarchRangeSliderModel.adjusting(thumb, increment: false, in: current, rules: rules)
                @unknown default:
                    break
                }
            }
    }

    private func drag(width: CGFloat, diameter: CGFloat, isRightToLeft: Bool) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { gesture in
                guard isEnabled else { return }
                let fraction = MonarchRangeSliderModel.fraction(
                    atX: gesture.location.x,
                    width: width,
                    thumbDiameter: diameter,
                    isRightToLeft: isRightToLeft
                )
                let raw = MonarchRangeSliderModel.value(atFraction: fraction, rules: rules)
                let thumb = activeThumb ?? MonarchRangeSliderModel.nearestThumb(to: raw, in: current, rules: rules)
                if activeThumb == nil {
                    activeThumb = thumb
                    onEditingChanged(true)
                }
                let next = MonarchRangeSliderModel.moving(thumb, to: raw, in: current, rules: rules)
                if next != value { value = next }
            }
            .onEnded { _ in
                guard activeThumb != nil else { return }
                activeThumb = nil
                onEditingChanged(false)
            }
    }
}

#if DEBUG
    #Preview("MonarchRangeSlider") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchRangeSlider("Price", value: .constant(20...80), showsValue: true)
                MonarchRangeSlider("Hours", value: .constant(9...17), in: 0...24, size: .sm)
                MonarchRangeSlider("Budget", value: .constant(50...50), isInvalid: true)
                MonarchRangeSlider("Disabled", value: .constant(10...40)).disabled(true)
            }
        }
    }
#endif
