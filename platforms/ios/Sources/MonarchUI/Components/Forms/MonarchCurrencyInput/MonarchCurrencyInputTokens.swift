import Foundation

/// The format decisions of ``MonarchCurrencyInput``.
enum MonarchCurrencyInputTokens {
    /// The system currency format with the currency's own number of decimals.
    /// The locale is applied by ``MonarchNumberInput`` from the environment.
    static func format(
        currencyCode: String,
        display: MonarchMoneyCurrencyDisplay,
        locale: Locale = .autoupdatingCurrent
    ) -> FloatingPointFormatStyle<Double>.Currency {
        let digits = MonarchCurrencyInputMath.fractionDigits(currencyCode: currencyCode)
        return FloatingPointFormatStyle<Double>.Currency(code: currencyCode, locale: locale)
            .presentation(MonarchMoneyTokens.presentation(display))
            .precision(.fractionLength(digits))
    }
}
