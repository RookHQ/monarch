import SwiftUI

/// A small coloured dot followed by a text label, such as "Running".
///
/// ```swift
/// MonarchStatusDot("Running", tone: .success)
/// MonarchStatusDot("Deploying", tone: .info, isPulsing: true)
/// ```
///
/// The label is required, so the colour is never the only signal. With
/// `isLabelHidden` the label is not drawn but is still read by VoiceOver;
/// show a legend near it, because sighted people then see only the colour.
///
/// Accessibility: the dot and the label are one element, read as the label.
/// The dot and the pulse are decorative. The pulse does not run when Reduce
/// Motion is on. The dot and the text scale with Dynamic Type.
public struct MonarchStatusDot: View {
    private let label: String
    private let tone: MonarchTone
    private let size: MonarchStatusDotSize
    private let isPulsing: Bool
    private let isLabelHidden: Bool

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var dotSize: CGFloat

    /// A status dot.
    ///
    /// - Parameters:
    ///   - label: The words for the status, already localised.
    ///   - tone: The meaning, which selects the colour of the dot.
    ///   - size: The size of the dot and the label.
    ///   - isPulsing: Whether a ring pulses around the dot, for something
    ///     that is live or in progress.
    ///   - isLabelHidden: Whether the label is only read by VoiceOver and
    ///     not drawn.
    public init(
        _ label: String,
        tone: MonarchTone = .neutral,
        size: MonarchStatusDotSize = .sm,
        isPulsing: Bool = false,
        isLabelHidden: Bool = false
    ) {
        self.label = label
        self.tone = tone
        self.size = size
        self.isPulsing = isPulsing
        self.isLabelHidden = isLabelHidden
        _dotSize = ScaledMetric(wrappedValue: MonarchStatusDotTokens.dotSize(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let style = MonarchStatusDotTokens.textStyle(size)
        HStack(alignment: .firstTextBaseline, spacing: MonarchStatusDotTokens.spacing(size)) {
            // The hidden text gives the dot the height and baseline of one
            // line of the label, so the dot sits on the first line.
            Text(MonarchStatusDotTokens.lineBox)
                .monarchTextStyle(style, weight: MonarchStatusDotTokens.fontWeight)
                .hidden()
                .frame(width: dotSize)
                .overlay {
                    MonarchStatusDotMark(
                        color: theme.colors[role: MonarchStatusDotTokens.dot(tone: tone)],
                        size: dotSize,
                        isPulsing: isPulsing
                    )
                }
            if !isLabelHidden {
                Text(label)
                    .monarchTextStyle(style, weight: MonarchStatusDotTokens.fontWeight)
                    .foregroundStyle(theme.colors[role: MonarchStatusDotTokens.label])
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
    }
}

/// The dot, with the pulse ring behind it while pulsing.
private struct MonarchStatusDotMark: View {
    let color: Color
    let size: CGFloat
    let isPulsing: Bool

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .background {
                if isPulsing && !reduceMotion {
                    MonarchStatusDotPulse(color: color)
                }
            }
    }
}

/// A ring that grows from the dot and fades, again and again. It exists only
/// while the dot pulses and Reduce Motion is off, so it always starts fresh.
private struct MonarchStatusDotPulse: View {
    let color: Color

    @State private var isAtEnd = false

    var body: some View {
        Circle()
            .fill(color)
            .scaleEffect(isAtEnd ? MonarchStatusDotTokens.pulseScale : 1)
            .opacity(isAtEnd ? 0 : 1)
            .onAppear {
                withAnimation(
                    .easeOut(duration: MonarchStatusDotTokens.pulseDuration).repeatForever(autoreverses: false)
                ) {
                    isAtEnd = true
                }
            }
    }
}

#if DEBUG
    #Preview("MonarchStatusDot") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchStatusDotSize.allCases, id: \.self) { size in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchStatusDot("Status", tone: tone, size: size)
                    }
                }
                MonarchStatusDot("Deploying", tone: .info, size: .md, isPulsing: true)
                MonarchStatusDot("Online", tone: .success, isLabelHidden: true)
            }
        }
    }
#endif
