/// The actions a running timer offers at one moment, resolved from its
/// state and its callbacks. `nil` hides an action.
struct MonarchRunningTimerActions {
    /// The action in progress, if any.
    let busy: MonarchRunningTimerAction?
    /// Whether an action runs outside the component.
    let isPending: Bool
    /// Starts a timer.
    let start: (@MainActor () -> Void)?
    /// Whether Start can be pressed.
    let isStartEnabled: Bool
    /// Stops the timer and logs the time.
    let stop: (@MainActor () -> Void)?
    /// Opens the earlier-stop sheet.
    let stopEarlier: (@MainActor () -> Void)?
    /// Asks to confirm Discard.
    let discard: (@MainActor () -> Void)?
    /// Opens what is being timed.
    let open: (@MainActor () -> Void)?

    /// Whether any action is in progress, here or outside.
    var isBusy: Bool { isPending || busy != nil }

    /// Whether the indicator has a menu: when it can stop earlier or discard.
    var hasMenu: Bool { stopEarlier != nil || discard != nil }
}
