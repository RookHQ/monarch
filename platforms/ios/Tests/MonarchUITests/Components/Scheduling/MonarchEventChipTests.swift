import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

/// A colour an app might give an event: the third data colour of the light theme.
private let eventColor = Color(monarch: MonarchTokens.light.colors.data3)

enum MonarchEventChipFixtures: ComponentFixtures {
    static let id = "event-chip"
    static let fixtures = [
        Fixture("dot statuses") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
                    MonarchEventChip("Stand-up", time: "09:30", status: status)
                }
            }
        },
        Fixture("bar statuses") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
                    MonarchEventChip(
                        "Design review",
                        time: "14:00 – 15:00",
                        detail: "Room 4",
                        status: status,
                        variant: .bar
                    )
                }
            }
        },
        Fixture("block statuses") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipStatus.allCases, id: \.self) { status in
                    MonarchEventChip(
                        "Workshop",
                        time: "10:00 – 12:00",
                        detail: "Lab",
                        status: status,
                        variant: .block
                    )
                    .frame(height: MonarchTokens.Space.space24)
                }
            }
        },
        Fixture("compact block") {
            MonarchEventChip("Short call", time: "16:00", detail: "Phone", variant: .block, isCompact: true)
                .frame(height: MonarchTokens.Space.space8)
        },
        Fixture("past") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipVariant.allCases, id: \.self) { variant in
                    MonarchEventChip("Retrospective", time: "08:00", variant: variant, isPast: true)
                        .frame(minHeight: MonarchTokens.Space.space8)
                }
            }
        },
        Fixture("event colour") {
            VStack(alignment: .leading) {
                MonarchEventChip("Dentist", time: "11:00", color: eventColor)
                MonarchEventChip("Dentist", time: "11:00", color: eventColor, variant: .bar)
                MonarchEventChip("Dentist", time: "11:00", color: eventColor, status: .tentative, variant: .bar)
            }
        },
        Fixture("as a button") {
            VStack(alignment: .leading) {
                ForEach(MonarchEventChipVariant.allCases, id: \.self) { variant in
                    MonarchEventChip("Planning", time: "13:00", variant: variant) {}
                }
                MonarchEventChip("Planning", time: "13:00", variant: .bar) {}
                    .disabled(true)
            }
        },
        Fixture("no time, long text") {
            VStack(alignment: .leading) {
                MonarchEventChip("Company holiday")
                MonarchEventChip(
                    "Quarterly planning with every team lead and the people who could not attend last time",
                    time: "09:00 – 17:30",
                    timeLabel: "9 in the morning to half past 5",
                    detail: "The large meeting room on the second floor, next to the kitchen",
                    status: .tentative,
                    variant: .bar,
                    statusLabel: "Waiting for an answer"
                )
                MonarchEventChip("", variant: .bar)
            }
        },
    ]
}

@Suite("MonarchEventChip")
@MainActor
struct MonarchEventChipTests {
    private func label(
        _ title: String = "Stand-up",
        time: String? = "09:30",
        timeLabel: String? = nil,
        status: MonarchEventChipStatus = .confirmed,
        statusLabel: String = "Tentative",
        detail: String? = nil,
        variant: MonarchEventChipVariant = .dot
    ) -> String {
        MonarchEventChipTokens.accessibilityLabel(
            title: title,
            time: time,
            timeLabel: timeLabel,
            status: status,
            statusLabel: statusLabel,
            detail: detail,
            variant: variant
        )
    }

    @Test("The spoken name is the title and the time")
    func spokenName() {
        #expect(label() == "Stand-up, 09:30")
        #expect(label(time: nil) == "Stand-up")
        #expect(label(time: "") == "Stand-up")
    }

    @Test("The long time replaces the visible time in the spoken name")
    func spokenTime() {
        #expect(label(timeLabel: "9:30 to 10:15") == "Stand-up, 9:30 to 10:15")
        #expect(label(time: nil, timeLabel: "All day") == "Stand-up, All day")
    }

    @Test("The status is spoken unless confirmed, in every variant")
    func spokenStatus() {
        #expect(label(status: .confirmed, statusLabel: "Confirmed") == "Stand-up, 09:30")
        for variant in MonarchEventChipVariant.allCases {
            #expect(label(status: .tentative, variant: variant) == "Stand-up, 09:30, Tentative")
        }
        #expect(label(status: .cancelled, statusLabel: "Called off") == "Stand-up, 09:30, Called off")
    }

    @Test("The detail is spoken in bar and block chips only")
    func spokenDetail() {
        #expect(label(detail: "Room 4", variant: .dot) == "Stand-up, 09:30")
        #expect(label(detail: "Room 4", variant: .bar) == "Stand-up, 09:30, Room 4")
        #expect(
            label(status: .declined, statusLabel: "Declined", detail: "Room 4", variant: .block)
                == "Stand-up, 09:30, Declined, Room 4"
        )
        #expect(label(detail: "", variant: .bar) == "Stand-up, 09:30")
    }

    @Test("The status names come from the component's strings table")
    func strings() {
        #expect(MonarchEventChipStrings.status(.confirmed) == "Confirmed")
        #expect(MonarchEventChipStrings.status(.tentative) == "Tentative")
        #expect(MonarchEventChipStrings.status(.cancelled) == "Cancelled")
        #expect(MonarchEventChipStrings.status(.declined) == "Declined")
    }

    @Test("Cancelled, declined and past events have a muted title")
    func titleColour() {
        #expect(MonarchEventChipTokens.titleRole(status: .confirmed, isPast: false) == \MonarchColorTokens.textDefault)
        #expect(MonarchEventChipTokens.titleRole(status: .tentative, isPast: false) == \MonarchColorTokens.textDefault)
        #expect(MonarchEventChipTokens.titleRole(status: .cancelled, isPast: false) == \MonarchColorTokens.textMuted)
        #expect(MonarchEventChipTokens.titleRole(status: .declined, isPast: false) == \MonarchColorTokens.textMuted)
        #expect(MonarchEventChipTokens.titleRole(status: .confirmed, isPast: true) == \MonarchColorTokens.textMuted)
    }

    @Test("Only cancelled and declined titles are struck through")
    func strikethrough() {
        #expect(!MonarchEventChipTokens.isStruckThrough(.confirmed))
        #expect(!MonarchEventChipTokens.isStruckThrough(.tentative))
        #expect(MonarchEventChipTokens.isStruckThrough(.cancelled))
        #expect(MonarchEventChipTokens.isStruckThrough(.declined))
    }

    @Test("The marker shows the status without colour: solid, hollow or dashed, muted")
    func marker() {
        for variant in MonarchEventChipVariant.allCases {
            #expect(MonarchEventChipTokens.marker(status: .confirmed, variant: variant) == .filled)
            #expect(MonarchEventChipTokens.marker(status: .cancelled, variant: variant) == .muted)
            #expect(MonarchEventChipTokens.marker(status: .declined, variant: variant) == .muted)
        }
        #expect(MonarchEventChipTokens.marker(status: .tentative, variant: .dot) == .hollow)
        #expect(MonarchEventChipTokens.marker(status: .tentative, variant: .bar) == .dashed)
        #expect(MonarchEventChipTokens.marker(status: .tentative, variant: .block) == .dashed)
    }

    @Test("A dot chip has no fill until it is pressed")
    func dotSurface() {
        for status in MonarchEventChipStatus.allCases {
            let rest = MonarchEventChipTokens.surface(variant: .dot, status: status, isPressed: false)
            #expect(rest == MonarchEventChipSurface(base: nil, tint: 0, isOutlined: false))
            let pressed = MonarchEventChipTokens.surface(variant: .dot, status: status, isPressed: true)
            #expect(pressed.base == \MonarchColorTokens.bgMuted)
            #expect(pressed.tint == 0)
        }
    }

    @Test("Bar and block chips are tinted with the event colour", arguments: [MonarchEventChipVariant.bar, .block])
    func tintedSurface(variant: MonarchEventChipVariant) {
        let confirmed = MonarchEventChipTokens.surface(variant: variant, status: .confirmed, isPressed: false)
        #expect(confirmed.base == \MonarchColorTokens.bgSurface)
        #expect(confirmed.tint == MonarchEventChipTokens.tintRatio)
        #expect(!confirmed.isOutlined)

        let tentative = MonarchEventChipTokens.surface(variant: variant, status: .tentative, isPressed: false)
        #expect(tentative.tint == MonarchEventChipTokens.tentativeTintRatio)
        #expect(tentative.tint < confirmed.tint)
        #expect(tentative.isOutlined)

        for status in [MonarchEventChipStatus.cancelled, .declined] {
            let inactive = MonarchEventChipTokens.surface(variant: variant, status: status, isPressed: false)
            #expect(inactive == MonarchEventChipSurface(base: \.bgMuted, tint: 0, isOutlined: false))
        }
    }

    @Test("Pressing changes the fill of every variant and status")
    func pressedSurface() {
        for variant in MonarchEventChipVariant.allCases {
            for status in MonarchEventChipStatus.allCases {
                let rest = MonarchEventChipTokens.surface(variant: variant, status: status, isPressed: false)
                let pressed = MonarchEventChipTokens.surface(variant: variant, status: status, isPressed: true)
                #expect(rest != pressed, "\(variant) \(status) has no pressed state")
                #expect(rest.isOutlined == pressed.isOutlined)
            }
        }
    }

    @Test("The status word is drawn in bar and regular block chips")
    func visibleStatus() {
        #expect(!MonarchEventChipTokens.showsStatus(status: .confirmed, variant: .bar, isCompact: false))
        #expect(!MonarchEventChipTokens.showsStatus(status: .tentative, variant: .dot, isCompact: false))
        #expect(MonarchEventChipTokens.showsStatus(status: .tentative, variant: .bar, isCompact: false))
        #expect(MonarchEventChipTokens.showsStatus(status: .tentative, variant: .bar, isCompact: true))
        #expect(MonarchEventChipTokens.showsStatus(status: .cancelled, variant: .block, isCompact: false))
        #expect(!MonarchEventChipTokens.showsStatus(status: .cancelled, variant: .block, isCompact: true))
    }

    @Test("The detail is drawn in bar and regular block chips")
    func visibleDetail() {
        #expect(!MonarchEventChipTokens.showsDetail("Room 4", variant: .dot, isCompact: false))
        #expect(MonarchEventChipTokens.showsDetail("Room 4", variant: .bar, isCompact: false))
        #expect(MonarchEventChipTokens.showsDetail("Room 4", variant: .block, isCompact: false))
        #expect(!MonarchEventChipTokens.showsDetail("Room 4", variant: .block, isCompact: true))
        #expect(!MonarchEventChipTokens.showsDetail(nil, variant: .bar, isCompact: false))
        #expect(!MonarchEventChipTokens.showsDetail("", variant: .bar, isCompact: false))
    }

    @Test("The corner follows the variant")
    func shape() {
        #expect(MonarchEventChipTokens.shape(.dot).cornerSize.width == CGFloat(MonarchTokens.Radius.xs))
        #expect(MonarchEventChipTokens.shape(.bar).cornerSize.width == CGFloat(MonarchTokens.Radius.sm))
        #expect(MonarchEventChipTokens.shape(.block).cornerSize.width == CGFloat(MonarchTokens.Radius.sm))
    }

    #if os(iOS)
        @Test("A chip with an action has a touch target of at least 44 points in height")
        func touchTarget() {
            let size = Host.layout(MonarchEventChip("Stand-up", time: "09:30") {})
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
