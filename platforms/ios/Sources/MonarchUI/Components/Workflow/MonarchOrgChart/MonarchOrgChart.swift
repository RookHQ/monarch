import SwiftUI

/// An organisation chart: people as cards under their managers, with teams
/// that open and close, teams loaded on demand, zoom and pan, dotted
/// reporting lines and drag to move. In a compact width or at an
/// accessibility text size it is an indented list.
///
/// ```swift
/// MonarchOrgChart(
///     people,
///     focusID: selectedID,
///     dottedLines: [MonarchOrgChartDottedLine(from: "mia", to: "jo")],
///     onSelect: { person in selectedID = person.id },
///     onLoadChildren: { manager in try await directory.loadTeam(of: manager.id) },
///     onMoveRequest: { person, target in move = (person, target) }
/// )
/// .frame(height: 480)
/// ```
///
/// Pass every loaded person flat, with `parentID`; the app owns the data and
/// the decisions. Opening a team whose `childCount` is larger than the
/// reports in the chart calls `onLoadChildren`, and the team opens when it
/// returns (the app adds the reports to `nodes`). `onMoveRequest` turns on
/// moving: drag a card onto a new manager or onto the background, or choose
/// Move from the card's menu; the app then moves the person (or asks where).
/// Moves into one's own team, onto an open position and onto the current
/// manager are refused. The chart scrolls itself in both directions, so give
/// it a height.
///
/// Accessibility: the chart is a container named "Organisation chart" and
/// reads in tree order, manager before team. Each person is one element
/// read as name, title, extra facts, "Open position", "Temporary reporting
/// line" and "Also reports to …"; the value says whether the team is shown
/// and how many direct reports there are. Actions open and close the team
/// and ask to move. The focused person has the selected trait. Avatars,
/// lines and the count chips are hidden. A team that fails to load is
/// announced. Zoom buttons have spoken names and announce the new level.
@_spi(Experimental) public struct MonarchOrgChart: View {
    private let tree: MonarchOrgChartTree
    private let externalExpanded: Binding<Set<String>>?
    private let focusID: String?
    private let dottedLines: [MonarchOrgChartDottedLine]
    private let layout: MonarchOrgChartLayout
    private let isZoomable: Bool
    private let labels: MonarchOrgChartLabels
    private let onSelect: (@MainActor (MonarchOrgChartNode) -> Void)?
    private let onLoadChildren: (@MainActor (MonarchOrgChartNode) async throws -> Void)?
    private let onMoveRequest: (@MainActor (MonarchOrgChartNode, MonarchOrgChartMoveTarget) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif
    @State private var ownExpanded: Set<String>
    @State private var loading: Set<String> = []
    @State private var dropTargetID: String?
    @State private var zoom: CGFloat = 1
    @State private var pinchStart: CGFloat?
    @State private var chartSize: CGSize = .zero
    @State private var viewportWidth: CGFloat = 0
    @State private var hasFitted = false

    /// An organisation chart.
    ///
    /// - Parameters:
    ///   - nodes: Every person and open position, flat. Reporting lines come from `parentID`;
    ///     a node whose manager is missing is top level. Cycles are not drawn.
    ///   - expanded: The ids of people whose team is shown. `nil` keeps the state inside the
    ///     chart, starting with the top level (and the level under it when there are three or
    ///     fewer top-level people) and the path to `focusID`.
    ///   - focusID: The person to highlight. The path to them opens and the chart scrolls to them.
    ///   - dottedLines: Secondary reporting lines, drawn as dashed curves in the chart layout and
    ///     read as "Also reports to …" in both layouts.
    ///   - layout: The chart, the list, or `auto` to choose by width and text size.
    ///   - isZoomable: Shows the zoom buttons and allows pinching, in the chart layout.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Called when a card is tapped, for example to open a panel.
    ///     Without it, cards are not buttons.
    ///   - onLoadChildren: Loads a team whose `childCount` is larger than the reports in
    ///     `nodes`. The app adds the reports to `nodes`; a thrown error is announced.
    ///   - onMoveRequest: Turns on moving. Receives the person and where they were dropped,
    ///     or `.unspecified` when Move was chosen from the menu.
    public init(
        _ nodes: [MonarchOrgChartNode],
        expanded: Binding<Set<String>>? = nil,
        focusID: String? = nil,
        dottedLines: [MonarchOrgChartDottedLine] = [],
        layout: MonarchOrgChartLayout = .auto,
        isZoomable: Bool = true,
        labels: MonarchOrgChartLabels = MonarchOrgChartLabels(),
        onSelect: (@MainActor (MonarchOrgChartNode) -> Void)? = nil,
        onLoadChildren: (@MainActor (MonarchOrgChartNode) async throws -> Void)? = nil,
        onMoveRequest: (@MainActor (MonarchOrgChartNode, MonarchOrgChartMoveTarget) -> Void)? = nil
    ) {
        let tree = MonarchOrgChartTree(nodes)
        self.tree = tree
        self.externalExpanded = expanded
        self.focusID = focusID
        self.dottedLines = dottedLines
        self.layout = layout
        self.isZoomable = isZoomable
        self.labels = labels
        self.onSelect = onSelect
        self.onLoadChildren = onLoadChildren
        self.onMoveRequest = onMoveRequest
        _ownExpanded = State(initialValue: tree.defaultExpanded(focusID: focusID))
    }

    /// The content of the view.
    public var body: some View {
        let resolved = MonarchOrgChartTokens.resolvedLayout(
            layout,
            isRegularWidth: isRegularWidth,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        VStack(alignment: .leading, spacing: MonarchOrgChartTokens.sectionSpacing) {
            if onMoveRequest != nil {
                Text(labels.moveHint ?? MonarchOrgChartStrings.moveHint)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchOrgChartTokens.hintColor])
                    .fixedSize(horizontal: false, vertical: true)
            }
            if resolved == .chart && isZoomable {
                zoomBar
            }
            ScrollViewReader { proxy in
                Group {
                    if resolved == .chart {
                        chart
                    } else {
                        ScrollView {
                            MonarchOrgChartList(context: context)
                                .padding(MonarchOrgChartTokens.chipSpacing)
                        }
                    }
                }
                .onAppear { reveal(focusID, with: proxy, animated: false) }
                .onChange(of: focusID) { reveal(focusID, with: proxy, animated: true) }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchOrgChartStrings.label)
    }

    // MARK: Chart

    private var chart: some View {
        ScrollView([.horizontal, .vertical]) {
            MonarchOrgChartZoomLayout(zoom: zoom) {
                HStack(alignment: .top, spacing: MonarchOrgChartTokens.siblingGap) {
                    ForEach(tree.roots) { root in
                        MonarchOrgChartBranch(node: root, context: context)
                    }
                }
                .overlayPreferenceValue(MonarchOrgChartCardFrames.self) { frames in
                    dotted(frames)
                }
                .padding(MonarchOrgChartTokens.canvasPadding)
                .onGeometryChange(for: CGSize.self) {
                    $0.size
                } action: {
                    chartSize = $0
                }
                .scaleEffect(zoom, anchor: .topLeading)
            }
            .contentShape(Rectangle())
            .dropDestination(
                for: String.self,
                action: { payloads, _ in dropOnBackground(payloads) },
                isTargeted: { _ in }
            )
        }
        .onGeometryChange(for: CGFloat.self) {
            $0.size.width
        } action: {
            viewportWidth = $0
        }
        .task(id: chartSize.width + viewportWidth) {
            // A chart wider than its pane opens zoomed out to fit the width, so no card starts cut off.
            guard isZoomable, !hasFitted, chartSize.width > 0, viewportWidth > 0 else { return }
            hasFitted = true
            if chartSize.width > viewportWidth {
                zoom = MonarchOrgChartTokens.clampedZoom(viewportWidth / chartSize.width)
            }
        }
        .simultaneousGesture(
            MagnifyGesture()
                .onChanged { value in
                    guard isZoomable else { return }
                    let start = pinchStart ?? zoom
                    pinchStart = start
                    zoom = MonarchOrgChartTokens.clampedZoom(start * value.magnification)
                }
                .onEnded { _ in pinchStart = nil }
        )
    }

    private func dotted(_ frames: [String: Anchor<CGRect>]) -> some View {
        GeometryReader { proxy in
            let color = theme.colors[role: MonarchOrgChartTokens.dottedColor]
            ForEach(dottedLines, id: \.self) { line in
                if let manager = frames[line.to], let report = frames[line.from] {
                    let curve = MonarchOrgChartTokens.dottedCurve(manager: proxy[manager], report: proxy[report])
                    MonarchOrgChartDottedShape(curve: curve, isDot: false)
                        .stroke(
                            color,
                            style: StrokeStyle(
                                lineWidth: MonarchOrgChartTokens.dottedWidth,
                                dash: MonarchOrgChartTokens.dottedDash
                            )
                        )
                    MonarchOrgChartDottedShape(curve: curve, isDot: true).fill(color)
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    private var zoomBar: some View {
        HStack(spacing: MonarchOrgChartTokens.toolbarSpacing) {
            zoomButton(
                labels.zoomOut ?? MonarchOrgChartStrings.zoomOut,
                systemImage: MonarchOrgChartTokens.zoomOutSymbol,
                isEnabled: MonarchOrgChartTokens.canZoomOut(zoom)
            ) { MonarchOrgChartTokens.zoomedOut(zoom) }
            Text(MonarchOrgChartTokens.zoomLevelText(zoom, labels: labels))
                .monarchTextStyle(.label)
                .monospacedDigit()
                .foregroundStyle(theme.colors.textMuted)
            zoomButton(
                labels.zoomIn ?? MonarchOrgChartStrings.zoomIn,
                systemImage: MonarchOrgChartTokens.zoomInSymbol,
                isEnabled: MonarchOrgChartTokens.canZoomIn(zoom)
            ) { MonarchOrgChartTokens.zoomedIn(zoom) }
            zoomButton(
                labels.zoomReset ?? MonarchOrgChartStrings.zoomReset,
                systemImage: MonarchOrgChartTokens.zoomResetSymbol,
                isEnabled: MonarchOrgChartTokens.canResetZoom(zoom)
            ) { 1 }
        }
    }

    private func zoomButton(
        _ title: String,
        systemImage: String,
        isEnabled: Bool,
        next: @escaping @MainActor () -> CGFloat
    ) -> some View {
        Button {
            withAnimation(theme.motion.animation(.fast, reduceMotion: reduceMotion)) {
                zoom = next()
            }
            AccessibilityNotification.Announcement(MonarchOrgChartTokens.zoomLevelText(zoom, labels: labels)).post()
        } label: {
            Label(title, systemImage: systemImage).labelStyle(.iconOnly)
        }
        .buttonStyle(.monarch(.ghost, size: .sm))
        .disabled(!isEnabled)
    }

    // MARK: State

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            true
        #endif
    }

    private var expanded: Set<String> {
        externalExpanded?.wrappedValue ?? ownExpanded
    }

    private func setExpanded(_ value: Set<String>) {
        if let externalExpanded {
            externalExpanded.wrappedValue = value
        } else {
            ownExpanded = value
        }
    }

    private var context: MonarchOrgChartContext {
        MonarchOrgChartContext(
            tree: tree,
            expanded: expanded,
            loading: loading,
            focusID: focusID,
            dropTargetID: dropTargetID,
            dottedLines: dottedLines,
            labels: labels,
            canSelect: onSelect != nil,
            canMove: onMoveRequest != nil,
            zoom: zoom,
            toggle: { toggle($0) },
            select: { onSelect?($0) },
            requestMove: { node, target in onMoveRequest?(node, target) },
            setDropTarget: { id, isTargeted in
                if isTargeted {
                    dropTargetID = id
                } else if dropTargetID == id {
                    dropTargetID = nil
                }
            }
        )
    }

    private func toggle(_ node: MonarchOrgChartNode) {
        switch tree.toggleDecision(node, expanded: expanded, loading: loading, canLoad: onLoadChildren != nil) {
        case .collapse:
            setExpanded(expanded.subtracting([node.id]))
        case .expand:
            setExpanded(expanded.union([node.id]))
        case .load:
            guard let onLoadChildren else { return }
            loading.insert(node.id)
            Task { @MainActor in
                do {
                    try await onLoadChildren(node)
                    // Teams opened or closed while this one loaded are kept.
                    setExpanded(expanded.union([node.id]))
                } catch {
                    AccessibilityNotification.Announcement(
                        labels.loadFailed?(node.name) ?? MonarchOrgChartStrings.loadFailed(node.name)
                    ).post()
                }
                loading.remove(node.id)
            }
        case .none:
            break
        }
    }

    private func reveal(_ id: String?, with proxy: ScrollViewProxy, animated: Bool) {
        guard let id, tree.byID[id] != nil else { return }
        let path = tree.ancestors(of: id)
        if !expanded.isSuperset(of: path) {
            setExpanded(expanded.union(path))
        }
        Task { @MainActor in
            await Task.yield()
            withAnimation(animated ? theme.motion.animation(.base, reduceMotion: reduceMotion) : nil) {
                proxy.scrollTo(id, anchor: .center)
            }
        }
    }

    private func dropOnBackground(_ payloads: [String]) -> Bool {
        guard let onMoveRequest,
            let id = payloads.lazy.compactMap(MonarchOrgChartTokens.movingID(fromPayload:)).first,
            tree.canMoveToTopLevel(id),
            let node = tree.byID[id]
        else { return false }
        onMoveRequest(node, .topLevel)
        return true
    }
}

#if DEBUG
    #Preview("MonarchOrgChart") {
        let people = [
            MonarchOrgChartNode(id: "ana", name: "Ana Silva", title: "Chief executive"),
            MonarchOrgChartNode(id: "jo", parentID: "ana", name: "Jo Park", title: "Head of engineering"),
            MonarchOrgChartNode(
                id: "noor",
                parentID: "ana",
                name: "Noor Ali",
                title: "People lead",
                meta: "Acting",
                isDashed: true,
                childCount: 2
            ),
            MonarchOrgChartNode(id: "ola", parentID: "jo", name: "Ola Berg", title: "Engineer"),
            MonarchOrgChartNode(id: "open", parentID: "jo", name: "Product designer", kind: .vacancy),
        ]
        MonarchPreviewMatrix {
            MonarchOrgChart(
                people,
                focusID: "ola",
                dottedLines: [MonarchOrgChartDottedLine(from: "ola", to: "noor")],
                layout: .chart,
                onSelect: { _ in }
            )
            .frame(height: MonarchOrgChartTokens.cardWidth * 2)
            MonarchOrgChart(people, layout: .list, onMoveRequest: { _, _ in })
                .frame(height: MonarchOrgChartTokens.cardWidth * 2)
        }
    }
#endif
