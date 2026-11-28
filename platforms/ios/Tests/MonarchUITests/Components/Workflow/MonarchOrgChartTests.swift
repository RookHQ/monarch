import Foundation
import MonarchTokens
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

enum MonarchOrgChartFixtures: ComponentFixtures {
    static let id = "org-chart"

    static let people = [
        MonarchOrgChartNode(id: "ana", name: "Ana Silva", title: "Chief executive"),
        MonarchOrgChartNode(id: "jo", parentID: "ana", name: "Jo Park", title: "Head of engineering"),
        MonarchOrgChartNode(id: "sam", parentID: "ana", name: "Sam Lee", title: "Head of finance", meta: "Finance"),
        MonarchOrgChartNode(
            id: "noor",
            parentID: "ana",
            name: "Noor Ali",
            title: "People lead",
            meta: "Acting",
            isDashed: true
        ),
        MonarchOrgChartNode(id: "ola", parentID: "jo", name: "Ola Berg", title: "Engineer"),
        MonarchOrgChartNode(id: "kai", parentID: "jo", name: "Kai Moss", title: "Engineer"),
        MonarchOrgChartNode(id: "open", parentID: "jo", name: "Product designer", meta: "Engineering", kind: .vacancy),
        MonarchOrgChartNode(id: "mia", parentID: "sam", name: "Mia Chen", title: "Accountant"),
        MonarchOrgChartNode(id: "leo", parentID: "noor", name: "Leo Grant", title: "Recruiter", childCount: 2),
    ]

    static let dotted = [MonarchOrgChartDottedLine(from: "mia", to: "jo")]
    static let height = MonarchOrgChartTokens.cardWidth * 2

    static let fixtures = [
        Fixture("chart, focused, dotted line, vacancy, dashed") {
            MonarchOrgChart(people, focusID: "mia", dottedLines: dotted, layout: .chart, onSelect: { _ in })
                .frame(height: height)
        },
        Fixture("chart with moving and lazy teams") {
            MonarchOrgChart(
                people,
                expanded: .constant(["ana", "jo", "sam", "noor"]),
                layout: .chart,
                onLoadChildren: { _ in },
                onMoveRequest: { _, _ in }
            )
            .frame(height: height)
        },
        Fixture("chart without zoom, collapsed") {
            MonarchOrgChart(people, expanded: .constant([]), layout: .chart, isZoomable: false)
                .frame(height: height)
        },
        Fixture("list, focused, with moving") {
            MonarchOrgChart(
                people,
                focusID: "leo",
                dottedLines: dotted,
                layout: .list,
                onSelect: { _ in },
                onMoveRequest: { _, _ in }
            )
            .frame(height: height)
        },
        Fixture("auto layout") {
            MonarchOrgChart(people).frame(height: height)
        },
        Fixture("deep list stops indenting") {
            MonarchOrgChart(
                (0..<8).map { index in
                    MonarchOrgChartNode(
                        id: "n\(index)",
                        parentID: index == 0 ? nil : "n\(index - 1)",
                        name: "Person \(index)",
                        title: "Level \(index + 1)"
                    )
                },
                expanded: .constant(Set((0..<8).map { "n\($0)" })),
                layout: .list
            )
            .frame(height: height)
        },
        Fixture("long text") {
            MonarchOrgChart(
                [
                    MonarchOrgChartNode(
                        id: "a",
                        name: "Alexandria Konstantinopoulou-Montgomery",
                        title: "Senior vice president of international partnerships and alliances",
                        meta: "Global partnerships, Europe, Middle East and Africa"
                    ),
                    MonarchOrgChartNode(id: "b", parentID: "a", name: "Bartholomew Featherstonehaugh"),
                ],
                layout: .chart
            )
            .frame(height: height)
        },
        Fixture("empty") {
            MonarchOrgChart([], layout: .chart).frame(height: height)
        },
        Fixture("custom labels") {
            MonarchOrgChart(
                people,
                layout: .chart,
                labels: MonarchOrgChartLabels(moveHint: "Glissez une personne", zoomLevel: { "\($0) %" }),
                onMoveRequest: { _, _ in }
            )
            .frame(height: height)
        },
        Fixture("disabled") {
            MonarchOrgChart(people, focusID: "jo", layout: .chart, onSelect: { _ in })
                .frame(height: height)
                .disabled(true)
        },
    ]
}

@Suite("MonarchOrgChart")
struct MonarchOrgChartTests {
    @Test("A chip's touch target is 44 points on screen at every zoom")
    func chipTarget() {
        typealias Tokens = MonarchOrgChartTokens
        let target = MonarchAccessibility.minimumHitTarget
        var zoom = Tokens.zoomMinimum
        while zoom <= Tokens.zoomMaximum {
            #expect(Tokens.chipTarget(zoom: zoom) * min(zoom, 1) >= target - 0.001, "zoom \(zoom)")
            #expect(Tokens.chipTarget(zoom: zoom) * zoom >= target - 0.001, "zoom \(zoom)")
            zoom += Tokens.zoomStep
        }
        #expect(Tokens.chipTarget(zoom: 1) == target)
        #expect(Tokens.chipTarget(zoom: 0.5) == target * 2)
    }

    private typealias Tokens = MonarchOrgChartTokens
    private let tree = MonarchOrgChartTree(MonarchOrgChartFixtures.people)
    private let labels = MonarchOrgChartLabels()

    private func node(_ id: String) throws -> MonarchOrgChartNode {
        try #require(tree.byID[id])
    }

    // MARK: Tree

    @Test("Reporting lines come from parentID; unknown, self and missing managers make a node top level")
    func structure() {
        let tree = MonarchOrgChartTree([
            MonarchOrgChartNode(id: "a", name: "A"),
            MonarchOrgChartNode(id: "b", parentID: "a", name: "B"),
            MonarchOrgChartNode(id: "c", parentID: "ghost", name: "C"),
            MonarchOrgChartNode(id: "d", parentID: "d", name: "D"),
            MonarchOrgChartNode(id: "b", parentID: "c", name: "Duplicate"),
        ])
        #expect(tree.roots.map(\.id) == ["a", "c", "d"])
        #expect(tree.reports(of: "a").map(\.id) == ["b"])
        #expect(tree.byID["b"]?.name == "B")
        #expect(tree.manager(of: "b") == "a")
        #expect(tree.manager(of: "c") == nil)
        #expect(tree.manager(of: "d") == nil)
    }

    @Test("A cycle is neither drawn nor followed forever")
    func cycles() {
        let tree = MonarchOrgChartTree([
            MonarchOrgChartNode(id: "root", name: "Root"),
            MonarchOrgChartNode(id: "x", parentID: "y", name: "X"),
            MonarchOrgChartNode(id: "y", parentID: "x", name: "Y"),
        ])
        #expect(tree.roots.map(\.id) == ["root"])
        #expect(tree.ancestors(of: "x") == ["y"])
        #expect(tree.visibleRows(expanded: ["root", "x", "y"]).map(\.node.id) == ["root"])
    }

    @Test("Ancestors are listed nearest first")
    func ancestors() {
        #expect(tree.ancestors(of: "leo") == ["noor", "ana"])
        #expect(tree.ancestors(of: "ana").isEmpty)
    }

    @Test("By default the top level opens, with the next level when there are three or fewer top-level people")
    func defaultExpansion() {
        #expect(tree.defaultExpanded(focusID: nil) == ["ana", "jo", "sam", "noor"])
        #expect(tree.defaultExpanded(focusID: "leo") == ["ana", "jo", "sam", "noor"])
        let wide = MonarchOrgChartTree(
            (0..<4).map { MonarchOrgChartNode(id: "r\($0)", name: "R\($0)") }
                + [
                    MonarchOrgChartNode(id: "c", parentID: "r0", name: "C"),
                    MonarchOrgChartNode(id: "g", parentID: "c", name: "G"),
                ]
        )
        #expect(wide.defaultExpanded(focusID: nil) == ["r0", "r1", "r2", "r3"])
        #expect(wide.defaultExpanded(focusID: "g") == ["r0", "r1", "r2", "r3", "c"])
    }

    @Test("Visible rows follow tree order with their level")
    func visibleRows() {
        let rows = tree.visibleRows(expanded: ["ana", "jo"])
        #expect(rows.map(\.node.id) == ["ana", "jo", "ola", "kai", "open", "sam", "noor"])
        #expect(rows.map(\.level) == [1, 2, 3, 3, 3, 2, 2])
        #expect(tree.visibleRows(expanded: []).map(\.node.id) == ["ana"])
    }

    @Test("Report counts include reports not loaded yet")
    func counts() throws {
        #expect(tree.reportCount(try node("jo")) == 3)
        #expect(tree.reportCount(try node("leo")) == 2)
        #expect(tree.hasReports(try node("leo")))
        #expect(tree.needsLoading(try node("leo")))
        #expect(!tree.needsLoading(try node("jo")))
        #expect(!tree.hasReports(try node("ola")))
    }

    @Test("Opening a team expands, loads first when reports are missing, and closing collapses")
    func toggling() throws {
        let leo = try node("leo")
        let jo = try node("jo")
        #expect(tree.toggleDecision(leo, expanded: [], loading: [], canLoad: true) == .load)
        #expect(tree.toggleDecision(leo, expanded: [], loading: [], canLoad: false) == .expand)
        #expect(tree.toggleDecision(leo, expanded: [], loading: ["leo"], canLoad: true) == .none)
        #expect(tree.toggleDecision(jo, expanded: [], loading: [], canLoad: true) == .expand)
        #expect(tree.toggleDecision(jo, expanded: ["jo"], loading: [], canLoad: true) == .collapse)
        #expect(tree.toggleDecision(try node("ola"), expanded: [], loading: [], canLoad: true) == .none)
    }

    @Test("Moves into one's own team, onto vacancies and onto the current manager are refused")
    func moving() {
        #expect(tree.canMove("ola", onto: "sam"))
        #expect(!tree.canMove("ola", onto: "jo"))
        #expect(!tree.canMove("jo", onto: "ola"))
        #expect(!tree.canMove("ana", onto: "leo"))
        #expect(!tree.canMove("jo", onto: "jo"))
        #expect(!tree.canMove("ola", onto: "open"))
        #expect(!tree.canMove("open", onto: "sam"))
        #expect(!tree.canMove("ghost", onto: "sam"))
        #expect(tree.canMoveToTopLevel("jo"))
        #expect(!tree.canMoveToTopLevel("ana"))
        #expect(!tree.canMoveToTopLevel("open"))
    }

    @Test("Drag payloads are marked so text from elsewhere is ignored")
    func payloads() {
        #expect(Tokens.movingID(fromPayload: Tokens.dragPayload("jo")) == "jo")
        #expect(Tokens.movingID(fromPayload: "jo") == nil)
        #expect(Tokens.movingID(fromPayload: Tokens.dragPayload("")) == nil)
    }

    // MARK: Zoom and layout

    @Test("Zoom stays between 40% and 160% in steps of 10%")
    func zoom() {
        #expect(Tokens.clampedZoom(0.1) == Tokens.zoomMinimum)
        #expect(Tokens.clampedZoom(3) == Tokens.zoomMaximum)
        #expect(Tokens.zoomPercent(Tokens.zoomedIn(1)) == 110)
        #expect(Tokens.zoomPercent(Tokens.zoomedOut(1)) == 90)
        #expect(Tokens.zoomPercent(Tokens.clampedZoom(1.234)) == 120)
        #expect(!Tokens.canZoomIn(Tokens.zoomMaximum))
        #expect(Tokens.canZoomIn(1))
        #expect(!Tokens.canZoomOut(Tokens.zoomMinimum))
        #expect(!Tokens.canResetZoom(1))
        #expect(Tokens.canResetZoom(Tokens.zoomedIn(1)))
        #expect(Tokens.zoomLevelText(Tokens.zoomedIn(1), labels: labels) == "110%")
        #expect(Tokens.zoomLevelText(1, labels: MonarchOrgChartLabels(zoomLevel: { "\($0) pc" })) == "100 pc")
    }

    @Test("Auto shows the chart only in a regular width at standard text sizes")
    func layouts() {
        #expect(Tokens.resolvedLayout(.auto, isRegularWidth: true, isAccessibilitySize: false) == .chart)
        #expect(Tokens.resolvedLayout(.auto, isRegularWidth: false, isAccessibilitySize: false) == .list)
        #expect(Tokens.resolvedLayout(.auto, isRegularWidth: true, isAccessibilitySize: true) == .list)
        #expect(Tokens.resolvedLayout(.chart, isRegularWidth: false, isAccessibilitySize: true) == .chart)
        #expect(Tokens.resolvedLayout(.list, isRegularWidth: true, isAccessibilitySize: false) == .list)
    }

    @Test("Connectors stop at the first and last report, and the list stops indenting")
    func connectors() {
        #expect(Tokens.connectorHalves(index: 0, count: 1).leading == false)
        #expect(Tokens.connectorHalves(index: 0, count: 1).trailing == false)
        #expect(Tokens.connectorHalves(index: 0, count: 3).leading == false)
        #expect(Tokens.connectorHalves(index: 0, count: 3).trailing == true)
        #expect(Tokens.connectorHalves(index: 1, count: 3).leading == true)
        #expect(Tokens.connectorHalves(index: 1, count: 3).trailing == true)
        #expect(Tokens.connectorHalves(index: 2, count: 3).leading == true)
        #expect(Tokens.connectorHalves(index: 2, count: 3).trailing == false)
        #expect(Tokens.indentSteps(level: 1) == 0)
        #expect(Tokens.indentSteps(level: 3) == 2)
        #expect(Tokens.indentSteps(level: 20) == Tokens.maximumIndentSteps)
        #expect(Tokens.chevronSymbol(isExpanded: false) != Tokens.chevronSymbol(isExpanded: true))
    }

    @Test("A dotted curve runs from the manager's bottom centre to the report's top centre")
    func dottedCurve() {
        let curve = Tokens.dottedCurve(
            manager: CGRect(x: 0, y: 0, width: 100, height: 50),
            report: CGRect(x: 200, y: 250, width: 100, height: 50)
        )
        #expect(curve.start == CGPoint(x: 50, y: 50))
        #expect(curve.end == CGPoint(x: 250, y: 250))
        #expect(curve.control1 == CGPoint(x: 50, y: 150))
        #expect(curve.control2 == CGPoint(x: 250, y: 150))
        let near = Tokens.dottedCurve(
            manager: CGRect(x: 0, y: 0, width: 100, height: 50),
            report: CGRect(x: 0, y: 60, width: 100, height: 50)
        )
        #expect(near.control1.y == 50 + Tokens.dottedMinimumPull)
    }

    // MARK: Colours

    @Test("Cards: surface, muted dashed vacancy, dashed temporary line, accent focus and drop target")
    func appearance() {
        let plain = Tokens.cardAppearance(
            kind: .person,
            isDashed: false,
            isFocused: false,
            isDropTarget: false,
            isEnabled: true
        )
        #expect(plain.fill == \.bgSurface)
        #expect(plain.border == \.borderSubtle)
        #expect(!plain.isDashed)
        #expect(plain.halo == nil)
        #expect(plain.name == \.textDefault)

        let vacancy = Tokens.cardAppearance(
            kind: .vacancy,
            isDashed: false,
            isFocused: false,
            isDropTarget: false,
            isEnabled: true
        )
        #expect(vacancy.fill == \.bgMuted)
        #expect(vacancy.border == \.borderStrong)
        #expect(vacancy.isDashed)

        let dashed = Tokens.cardAppearance(
            kind: .person,
            isDashed: true,
            isFocused: false,
            isDropTarget: false,
            isEnabled: true
        )
        #expect(dashed.isDashed)
        #expect(dashed.fill == \.bgSurface)

        let focused = Tokens.cardAppearance(
            kind: .person,
            isDashed: false,
            isFocused: true,
            isDropTarget: false,
            isEnabled: true
        )
        #expect(focused.border == \.accentSolid)
        #expect(focused.halo == \.accentSubtle)

        let target = Tokens.cardAppearance(
            kind: .person,
            isDashed: false,
            isFocused: true,
            isDropTarget: true,
            isEnabled: true
        )
        #expect(target.fill == \.accentSubtle)
        #expect(target.border == \.accentStrong)

        let disabled = Tokens.cardAppearance(
            kind: .person,
            isDashed: false,
            isFocused: true,
            isDropTarget: false,
            isEnabled: false
        )
        #expect(disabled.name == \.textDisabled)
        #expect(disabled.secondary == \.textDisabled)
        #expect(disabled.border == \.borderSubtle)
        #expect(disabled.halo == nil)
    }

    // MARK: Accessibility

    @Test("A card is read with its title, meta and the facts its shape shows")
    func labelsRead() throws {
        #expect(
            Tokens.accessibilityLabel(try node("jo"), dottedManagers: [], labels: labels)
                == "Jo Park, Head of engineering"
        )
        #expect(
            Tokens.accessibilityLabel(try node("open"), dottedManagers: [], labels: labels)
                == "Product designer, Engineering, Open position"
        )
        #expect(
            Tokens.accessibilityLabel(try node("noor"), dottedManagers: [], labels: labels)
                == "Noor Ali, People lead, Acting, Temporary reporting line"
        )
        let managers = tree.dottedManagers(of: "mia", lines: MonarchOrgChartFixtures.dotted)
        #expect(managers == ["Jo Park"])
        #expect(
            Tokens.accessibilityLabel(try node("mia"), dottedManagers: managers, labels: labels)
                == "Mia Chen, Accountant, Also reports to Jo Park"
        )
        let custom = MonarchOrgChartLabels(vacant: "Poste ouvert", alsoReportsTo: { "Aussi \($0)" })
        #expect(
            Tokens.accessibilityLabel(try node("open"), dottedManagers: ["X"], labels: custom)
                == "Product designer, Engineering, Poste ouvert, Aussi X"
        )
        #expect(
            Tokens.accessibilityLabel(
                MonarchOrgChartNode(id: "x", name: "X", title: "  "),
                dottedManagers: [],
                labels: labels
            ) == "X"
        )
    }

    @Test("The value says whether the team is shown and its size")
    func valuesRead() {
        #expect(
            Tokens.accessibilityValue(
                reportCount: 3,
                hasReports: true,
                isExpanded: true,
                isLoading: false,
                labels: labels
            )
                == "Team shown, 3 direct reports"
        )
        #expect(
            Tokens.accessibilityValue(
                reportCount: 1,
                hasReports: true,
                isExpanded: false,
                isLoading: false,
                labels: labels
            )
                == "Team hidden, 1 direct report"
        )
        #expect(
            Tokens.accessibilityValue(
                reportCount: 2,
                hasReports: true,
                isExpanded: false,
                isLoading: true,
                labels: labels
            )
                == "Loading team, 2 direct reports"
        )
        #expect(
            Tokens.accessibilityValue(
                reportCount: 0,
                hasReports: false,
                isExpanded: false,
                isLoading: false,
                labels: labels
            )
            .isEmpty
        )
        #expect(
            Tokens.accessibilityValue(
                reportCount: 2,
                hasReports: true,
                isExpanded: true,
                isLoading: false,
                labels: MonarchOrgChartLabels(reports: { "\($0) personnes" }, expanded: "Ouvert")
            ) == "Ouvert, 2 personnes"
        )
    }

    @Test("The team action says what it does, and is absent for a leaf or while loading")
    func toggleAction() {
        #expect(
            Tokens.toggleActionName(hasReports: true, isExpanded: false, isLoading: false, labels: labels)
                == "Show team"
        )
        #expect(
            Tokens.toggleActionName(hasReports: true, isExpanded: true, isLoading: false, labels: labels) == "Hide team"
        )
        #expect(Tokens.toggleActionName(hasReports: true, isExpanded: false, isLoading: true, labels: labels) == nil)
        #expect(Tokens.toggleActionName(hasReports: false, isExpanded: false, isLoading: false, labels: labels) == nil)
        #expect(
            Tokens.toggleActionName(
                hasReports: true,
                isExpanded: false,
                isLoading: false,
                labels: MonarchOrgChartLabels(expand: "Ouvrir")
            ) == "Ouvrir"
        )
    }

    @Test("Built-in strings resolve")
    func strings() {
        let values = [
            MonarchOrgChartStrings.label, MonarchOrgChartStrings.vacant, MonarchOrgChartStrings.dashed,
            MonarchOrgChartStrings.alsoReportsTo("A"), MonarchOrgChartStrings.reports(1),
            MonarchOrgChartStrings.reports(4), MonarchOrgChartStrings.expanded, MonarchOrgChartStrings.collapsed,
            MonarchOrgChartStrings.loading, MonarchOrgChartStrings.expand, MonarchOrgChartStrings.collapse,
            MonarchOrgChartStrings.move, MonarchOrgChartStrings.moveHint, MonarchOrgChartStrings.loadFailed("A"),
            MonarchOrgChartStrings.zoomIn, MonarchOrgChartStrings.zoomOut, MonarchOrgChartStrings.zoomReset,
            MonarchOrgChartStrings.zoomLevel(100),
        ]
        for value in values {
            #expect(!value.hasPrefix("monarch.orgChart."))
        }
        #expect(MonarchOrgChartStrings.loadFailed("Ana") == "Could not load Ana’s team.")
        #expect(MonarchOrgChartStrings.reports(4) == "4 direct reports")
        #expect(MonarchOrgChartStrings.zoomLevel(100) == "100%")
    }
}
