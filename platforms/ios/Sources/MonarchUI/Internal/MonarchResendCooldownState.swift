/// A resend with a cooldown: the send in flight, the seconds left before
/// another send, success and failure. Pure, so it is tested without a view.
struct MonarchResendCooldownState: Equatable, Sendable {
    /// A send is in flight.
    private(set) var isSending = false
    /// Whole seconds before another send is allowed; 0 when allowed.
    private(set) var remaining: Int
    /// The last send succeeded. Cleared by the next attempt.
    private(set) var didSend = false
    /// The last failure's message.
    private(set) var errorMessage: String?

    init(remaining: Int = 0) {
        self.remaining = max(0, remaining)
    }

    /// Whether a send is allowed now.
    var canSend: Bool { !isSending && remaining == 0 }

    /// Starts a send. Returns `false`, and changes nothing, while sending or
    /// cooling down.
    mutating func begin() -> Bool {
        guard canSend else { return false }
        isSending = true
        didSend = false
        errorMessage = nil
        return true
    }

    /// The send succeeded: the cooldown starts.
    mutating func succeed(cooldown: Int) {
        isSending = false
        didSend = true
        remaining = max(0, cooldown)
    }

    /// The send failed: no cooldown starts.
    mutating func fail(message: String) {
        isSending = false
        errorMessage = message
    }

    /// One second passed.
    mutating func tick() {
        remaining = max(0, remaining - 1)
    }
}
