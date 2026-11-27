import nativeFacts from 'virtual:monarch-native-facts';
import android from 'virtual:monarch-native-platform/android';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { href } from '../../lib/router.js';
import { Table } from '../../features/content/inline.js';
import {
  GuideLink,
  PlatformMeta,
  Readme,
  Unreleased,
  hasGuide,
} from '../../features/native/landing.js';

const facts = nativeFacts.android;
const { sections } = android;
const code = (text: string) => <code className="docs-code-inline">{text}</code>;

/** The toolchain the library is built with, from `gradle/libs.versions.toml` and the Gradle wrapper. */
function Toolchain() {
  const r = facts.requirements;
  const all: [item: string, version: string | null | undefined][] = [
    ['Monarch for Android', facts.version],
    ['minSdk', r.minSdk],
    ['compileSdk', r.compileSdk],
    ['targetSdk', r.targetSdk],
    ['Kotlin and the Compose compiler plugin', r.kotlin],
    ['Android Gradle plugin', r.agp],
    ['Compose BOM', r.composeBom],
    ['JDK toolchain', r.jdk],
    ['Bytecode target', r.jvmTarget],
    ['Gradle', r.gradle],
  ];
  const rows = all.flatMap(([item, version]) => (version ? [[item, version] as const] : []));
  if (rows.length === 0) return null;
  return (
    <>
      <h3 className="docs-subtitle">Versions the library is built with</h3>
      <Table
        label="Versions Monarch for Android is built with"
        head={['Item', 'Version']}
        rows={rows.map(([item, version]) => [item, code(version)])}
      />
      <p className="docs-footnote">
        Read from {code('platforms/android/gradle/libs.versions.toml')} and the Gradle wrapper when
        this site was built. Every version is written there and nowhere else.
      </p>
    </>
  );
}

/**
 * `/android/get-started/`: requirements and toolchain, installation, the
 * first screen, the theme and the tokens. The text is the README's own
 * sections; the versions are read from the version catalog (plugins/native.ts).
 */
export function AndroidGetStartedPage() {
  return (
    <PageFrame
      eyebrow="Android"
      title="Get started with Monarch for Android"
      lead="Build the libraries from source, add them to the app, wrap it in MonarchTheme and use the tokens. Everything here is the library README's own text."
      meta={<PlatformMeta platform="android" facts={facts} />}
    >
      <Unreleased facts={facts}>
        Nothing is published to Maven Central yet, and the API can still change. The first release
        ships with the next {code('@rookcom/monarch-react')} patch, at the same version. To try it
        now, build it from source as described under Installation below.
      </Unreleased>

      <PageSection id="requirements" title="Requirements and compatibility">
        <Readme platform="android" html={sections.requirements} />
        <Toolchain />
        {hasGuide('compatibility') ? (
          <p className="docs-text">
            <GuideLink id="compatibility">The compatibility matrix</GuideLink> records the tested
            toolchain and OS range. Monarch claims support only for what is in it.
          </p>
        ) : null}
      </PageSection>

      <PageSection id="installation" title="Installation">
        <Readme platform="android" html={sections['install-from-source']} />
      </PageSection>

      <PageSection id="getting-started" title="Getting started">
        <Readme platform="android" html={sections['getting-started']} />
      </PageSection>

      <PageSection id="theme-setup" title="Theme setup">
        <Readme platform="android" html={sections['theme-setup']} />
        {sections['how-monarch-maps-to-material-3'] ? (
          <>
            <h3 className="docs-subtitle">How Monarch maps to Material 3</h3>
            <Readme platform="android" html={sections['how-monarch-maps-to-material-3']} />
          </>
        ) : null}
      </PageSection>

      <PageSection id="tokens" title="Token usage">
        <Readme platform="android" html={sections.tokens} />
        <p className="docs-text">
          The values are the ones on the <a href={href('/colours')}>Colours</a> and{' '}
          <a href={href('/tokens')}>Tokens</a> pages: one source generates the CSS variables and the
          Kotlin constants.
        </p>
      </PageSection>
    </PageFrame>
  );
}
