/// Built-in strings of the marketing consent checkbox, from the
/// `MonarchMarketingConsentCheckbox` strings table.
enum MonarchMarketingConsentCheckboxStrings {
    private static let table = "MonarchMarketingConsentCheckbox"

    /// The question when one channel is asked about: "Send me product news by email".
    static func question(_ channel: MonarchMarketingChannel) -> String {
        MonarchStrings.string("monarch.marketingConsentCheckbox.question.\(channel.rawValue)", table: table)
    }

    /// The question above several channels.
    static var groupQuestion: String {
        MonarchStrings.string("monarch.marketingConsentCheckbox.groupQuestion", table: table)
    }

    /// The label of a channel's checkbox when several are asked about.
    static func channel(_ channel: MonarchMarketingChannel) -> String {
        MonarchStrings.string("monarch.marketingConsentCheckbox.channel.\(channel.rawValue)", table: table)
    }

    /// The note that consent can be withdrawn.
    static var unsubscribe: String {
        MonarchStrings.string("monarch.marketingConsentCheckbox.unsubscribe", table: table)
    }
}
