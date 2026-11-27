import Foundation

/// The stored proof of one consent decision.
public struct MonarchConsentRecord: Sendable, Hashable, Identifiable {
    /// The id of the decision.
    public let id: String
    /// When the decision was made.
    public let date: Date
    /// How the decision was made.
    public let method: MonarchConsentMethod
    /// The policy version the person saw.
    public let policyVersion: String
    /// The rules that applied.
    public let regime: MonarchConsentRegime
    /// Every category id with whether it was allowed.
    public let categories: [String: Bool]
    /// Whether a Global Privacy Control signal was present.
    public let isGlobalPrivacyControl: Bool

    /// A consent record.
    ///
    /// - Parameters:
    ///   - id: The id of the decision.
    ///   - date: When the decision was made.
    ///   - method: How the decision was made.
    ///   - policyVersion: The policy version the person saw.
    ///   - regime: The rules that applied.
    ///   - categories: Every category id with whether it was allowed.
    ///   - isGlobalPrivacyControl: Whether a Global Privacy Control signal was present.
    public init(
        id: String,
        date: Date,
        method: MonarchConsentMethod,
        policyVersion: String,
        regime: MonarchConsentRegime,
        categories: [String: Bool],
        isGlobalPrivacyControl: Bool = false
    ) {
        self.id = id
        self.date = date
        self.method = method
        self.policyVersion = policyVersion
        self.regime = regime
        self.categories = categories
        self.isGlobalPrivacyControl = isGlobalPrivacyControl
    }
}
