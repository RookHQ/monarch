import Foundation

/// A person's choice about marketing messages, with the wording they saw, as proof of consent.
public struct MonarchMarketingConsent: Sendable, Hashable {
    /// One entry for each channel that was shown: `true` when ticked.
    public let channels: [MonarchMarketingChannel: Bool]
    /// The exact wording shown: the question, the channel labels, the
    /// description, the unsubscribe note and the title of the policy link,
    /// in reading order, with whitespace collapsed.
    public let text: String
    /// When the choice changed.
    public let date: Date

    /// A consent record.
    public init(channels: [MonarchMarketingChannel: Bool], text: String, date: Date) {
        self.channels = channels
        self.text = text
        self.date = date
    }

    /// The channels the person agreed to.
    public var granted: Set<MonarchMarketingChannel> {
        Set(channels.filter(\.value).keys)
    }
}
