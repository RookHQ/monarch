/// Replacements for the built-in strings of ``MonarchAiComputerView``.
///
/// Every property is `nil` by default, which keeps the built-in string.
/// Closures that receive a surface word their text for a browser or a computer.
@_spi(Experimental) public struct MonarchAiComputerViewLabels: Sendable {
    /// The spoken name of the whole view. Built in: "Assistant browser view".
    public var label: (@Sendable (MonarchAiComputerViewSurface) -> String)?
    /// The name of a status. Built in: "Working", "Waiting for you"…
    public var status: (@Sendable (MonarchAiComputerViewStatus) -> String)?
    /// The spoken name of an action kind, read before its label. Built in: "Click", "Type"…
    public var kind: (@Sendable (MonarchAiComputerActionKind) -> String)?
    /// The banner while the agent holds control. Built in: "The assistant is controlling this browser".
    public var agentInControl: (@Sendable (MonarchAiComputerViewSurface) -> String)?
    /// The banner while the person holds control. Built in: "You are controlling this browser".
    public var youInControl: (@Sendable (MonarchAiComputerViewSurface) -> String)?
    /// The title of the notice while the agent waits. Built in: "The assistant needs you to continue".
    public var waitingTitle: String?
    /// The reason shown when `waitingReason` is not given. Built in: "This step is one the assistant must not do itself."
    public var waitingFallback: String?
    /// The pause button. Built in: "Pause".
    public var pause: String?
    /// The resume button. Built in: "Resume".
    public var resume: String?
    /// The take over button. Built in: "Take over".
    public var takeOver: String?
    /// The hand back button. Built in: "Hand back".
    public var handBack: String?
    /// The stop button. Built in: "Stop".
    public var stop: String?
    /// The question of the stop confirmation. Built in: "Stop the assistant? It will not finish the task."
    public var stopQuestion: String?
    /// The button that confirms stopping. Built in: "Stop now".
    public var stopConfirm: String?
    /// The button that keeps the agent going. Built in: "Keep going".
    public var stopCancel: String?
    /// The heading and spoken name of the log. Built in: "Actions".
    public var logLabel: String?
    /// The disclosure of the compact log, with the number of actions. Built in: "Actions (12)".
    public var logToggle: (@Sendable (Int) -> String)?
    /// Shown when there are no actions. Built in: "No actions yet."
    public var empty: String?
    /// Shown in place of a withheld detail. Built in: "Hidden".
    public var hidden: String?
    /// Shown and announced in place of a sensitive action's label. Built in: "Private action".
    public var sensitiveLabel: String?
    /// Shown before the first screenshot. Built in: "No screenshot yet".
    public var noFrame: String?
    /// Shown when the screenshot address is not allowed. Built in: "Screenshot unavailable".
    public var frameUnavailable: String?
    /// Shown when the screenshot may show private information. Built in: "Screenshot hidden: it may show private information".
    public var frameHidden: String?
    /// The description of a screenshot without `alt`. Built in: "Screenshot of the browser the assistant is using".
    public var frameAlt: (@Sendable (MonarchAiComputerViewSurface) -> String)?
    /// Joins the screenshot description and the latest action. Built in: "<description>. Latest action: <action>".
    public var latestAction: (@Sendable (String, String) -> String)?
    /// The caption under the screenshot, from the formatted time. Built in: "Screenshot from 10:42:05".
    public var capturedAt: (@Sendable (String) -> String)?
    /// Read before the address. Built in: "Address".
    public var address: String?
    /// The button and accessibility action that enlarge the screenshot. Built in: "Zoom screenshot".
    public var zoom: String?
    /// The button that shows older actions. Built in: "Show 3 earlier actions".
    public var showEarlier: (@Sendable (Int) -> String)?
    /// Announced when several actions arrived together. Built in: "3 new actions. Latest: <action>".
    public var announceActions: (@Sendable (Int, String) -> String)?
    /// Announced when the status changes. Built in: "Assistant status: <status>".
    public var announceStatus: (@Sendable (String) -> String)?
    /// Announced when the person takes control. Built in: "You are in control of this browser. The assistant has stopped acting."
    public var announceTakenOver: (@Sendable (MonarchAiComputerViewSurface) -> String)?
    /// Announced when control goes back to the agent. Built in: "The assistant is in control of this browser again."
    public var announceReleased: (@Sendable (MonarchAiComputerViewSurface) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil,
        status: (@Sendable (MonarchAiComputerViewStatus) -> String)? = nil,
        kind: (@Sendable (MonarchAiComputerActionKind) -> String)? = nil,
        agentInControl: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil,
        youInControl: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil,
        waitingTitle: String? = nil,
        waitingFallback: String? = nil,
        pause: String? = nil,
        resume: String? = nil,
        takeOver: String? = nil,
        handBack: String? = nil,
        stop: String? = nil,
        stopQuestion: String? = nil,
        stopConfirm: String? = nil,
        stopCancel: String? = nil,
        logLabel: String? = nil,
        logToggle: (@Sendable (Int) -> String)? = nil,
        empty: String? = nil,
        hidden: String? = nil,
        sensitiveLabel: String? = nil,
        noFrame: String? = nil,
        frameUnavailable: String? = nil,
        frameHidden: String? = nil,
        frameAlt: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil,
        latestAction: (@Sendable (String, String) -> String)? = nil,
        capturedAt: (@Sendable (String) -> String)? = nil,
        address: String? = nil,
        zoom: String? = nil,
        showEarlier: (@Sendable (Int) -> String)? = nil,
        announceActions: (@Sendable (Int, String) -> String)? = nil,
        announceStatus: (@Sendable (String) -> String)? = nil,
        announceTakenOver: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil,
        announceReleased: (@Sendable (MonarchAiComputerViewSurface) -> String)? = nil
    ) {
        self.label = label
        self.status = status
        self.kind = kind
        self.agentInControl = agentInControl
        self.youInControl = youInControl
        self.waitingTitle = waitingTitle
        self.waitingFallback = waitingFallback
        self.pause = pause
        self.resume = resume
        self.takeOver = takeOver
        self.handBack = handBack
        self.stop = stop
        self.stopQuestion = stopQuestion
        self.stopConfirm = stopConfirm
        self.stopCancel = stopCancel
        self.logLabel = logLabel
        self.logToggle = logToggle
        self.empty = empty
        self.hidden = hidden
        self.sensitiveLabel = sensitiveLabel
        self.noFrame = noFrame
        self.frameUnavailable = frameUnavailable
        self.frameHidden = frameHidden
        self.frameAlt = frameAlt
        self.latestAction = latestAction
        self.capturedAt = capturedAt
        self.address = address
        self.zoom = zoom
        self.showEarlier = showEarlier
        self.announceActions = announceActions
        self.announceStatus = announceStatus
        self.announceTakenOver = announceTakenOver
        self.announceReleased = announceReleased
    }
}
