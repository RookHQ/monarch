import SwiftUI

/// Sets the digits of the text in a view to one fixed width, so that
/// columns of figures line up and a value that changes in place does not
/// move the text around it.
///
/// ```swift
/// Text(elapsed, format: .time(pattern: .minuteSecond))
///     .monarchTabularNumber()
/// ```
///
/// The style is the system's fixed-width digits for the font in use; it
/// changes neither the size nor the colour, so add a type role with
/// `monarchTextStyle(_:)`. To format a number that may be missing, use
/// `Text(monarchTabular:format:emptyText:emptyLabel:)`, which applies the
/// same digits.
///
/// Accessibility: nothing changes for VoiceOver; the text is read as before.
public struct MonarchTabularNumberStyle: ViewModifier {
    /// A tabular number style.
    public init() {}

    /// The content with fixed-width digits.
    public func body(content: Content) -> some View {
        content.monospacedDigit()
    }
}

extension View {
    /// Sets the digits of the text in this view to one fixed width.
    ///
    /// Use it on text that is already formatted and on text that counts,
    /// such as a timer. See ``MonarchTabularNumberStyle``.
    public func monarchTabularNumber() -> some View {
        modifier(MonarchTabularNumberStyle())
    }
}

#if DEBUG
    #Preview("MonarchTabularNumberStyle") {
        MonarchPreviewMatrix {
            VStack(alignment: .trailing) {
                Text(monarchTabular: 1_111.11, format: .number.precision(.fractionLength(2)))
                Text(monarchTabular: 9_048.5, format: .number.precision(.fractionLength(2)))
                Text(monarchTabular: Double?.none, format: .number)
                Text(monarchTabular: 0.125, format: .percent)
                Text("11:11").monarchTabularNumber()
            }
            .monarchTextStyle(.body)
        }
    }
#endif
