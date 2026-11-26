import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchDragHandleFixtures: ComponentFixtures {
    static let id = "drag-handle"
    static let fixtures = [
        Fixture("states") {
            HStack {
                ForEach(MonarchDragHandleState.allCases, id: \.self) { state in
                    MonarchDragHandle("Reorder Design review", state: state) { _ in }
                }
                MonarchDragHandle("Reorder Design review", state: .grabbed).disabled(true)
            }
        },
        Fixture("axes and sizes") {
            VStack {
                ForEach(MonarchDragHandleSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach(MonarchDragHandleAxis.allCases, id: \.self) { axis in
                            MonarchDragHandle("Reorder", axis: axis, size: size) { _ in }
                        }
                    }
                }
            }
        },
        Fixture("in a row, with a hint and overridden strings") {
            HStack {
                MonarchDragHandle(
                    "Reorder Quarterly planning",
                    state: .dropped,
                    hint: "Use the actions to move this row.",
                    actionLabels: [.up: "Earlier", .down: "Later"],
                    stateLabels: [.dropped: "Placed"]
                ) { _ in }
                Text("Quarterly planning with a title long enough to wrap onto a second line")
            }
        },
    ]
}

@Suite("MonarchDragHandle")
struct MonarchDragHandleTests {
    typealias Tokens = MonarchDragHandleTokens

    @Test("At rest the handle is muted with no fill")
    func idle() {
        let idle = Tokens.appearance(state: .idle, isEnabled: true)
        #expect(idle.background == nil)
        #expect(idle.border == nil)
        #expect(idle.foreground == \MonarchColorTokens.textMuted)
    }

    @Test("A grabbed handle uses the accent and gains an outline, so the state is not colour alone")
    func grabbed() {
        let grabbed = Tokens.appearance(state: .grabbed, isEnabled: true)
        #expect(grabbed.background == \MonarchColorTokens.accentSubtle)
        #expect(grabbed.foreground == \MonarchColorTokens.accentStrong)
        #expect(grabbed.border == \MonarchColorTokens.accentStrong)
    }

    @Test("A dropped handle uses the success roles")
    func dropped() {
        let dropped = Tokens.appearance(state: .dropped, isEnabled: true)
        #expect(dropped.background == \MonarchColorTokens.successSubtle)
        #expect(dropped.foreground == \MonarchColorTokens.successStrong)
        #expect(dropped.border == nil)
    }

    @Test("A disabled handle looks the same in every state", arguments: MonarchDragHandleState.allCases)
    func disabled(state: MonarchDragHandleState) {
        let disabled = Tokens.appearance(state: state, isEnabled: false)
        #expect(disabled == MonarchSurfaceAppearance(background: nil, foreground: \.textDisabled, border: nil))
    }

    @Test("Each axis offers the move actions of its directions, in order")
    func directions() {
        #expect(Tokens.directions(axis: .vertical) == [.up, .down])
        #expect(Tokens.directions(axis: .horizontal) == [.backward, .forward])
        #expect(Tokens.directions(axis: .both) == [.up, .down, .backward, .forward])
    }

    @Test("Action names come from the strings table unless overridden")
    func actionNames() {
        #expect(Tokens.actionName(.up) == "Move up")
        #expect(Tokens.actionName(.down) == "Move down")
        #expect(Tokens.actionName(.backward) == "Move backward")
        #expect(Tokens.actionName(.forward) == "Move forward")
        #expect(Tokens.actionName(.up, overrides: [.up: "Earlier"]) == "Earlier")
        #expect(Tokens.actionName(.down, overrides: [.up: "Earlier"]) == "Move down")
        #expect(Tokens.actionName(.up, overrides: [.up: ""]) == "Move up")
    }

    @Test("The state is spoken as the value, and a handle at rest has none")
    func accessibilityValue() {
        #expect(Tokens.accessibilityValue(state: .idle).isEmpty)
        #expect(Tokens.accessibilityValue(state: .idle, overrides: [.idle: "Resting"]).isEmpty)
        #expect(Tokens.accessibilityValue(state: .grabbed) == "Picked up")
        #expect(Tokens.accessibilityValue(state: .dropped) == "Dropped")
        #expect(Tokens.accessibilityValue(state: .dropped, overrides: [.dropped: "Placed"]) == "Placed")
        #expect(Tokens.accessibilityValue(state: .grabbed, overrides: [.dropped: "Placed"]) == "Picked up")
    }

    @Test("The grip has six dots along one axis and nine for both, inside the glyph and symmetric")
    func dots() {
        #expect(Tokens.dots(axis: .vertical).count == 6)
        #expect(Tokens.dots(axis: .horizontal).count == 6)
        #expect(Tokens.dots(axis: .both).count == 9)
        #expect(Set(Tokens.dots(axis: .vertical).map(\.x)).count == 2)
        #expect(Set(Tokens.dots(axis: .horizontal).map(\.y)).count == 2)
        for axis in MonarchDragHandleAxis.allCases {
            let dots = Tokens.dots(axis: axis)
            for dot in dots {
                #expect(dot.x - Tokens.dotRadius >= 0 && dot.x + Tokens.dotRadius <= Tokens.gridUnits)
                #expect(dot.y - Tokens.dotRadius >= 0 && dot.y + Tokens.dotRadius <= Tokens.gridUnits)
                // Mirrored across the vertical centre line there is another dot: no RTL variant is needed.
                #expect(dots.contains(CGPoint(x: Tokens.gridUnits - dot.x, y: dot.y)))
            }
        }
    }

    @Test("The grip path stays inside the rectangle it is given", arguments: MonarchDragHandleAxis.allCases)
    func gripPath(axis: MonarchDragHandleAxis) {
        let rect = CGRect(x: 10, y: 20, width: 32, height: 32)
        let bounds = MonarchDragHandleGrip(axis: axis).path(in: rect).boundingRect
        #expect(!bounds.isEmpty)
        #expect(rect.insetBy(dx: -0.001, dy: -0.001).contains(bounds))
        #expect(abs(bounds.midX - rect.midX) < 0.001 && abs(bounds.midY - rect.midY) < 0.001)
    }

    @Test("Sizes come from the tokens")
    func sizes() {
        #expect(Tokens.boxSize(.sm) == 24)
        #expect(Tokens.boxSize(.md) == 32)
        #expect(Tokens.glyphSize == 16)
    }

    @Test("Both sizes keep a 44-point touch target", arguments: MonarchDragHandleSize.allCases)
    @MainActor
    func hitTarget(size: MonarchDragHandleSize) {
        let layout = Host.layout(MonarchDragHandle("Reorder", size: size).fixedSize())
        #expect(layout.height >= MonarchAccessibility.minimumHitTarget)
    }
}
