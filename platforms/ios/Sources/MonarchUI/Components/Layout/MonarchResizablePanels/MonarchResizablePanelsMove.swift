/// One step of the separator, from the keyboard or from VoiceOver.
enum MonarchResizablePanelsMove: Sendable, Hashable, CaseIterable {
    /// Makes the first panel smaller by one step.
    case decrease
    /// Makes the first panel larger by one step.
    case increase
    /// Makes the first panel as small as the range allows.
    case minimum
    /// Makes the first panel as large as the range allows.
    case maximum
}
