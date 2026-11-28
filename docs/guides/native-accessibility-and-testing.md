# Accessibility and testing on Android and iOS

What every Monarch component on Android (`platforms/android`) and iOS
(`platforms/ios`) promises to people who use assistive technology, how each
promise is implemented and tested in this repository, and what has not been
verified yet. The architecture is in
[NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md); the component
checklist is in [contributing](native-contributing.md#what-every-component-ships-with).

Accessibility is part of the component contract. A component that does not
meet it is not finished, whatever it looks like. Do not trade accessibility
for visual similarity with the web or with the other platform.

Read [Not yet verified](#not-yet-verified) before relying on anything here:
the automated tests are real, but no component has been used with TalkBack or
VoiceOver on a device.

## Android contract

| Requirement            | What a component does                                                                                                     | How it is implemented                                                                                                                                                                                          | How it is tested                                                                                                                                                                     |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| TalkBack and semantics | Exposes role, state and label. A labelled control and its label are one element. Decorative icons are hidden              | Material 3 semantics are kept where a control is wrapped; `Modifier.toggleable` and `selectable` with a `Role` on the labelled rows; `stateDescription` on a loading button; `heading()` on heading type roles | Per-component tests assert the role, label, state and click action (`MonarchButtonTest`, `MonarchCheckboxRowTest`, `MonarchSwitchRowTest`, `MonarchRadioRowTest`, `MonarchIconTest`) |
| Announcements          | A banner can be a polite or assertive live region; silent by default                                                      | `MonarchBannerAnnouncement` sets the `liveRegion` semantics                                                                                                                                                    | `MonarchBannerTest`                                                                                                                                                                  |
| Touch targets (48dp)   | Every enabled element that takes a tap receives taps in, and reserves in the layout, at least 48 by 48dp                  | `Modifier.monarchMinimumTouchTarget()`, `MonarchTouchTarget.Minimum`                                                                                                                                           | `AccessibilityTest`; `assertTouchTargetAtLeast48dp` in per-component tests; `ComponentMatrixTest` checks every tappable node of every fixture                                        |
| Font scale             | Text is in sp and grows with the font size setting; at the largest scale text wraps instead of being cut off              | Type roles in `foundation/MonarchTypography.kt` use sp sizes and em line heights                                                                                                                               | `FoundationTest`; `assertTextFullyVisible` at `LargestFontScale` in per-component tests; the matrix and `InventoryExamplesTest` render at the largest scale                          |
| Right to left          | Start and end, never left and right. Icons that point a direction can mirror                                              | Layout uses start and end; `MonarchIcon(painter, mirrorInRtl = true)`                                                                                                                                          | `assertIsBefore` in `MonarchButtonTest`, `MonarchBannerTest`; the matrix renders every fixture right to left                                                                         |
| Focus order            | Reading and focus order follow the layout order in both directions                                                        | Composition order; no custom traversal                                                                                                                                                                         | Indirectly, through the right-to-left order assertions. No test walks the focus order                                                                                                |
| Keyboard               | A focused element shows a visible focus ring in the theme's focus colour                                                  | `Modifier.monarchFocusRing(shape)`                                                                                                                                                                             | `AccessibilityTest` (`theFocusRingIsDrawnInTheFocusColourOnlyWhileFocused`, `theFocusRingFollowsTheThemeWhileFocused`). No test presses Tab or arrow keys                            |
| Contrast               | Colours come only from theme roles that the token contrast report covers; disabled states use disabled roles, not opacity | `MonarchTheme.colors`; the system contrast setting (Android 14 and later) selects the high-contrast themes through `isSystemInHighContrast()`                                                                  | `SourceRulesTest` (no literal colours, no token primitives, no alpha dimming in components); `ThemeResolutionTest`; `MonarchThemeTest`; `npm run contrast:check` for the token pairs |
| Reduced motion         | When animations are off, Monarch motion becomes an immediate change                                                       | `isReducedMotionEnabled()` reads the animator duration scale; `MonarchTheme.motion.reducedMotion`                                                                                                              | `AccessibilityTest`, `FoundationTest` (`reducedMotionTurnsASpecIntoAnImmediateChange`), `MonarchThemeTest`                                                                           |

## iOS contract

| Requirement                 | What a component does                                                                                              | How it is implemented                                                                                                                             | How it is tested                                                                                                                                                    |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| VoiceOver                   | Exposes role, value and label. Styles keep the standard controls, so roles and gestures are the system's           | `ButtonStyle`, `ToggleStyle` and `ProgressViewStyle` over standard controls; `accessibilityElement(children:)` to group; `accessibilityAddTraits` | Logic tests of the label and value a component computes (for example `MonarchCopyButtonTests`). The spoken output itself is not tested                              |
| Labels and hints            | Every control has a label; icon-only controls require one; hints only where the result of an action is not obvious | `accessibilityLabel`, `accessibilityHint`, `accessibilityValue`; spoken strings in `Resources/en.lproj/<Name>.strings`                            | Per-component suites under `Tests/MonarchUITests/Components/`                                                                                                       |
| Announcements               | Components that change state on their own post a notification                                                      | `AccessibilityNotification`                                                                                                                       | Not observable in a host test                                                                                                                                       |
| Touch targets (44pt)        | At least 44 by 44 points, also when the visible control is smaller                                                 | `monarchMinimumHitTarget()`, `MonarchAccessibility.minimumHitTarget`                                                                              | `FoundationsTests`; per-component size tests inside `#if os(iOS)`, so they run on the simulator only                                                                |
| Dynamic Type                | All text and icons scale, including the accessibility sizes                                                        | Type roles through `.monarchTextStyle(_:)` and `ScaledMetric`                                                                                     | `ComponentMatrixTests` renders every fixture at `.accessibility5`; growth checks run on the simulator only ([host limits](#host-only-dynamic-type-limits-on-macos)) |
| Right to left               | Leading and trailing, never left and right                                                                         | Layout uses leading and trailing                                                                                                                  | `ComponentMatrixTests` renders every fixture right to left, at the default and the largest size                                                                     |
| Focus                       | A focused element shows the focus ring; focus is driven by `@FocusState`                                           | `monarchFocusRing(_:in:)`                                                                                                                         | Not tested beyond layout and drawing                                                                                                                                |
| Switch Control and keyboard | Every action is reachable as an accessibility element or action                                                    | Standard controls; `accessibilityAction` and `accessibilityRepresentation` where a custom view replaces one                                       | Not tested                                                                                                                                                          |
| Contrast                    | Colours come only from theme roles                                                                                 | `@Environment(\.monarchTheme)`; never `MonarchTokens.<theme>.colors` in a view                                                                    | `ThemeTests`; review; `npm run contrast:check` for the token pairs                                                                                                  |
| Increase Contrast           | Selects the high-contrast themes                                                                                   | `.monarchTheme()` resolves `colorSchemeContrast` through `MonarchAppearance.resolve`                                                              | `ThemeTests` (all four resolutions); `ComponentMatrixTests` renders every fixture in `hc-light` and `hc-dark`                                                       |
| Differentiate Without Color | Status is not carried by colour alone                                                                              | Tone icons and labels; `accessibilityDifferentiateWithoutColor` in the components that show a coloured dot or state                               | Review                                                                                                                                                              |
| Reduce Motion               | Press, checkbox and indeterminate-progress animations are removed                                                  | `monarchAnimation(_:value:)`, `MonarchMotion.animation(_:easing:reduceMotion:)`, `accessibilityReduceMotion`                                      | `FoundationsTests` (`reduceMotion`)                                                                                                                                 |

## How the tests are organised

### Android

All tests run on the JVM with Robolectric, which draws with the real Android
graphics code (`robolectric.graphicsMode=NATIVE`), so layout and text are
measured. No emulator or device is needed. From the bottom of the pyramid up:

| Layer                    | What it checks                                                                                                                                                   | Where                                                                                                   |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Source rules             | No literal colours, no token primitives in components, no literal dp or sp, no alpha dimming, a preview file per component folder, one public component per file | `monarch-compose/src/test/kotlin/dev/monarchui/rules/SourceRulesTest.kt`                                |
| Static analysis and lint | detekt with no baseline; Android lint with warnings as errors                                                                                                    | `config/detekt/detekt.yml`; `lint` blocks in `build-logic/src/main/kotlin/monarch.android.*.gradle.kts` |
| Logic                    | Theme resolution, the Material mapping, colours, type roles, spacing, motion                                                                                     | `theme/`, `foundation/`, `internal/` under `monarch-compose/src/test/kotlin/dev/monarchui/`             |
| Component behaviour      | Semantics, states, touch targets, font scale, right to left, colours per theme                                                                                   | `components/<family>/Monarch<Name>Test.kt`, with helpers in `testing/MonarchAssertions.kt`              |
| Accessibility helpers    | Touch target, focus ring, reduced motion                                                                                                                         | `accessibility/AccessibilityTest.kt`                                                                    |
| Component matrix         | Every fixture in four themes, at the default and largest font scale, left to right and right to left: composes, lays out, 48dp targets                           | `testing/ComponentMatrixTest.kt`; fixtures are `object <Name>Fixtures : ComponentFixtures`              |
| Compiled examples        | Every example in `inventory/<id>.json` compiles and composes in four themes at the largest font scale                                                            | `testing/InventoryExamplesTest.kt`; sources generated by the `generateComponentTestSources` task        |
| Previews                 | Every `@Preview` function composes                                                                                                                               | `src/testDebug/kotlin/dev/monarchui/testing/ComponentPreviewsTest.kt`                                   |
| README examples          | The Kotlin in `platforms/android/README.md` compiles and composes                                                                                                | `docs/ReadmeExamples.kt`, `docs/ReadmeExamplesTest.kt`                                                  |
| Sample app               | The catalogue builds, debug and minified release, and its pages compose                                                                                          | `sample/src/test/kotlin/dev/monarchui/sample/`; `:sample:assembleDebug`, `:sample:assembleRelease`      |
| Public API               | The binary API matches the committed dump                                                                                                                        | `monarch-tokens/api/`, `monarch-compose/api/`; the `apiCheck` task                                      |

Fixtures are not registered by hand. The `generateComponentTestSources` task
finds every `ComponentFixtures` object under `src/test` and writes the
registry into `build/`.

### iOS

Tests use Swift Testing. They run on the Mac host with `swift test`, and on
an iOS Simulator with `xcodebuild test`; `Scripts/verify.sh` runs both.

| Layer                    | What it checks                                                                                                                    | Where                                                                                                           |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| Lint                     | `swift format lint --strict`, which requires a documentation comment on every public declaration                                  | `platforms/ios/.swift-format`; `Scripts/lint.sh`                                                                |
| Inventory conventions    | Every `inventory/<id>.json` is valid against the schema; coverage, class and kind agree; `build` entries have their source folder | `Scripts/generate.py` (`--check` in `verify.sh`)                                                                |
| Tokens                   | Theme ids, accents, `withAccent`                                                                                                  | `Tests/MonarchTokensTests/`                                                                                     |
| Logic and theme          | Appearance resolution, colour mapping, foundations, internals                                                                     | `Tests/MonarchUITests/Core/` (`ThemeTests`, `FoundationsTests`, `InternalsTests`)                               |
| Component behaviour      | One suite per component: labels, values, states, sizes                                                                            | `Tests/MonarchUITests/Components/<Family>/<Name>Tests.swift`                                                    |
| Component matrix         | Every fixture in four themes, at the default and largest Dynamic Type size, left to right and right to left: lays out and draws   | `Tests/MonarchUITests/Matrix/ComponentMatrixTests.swift`; fixtures are `enum <Name>Fixtures: ComponentFixtures` |
| Compiled examples        | Every example in the inventory compiles and renders in four themes                                                                | `Tests/MonarchUITests/Matrix/InventoryExampleTests.swift`; generated `Tests/MonarchUITests/Generated/Examples/` |
| README examples          | The view examples in `platforms/ios/README.md` and the DocC article                                                               | `Tests/MonarchUITests/Core/ReadmeExamplesTests.swift`                                                           |
| Documentation and sample | DocC builds without warnings; the catalogue app builds for the simulator with warnings as errors                                  | `Scripts/verify.sh` (full mode)                                                                                 |
| Public API               | The public declarations match the baseline                                                                                        | `platforms/ios/api/`; `Scripts/api.sh`                                                                          |

`Scripts/generate.py` finds every `ComponentFixtures` enum under
`Tests/MonarchUITests/Components` and writes
`Tests/MonarchUITests/Generated/FixtureRegistry.generated.swift`.

## Commands

Android, from `platforms/android`:

| Task                                                             | Command                                                                                                |
| ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| Every gate: inventory, tests, API, detekt, lint, sample, publish | `./gradlew verifyMonarch`                                                                              |
| Before handing a change over (skips the API check)               | `./gradlew verifyLocal`                                                                                |
| The fast loop for one component                                  | `./gradlew componentCheck -Pcomponent=MonarchButton`                                                   |
| All unit and Compose UI tests of the library                     | `./gradlew :monarch-compose:testDebugUnitTest`                                                         |
| One test class                                                   | `./gradlew :monarch-compose:testDebugUnitTest --tests "dev.monarchui.accessibility.AccessibilityTest"` |
| The matrix only                                                  | `./gradlew :monarch-compose:testDebugUnitTest --tests "*.ComponentMatrixTest"`                         |

iOS, from `platforms/ios` (set `DEVELOPER_DIR` if Xcode is not the selected
developer directory):

| Task                                | Command                                      |
| ----------------------------------- | -------------------------------------------- |
| Every gate, including the simulator | `Scripts/verify.sh`                          |
| The gates that need no simulator    | `Scripts/verify.sh --host-only`              |
| A working copy, no API baseline     | `Scripts/verify.sh --local`                  |
| The fast loop for one component     | `Scripts/verify.sh --component MonarchBadge` |
| Host tests only                     | `swift test`                                 |
| The matrix only, on the host        | `swift test --filter ComponentMatrixTests`   |

The simulator used by `Scripts/verify.sh` is set with
`MONARCH_IOS_DESTINATION` (default
`platform=iOS Simulator,name=iPhone 17 Pro,OS=latest`).

## Not yet verified

Stated plainly, so that nobody reads the tables above as more than they are.

### No device or screen reader testing

- No component has been used with TalkBack or VoiceOver, on a device or a
  simulator. The accessibility entries in the inventory describe what the
  code sets, not what a screen reader was heard to say.
- Switch Control, Voice Control, Full Keyboard Access on iOS, and Switch
  Access and a hardware keyboard on Android have not been tried.
- The Accessibility Inspector (Xcode) and the Accessibility Scanner (Android)
  have not been run.
- Android tests have never run on an emulator or a device. The JVM tests do
  not exercise a real accessibility service.

### Host-only Dynamic Type limits on macOS

`swift test` runs on macOS, which has no Dynamic Type. Setting
`dynamicTypeSize` in the environment changes the value SwiftUI reports, but
the system fonts do not grow on the host. On the host, the matrix therefore
proves only that every fixture lays out and draws at `.accessibility5`; that
text grows, wraps and keeps 44-point targets is checked only by the tests
inside `#if os(iOS)`, which run on the simulator. `Scripts/verify.sh
--host-only`, `--local` and `--component` never run them.

Native text fields, switches and spinners do not draw in SwiftUI's
`ImageRenderer`, so for those the matrix relies on the layout pass in a
hosting view.

### No screenshot or snapshot tests

Neither platform has screenshot or snapshot tests. The rendering tests check
that a component lays out and draws, not what it looks like. The reasons:

- On Android, the usual tools are third-party libraries and Gradle plugins.
  Each would need the justification in the
  [dependency rules](../governance/native-platform-evolution.md#rules-that-prevent-upgrade-lock-in),
  and would add a plugin that has to keep up with every AGP and Gradle
  upgrade.
- On iOS, `ImageRenderer` does not draw several native controls, and a
  simulator renderer would tie the baselines to one Xcode and one simulator
  runtime.
- Font rendering differs between macOS and Linux, so baselines would have to
  be recorded and compared on one runner image.

They are worth adding where a component draws something itself (class C
components such as the badge and the banner). That is a decision for the
maintainers, recorded here when it is made.

### Other gaps

- No test walks the focus order or presses keys on either platform.
- Reduced motion is tested at the level of the motion values, not by
  observing an animation.
- Only English strings exist; translated, longer strings have not been tried.
- Contrast is checked for the token pairs in
  [CONTRAST-MATRIX](../architecture/CONTRAST-MATRIX.md), not by measuring a
  rendered component.

## Manual screen reader checklist

Run before a stable release of a platform, on the oldest and the newest
supported OS where a device or emulator is available, with the catalogue
sample app. Record in the release pull request what was checked, on which
device or simulator and OS version, and every failure as an issue. Do not
tick an item that was not done.

### Android: TalkBack

Build and install the catalogue: `./gradlew :sample:installDebug` from
`platforms/android`.

- [ ] Turn on TalkBack. Swipe through every catalogue page. Each component is
      announced once, with its role (button, checkbox, switch, radio
      button, heading) and its label; nothing is announced twice.
- [ ] Checkbox, switch and radio rows: one swipe reaches the whole row, the
      state is announced, and a double tap changes it and announces the new
      state.
- [ ] A loading button announces its loading state and does not act on a
      double tap. A disabled button is announced as disabled.
- [ ] Decorative icons are silent; labelled icons are announced with their
      label.
- [ ] A polite banner is announced after the current speech; an assertive
      banner interrupts; a default banner is silent until reached.
- [ ] Text fields announce the label, the helper text and, after an error,
      the error message.
- [ ] Explore by touch: every tappable element is found with a finger, and
      no two targets overlap.
- [ ] Settings > Accessibility > Display size and text: at the largest font
      size and display size, no text is cut off and every page scrolls.
- [ ] Turn on Remove animations. Pressing, toggling and progress show no
      animation.
- [ ] Android 14 or later: set the contrast to high. The high-contrast theme
      is used.
- [ ] Set the device language to Arabic or Hebrew, or force right-to-left
      layout in the developer options. Every page mirrors and the reading
      order follows.
- [ ] Connect a hardware keyboard. Tab moves through every focusable element
      in reading order, the focus ring is visible on each, and Enter or
      Space activates.
- [ ] Switch Access: every action can be reached and performed.
- [ ] Run the Accessibility Scanner on each catalogue page and record the
      findings.

### iOS: VoiceOver

Build the catalogue with `Scripts/verify.sh --sample` from `platforms/ios`,
then open `Sample/MonarchCatalogue.xcodeproj` and run `MonarchCatalogue` on a
device or simulator. VoiceOver itself needs a device; on the simulator use the
Accessibility Inspector.

- [ ] Turn on VoiceOver. Swipe through every catalogue page. Each component
      is announced once, with its trait (button, heading, toggle) and label,
      and a value where it has one.
- [ ] Hints are read only where the result of the action is not obvious.
- [ ] The checkbox toggle style is announced as a toggle with its state,
      and a double tap changes it.
- [ ] A loading button announces its loading state. A disabled control is
      announced as dimmed.
- [ ] Components that announce on their own (for example the typing indicator) are heard
      once, at the right time.
- [ ] Settings > Accessibility > Display & Text Size > Larger Text: at the
      largest accessibility size, no text is cut off and every page scrolls.
      Repeat at the smallest size.
- [ ] Turn on Increase Contrast. The high-contrast theme is used in light
      and dark.
- [ ] Turn on Reduce Motion. Pressing, toggling and progress show no
      animation.
- [ ] Turn on Differentiate Without Colour. No state is shown by colour
      alone.
- [ ] Set the device language to Arabic or Hebrew. Every page mirrors.
- [ ] Switch Control: every action can be reached and performed, in a
      sensible order.
- [ ] Full Keyboard Access with a hardware keyboard: Tab reaches every
      control, the focus ring is visible, and Space activates.
- [ ] Voice Control with "Show names": every control has a name that can be
      spoken.
- [ ] Run the Accessibility Inspector audit on each catalogue page and record
      the findings.

## Related

- [NATIVE-PLATFORMS](../architecture/NATIVE-PLATFORMS.md)
- [NATIVE-COMPATIBILITY](../architecture/NATIVE-COMPATIBILITY.md)
- [Contributing to the native platforms](native-contributing.md)
- [Native adaptive layout](native-adaptive-layout.md)
- [Accessibility statement](../governance/accessibility-statement.md)
- [`platforms/android/README.md`](../../platforms/android/README.md)
- [`platforms/ios/docs/ADDING-COMPONENTS.md`](../../platforms/ios/docs/ADDING-COMPONENTS.md)
