import SwiftUI

/// The Monarch spinner: a ring that is open on one side and turns, for the standard `ProgressView`.
///
/// ```swift
/// ProgressView()
///     .progressViewStyle(.monarchSpinner)
///     .controlSize(.small)
/// ```
///
/// The ring is 2 points wide and takes the colour of the foreground style (the text colour by default).
/// The sizes follow `controlSize`: 14 points for `.small`, 16 for `.regular` and 24 for `.large`, as on the web.
///
/// Accessibility: the progress view keeps the label the app gives it. The turn stops when Reduce Motion is on
/// and the open ring stays.
public struct MonarchSpinnerStyle: ProgressViewStyle {
    /// A spinner style.
    public init() {}

    /// The styled view for a configuration.
    public func makeBody(configuration: Configuration) -> some View {
        MonarchArcSpinner()
    }
}

extension ProgressViewStyle where Self == MonarchSpinnerStyle {
    /// The Monarch spinner: an open ring that turns.
    public static var monarchSpinner: MonarchSpinnerStyle { MonarchSpinnerStyle() }
}

/// The open ring: the shape of the web's spinner, shared by the spinner style and the steps that wait.
struct MonarchArcSpinner: View {
    @Environment(\.controlSize) private var controlSize
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isTurning = false
    private let side: CGFloat?
    private let color: Color?

    init(side: CGFloat? = nil, color: Color? = nil) {
        self.side = side
        self.color = color
    }

    private var resolvedSide: CGFloat {
        if let side { return side }
        switch controlSize {
        case .mini, .small: return 14
        case .large, .extraLarge: return 24
        default: return 16
        }
    }

    var body: some View {
        Circle()
            .trim(from: 0, to: 0.75)
            .stroke(style: StrokeStyle(lineWidth: 2, lineCap: .butt))
            .foregroundStyle(color ?? .primary)
            .rotationEffect(.degrees(isTurning ? 360 : 0))
            .frame(width: resolvedSide, height: resolvedSide)
            .onAppear {
                guard !reduceMotion else { return }
                withAnimation(.linear(duration: 0.7).repeatForever(autoreverses: false)) { isTurning = true }
            }
            .accessibilityHidden(true)
    }
}
