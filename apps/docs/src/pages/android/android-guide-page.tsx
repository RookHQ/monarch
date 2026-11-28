import nativeFacts from 'virtual:monarch-native-facts';
import android from 'virtual:monarch-native-platform/android';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { NEW_TAB } from '../../lib/external.js';
import { href } from '../../lib/router.js';
import { CodeBlock } from '../../features/code/code-block.js';
import { REPO_FILE } from '../../features/native/labels.js';
import {
  AdaptiveTopics,
  CatalogueSummary,
  Elsewhere,
  Examples,
  GuideLink,
  PlatformMeta,
  PlatformTerms,
  PreviewGallery,
  Readme,
  Reference,
  Unreleased,
  hasGuide,
} from '../../features/native/landing.js';

const facts = nativeFacts.android;
const { sections } = android;
const code = (text: string) => <code className="docs-code-inline">{text}</code>;

/**
 * `/android/guide/`: how Monarch for Android is built and what it covers, the
 * long form behind the landing page (`/android/`).
 * Prose about the approach is written here from docs/architecture/NATIVE-PLATFORMS.md
 * and docs/guides/native-adaptive-layout.md; installation, setup, tokens and
 * accessibility are the README's own sections; versions and counts are read
 * at build time (plugins/native.ts).
 */
export function AndroidGuidePage() {
  return (
    <PageFrame
      eyebrow="Android"
      title="The Monarch for Android guide"
      lead="The Android implementation of Monarch, written in Kotlin with Jetpack Compose. It shares the tokens, the role names, the component vocabulary and the accessibility rules with the web and iOS implementations. It looks like Monarch and behaves like Android."
      meta={<PlatformMeta platform="android" facts={facts} />}
    >
      <Unreleased facts={facts}>
        Nothing is published to Maven Central yet, and the API can still change. The first release
        ships with the next {code('@rookcom/monarch-react')} patch, at the same version. To try it
        now, build it from source as described in{' '}
        <a href={href('/android/get-started', 'installation')}>Get started</a>.
      </Unreleased>

      <PageSection id="foundation" title="Kotlin and Jetpack Compose">
        <p className="docs-text">
          Monarch for Android is a Gradle build of its own under {code('platforms/android')}. It
          needs a JDK and the Android SDK, and nothing from the web packages at build time: the
          generated token file is committed.
        </p>
        <Readme platform="android" html={sections.modules} />
        <p className="docs-text">
          The code is layered, and a lower layer never imports a higher one: tokens (generated
          constants, no UI framework), the theme (maps tokens to Material 3's theme types),
          foundations (type styles, spacing, shape, elevation, motion and accessibility helpers) and
          components, which read the theme only.
        </p>
      </PageSection>

      <PageSection id="philosophy" title="Android first">
        <p className="docs-text">
          Monarch has one design language and three implementations. The Android one is not a port
          of the React components. A Monarch screen on Android looks like Monarch and behaves like
          Android. Fields, buttons, checkboxes, radio buttons, segmented controls, tabs, chips,
          badges, cards, banners, progress indicators and list rows are drawn by Monarch from
          Compose foundation, with Monarch's own shapes, colours and type, and the platform's touch
          targets, focus, haptics and accessibility semantics. Presented surfaces and text machinery
          stay the platform's, themed with Monarch's shape, colour and type: dialogs, sheets, menus,
          date and time pickers, tooltips, the snackbar host, the keyboard, text selection and
          scrolling. Material types do not appear in Monarch's public signatures.
        </p>
        <ul className="docs-list">
          <li>
            <strong>Material 3 for the platform layers.</strong> {code('MonarchTheme')} wraps{' '}
            {code('MaterialTheme')} with a colour scheme, typography and shapes derived from the
            Monarch values in one mapping, so a Material 3 component an app places inside is themed,
            and the platform layers Monarch uses (dialogs, sheets, menus, pickers) take Monarch's
            look.
          </li>
          <li>
            <strong>Back belongs to the system.</strong> Use {code('BackHandler')} or{' '}
            {code('PredictiveBackHandler')} and a navigation library that supports predictive back.
            Monarch does not wrap them and no Monarch component intercepts back.
          </li>
          <li>
            <strong>Edge-to-edge is the platform's.</strong> Use {code('enableEdgeToEdge()')} and
            Compose's window inset APIs. Monarch does not wrap them and adds no inset handling of
            its own.
          </li>
          <li>
            <strong>Dynamic colour is off by default.</strong> Material's dynamic colour replaces
            the colour scheme with one derived from the wallpaper. In Monarch the accent identifies
            the product, and every colour role is checked for contrast in all four themes by the
            token tests; a wallpaper-derived scheme has neither property. An app can opt in with{' '}
            {code('MonarchTheme(dynamicColor = true)')}: on Android 12 and later, plain Material 3
            components then take the wallpaper-based scheme, and Monarch components keep Monarch
            colours.
          </li>
          <li>
            <strong>Android's measures.</strong> Touch targets are at least 48dp, text is in sp and
            grows with the font size setting, and layouts use start and end so they mirror in
            right-to-left languages.
          </li>
        </ul>
        <p className="docs-text">
          Where a component differs from its iOS and web counterparts, its page says how and why
          under <em>Platform behaviour</em>.
        </p>
      </PageSection>

      <PageSection id="components" title="Component catalogue">
        <CatalogueSummary platform="android" facts={facts} />
      </PageSection>

      <PageSection id="previews" title="Component previews">
        <PreviewGallery platform="android" items={android.previews} />
        <p className="docs-text">
          {android.previews.length
            ? 'These images are rendered from the Android components. '
            : 'This site does not show rendered images of the Android components yet. '}
          To see every component in all four themes, run the catalogue app in{' '}
          {code('platforms/android/sample')}.
        </p>
        {facts.commands.sampleInstall ? (
          <CodeBlock
            code={`cd platforms/android\n${facts.commands.sampleInstall}`}
            label="Code: install the Android catalogue app"
            lang="sh"
          />
        ) : null}
        <p className="docs-text">
          In Android Studio, each component also has Compose previews, a {code('<Name>Previews.kt')}{' '}
          file, in light, dark and the largest font scale.
        </p>
      </PageSection>

      <PageSection id="examples" title="Code examples">
        <p className="docs-text">
          Examples come from the inventory. Each is copied from a test source set where it is
          compiled and composed, and a test fails when the documentation and that source differ.
        </p>
        <Examples platform="android" examples={android.examples} />
      </PageSection>

      <PageSection id="accessibility" title="Accessibility">
        <Readme platform="android" html={sections.accessibility} />
        <p className="docs-text">
          Each component's page lists its own behaviour.
          {hasGuide('accessibility') ? (
            <>
              {' '}
              <GuideLink id="accessibility">The accessibility and testing guide</GuideLink> covers
              the rules every native component follows.
            </>
          ) : null}
        </p>
      </PageSection>

      <PageSection id="adaptive-layouts" title="Adaptive layouts">
        <p className="docs-text">
          Monarch does not wrap the platform's layout, window, navigation or presentation APIs. Its
          own dependencies are Compose UI, Foundation and Material 3; an app that uses the adaptive
          libraries ({code('androidx.compose.material3.adaptive')}, {code('androidx.window')}) adds
          them itself.
        </p>
        <AdaptiveTopics platform="android" facts={facts} />
      </PageSection>

      {android.foundations.length ? (
        <PageSection id="reference" title="Theme and foundations reference">
          <p className="docs-text">
            Inventory entries that have no web component, documented here in full.
          </p>
          <Reference platform="android" foundations={android.foundations} />
        </PageSection>
      ) : null}

      <PageSection id="releases" title="Migration and release notes">
        <p className="docs-text">
          Monarch for Android has the version of {code('@rookcom/monarch-react')}, in the{' '}
          {code('0.14.x')} patch series, shared with Monarch for iOS; it is the Maven version of
          both artifacts. A deprecated API is removed no sooner than 90 days and two patch releases
          after the release that deprecates it. The{' '}
          <a href={href('/android/releases')}>release notes</a> are its changelog.
          {facts.unreleased && facts.versions <= 1
            ? ' There is no migration guide yet, because there is no earlier version to migrate from.'
            : ''}
        </p>
        <Readme platform="android" html={sections['public-api-policy']} />
        <p className="docs-text">
          The rules for deprecation and migration guides are in{' '}
          <a {...NEW_TAB} href={`${REPO_FILE}docs/governance/native-api-stability.md`}>
            Native API stability
          </a>
          .
        </p>
      </PageSection>

      <PageSection id="platform-terms" title="Platform terms">
        <PlatformTerms platform="android" />
      </PageSection>

      <PageSection id="elsewhere" title="Elsewhere in Monarch">
        <Elsewhere platform="android" />
      </PageSection>
    </PageFrame>
  );
}
