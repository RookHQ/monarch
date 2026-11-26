# Adaptive layout on Android and iOS

Practical guidance for applications that use Monarch for Android or Monarch
for iOS: insets and safe areas, the keyboard, window sizes, navigation, sheets
and text scaling.

Monarch supplies colour, type, spacing, shape and a small set of components.
It does **not** wrap the platform's layout, window, navigation or presentation
APIs, and it does not intend to: these are class A, native adoption, in the
[component classification](../architecture/NATIVE-PLATFORMS.md#components).
Each section below names the platform API to use and says so again, so that
nobody looks for a Monarch equivalent.

The snippets show platform API. Where they use Monarch, they use only the
theme entry points and theme values. They leave out imports and the types an
application would define itself (`HomeScreen`, `Invoice` and so on).

## Android

Monarch for Android is a Jetpack Compose library over Material 3. Everything
here is standard AndroidX and Compose; start from the official guidance on
developer.android.com, which stays current when the platform changes.

Monarch's own dependencies are Compose UI, Foundation and Material 3. The
adaptive libraries named below
(`androidx.compose.material3.adaptive`, `androidx.window`) are not
dependencies of Monarch. An application that uses them adds them itself.

### Edge-to-edge and window insets

Use `enableEdgeToEdge()` from `androidx.activity` and Compose's window inset
APIs. Monarch does not wrap them and adds no inset handling of its own.

Edge-to-edge is the default for applications that target Android 15 (API 35)
or later, and from API 36 an application can no longer opt out. Call
`enableEdgeToEdge()` anyway, so earlier Android versions behave the same way.

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MonarchTheme {
                Scaffold { innerPadding ->
                    HomeScreen(contentPadding = innerPadding)
                }
            }
        }
    }
}
```

- Material 3's `Scaffold`, `TopAppBar`, `NavigationBar` and
  `ModalBottomSheet` handle their own insets. Pass `innerPadding` on to the
  content; do not drop it.
- For a scrolling list, pass the padding as `contentPadding` so that content
  scrolls behind the system bars and still comes to rest clear of them:
  `LazyColumn(contentPadding = innerPadding)`.
- Outside a `Scaffold`, use `Modifier.safeDrawingPadding()`, or
  `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`. `safeDrawing`
  covers the system bars, the display cutout and the keyboard.
- Backgrounds go edge to edge and content is inset. Paint the root with the
  canvas role and inset what is inside it:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(MonarchTheme.colors.bgCanvas)
        .safeDrawingPadding(),
) {
    // content
}
```

- Add Monarch spacing in addition to insets, never instead of them. An inset
  is where the system draws; spacing is a design decision.
- When a parent has already applied an inset as padding, call
  `Modifier.consumeWindowInsets(…)` so that children do not apply it twice.

### Keyboard (IME)

Use `Modifier.imePadding()` and the `WindowInsets.ime` inset. Monarch does
not wrap them.

Set `android:windowSoftInputMode="adjustResize"` on the activity in the
manifest. Then inset the scrolling container:

```kotlin
Scaffold { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // fields
    }
}
```

The order matters: the scaffold's padding is applied and consumed first, so
`imePadding()` adds only the part of the keyboard that is not already
covered by the navigation bar inset.

- Make every form scrollable. A focused field is then brought into view above
  the keyboard by Compose.
- Move through the fields with the keyboard's action button:
  `KeyboardOptions(imeAction = ImeAction.Next)` with
  `KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })`,
  and `ImeAction.Done` with `focusManager.clearFocus()` on the last field.
  `LocalFocusManager.current` provides `focusManager`.
- Set `keyboardType`, `capitalization` and `autoCorrectEnabled` in
  `KeyboardOptions` to match the content, and a `contentType` semantics
  property so that autofill and password managers work.
- A button that must stay reachable while typing belongs inside the
  container that has `imePadding()`, or in the `bottomBar` of a `Scaffold`
  that has it.
- Do not measure the keyboard or animate views by hand. To react to it, read
  the `WindowInsets.ime` inset.
- Hardware keyboards are common on tablets, foldables and ChromeOS. Check
  that Tab order follows reading order and that every action is reachable
  without touch.

### Window size classes

Use `currentWindowAdaptiveInfo()` from
`androidx.compose.material3.adaptive`, which returns the `WindowSizeClass` of
`androidx.window`. Monarch does not wrap it.

```kotlin
val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
val showTwoPanes =
    sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
```

- Decide layout from the **window**, never from the device type, the screen
  size or the orientation. A phone app can be a narrow window on a tablet,
  and a tablet app can be 320dp wide in split screen.
- The breakpoints are 600dp (medium) and 840dp (expanded) for width, and
  480dp and 900dp for height. Compact is anything below medium.
- Read the size class once near the top of the screen and pass the decision
  down as a parameter. A component that reads the window cannot be previewed
  or reused in a pane.
- Monarch generates the web breakpoints as `MonarchTokens.Breakpoint`
  (window widths in dp). They exist so that a design that names a web
  breakpoint can be matched. For the standard decisions (which navigation,
  one pane or two) use the window size classes, because the Material
  adaptive components are built around them.

### Foldables and tablets

Use the Material 3 adaptive layouts and, for posture, `androidx.window`.
Monarch does not wrap them.

| Need                                           | Platform API                                                                             |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------- |
| Bottom bar on phones, rail or drawer when wide | `NavigationSuiteScaffold`                                                                |
| List and detail side by side                   | `NavigableListDetailPaneScaffold`, `ListDetailPaneScaffold`                              |
| Main content with a supporting pane            | `SupportingPaneScaffold`                                                                 |
| Fold state, hinge position, tabletop posture   | `currentWindowAdaptiveInfo().windowPosture`, or `WindowInfoTracker` and `FoldingFeature` |

Placed inside `MonarchTheme { }`, these take Monarch's colours, type and
shapes through the Material theme like any other Material 3 component.

- Do not lock orientation and do not declare the activity non-resizable.
  From Android 16, for applications that target API 36, the system ignores
  orientation, resizability and aspect-ratio restrictions on displays at
  least 600dp wide.
- Keep state across configuration changes. Folding, unfolding, rotating and
  resizing all recreate or reconfigure the activity: hold screen state in a
  `ViewModel` and transient UI state in `rememberSaveable`.
- Keep interactive content away from the hinge. In tabletop posture, put
  content above the fold and controls below it.
- Limit line length on wide windows. Constrain reading content with
  `Modifier.widthIn(max = …)` rather than letting text span a tablet.
- Support a pointer and a keyboard: hover and focus states, context menus on
  secondary click, and standard shortcuts.
- Test in split screen, in a freeform desktop window and on a foldable
  emulator, not only on a phone.

### Predictive back

Use `BackHandler` or `PredictiveBackHandler` from `androidx.activity.compose`,
and a navigation library that supports predictive back. Monarch does not wrap
them and no Monarch component intercepts back.

```kotlin
BackHandler(enabled = hasUnsavedChanges) {
    showDiscardDialog = true
}
```

- Opt in with `android:enableOnBackInvokedCallback="true"` on the
  `<application>` element. Applications that target API 36 have it on by
  default on Android 16.
- Do not override `onBackPressed()` or listen for `KEYCODE_BACK`. With
  predictive back enabled the system does not call them.
- Keep `enabled` false whenever you would not handle back yourself. An
  enabled handler turns off the system's back preview for that screen.
- Material 3's bottom sheets, navigation drawer and search bar already
  animate with the back gesture. Navigation Compose handles the back stack
  and its transitions.
- Back closes the most recent transient thing first: the keyboard, a menu, a
  sheet, a dialog, then the screen. Do not use it for anything destructive
  without a confirmation.

### System bars

Use `enableEdgeToEdge()` and its `SystemBarStyle` parameters. Monarch does
not set system bar colours or icon appearance.

With edge-to-edge the bars are transparent, your content draws behind them,
and `enableEdgeToEdge()` chooses light or dark bar icons from the system's
dark theme setting. That is correct when `MonarchTheme` follows the system,
which is its default.

If the application fixes the scheme, for example `MonarchScheme.Dark` while
the system is light, tell the system which icons to draw:

```kotlin
enableEdgeToEdge(
    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
)
```

`Color` here is `android.graphics.Color`. Use `SystemBarStyle.light(…)` for a
fixed light scheme, and call `enableEdgeToEdge` again when an in-app theme
setting changes.

- `Window.statusBarColor` and `Window.navigationBarColor` are deprecated, and
  have no effect on Android 15 and later for applications that target API 35
  or later. Draw your own background behind the bar instead, with a Monarch
  surface role.
- With three-button navigation the system adds a translucent scrim behind the
  buttons. Leave it on unless your own surface behind the bar gives the
  buttons enough contrast.
- Hide the system bars only for immersive content such as media, with
  `WindowInsetsControllerCompat`.

### Dynamic colour

Dynamic colour is **off by default** in Monarch. `MonarchTheme` has a
`dynamicColor` parameter to turn it on.

Why it is off: Material's dynamic colour replaces the colour scheme with one
derived from the person's wallpaper. In Monarch the accent identifies the
Rook product and is part of the brand, and every colour role is proven for
contrast in all four themes by the token tests. A wallpaper-derived scheme
has neither property.

```kotlin
MonarchTheme(dynamicColor = true) {
    // content
}
```

What opting in does, and does not do:

- On Android 12 (API 31) and later, Material 3 components inside the theme
  take the wallpaper-based scheme. On earlier versions the parameter has no
  effect.
- Monarch components keep Monarch colours, and `MonarchTheme.colors` still
  returns Monarch roles. A screen therefore mixes two palettes. Check every
  screen for pairs that no longer have enough contrast; the Monarch contrast
  report does not cover them.
- The high-contrast themes exist for people who ask the system for more
  contrast. Dynamic colour replaces the Material side of that too, so test
  with the contrast setting raised before shipping it.

Use it only where a product has decided that following the device matters
more than the product accent.

### Font scale

Use `sp` for text, which Monarch's type roles already do, and let layouts
grow. Monarch does not cap, clamp or override the system font scale.

People can raise the font scale to 200%, and from Android 14 the scaling is
non-linear: large text grows less than small text.

- Take text styles from `MonarchTheme.typography` (or `MonarchText`). Do not
  convert a type size to `dp`.
- Do not give a fixed height to anything that contains text. Use
  `Modifier.heightIn(min = …)` or `defaultMinSize`, so the minimum touch
  target is kept and the element can still grow.
- Let text wrap. Truncate with `maxLines` only where the full text is
  reachable elsewhere.
- When a row no longer fits at large scales, stack it:

```kotlin
val stacked = LocalDensity.current.fontScale >= 1.5f
if (stacked) {
    Column { Label(); Value() }
} else {
    Row { Label(); Value() }
}
```

- Icons that sit in a line of text should scale with it; icons that are
  controls keep their size and their 48dp touch target.
- Preview at `fontScale = 2f` (`@Preview(fontScale = 2f)`), and test long
  strings and a right-to-left locale at the same time.

## iOS

Monarch for iOS is a SwiftUI library. Everything here is standard SwiftUI;
start from Apple's developer documentation and the Human Interface
Guidelines, which stay current when the platform changes. The deployment
target is iOS 17, and every API below is available there.

### Safe areas

Use SwiftUI's safe area: content stays inside it by default. Use
`safeAreaInset(edge:)` to add your own bars, `safeAreaPadding(_:)` to add
space, and `ignoresSafeArea(_:edges:)` only for backgrounds. Monarch does not
wrap them.

```swift
struct OrderView: View {
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        ScrollView {
            OrderSummary()
                .padding(theme.spacing.space4)
        }
        .background(theme.colors.bgCanvas)
        .safeAreaInset(edge: .bottom) {
            Button("Place order", action: placeOrder)
                .buttonStyle(.borderedProminent)
                .padding(theme.spacing.space4)
                .frame(maxWidth: .infinity)
                .background(.bar)
        }
    }

    private func placeOrder() {}
}
```

- A colour passed to `background(_:)` extends under the safe area by default,
  while the content in front stays inside it. That is the behaviour you want
  for a canvas colour, and it needs no `ignoresSafeArea`.
- `safeAreaInset` shrinks the safe area for the content behind it, so a
  scroll view scrolls under the bar and still rests clear of it.
- Do not use `ignoresSafeArea()` on content, and do not hard-code the height
  of the status bar, the home indicator or the Dynamic Island.
- Add Monarch spacing in addition to the safe area, never instead of it.

### Keyboard avoidance and focus

SwiftUI moves content out of the keyboard's way by itself: the keyboard is
part of the safe area. Use `@FocusState` with `focused(_:equals:)` to move
focus, `submitLabel(_:)` and `onSubmit(_:)` for the return key, and
`scrollDismissesKeyboard(_:)` for dismissal. Monarch does not wrap them.

```swift
struct SignInForm: View {
    private enum Field { case email, password }

    @State private var email = ""
    @State private var password = ""
    @FocusState private var focus: Field?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .focused($focus, equals: .email)
                    .submitLabel(.next)
                    .onSubmit { focus = .password }

                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .focused($focus, equals: .password)
                    .submitLabel(.go)
                    .onSubmit(signIn)
            }
            .padding()
        }
        .scrollDismissesKeyboard(.interactively)
    }

    private func signIn() {}
}
```

- Put forms in a `ScrollView`, `Form` or `List`. The focused field is then
  scrolled into view above the keyboard.
- Do not observe keyboard notifications or add keyboard-height padding by
  hand. If one view must stay put, opt that view out with
  `ignoresSafeArea(.keyboard)`.
- Set `textContentType` and `keyboardType`, so AutoFill and the right
  keyboard appear.
- Add actions above the keyboard with a toolbar item placed at `.keyboard`.
- On iPad and with Full Keyboard Access, people use a hardware keyboard.
  Check Tab order, give primary actions a `keyboardShortcut`, and never make
  a touch gesture the only way to do something.

### Size classes and iPad layouts

Use `@Environment(\.horizontalSizeClass)` and `verticalSizeClass`, and
layouts that adapt by themselves (`ViewThatFits`, `AnyLayout`, `Grid`).
Monarch does not wrap them.

```swift
struct SummaryAndDetails: View {
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let layout =
            horizontalSizeClass == .compact
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: theme.spacing.space4))
            : AnyLayout(HStackLayout(alignment: .top, spacing: theme.spacing.space6))

        layout {
            SummaryPanel()
            DetailsPanel()
        }
    }
}
```

- Decide from the size class and the space available, never from the device
  idiom. An iPad app is compact in Slide Over and in a narrow window, and
  large iPhones are regular in landscape.
- Prefer `ViewThatFits` when the question is "does this fit", and a size
  class when the question is "which structure".
- `AnyLayout` keeps the identity and state of the child views when the layout
  changes, so a rotation does not reset them.
- Limit line length on wide layouts with `frame(maxWidth:)`, or
  `containerRelativeFrame` for a share of the container.
- Monarch generates the web breakpoints as `MonarchTokens.Breakpoint`
  (window widths in points) for matching a design that names one. For the
  standard decisions use size classes, because the system containers are
  built around them.

### Multitasking and resizable windows

Lay out from the space the view is given. Use `GeometryReader`,
`containerRelativeFrame` or `onGeometryChange` when you need a size, and the
scene APIs (`WindowGroup`, `@SceneStorage`) for windows and their state.
Monarch does not wrap them.

- Never read the screen size. `UIScreen.main` is deprecated, and the screen
  is not the window: in Split View, Slide Over, Stage Manager and the
  windowed mode of iPadOS 26, the window can be any size and can change at
  any time.
- Support every orientation on iPad and do not require full screen.
  `UIRequiresFullScreen` is deprecated from iPadOS 26.
- Expect the size class to change while the app runs, and keep navigation and
  scroll position when it does. The system containers below do this for you.
- Keep per-window state per scene. Two windows of the same app must not share
  a selection through a global.
- Support the pointer and drag and drop where content can be moved: these are
  how people work across windows.
- Test by resizing the window continuously, not only at the fixed Split View
  sizes.

### NavigationStack and NavigationSplitView

Use `NavigationStack` for a single column of pushed screens and
`NavigationSplitView` for a sidebar or list with a detail. Use `TabView` for
top-level sections. Monarch does not provide a navigation component, a
navigation bar or a tab bar.

```swift
struct InvoiceBrowser: View {
    let invoices: [Invoice]
    @State private var selection: Invoice.ID?

    var body: some View {
        NavigationSplitView {
            List(invoices, selection: $selection) { invoice in
                Text(invoice.title)
            }
            .navigationTitle("Invoices")
        } detail: {
            if let selection {
                InvoiceDetail(id: selection)
            } else {
                ContentUnavailableView("Select an invoice", systemImage: "doc.text")
            }
        }
    }
}
```

`NavigationSplitView` shows columns when the width is regular and collapses
to a single stack when it is compact, including when an iPad window is made
narrow. One piece of code covers iPhone, iPad and every window size.

For a stack, drive navigation from values so that it can be restored and
deep-linked:

```swift
struct InvoiceStack: View {
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            InvoiceList()
                .navigationDestination(for: Invoice.ID.self) { id in
                    InvoiceDetail(id: id)
                }
        }
    }
}
```

- Apply `.monarchTheme()` outside the navigation container, at the root.
  The theme sets the tint, which navigation bars, toolbars and tab bars use.
- Leave bar titles, back buttons, the swipe-back gesture and bar materials to
  the system. Do not apply a Monarch type role to a navigation title, and do
  not replace the back button.
- Put actions in `toolbar` with semantic placements (`.confirmationAction`,
  `.cancellationAction`, `.primaryAction`), so the system places them
  correctly on each device.

### Sheets and detents

Use `sheet`, `fullScreenCover`, `popover`, `alert` and `confirmationDialog`,
with `presentationDetents(_:)` for partial-height sheets. Monarch does not
provide a sheet, dialog or popover component.

```swift
struct InvoiceScreen: View {
    @State private var isFiltering = false

    var body: some View {
        InvoiceList()
            .toolbar {
                Button("Filter", systemImage: "line.3.horizontal.decrease") {
                    isFiltering = true
                }
            }
            .sheet(isPresented: $isFiltering) {
                NavigationStack {
                    FilterForm()
                        .navigationTitle("Filter")
                        .navigationBarTitleDisplayMode(.inline)
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
    }
}
```

- Presentation modifiers go on the content of the sheet, as above.
- A sheet inherits the SwiftUI environment of the view that presents it,
  including the Monarch theme, as long as `.monarchTheme()` is applied
  above that view.
- Offer `.large` among the detents when the content can grow with Dynamic
  Type, so that nothing becomes unreachable. Make the sheet's content
  scrollable.
- On iPad and in regular width the system presents sheets as centred cards
  and `confirmationDialog` as a popover. Let it; do not build a custom
  presentation to force the iPhone appearance.
- Give every sheet a visible way to close it, in addition to the swipe. If
  closing would lose edits, use `interactiveDismissDisabled(_:)` and confirm.
- Use `alert` for something the person must acknowledge and
  `confirmationDialog` for a choice that follows from their action,
  including destructive ones (`role: .destructive`).

### Dynamic Type

Use Monarch's type roles, which scale with Dynamic Type, and let layouts
grow. Use `@ScaledMetric` for sizes that should follow text, and
`dynamicTypeSize` to change structure at the accessibility sizes. Monarch
does not cap or override the text-size setting.

```swift
struct LabelledValue: View {
    let label: String
    let value: String

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .body) private var iconSize: CGFloat = 20

    var body: some View {
        let layout =
            dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading))
            : AnyLayout(HStackLayout(alignment: .firstTextBaseline))

        layout {
            Image(systemName: "calendar")
                .font(.system(size: iconSize))
                .accessibilityHidden(true)
            Text(label)
                .monarchTextStyle(.body)
            Text(value)
                .monarchTextStyle(.bodyStrong)
        }
    }
}
```

- Apply `monarchTextStyle(_:)` to text. Do not set a fixed font size next to
  it.
- Do not give a fixed height to anything that contains text. Use
  `frame(minHeight:)`, so the 44-point touch target is kept and the element
  can still grow.
- Let text wrap. Use `lineLimit` only where the full text is reachable
  elsewhere.
- Do not restrict the range with `dynamicTypeSize(_:)` to make a layout fit.
  Restructure it at the accessibility sizes, as above. A narrow limit is
  acceptable only on a control the system itself limits, such as a tab bar.
- Preview at the largest size
  (`.dynamicTypeSize(.accessibility5)` in a `#Preview`), and test long
  strings and a right-to-left language at the same time.

## On both platforms

- Lay out with leading and trailing, never left and right, so right-to-left
  languages work.
- Keep the minimum touch target, 48dp on Android and 44 points on iOS, at
  every window size and text size. Monarch components already do.
- Take colour from the theme's roles so that the layout stays correct in
  dark and high-contrast themes
  ([native tokens](../governance/native-tokens.md#the-rule-for-components)).
- Do not build one platform's pattern on the other. A bottom sheet with
  detents, a navigation rail and a back gesture each belong to their own
  platform, and the same design can and should differ between them.
- When a platform release changes any API named here, follow the platform.
  This guide is updated through the
  [platform evolution](../governance/native-platform-evolution.md) procedure.

## Related

- [NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md)
- [Native accessibility and testing](native-accessibility-and-testing.md)
- [Contributing to the native platforms](native-contributing.md)
- [NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md)
