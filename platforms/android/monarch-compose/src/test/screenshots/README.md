# Android screenshot baselines

Every component is rendered by `ComponentScreenshotTest` (in `src/test/kotlin/.../screenshots/`) and compared with
the images here and in `platforms/android/previews/`.

## What an image shows

One image per component and variant. It stacks every fixture of the component (the `ComponentFixtures` that
`ComponentMatrixTest` uses), or, for the native and compose recipes that have no fixtures, every inventory example.
Each cell has its fixture name in small type above it. Popups (dialogs, sheets, menus) are captured as the whole
window, with long runs of empty scrim rows shortened.

## Matrix (decision D9)

- Every component: `light` and `dark`, text scale 1.0, left to right.
- The core set in `core-components.txt` (30 components): also `hc-light` and `hc-dark`, and each theme at text
  scale 2.0 (`.x2`) and right to left (`.rtl`): 16 images each.
- `ScreenshotTargets.excluded` lists the components that cannot be shown in a repeatable image, with the reason.

## Where the files are

- `src/test/screenshots/<id>/<id>.<theme>[.x2][.rtl].png`: every baseline, the default light and dark images
  included.
- `platforms/android/previews/<id>.<theme>.png`: the docs previews. This is a different set (the primary example of
  each component, set in Poppins, written by `recordPreviews`, see `docs/PREVIEWS.md`). The docs read them from
  there. They are not baselines, and `verifyScreenshots` does not read them.
- The baselines use the system font (the library default). The docs previews use Poppins (D10.4).

## Size

Each PNG is at most 40,000 bytes, written as a palette image (`ScreenshotPixels`). An image with 256 colours or
fewer is stored exactly. Otherwise a median-cut quantiser gives it a 256-colour palette whose entries are the most
frequent colour of each box, so a large flat fill (a pale pill, a dark card) keeps its exact colour; if the file is
still too big the palette shrinks (128, 64, 32, 16), then the image is scaled down (0.75, 0.5, 0.35). The same step
runs before the compare, so it never causes a diff. A frame counts only after two equal frames in a row. No Git LFS.

## Commands (from `platforms/android`)

- `./gradlew verifyScreenshots` compares. `./gradlew verifyMonarch` includes it, because the screenshot test is part
  of `testDebugUnitTest`. Skip it in a larger run with `-Pmonarch.screenshots=off`.
- `./gradlew recordScreenshots` writes the baselines. Record only on the reference environment: Robolectric with
  native graphics, on this Mac, US English, UTC, 320 by 1000dp at density 1.0.
- `-Pmonarch.screenshots.ids=a,b` limits a run to those components.
- A failed compare writes `<name>_actual.png` and `<name>_compare.png` to
  `monarch-compose/build/outputs/screenshots/` and prints both paths.
- `node scripts/native/contact-sheet.mjs --platform android` builds `platforms/android/build/contact-sheet.html`.
