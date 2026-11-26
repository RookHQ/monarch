/// What the agent in ``MonarchAiComputerView`` operates. It only changes the wording.
@_spi(Experimental) public enum MonarchAiComputerViewSurface: String, Sendable, Hashable, CaseIterable {
    /// A web browser.
    case browser
    /// A whole computer.
    case computer
}
