# Getting started

Install the theme, read it from the environment, and style standard controls.

## Install the theme

Apply `monarchTheme()` once, at the root of the app. It follows Dark Mode and
Increase Contrast, and sets the tint so system controls take the accent.

```swift
import MonarchUI
import SwiftUI

@main
struct ExampleApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .monarchTheme()
        }
    }
}
```

To fix the scheme or the contrast, or to use another accent:

```swift
import MonarchTokens

ContentView()
    .monarchTheme(scheme: .dark, contrast: .more, accent: MonarchTokens.Accents.neutral)
```

## Read the theme

Views read colours, spacing and radii from the environment. Do not read
`MonarchTokens` colours directly, or the view will not follow the theme.

```swift
struct InvoiceRow: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Text("Invoice 1042")
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors.textDefault)
            .padding(theme.spacing.space4)
            .background(theme.colors.bgSurface)
    }
}
```

## Style controls

Monarch provides styles that give SwiftUI controls the Monarch look. The control keeps its platform behavior, accessibility and keyboard handling.

```swift
Button("Save") { save() }
    .buttonStyle(.monarch(.primary))

Toggle("Email me a receipt", isOn: $wantsReceipt)
    .toggleStyle(.monarchCheckbox)

ProgressView("Uploading", value: 0.4)
    .progressViewStyle(.monarchLinear)
```

A plain `Toggle` (a switch), a `Picker` and a circular `ProgressView` need no
Monarch style; they take the accent from the tint.

## Use a custom font

Monarch uses the system typeface. To use a font the app bundles, register it
with the app as usual and pass its family name:

```swift
ContentView()
    .monarchTheme(typography: MonarchTypography(fontFamily: "Poppins"))
```

Text still scales with Dynamic Type.
