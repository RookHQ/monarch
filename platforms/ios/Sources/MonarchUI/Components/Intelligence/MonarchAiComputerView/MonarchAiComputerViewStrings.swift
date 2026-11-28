/// Built-in strings of the computer view, from the `MonarchAiComputerView` strings table.
enum MonarchAiComputerViewStrings {
    private static let table = "MonarchAiComputerView"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiComputerView.\(key)", table: table)
    }

    private static func string(_ key: String, _ argument: any CVarArg) -> String {
        MonarchStrings.string("monarch.aiComputerView.\(key)", table: table, argument)
    }

    /// The spoken name of the view.
    static func label(_ surface: MonarchAiComputerViewSurface) -> String { string("label.\(surface.rawValue)") }
    /// The name of a status.
    static func status(_ status: MonarchAiComputerViewStatus) -> String { string("status.\(status.rawValue)") }
    /// The name of an action kind.
    static func kind(_ kind: MonarchAiComputerActionKind) -> String { string("kind.\(kind.rawValue)") }
    /// The banner while the agent holds control.
    static func agentInControl(_ surface: MonarchAiComputerViewSurface) -> String {
        string("agentInControl.\(surface.rawValue)")
    }
    /// The banner while the person holds control.
    static func youInControl(_ surface: MonarchAiComputerViewSurface) -> String {
        string("youInControl.\(surface.rawValue)")
    }
    /// The waiting notice title.
    static var waitingTitle: String { string("waitingTitle") }
    /// The waiting reason without one from the app.
    static var waitingFallback: String { string("waitingFallback") }
    /// Pause.
    static var pause: String { string("pause") }
    /// Resume.
    static var resume: String { string("resume") }
    /// Take over.
    static var takeOver: String { string("takeOver") }
    /// Hand back.
    static var handBack: String { string("handBack") }
    /// Stop.
    static var stop: String { string("stop") }
    /// The stop question.
    static var stopQuestion: String { string("stopQuestion") }
    /// Confirms stopping.
    static var stopConfirm: String { string("stopConfirm") }
    /// Cancels stopping.
    static var stopCancel: String { string("stopCancel") }
    /// The log heading.
    static var logLabel: String { string("logLabel") }
    /// The compact log disclosure.
    static func logToggle(_ count: Int) -> String { string("logToggle", count) }
    /// No actions yet.
    static var empty: String { string("empty") }
    /// A withheld detail.
    static var hidden: String { string("hidden") }
    /// A sensitive action's label.
    static var sensitiveLabel: String { string("sensitiveLabel") }
    /// Before the first screenshot.
    static var noFrame: String { string("noFrame") }
    /// A refused screenshot address.
    static var frameUnavailable: String { string("frameUnavailable") }
    /// A hidden screenshot.
    static var frameHidden: String { string("frameHidden") }
    /// The description of a screenshot without one.
    static func frameAlt(_ surface: MonarchAiComputerViewSurface) -> String { string("frameAlt.\(surface.rawValue)") }
    /// The description and the latest action.
    static func latestAction(_ description: String, _ action: String) -> String {
        MonarchStrings.string("monarch.aiComputerView.latestAction", table: table, description, action)
    }
    /// The capture caption.
    static func capturedAt(_ time: String) -> String { string("capturedAt", time) }
    /// Before the address.
    static var address: String { string("address") }
    /// The zoom button.
    static var zoom: String { string("zoom") }
    /// The button that shows older actions.
    static func showEarlier(_ count: Int) -> String {
        count == 1 ? string("showEarlierOne") : string("showEarlierOther", count)
    }
    /// Several new actions.
    static func announceActions(_ count: Int, _ latest: String) -> String {
        MonarchStrings.string("monarch.aiComputerView.announceActions", table: table, count, latest)
    }
    /// A status change.
    static func announceStatus(_ status: String) -> String { string("announceStatus", status) }
    /// The person took control.
    static func announceTakenOver(_ surface: MonarchAiComputerViewSurface) -> String {
        string("announceTakenOver.\(surface.rawValue)")
    }
    /// Control went back to the agent.
    static func announceReleased(_ surface: MonarchAiComputerViewSurface) -> String {
        string("announceReleased.\(surface.rawValue)")
    }
}
