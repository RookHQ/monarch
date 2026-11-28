import SwiftUI

/// An amount of money in one currency, written by the system's currency
/// format in fixed-width digits.
///
/// ```swift
/// MonarchMoney(invoice.total, currencyCode: "EUR")
/// MonarchMoney(minorUnits: 129_900, currencyCode: "INR", notation: .compact)
/// ```
///
/// Formatting is the system's `Decimal.FormatStyle.Currency`, in the
/// environment's locale unless `locale` is given. Monarch adds the type
/// role, tabular digits, an empty state and the spoken form. It never
/// converts between currencies.
///
/// Accessibility: a compact amount ($1.3M) is read as the full amount. A
/// missing amount shows a dash read as "No amount". A negative amount always
/// keeps its sign or parentheses; `tonesNegative` only adds the danger text
/// colour.
public struct MonarchMoney: View {
    private let amount: Decimal?
    private let currencyCode: String
    private let notation: MonarchMoneyNotation
    private let signDisplay: MonarchMoneySignDisplay
    private let currencyDisplay: MonarchMoneyCurrencyDisplay
    private let usesAccountingSign: Bool
    private let fractionDigits: ClosedRange<Int>?
    private let locale: Locale?
    private let textStyle: MonarchTextStyle
    private let tonesNegative: Bool
    private let emptyText: String?
    private let emptyLabel: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var environmentLocale

    /// An amount.
    ///
    /// - Parameters:
    ///   - amount: The amount in major units (19.99 for $19.99), or `nil`
    ///     when there is none.
    ///   - currencyCode: The ISO 4217 code, such as `USD`, `EUR` or `INR`.
    ///   - notation: Every digit, or a shortened amount such as $1.3M.
    ///   - signDisplay: When a plus or minus sign is shown.
    ///   - currencyDisplay: How the currency is written.
    ///   - usesAccountingSign: Whether negatives are written in parentheses
    ///     where the locale does.
    ///   - fractionDigits: The least and most digits after the decimal
    ///     separator. Default: the currency's own.
    ///   - locale: The locale to write in. Default: the environment's.
    ///   - textStyle: The type role.
    ///   - tonesNegative: Whether a negative amount uses the danger text colour.
    ///   - emptyText: Replaces the built-in "—" shown for a missing amount.
    ///   - emptyLabel: Replaces the built-in "No amount" read for a missing amount.
    public init(
        _ amount: Decimal?,
        currencyCode: String,
        notation: MonarchMoneyNotation = .standard,
        signDisplay: MonarchMoneySignDisplay = .automatic,
        currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol,
        usesAccountingSign: Bool = false,
        fractionDigits: ClosedRange<Int>? = nil,
        locale: Locale? = nil,
        textStyle: MonarchTextStyle = .body,
        tonesNegative: Bool = false,
        emptyText: String? = nil,
        emptyLabel: String? = nil
    ) {
        self.amount = amount
        self.currencyCode = currencyCode
        self.notation = notation
        self.signDisplay = signDisplay
        self.currencyDisplay = currencyDisplay
        self.usesAccountingSign = usesAccountingSign
        self.fractionDigits = fractionDigits
        self.locale = locale
        self.textStyle = textStyle
        self.tonesNegative = tonesNegative
        self.emptyText = emptyText
        self.emptyLabel = emptyLabel
    }

    /// An amount stored in minor units: 1999 with USD is $19.99, and 1999
    /// with JPY is ¥1,999. The conversion is exact.
    ///
    /// - Parameters:
    ///   - minorUnits: The amount in the currency's minor units (cents,
    ///     paise), or `nil` when there is none.
    ///   - currencyCode: The ISO 4217 code, such as `USD`, `EUR` or `INR`.
    ///   - notation: Every digit, or a shortened amount such as $1.3M.
    ///   - signDisplay: When a plus or minus sign is shown.
    ///   - currencyDisplay: How the currency is written.
    ///   - usesAccountingSign: Whether negatives are written in parentheses
    ///     where the locale does.
    ///   - fractionDigits: The least and most digits after the decimal
    ///     separator. Default: the currency's own.
    ///   - locale: The locale to write in. Default: the environment's.
    ///   - textStyle: The type role.
    ///   - tonesNegative: Whether a negative amount uses the danger text colour.
    ///   - emptyText: Replaces the built-in "—" shown for a missing amount.
    ///   - emptyLabel: Replaces the built-in "No amount" read for a missing amount.
    public init(
        minorUnits: Int?,
        currencyCode: String,
        notation: MonarchMoneyNotation = .standard,
        signDisplay: MonarchMoneySignDisplay = .automatic,
        currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol,
        usesAccountingSign: Bool = false,
        fractionDigits: ClosedRange<Int>? = nil,
        locale: Locale? = nil,
        textStyle: MonarchTextStyle = .body,
        tonesNegative: Bool = false,
        emptyText: String? = nil,
        emptyLabel: String? = nil
    ) {
        self.init(
            minorUnits.map { MonarchMoneyTokens.decimal(minorUnits: $0, currencyCode: currencyCode) },
            currencyCode: currencyCode,
            notation: notation,
            signDisplay: signDisplay,
            currencyDisplay: currencyDisplay,
            usesAccountingSign: usesAccountingSign,
            fractionDigits: fractionDigits,
            locale: locale,
            textStyle: textStyle,
            tonesNegative: tonesNegative,
            emptyText: emptyText,
            emptyLabel: emptyLabel
        )
    }

    /// The content of the view.
    public var body: some View {
        let foreground = MonarchMoneyTokens.foreground(amount: amount, tonesNegative: tonesNegative)
        content
            .monarchTextStyle(textStyle)
            .monospacedDigit()
            .foregroundStyle(foreground.map { AnyShapeStyle(theme.colors[role: $0]) } ?? AnyShapeStyle(.foreground))
    }

    @ViewBuilder private var content: some View {
        if let amount {
            let format = MonarchMoneyTokens.Format(
                currencyCode: currencyCode,
                notation: notation,
                signDisplay: signDisplay,
                currencyDisplay: currencyDisplay,
                usesAccountingSign: usesAccountingSign,
                fractionDigits: fractionDigits,
                locale: locale ?? environmentLocale
            )
            let texts = MonarchMoneyTokens.texts(amount, format: format)
            Text(verbatim: texts.shown)
                .accessibilityLabel(Text(verbatim: texts.spoken))
        } else {
            Text(verbatim: MonarchMoneyTokens.emptyText(emptyText))
                .accessibilityLabel(Text(verbatim: MonarchMoneyTokens.emptyLabel(emptyLabel)))
        }
    }
}

extension MonarchMoney {
    /// The text ``MonarchMoney`` shows for an amount, for exports, chart
    /// labels and accessibility labels of your own.
    ///
    /// - Parameters:
    ///   - amount: The amount in major units.
    ///   - currencyCode: The ISO 4217 code.
    ///   - notation: Every digit, or a shortened amount.
    ///   - signDisplay: When a plus or minus sign is shown.
    ///   - currencyDisplay: How the currency is written.
    ///   - usesAccountingSign: Whether negatives are written in parentheses
    ///     where the locale does.
    ///   - fractionDigits: The least and most digits after the decimal separator.
    ///   - locale: The locale to write in.
    /// - Returns: The formatted amount.
    nonisolated public static func formatted(
        _ amount: Decimal,
        currencyCode: String,
        notation: MonarchMoneyNotation = .standard,
        signDisplay: MonarchMoneySignDisplay = .automatic,
        currencyDisplay: MonarchMoneyCurrencyDisplay = .symbol,
        usesAccountingSign: Bool = false,
        fractionDigits: ClosedRange<Int>? = nil,
        locale: Locale = .autoupdatingCurrent
    ) -> String {
        MonarchMoneyTokens.string(
            amount,
            format: MonarchMoneyTokens.Format(
                currencyCode: currencyCode,
                notation: notation,
                signDisplay: signDisplay,
                currencyDisplay: currencyDisplay,
                usesAccountingSign: usesAccountingSign,
                fractionDigits: fractionDigits,
                locale: locale
            )
        )
    }
}

#if DEBUG
    #Preview("MonarchMoney") {
        MonarchPreviewMatrix {
            VStack(alignment: .trailing) {
                MonarchMoney(1_234.5, currencyCode: "USD")
                MonarchMoney(minorUnits: 129_900, currencyCode: "INR")
                MonarchMoney(1_250_000, currencyCode: "EUR", notation: .compact)
                MonarchMoney(-42, currencyCode: "USD", usesAccountingSign: true, tonesNegative: true)
                MonarchMoney(42, currencyCode: "USD", signDisplay: .exceptZero, currencyDisplay: .code)
                MonarchMoney(nil, currencyCode: "USD")
            }
        }
    }
#endif
