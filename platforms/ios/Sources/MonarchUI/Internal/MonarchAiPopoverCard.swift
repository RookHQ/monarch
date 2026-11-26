import SwiftUI

private struct MonarchAiPopoverCardKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether an AI tray or prompt draws as an inline, bordered card (the web popover) instead of sheet content.
    var monarchAiPopoverCard: Bool {
        get { self[MonarchAiPopoverCardKey.self] }
        set { self[MonarchAiPopoverCardKey.self] = newValue }
    }
}

extension View {
    /// Draws a Monarch AI tray or prompt, such as ``MonarchAiBackgroundTasks`` or ``MonarchAiInlinePrompt``, as an
    /// inline bordered card with its own heading, the way the web shows it in a popover, instead of as the content
    /// of a sheet. Use it to place the surface under its trigger on a screen that is not a sheet.
    public func monarchAiPopoverCard(_ isOn: Bool = true) -> some View {
        environment(\.monarchAiPopoverCard, isOn)
    }
}
