import MonarchTokens
import SwiftUI

/// A saved payment card: network, last four digits, expiry and holder, a
/// Default badge, and actions to make it the default or remove it after a
/// confirmation.
///
/// ```swift
/// MonarchPaymentMethodCard(
///     brand: "visa",
///     last4: "4242",
///     expiryMonth: 8,
///     expiryYear: 2027,
///     holderName: "Ana Silva",
///     onMakeDefault: { try await billing.makeDefault(card) },
///     onRemove: { try await billing.remove(card) }
/// )
/// ```
///
/// It only displays a card. It never shows or collects a full card number:
/// adding a card belongs to the payment provider's own SDK or hosted form.
/// Each action appears only when its closure is given. While an action runs
/// its button shows progress; when it throws, the error is shown under the
/// card with Try again.
///
/// Accessibility: the name, badges, holder and expiry are read as one
/// heading. Expiry is a badge with text and an icon, not only a colour.
/// Each action is read as its title and the card ("Remove, Visa ending in
/// 4242"), so the buttons of a list of cards can be told apart. Removal asks
/// for confirmation in a system alert. A failure is announced.
public struct MonarchPaymentMethodCard<Actions: View>: View {
    private let brand: String
    private let last4: String
    private let expiryMonth: Int?
    private let expiryYear: Int?
    private let holderName: String?
    private let brandImage: Image?
    private let expiry: MonarchPaymentMethodCardExpiry?
    private let now: Date
    private let isDefault: Bool
    private let isRemoveDisabled: Bool
    private let labels: MonarchPaymentMethodCardLabels
    private let onMakeDefault: (@MainActor () async throws -> Void)?
    private let onRemove: (@MainActor () async throws -> Void)?
    private let actions: Actions

    @State private var running: Action?
    @State private var failure: (action: Action, message: String)?
    @State private var isConfirmingRemoval = false

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var iconSize: CGFloat = MonarchTokens.Space.space8

    private enum Action: Equatable {
        case makeDefault
        case remove
    }

    /// A saved card with actions of the app's own after Make default and Remove.
    ///
    /// - Parameters:
    ///   - brand: A network id (`visa`, `mastercard`, `amex`, `rupay`,
    ///     `discover`, `diners`, `jcb`, `unionpay`, `maestro`) or any
    ///     display name, already localised.
    ///   - last4: The last four digits, as the payment provider returns them.
    ///   - expiryMonth: The expiry month, 1 to 12.
    ///   - expiryYear: The expiry year, with two or four digits.
    ///   - holderName: The cardholder's name.
    ///   - brandImage: The network's logo. Default: a card symbol. Decorative.
    ///   - expiry: Replaces the status worked out from the date, when the
    ///     payment provider already says it.
    ///   - now: Today, for working out the status.
    ///   - isDefault: Whether this method is charged by default. Shows a
    ///     Default badge and hides Make default.
    ///   - isRemoveDisabled: Disables Remove, for example on the only method
    ///     of an active subscription.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMakeDefault: Makes the card the default. Shows Make default on a
    ///     card that is not the default. Throw to show the error.
    ///   - onRemove: Removes the card after confirmation. Shows Remove. Throw
    ///     to show the error.
    ///   - actions: Further buttons after Make default and Remove.
    public init(
        brand: String,
        last4: String,
        expiryMonth: Int? = nil,
        expiryYear: Int? = nil,
        holderName: String? = nil,
        brandImage: Image? = nil,
        expiry: MonarchPaymentMethodCardExpiry? = nil,
        now: Date = .now,
        isDefault: Bool = false,
        isRemoveDisabled: Bool = false,
        labels: MonarchPaymentMethodCardLabels = MonarchPaymentMethodCardLabels(),
        onMakeDefault: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.brand = brand
        self.last4 = last4
        self.expiryMonth = expiryMonth
        self.expiryYear = expiryYear
        self.holderName = holderName
        self.brandImage = brandImage
        self.expiry = expiry
        self.now = now
        self.isDefault = isDefault
        self.isRemoveDisabled = isRemoveDisabled
        self.labels = labels
        self.onMakeDefault = onMakeDefault
        self.onRemove = onRemove
        self.actions = actions()
    }

    private var name: String {
        MonarchPaymentMethodCardTokens.name(brand: brand, last4: last4, labels: labels)
    }

    private var status: MonarchPaymentMethodCardExpiry {
        MonarchPaymentMethodCardTokens.status(month: expiryMonth, year: expiryYear, override: expiry, now: now)
    }

    /// The content of the view.
    public var body: some View {
        MonarchCard {
            HStack(alignment: .top, spacing: MonarchPaymentMethodCardTokens.iconSpacing) {
                icon
                details
            }
            buttons
            if let failure {
                MonarchInlineErrorRetry(failure.message) {
                    run(failure.action)
                }
            }
        }
        .overlay {
            // The default method's border is the accent's, as on the web.
            if isDefault {
                RoundedRectangle(cornerRadius: MonarchCardTokens.cornerRadius, style: .continuous)
                    .strokeBorder(theme.colors.accentBorder, lineWidth: 1)
                    .allowsHitTesting(false)
            }
        }
        .alert(
            labels.removeTitle?(name) ?? MonarchPaymentMethodCardStrings.removeTitle(name),
            isPresented: $isConfirmingRemoval
        ) {
            Button(labels.removeConfirm ?? MonarchPaymentMethodCardStrings.removeConfirm, role: .destructive) {
                run(.remove)
            }
            Button(labels.cancel ?? MonarchPaymentMethodCardStrings.cancel, role: .cancel) {}
        } message: {
            Text(labels.removeMessage ?? MonarchPaymentMethodCardStrings.removeMessage)
        }
    }

    @ViewBuilder private var icon: some View {
        Group {
            if let brandImage {
                brandImage.resizable().scaledToFit()
            } else {
                Image(systemName: MonarchPaymentMethodCardTokens.cardSymbol).resizable().scaledToFit()
            }
        }
        .padding(MonarchTokens.Space.space1_5)
        .frame(width: iconSize * 1.375, height: iconSize)
        .background(
            theme.colors.bgSubtle,
            in: RoundedRectangle(cornerRadius: MonarchTokens.Radius.sm, style: .continuous)
        )
        .foregroundStyle(theme.colors[role: MonarchPaymentMethodCardTokens.iconColor])
        .accessibilityHidden(true)
    }

    private var details: some View {
        let status = status
        let badge = MonarchPaymentMethodCardTokens.expiryBadge(status, labels: labels)
        let metaParts = MonarchPaymentMethodCardTokens.metaParts(
            holderName: holderName,
            date: MonarchPaymentMethodCardTokens.expiryDate(month: expiryMonth, year: expiryYear),
            status: status,
            labels: labels
        )
        return VStack(alignment: .leading, spacing: MonarchPaymentMethodCardTokens.stackSpacing) {
            MonarchFlowLayout(spacing: MonarchPaymentMethodCardTokens.rowSpacing) {
                Text(name)
                    .monarchTextStyle(MonarchPaymentMethodCardTokens.nameStyle)
                    .foregroundStyle(theme.colors.textDefault)
                if isDefault {
                    MonarchBadge(
                        labels.defaultBadge ?? MonarchPaymentMethodCardStrings.defaultBadge,
                        tone: .accent,
                        size: .sm
                    )
                }
                if let badge {
                    MonarchBadge(badge.text, tone: badge.tone, size: .sm)
                }
            }
            if !metaParts.isEmpty {
                // The holder and the expiry sit side by side with a gap, as on the web; an expired date is red.
                MonarchFlowLayout(spacing: MonarchPaymentMethodCardTokens.rowSpacing) {
                    ForEach(Array(metaParts.enumerated()), id: \.offset) { _, part in
                        Text(part.text)
                            .monarchTextStyle(MonarchPaymentMethodCardTokens.metaStyle)
                            .foregroundStyle(
                                part.isExpired
                                    ? theme.colors.dangerStrong
                                    : theme.colors[role: MonarchPaymentMethodCardTokens.metaColor]
                            )
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private var buttons: some View {
        let showsMakeDefault = MonarchPaymentMethodCardTokens.showsMakeDefault(
            isDefault: isDefault,
            hasAction: onMakeDefault != nil
        )
        if showsMakeDefault || onRemove != nil || Actions.self != EmptyView.self {
            MonarchFlowLayout(spacing: MonarchPaymentMethodCardTokens.rowSpacing) {
                if showsMakeDefault {
                    let title = labels.makeDefault ?? MonarchPaymentMethodCardStrings.makeDefault
                    MonarchButton(title, size: .sm, isLoading: running == .makeDefault) {
                        run(.makeDefault)
                    }
                    .disabled(!MonarchPaymentMethodCardTokens.canMakeDefault(status: status))
                    .accessibilityLabel(MonarchPaymentMethodCardTokens.actionLabel(title, name: name, labels: labels))
                }
                if onRemove != nil {
                    let title = labels.remove ?? MonarchPaymentMethodCardStrings.remove
                    MonarchButton(title, variant: .ghost, size: .sm, isLoading: running == .remove) {
                        isConfirmingRemoval = true
                    }
                    .disabled(isRemoveDisabled)
                    .accessibilityLabel(MonarchPaymentMethodCardTokens.actionLabel(title, name: name, labels: labels))
                }
                actions
            }
        }
    }

    private func run(_ action: Action) {
        guard running == nil else { return }
        let work = action == .makeDefault ? onMakeDefault : onRemove
        guard let work else { return }
        running = action
        failure = nil
        Task { @MainActor in
            do {
                try await work()
            } catch {
                let message = MonarchPaymentMethodCardTokens.failure(error, labels: labels)
                failure = (action, message)
                AccessibilityNotification.Announcement(message).post()
            }
            running = nil
        }
    }
}

extension MonarchPaymentMethodCard where Actions == EmptyView {
    /// A saved card.
    ///
    /// - Parameters:
    ///   - brand: A network id (`visa`, `mastercard`, `amex`, …) or any
    ///     display name, already localised.
    ///   - last4: The last four digits, as the payment provider returns them.
    ///   - expiryMonth: The expiry month, 1 to 12.
    ///   - expiryYear: The expiry year, with two or four digits.
    ///   - holderName: The cardholder's name.
    ///   - brandImage: The network's logo. Default: a card symbol. Decorative.
    ///   - expiry: Replaces the status worked out from the date.
    ///   - now: Today, for working out the status.
    ///   - isDefault: Whether this method is charged by default.
    ///   - isRemoveDisabled: Disables Remove.
    ///   - labels: Replacements for the built-in strings.
    ///   - onMakeDefault: Makes the card the default. Throw to show the error.
    ///   - onRemove: Removes the card after confirmation. Throw to show the error.
    public init(
        brand: String,
        last4: String,
        expiryMonth: Int? = nil,
        expiryYear: Int? = nil,
        holderName: String? = nil,
        brandImage: Image? = nil,
        expiry: MonarchPaymentMethodCardExpiry? = nil,
        now: Date = .now,
        isDefault: Bool = false,
        isRemoveDisabled: Bool = false,
        labels: MonarchPaymentMethodCardLabels = MonarchPaymentMethodCardLabels(),
        onMakeDefault: (@MainActor () async throws -> Void)? = nil,
        onRemove: (@MainActor () async throws -> Void)? = nil
    ) {
        self.init(
            brand: brand,
            last4: last4,
            expiryMonth: expiryMonth,
            expiryYear: expiryYear,
            holderName: holderName,
            brandImage: brandImage,
            expiry: expiry,
            now: now,
            isDefault: isDefault,
            isRemoveDisabled: isRemoveDisabled,
            labels: labels,
            onMakeDefault: onMakeDefault,
            onRemove: onRemove,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchPaymentMethodCard") {
        MonarchPreviewMatrix {
            VStack {
                MonarchPaymentMethodCard(
                    brand: "visa",
                    last4: "4242",
                    expiryMonth: 8,
                    expiryYear: 2_030,
                    holderName: "Ana Silva",
                    isDefault: true,
                    onRemove: {}
                )
                MonarchPaymentMethodCard(
                    brand: "mastercard",
                    last4: "4444",
                    expiry: .expiring,
                    onMakeDefault: {},
                    onRemove: {}
                )
                MonarchPaymentMethodCard(brand: "amex", last4: "0005", expiry: .expired, onMakeDefault: {})
            }
        }
    }
#endif
