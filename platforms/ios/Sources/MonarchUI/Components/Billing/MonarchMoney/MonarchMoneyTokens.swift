import Foundation

/// Pure formatting and colour logic of ``MonarchMoney``.
enum MonarchMoneyTokens {
    /// Everything that decides how an amount is written, apart from the amount.
    struct Format: Equatable, Sendable {
        var currencyCode: String
        var notation: MonarchMoneyNotation = .standard
        var signDisplay: MonarchMoneySignDisplay = .automatic
        var currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol
        var usesAccountingSign = false
        var fractionDigits: ClosedRange<Int>?
        var locale: Locale
    }

    /// The number of minor-unit digits of an ISO 4217 currency: USD 2,
    /// JPY 0, KWD 3. Unknown codes give 2, the system's default.
    static func fractionDigits(currencyCode: String) -> Int {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = currencyCode
        return formatter.maximumFractionDigits
    }

    /// An amount in minor units (cents, paise) as an exact decimal amount.
    static func decimal(minorUnits: Int, currencyCode: String) -> Decimal {
        let magnitude = Decimal(minorUnits.magnitude)
        return Decimal(
            sign: minorUnits < 0 ? .minus : .plus,
            exponent: -fractionDigits(currencyCode: currencyCode),
            significand: magnitude
        )
    }

    /// The system's sign strategy for a sign display.
    static func signStrategy(
        _ display: MonarchMoneySignDisplay,
        accounting: Bool
    ) -> CurrencyFormatStyleConfiguration.SignDisplayStrategy {
        switch (display, accounting) {
        case (.automatic, false): .automatic
        case (.automatic, true): .accounting
        case (.always, false): .always(showZero: true)
        case (.always, true): .accountingAlways(showZero: true)
        case (.exceptZero, false): .always(showZero: false)
        case (.exceptZero, true): .accountingAlways(showZero: false)
        case (.never, _): .never
        }
    }

    /// The system's presentation for a currency display.
    static func presentation(
        _ display: MonarchMoneyCurrencyDisplay
    ) -> CurrencyFormatStyleConfiguration.Presentation {
        switch display {
        case .symbol: .standard
        case .narrowSymbol: .narrow
        case .code: .isoCode
        case .name: .fullName
        }
    }

    /// The system's currency format style for a format, without notation.
    static func currencyStyle(_ format: Format) -> Decimal.FormatStyle.Currency {
        var style = Decimal.FormatStyle.Currency(code: format.currencyCode, locale: format.locale)
            .presentation(presentation(format.currencyDisplay))
            .sign(strategy: signStrategy(format.signDisplay, accounting: format.usesAccountingSign))
        if let digits = format.fractionDigits {
            style = style.precision(.fractionLength(digits))
        }
        return style
    }

    /// The amount as text, written by the system's currency format style.
    static func string(_ amount: Decimal, format: Format) -> String {
        let style = currencyStyle(format)
        guard format.notation == .compact else { return amount.formatted(style) }
        if #available(iOS 18, macOS 15, *) {
            return MonarchFormats.capitalisedCompact(amount.formatted(style.notation(.compactName)))
        }
        return assembledCompact(amount, format: format)
    }

    /// The compact form before iOS 18, where the currency style has no
    /// notation: the currency is written around a placeholder number, which
    /// is then replaced by the system's compact form of the amount's size.
    static func assembledCompact(_ amount: Decimal, format: Format) -> String {
        let placeholder: Decimal = 2
        let signed = amount < 0 ? -placeholder : (amount == 0 ? 0 : placeholder)
        let template = signed.formatted(currencyStyle(format).precision(.fractionLength(0)))
        let digit = (signed == 0 ? Decimal(0) : placeholder).formatted(.number.locale(format.locale))
        var compact = Decimal.FormatStyle(locale: format.locale).notation(.compactName)
        if let digits = format.fractionDigits {
            compact = compact.precision(.fractionLength(digits))
        }
        let magnitude = (amount < 0 ? -amount : amount).formatted(compact)
        guard let range = template.range(of: digit) else { return amount.formatted(currencyStyle(format)) }
        return MonarchFormats.capitalisedCompact(template.replacingCharacters(in: range, with: magnitude))
    }

    /// What is shown and what VoiceOver reads. They differ only for a
    /// compact amount, whose full value is read.
    static func texts(_ amount: Decimal, format: Format) -> (shown: String, spoken: String) {
        let shown = string(amount, format: format)
        guard format.notation == .compact else { return (shown, shown) }
        var full = format
        full.notation = .standard
        return (shown, string(amount, format: full))
    }

    /// The text colour: muted for a missing amount, the danger text colour for
    /// a negative amount when negatives are toned, otherwise the inherited
    /// colour (`nil`).
    static func foreground(amount: Decimal?, tonesNegative: Bool) -> MonarchColorRole? {
        guard let amount else { return \.textMuted }
        return tonesNegative && amount < 0 ? \.dangerStrong : nil
    }

    /// The text shown in place of a missing amount.
    static func emptyText(_ override: String?) -> String {
        override ?? MonarchMoneyStrings.empty
    }

    /// What VoiceOver reads in place of a missing amount.
    static func emptyLabel(_ override: String?) -> String {
        override ?? MonarchMoneyStrings.emptyLabel
    }
}
