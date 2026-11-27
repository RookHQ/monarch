import MonarchTokens
import SwiftUI

/// Two panels with a separator the person drags to resize them.
///
/// The app owns the size: `fraction` is the share of the first panel, from 0
/// to 1. Keep it in `@SceneStorage` or `@AppStorage` to restore the layout.
///
/// ```swift
/// MonarchResizablePanels(
///     fraction: $listShare,
///     first: { MessageList() },
///     second: { MessageDetail() }
/// )
/// ```
///
/// The panels fill the space the view is offered, so give it a height when
/// it is inside a scroll view. Nest one inside a panel for three panes. For
/// a list and a detail that become one pane on a phone, use
/// `NavigationSplitView`: this view is for panes that stay on screen together.
///
/// Accessibility: the separator is one adjustable element named "Resize
/// panels" whose value is the share of the first panel ("40%"). Swiping up
/// or down with VoiceOver moves it by `step`. With a hardware keyboard it
/// takes focus, the arrow keys along the axis move it by `step`, and Home
/// and End move it to the ends of `range`. Its touch target is 44 points
/// across. While dragged or focused the line is thicker as well as accented.
public struct MonarchResizablePanels<First: View, Second: View>: View {
    @Binding private var fraction: Double
    private let axis: MonarchResizablePanelsAxis
    private let range: ClosedRange<Double>
    private let step: Double
    private let showsGrip: Bool
    private let handleLabel: String?
    private let stacksWhenCompact: Bool
    private let collapsesFirst: Bool
    private let onResizeEnded: (@MainActor (Double) -> Void)?
    private let first: First
    private let second: Second

    @State private var dragStart: Double?
    @FocusState private var isFocused: Bool
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    /// Two resizable panels.
    ///
    /// - Parameters:
    ///   - fraction: The share of the first panel, from 0 to 1.
    ///   - axis: Side by side, or stacked.
    ///   - range: The smallest and largest share of the first panel.
    ///   - step: The share one arrow key press or one VoiceOver swipe moves the separator.
    ///   - showsGrip: Whether a grip is drawn in the middle of the separator.
    ///   - handleLabel: Replaces the built-in spoken name "Resize panels".
    ///   - stacksWhenCompact: Whether side-by-side panels are stacked at their natural
    ///     height, with a plain divider, when the width is compact (a phone in portrait).
    ///   - collapsesFirst: Whether dragging the first panel below the smallest share collapses it to nothing
    ///     (the share becomes 0), and dragging back out opens it again at the smallest share.
    ///   - onResizeEnded: Called with the share when a drag ends and after each keyboard
    ///     or VoiceOver step. Save the layout here.
    ///   - first: The leading or top panel.
    ///   - second: The trailing or bottom panel.
    public init(
        fraction: Binding<Double>,
        axis: MonarchResizablePanelsAxis = .horizontal,
        range: ClosedRange<Double> = 0.1...0.9,
        step: Double = 0.05,
        showsGrip: Bool = false,
        handleLabel: String? = nil,
        stacksWhenCompact: Bool = true,
        collapsesFirst: Bool = false,
        onResizeEnded: (@MainActor (Double) -> Void)? = nil,
        @ViewBuilder first: () -> First,
        @ViewBuilder second: () -> Second
    ) {
        _fraction = fraction
        self.axis = axis
        self.range = MonarchResizablePanelsTokens.normalized(range)
        self.step = step
        self.showsGrip = showsGrip
        self.handleLabel = handleLabel
        self.stacksWhenCompact = stacksWhenCompact
        self.collapsesFirst = collapsesFirst
        self.onResizeEnded = onResizeEnded
        self.first = first()
        self.second = second()
    }

    /// The content of the view.
    public var body: some View {
        let isStacked = MonarchResizablePanelsTokens.isStacked(
            axis: axis,
            stacksWhenCompact: stacksWhenCompact,
            isCompact: horizontalSizeClass == .compact
        )
        if isStacked {
            VStack(spacing: 0) {
                first
                MonarchDivider()
                second
            }
        } else {
            GeometryReader { proxy in
                split(total: isHorizontal ? proxy.size.width : proxy.size.height)
            }
        }
    }

    private var isHorizontal: Bool { axis == .horizontal }

    private var current: Double {
        if collapsesFirst, fraction <= 0 { return 0 }
        return MonarchResizablePanelsTokens.clamped(fraction, to: range)
    }

    private func split(total: CGFloat) -> some View {
        let length = MonarchResizablePanelsTokens.firstLength(fraction: current, total: total)
        let layout = isHorizontal ? AnyLayout(HStackLayout(spacing: 0)) : AnyLayout(VStackLayout(spacing: 0))
        return layout {
            first
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .frame(width: isHorizontal ? length : nil, height: isHorizontal ? nil : length)
                .clipped()
            handle(total: total)
                .zIndex(1)
            second
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()
        }
    }

    private var gripShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
    }

    private func handle(total: CGFloat) -> some View {
        let isActive = dragStart != nil || isFocused
        let color = theme.colors[
            role: MonarchResizablePanelsTokens.handleColor(isActive: isActive, isEnabled: isEnabled)
        ]
        let thickness =
            isActive
            ? MonarchResizablePanelsTokens.activeLineThickness : MonarchResizablePanelsTokens.lineThickness
        let target = MonarchAccessibility.minimumHitTarget
        // The line keeps its layout thickness; the thicker active line, the grip and the
        // touch target are overlays, so the panels do not move when the state changes.
        return Color.clear
            .frame(
                width: isHorizontal ? MonarchResizablePanelsTokens.lineThickness : nil,
                height: isHorizontal ? nil : MonarchResizablePanelsTokens.lineThickness
            )
            .overlay {
                Rectangle()
                    .fill(color)
                    .frame(width: isHorizontal ? thickness : nil, height: isHorizontal ? nil : thickness)
            }
            .overlay {
                if showsGrip {
                    // Two rows of three dots (or three of two when the panels stack), as the web draws its grip.
                    let dots = isHorizontal ? (columns: 2, rows: 3) : (columns: 3, rows: 2)
                    Grid(
                        horizontalSpacing: MonarchResizablePanelsTokens.gripDotSpacing,
                        verticalSpacing: MonarchResizablePanelsTokens.gripDotSpacing
                    ) {
                        ForEach(0..<dots.rows, id: \.self) { _ in
                            GridRow {
                                ForEach(0..<dots.columns, id: \.self) { _ in
                                    Circle()
                                        .fill(isActive ? color : theme.colors.textMuted)
                                        .frame(
                                            width: MonarchResizablePanelsTokens.gripDotSize,
                                            height: MonarchResizablePanelsTokens.gripDotSize
                                        )
                                }
                            }
                        }
                    }
                    .padding(MonarchResizablePanelsTokens.gripDotSpacing)
                    .background(theme.colors.bgSurface, in: gripShape)
                    .overlay(gripShape.strokeBorder(color, lineWidth: MonarchTokens.Space.px))
                }
            }
            .overlay {
                Color.clear
                    .frame(width: isHorizontal ? target : nil, height: isHorizontal ? nil : target)
                    .contentShape(Rectangle())
                    .gesture(drag(total: total), including: isEnabled ? .all : .none)
                    .focusable(isEnabled)
                    .focused($isFocused)
                    .onKeyPress(keys: [.leftArrow, .rightArrow, .upArrow, .downArrow, .home, .end]) { press in
                        let move = MonarchResizablePanelsTokens.move(
                            for: press.key,
                            axis: axis,
                            isRightToLeft: layoutDirection == .rightToLeft
                        )
                        guard let move else { return .ignored }
                        apply(move)
                        return .handled
                    }
                    .accessibilityElement()
                    .accessibilityLabel(handleLabel ?? MonarchResizablePanelsStrings.handle)
                    .accessibilityValue(MonarchResizablePanelsTokens.accessibilityValue(fraction: current))
                    .accessibilityAdjustableAction { direction in
                        switch direction {
                        case .increment: apply(.increase)
                        case .decrement: apply(.decrease)
                        @unknown default: break
                        }
                    }
            }
            .monarchAnimation(.fast, value: isActive)
    }

    private func drag(total: CGFloat) -> some Gesture {
        // The global space, because the separator's own space moves with it during the drag.
        DragGesture(minimumDistance: 0, coordinateSpace: .global)
            .onChanged { value in
                let start = dragStart ?? current
                dragStart = start
                let reach = collapsesFirst ? 0...range.upperBound : range
                let proposed = MonarchResizablePanelsTokens.fraction(
                    start: start,
                    translation: isHorizontal ? value.translation.width : value.translation.height,
                    total: total,
                    isMirrored: isHorizontal && layoutDirection == .rightToLeft,
                    range: reach
                )
                // Below half the smallest share the panel collapses; between that and the smallest it holds there.
                fraction =
                    collapsesFirst && proposed < range.lowerBound / 2
                    ? 0 : max(proposed, collapsesFirst ? range.lowerBound : 0)
            }
            .onEnded { _ in
                dragStart = nil
                onResizeEnded?(current)
            }
    }

    private func apply(_ move: MonarchResizablePanelsMove) {
        guard isEnabled else { return }
        var next = MonarchResizablePanelsTokens.fraction(current, applying: move, step: step, range: range)
        if collapsesFirst {
            if current == 0, move == .increase { next = range.lowerBound }
            if current <= range.lowerBound, move == .decrease { next = 0 }
        }
        fraction = next
        onResizeEnded?(next)
    }
}

#if DEBUG
    #Preview("MonarchResizablePanels") {
        MonarchPreviewMatrix {
            VStack {
                ForEach(MonarchResizablePanelsAxis.allCases, id: \.self) { axis in
                    MonarchResizablePanels(
                        fraction: .constant(0.4),
                        axis: axis,
                        showsGrip: true,
                        stacksWhenCompact: false,
                        first: { Text("First").frame(maxWidth: .infinity, maxHeight: .infinity) },
                        second: { Text("Second").frame(maxWidth: .infinity, maxHeight: .infinity) }
                    )
                    .frame(height: MonarchTokens.Space.space24 * 2)
                }
            }
        }
    }
#endif
