import SwiftUI

/// Nested goals (objectives and key results) with progress, status, owner
/// and alignment. Goals with children open and close; each goal can carry
/// its own actions, and top-level goals can offer "Add key result".
///
/// ```swift
/// MonarchGoalTree(
///     goals,
///     onAddChild: { parent in addKeyResult(to: parent) },
///     actions: { goal in
///         Menu {
///             Button("Check in") { checkIn(goal) }
///             Button("History") { showHistory(goal) }
///         } label: {
///             Label("More", systemImage: "ellipsis")
///         }
///         .labelStyle(.iconOnly)
///         .buttonStyle(.monarch(.ghost, size: .sm))
///     }
/// )
/// ```
///
/// Goals are given flat; nesting comes from `parentID`, and order is kept
/// among siblings. Every goal with children is open at first unless the
/// app passes `expandedIDs`. In the quiet view (the default) children that
/// are on track or not started hide their badge, so the goals that need
/// attention stand out; their status is still spoken.
///
/// Accessibility: each goal is one element read as its title, status,
/// owner, facts, alignment and, when closed, its number of children; its
/// value is the progress. The open and close button is a separate element
/// named "Expand …" or "Collapse …", with a 44-point target. Status badges
/// carry a symbol and a word, so they do not rely on colour.
@_spi(Experimental) public struct MonarchGoalTree<Actions: View>: View {
    private let goals: [MonarchGoal]
    private let expandedIDs: Binding<Set<String>>?
    private let isQuietChildren: Bool
    private let labels: MonarchGoalTreeLabels
    private let canAddChild: ((MonarchGoal, Int) -> Bool)?
    private let onAddChild: (@MainActor (MonarchGoal) -> Void)?
    private let actions: (MonarchGoal) -> Actions

    @State private var localExpanded: Set<String>?
    @Environment(\.monarchTheme) private var theme

    /// A goal tree with actions on every goal.
    ///
    /// - Parameters:
    ///   - goals: Every goal, flat. Nesting comes from `parentID`; order is kept among siblings.
    ///   - expandedIDs: The ids of open goals. Kept inside, with every goal that has children open, when `nil`.
    ///   - isQuietChildren: Whether children that are on track or not started hide their badge.
    ///   - labels: Replacements for the built-in strings.
    ///   - canAddChild: Which goals get the add-child button, from the goal and its depth. Top-level goals when `nil`.
    ///   - onAddChild: Adds "Add key result" under the goals `canAddChild` allows. Receives the parent.
    ///   - actions: Controls at the end of each goal's row, such as a `Menu`.
    public init(
        _ goals: [MonarchGoal],
        expandedIDs: Binding<Set<String>>? = nil,
        isQuietChildren: Bool = true,
        labels: MonarchGoalTreeLabels = MonarchGoalTreeLabels(),
        canAddChild: ((MonarchGoal, Int) -> Bool)? = nil,
        onAddChild: (@MainActor (MonarchGoal) -> Void)? = nil,
        @ViewBuilder actions: @escaping (MonarchGoal) -> Actions
    ) {
        self.goals = goals
        self.expandedIDs = expandedIDs
        self.isQuietChildren = isQuietChildren
        self.labels = labels
        self.canAddChild = canAddChild
        self.onAddChild = onAddChild
        self.actions = actions
    }

    private var expanded: Binding<Set<String>> {
        if let expandedIDs { return expandedIDs }
        return Binding(
            get: { localExpanded ?? MonarchGoalTreeTokens.defaultExpanded(goals) },
            set: { localExpanded = $0 }
        )
    }

    /// The content of the view.
    public var body: some View {
        if goals.isEmpty {
            MonarchEmptyState(labels.empty, systemImage: MonarchGoalTreeTokens.emptySymbol, size: .sm)
                .frame(maxWidth: .infinity)
        } else {
            let structure = MonarchGoalTreeTokens.structure(goals)
            VStack(alignment: .leading, spacing: MonarchGoalTreeTokens.rowSpacing) {
                ForEach(structure.roots) { goal in
                    MonarchGoalTreeNode(
                        goal: goal,
                        depth: 0,
                        trail: [],
                        children: structure.children,
                        expanded: expanded,
                        isQuietChildren: isQuietChildren,
                        labels: labels,
                        canAddChild: canAddChild,
                        onAddChild: onAddChild,
                        actions: actions
                    )
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .contain)
        }
    }
}

extension MonarchGoalTree where Actions == EmptyView {
    /// A goal tree.
    ///
    /// - Parameters:
    ///   - goals: Every goal, flat. Nesting comes from `parentID`; order is kept among siblings.
    ///   - expandedIDs: The ids of open goals. Kept inside, with every goal that has children open, when `nil`.
    ///   - isQuietChildren: Whether children that are on track or not started hide their badge.
    ///   - labels: Replacements for the built-in strings.
    ///   - canAddChild: Which goals get the add-child button, from the goal and its depth. Top-level goals when `nil`.
    ///   - onAddChild: Adds "Add key result" under the goals `canAddChild` allows. Receives the parent.
    public init(
        _ goals: [MonarchGoal],
        expandedIDs: Binding<Set<String>>? = nil,
        isQuietChildren: Bool = true,
        labels: MonarchGoalTreeLabels = MonarchGoalTreeLabels(),
        canAddChild: ((MonarchGoal, Int) -> Bool)? = nil,
        onAddChild: (@MainActor (MonarchGoal) -> Void)? = nil
    ) {
        self.init(
            goals,
            expandedIDs: expandedIDs,
            isQuietChildren: isQuietChildren,
            labels: labels,
            canAddChild: canAddChild,
            onAddChild: onAddChild,
            actions: { _ in EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchGoalTree") {
        MonarchPreviewMatrix {
            MonarchGoalTree(
                [
                    MonarchGoal(
                        id: "o1",
                        title: "Grow repeat customers",
                        progress: 0.45,
                        status: .onTrack,
                        ownerName: "Ana Ruiz",
                        meta: "Q2"
                    ),
                    MonarchGoal(
                        id: "k1",
                        parentID: "o1",
                        title: "Raise repeat rate to 35%",
                        progress: 0.6,
                        status: .onTrack
                    ),
                    MonarchGoal(id: "k2", parentID: "o1", title: "Cut churn to 4%", progress: 0.2, status: .atRisk),
                    MonarchGoal(id: "o2", title: "Ship the mobile app", progress: 1, status: .done),
                ],
                onAddChild: { _ in }
            )
        }
    }
#endif
