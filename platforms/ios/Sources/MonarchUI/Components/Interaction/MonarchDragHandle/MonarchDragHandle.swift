import SwiftUI

/// The grip an item is dragged by, with move actions for VoiceOver.
///
/// The handle owns the look, the 44-point touch target, the spoken name and
/// the accessibility actions. The view that moves items owns the gesture and
/// the order: attach `gesture(_:)` or `draggable(_:)` to the handle, pass the
/// drag `state`, and move the item in `onMove`.
///
/// ```swift
/// MonarchDragHandle("Reorder Design review", state: isLifted ? .grabbed : .idle) { direction in
///     move(item, direction)
/// }
/// .gesture(reorderGesture)
/// ```
///
/// In a `List`, use the system reordering (`onMove(perform:)`) instead.
///
/// Accessibility: the handle is one element named by `label`. For each
/// direction of `axis` it offers a named action ("Move up", "Move down",
/// "Move backward", "Move forward") that calls `onMove`, so VoiceOver and
/// Switch Control can reorder without a drag. While grabbed or dropped the
/// state is the element's value ("Picked up", "Dropped"), and the grabbed
/// handle gains an outline, so the state does not depend on colour. The grip
/// scales with Dynamic Type.
public struct MonarchDragHandle: View {
    private let label: String
    private let state: MonarchDragHandleState
    private let axis: MonarchDragHandleAxis
    private let hint: String?
    private let actionLabels: [MonarchDragHandleDirection: String]
    private let stateLabels: [MonarchDragHandleState: String]
    private let onMove: (@MainActor (MonarchDragHandleDirection) -> Void)?

    @ScaledMetric private var boxSize: CGFloat
    @ScaledMetric(relativeTo: .body) private var glyphSize = MonarchDragHandleTokens.glyphSize
    @Environment(\.isEnabled) private var isEnabled

    /// A drag handle.
    ///
    /// - Parameters:
    ///   - label: The spoken name, naming the item: "Reorder Design review".
    ///   - state: Where the item is in a drag. The view that moves items owns it.
    ///   - axis: Which way the item moves, which selects the grip and the actions.
    ///   - size: The size of the visible handle.
    ///   - hint: A spoken hint, already localised. None by default.
    ///   - actionLabels: Replaces the built-in names of the move actions.
    ///   - stateLabels: Replaces the built-in spoken values "Picked up" and "Dropped".
    ///   - onMove: Moves the item one step. Without it the handle has no actions.
    public init(
        _ label: String,
        state: MonarchDragHandleState = .idle,
        axis: MonarchDragHandleAxis = .vertical,
        size: MonarchDragHandleSize = .md,
        hint: String? = nil,
        actionLabels: [MonarchDragHandleDirection: String] = [:],
        stateLabels: [MonarchDragHandleState: String] = [:],
        onMove: (@MainActor (MonarchDragHandleDirection) -> Void)? = nil
    ) {
        self.label = label
        self.state = state
        self.axis = axis
        self.hint = hint
        self.actionLabels = actionLabels
        self.stateLabels = stateLabels
        self.onMove = onMove
        _boxSize = ScaledMetric(wrappedValue: MonarchDragHandleTokens.boxSize(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchDragHandleTokens.appearance(state: state, isEnabled: isEnabled)
        MonarchDragHandleGrip(axis: axis)
            .frame(width: glyphSize, height: glyphSize)
            .frame(width: boxSize, height: boxSize)
            .monarchSurface(appearance, in: MonarchDragHandleTokens.shape)
            .monarchAnimation(.fast, value: state)
            .monarchMinimumHitTarget()
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(label)
            .accessibilityValue(MonarchDragHandleTokens.accessibilityValue(state: state, overrides: stateLabels))
            .accessibilityHint(hint ?? "")
            .accessibilityActions {
                if let onMove, isEnabled {
                    ForEach(MonarchDragHandleTokens.directions(axis: axis), id: \.self) { direction in
                        Button(MonarchDragHandleTokens.actionName(direction, overrides: actionLabels)) {
                            onMove(direction)
                        }
                    }
                }
            }
    }
}

#if DEBUG
    #Preview("MonarchDragHandle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    ForEach(MonarchDragHandleState.allCases, id: \.self) { state in
                        MonarchDragHandle("Reorder", state: state)
                    }
                    MonarchDragHandle("Reorder").disabled(true)
                }
                HStack {
                    ForEach(MonarchDragHandleAxis.allCases, id: \.self) { axis in
                        MonarchDragHandle("Reorder", axis: axis, size: .sm)
                    }
                }
            }
        }
    }
#endif
