/// Whether sending a request back in a ``MonarchApprovalQueue`` asks for a reason.
@_spi(Experimental) public enum MonarchApprovalQueueRejectReason: Sendable, Hashable, CaseIterable {
    /// A reason must be written. The default.
    case required
    /// A reason can be written.
    case optional
    /// No reason is asked; a confirmation is shown instead.
    case none
}
