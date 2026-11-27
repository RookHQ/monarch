/**
 * Pieces shared by the Android and iOS landing pages. The pages themselves
 * (pages/android, pages/ios) are written separately: each has its own
 * sections, order and wording. What is shared is how repository data is shown.
 */
import '../../styles/native.css';
import type { ReactNode } from 'react';
import { Alert, Badge, Button } from '@rookcom/monarch-react';
import { guides } from 'virtual:monarch-native-nav';
import type {
  InventoryEntry,
  NativeFacts,
  NativePlatformDocs,
  Previews,
} from '../../../plugins/native.js';
import { NEW_TAB } from '../../lib/external.js';
import { href } from '../../lib/router.js';
import { PLATFORM_NAMES, REPO_FILE, STATUS, STATUS_ORDER, type Platform } from './labels.js';
import { NativeCode } from './native-code.js';
import { NativeDocs, exampleCode } from './native-docs.js';
import { NativePreview } from './native-preview.js';

/** The native guides, in reading order. Only those whose file exists have a page. */
export const GUIDE_LABELS: readonly (readonly [id: string, label: string])[] = [
  ['adaptive-layout', 'adaptive layout'],
  ['accessibility', 'accessibility and testing'],
  ['compatibility', 'compatibility'],
  ['contributing', 'contributing'],
];

/** True when the native guide has a page on this site (its file exists). */
export const hasGuide = (id: string): boolean => guides.includes(id);

/** A link to a native guide's page. A guide that is not written yet is named without a link. */
export function GuideLink({
  id,
  anchor,
  children,
}: {
  id: string;
  anchor?: string;
  children: ReactNode;
}) {
  return hasGuide(id) ? <a href={href(`/native/${id}`, anchor)}>{children}</a> : <>{children}</>;
}

/**
 * A section of the platform's README, rendered at build time. The text is the
 * repository's own, so the page cannot disagree with it. When the README has
 * no such section, a link to the README stands in.
 */
export function Readme({ platform, html }: { platform: Platform; html: string | undefined }) {
  if (html)
    return (
      <div className="docs-prose docs-prose--embedded" dangerouslySetInnerHTML={{ __html: html }} />
    );
  return (
    <p className="docs-text">
      See the{' '}
      <a {...NEW_TAB} href={`${REPO_FILE}platforms/${platform}/README.md`}>
        README of Monarch for {PLATFORM_NAMES[platform]}
      </a>
      .
    </p>
  );
}

/** Version and release state under the page title, and the two main links. */
export function PlatformMeta({
  platform,
  facts,
}: {
  platform: Platform;
  facts: NativeFacts[Platform];
}) {
  return (
    <>
      {facts.version ? (
        <Badge size="sm" variant="outline">
          {`Version ${facts.version}`}
        </Badge>
      ) : null}
      {facts.unreleased ? (
        <Badge size="sm" tone="warning">
          Unreleased
        </Badge>
      ) : null}
      <Button asChild variant="primary" size="sm">
        <a href={href(`/${platform}/components`)}>Component catalogue</a>
      </Button>
      <Button asChild size="sm">
        <a href={href(`/${platform}/releases`)}>Release notes</a>
      </Button>
    </>
  );
}

/** Shown only while the changelog's newest version is marked unreleased. */
export function Unreleased({
  facts,
  children,
}: {
  facts: NativeFacts[Platform];
  children: ReactNode;
}) {
  if (!facts.unreleased) return null;
  return (
    <Alert
      tone="warning"
      title={facts.version ? `Version ${facts.version} is unreleased` : 'Unreleased'}
    >
      {children}
    </Alert>
  );
}

/** Counted progress of the inventory against the web catalogue, with a link to the catalogue. */
export function CatalogueSummary({
  platform,
  facts,
}: {
  platform: Platform;
  facts: NativeFacts[Platform];
}) {
  const name = PLATFORM_NAMES[platform];
  const other = facts.entries - facts.covered;
  return (
    <>
      <dl className="docs-facts">
        <div>
          <dt>Web components with an {name} entry</dt>
          <dd>
            {facts.covered}
            <span className="docs-facts__of"> of {facts.total}</span>
          </dd>
        </div>
        {STATUS_ORDER.filter((s) => s !== 'planned' && (facts.status[s] ?? 0) > 0).map((s) => (
          <div key={s}>
            <dt>{STATUS[s].label}</dt>
            <dd>{facts.status[s]}</dd>
          </div>
        ))}
        <div>
          <dt>Planned, no entry yet</dt>
          <dd>{facts.total - facts.covered}</dd>
        </div>
      </dl>
      <p className="docs-footnote">
        Counted from <code className="docs-code-inline">platforms/{platform}/inventory</code> and{' '}
        <code className="docs-code-inline">platforms/parity.json</code> when this site was built.
        {other > 0
          ? ` ${other} more ${other === 1 ? 'entry documents' : 'entries document'} foundations with no web component.`
          : ''}
      </p>
      <p className="docs-text">
        The <a href={href(`/${platform}/components`)}>{name} component catalogue</a> lists every
        entry by family with its coverage and status (available, experimental, planned or
        deprecated) and links to its documentation. Each component's {name} documentation is a
        section of its page under <a href={href('/components')}>Components</a>, next to the web
        documentation, so the component is described once.
      </p>
    </>
  );
}

/** Preview images of a few components, when the build found any; otherwise nothing. */
export function PreviewGallery({
  platform,
  items,
}: {
  platform: Platform;
  items: readonly { id: string; title: string; previews: Previews }[];
}) {
  if (items.length === 0) return null;
  return (
    <ul className="docs-native-previews">
      {items.map((item) => (
        <li key={item.id}>
          <NativePreview platform={platform} title={item.title} previews={item.previews} />
          <a href={href(`/components/${item.id}`, platform)}>{item.title}</a>
        </li>
      ))}
    </ul>
  );
}

/** The first compiled example of a few inventory entries. */
export function Examples({
  platform,
  examples,
}: {
  platform: Platform;
  examples: readonly {
    id: string;
    title: string;
    name: string | null;
    example: InventoryEntry['examples'][number];
  }[];
}) {
  const name = PLATFORM_NAMES[platform];
  if (examples.length === 0)
    return (
      <p className="docs-text">
        The {name} inventory has no examples yet. They will appear here and on each component's
        page.
      </p>
    );
  return (
    <>
      {examples.map((e) => (
        <figure key={e.id} className="docs-native__example">
          <figcaption className="docs-native__caption">
            <a href={href(`/components/${e.id}`, platform)}>{e.title}</a>: {e.example.title}
          </figcaption>
          <NativeCode
            platform={platform}
            code={exampleCode(e.example)}
            label={`Code: ${e.title}, ${e.example.title}`}
          />
        </figure>
      ))}
    </>
  );
}

/** Inventory entries that have no web component page (theme, icon), documented in full. */
export function Reference({
  platform,
  foundations,
}: {
  platform: Platform;
  foundations: readonly { id: string; title: string; docs: NativePlatformDocs }[];
}) {
  return (
    <>
      {foundations.map((f) => (
        <section key={f.id} aria-labelledby={`reference-${f.id}`}>
          <h3 id={`reference-${f.id}`} className="docs-subtitle">
            {f.title}
          </h3>
          <NativeDocs
            platform={platform}
            id={`reference-${f.id}`}
            title={f.title}
            docs={f.docs}
            level={4}
          />
        </section>
      ))}
    </>
  );
}

/** The topics the adaptive layout guide covers for the platform, read from its headings. */
export function AdaptiveTopics({
  platform,
  facts,
}: {
  platform: Platform;
  facts: NativeFacts[Platform];
}) {
  if (facts.adaptiveTopics.length === 0) return null;
  return (
    <>
      <p className="docs-text">
        The{' '}
        <GuideLink id="adaptive-layout" anchor={platform}>
          adaptive layout guide
        </GuideLink>{' '}
        names the platform API for each of these and shows it with the Monarch theme:
      </p>
      <ul className="docs-list">
        {facts.adaptiveTopics.map((topic) => (
          <li key={topic}>{topic}</li>
        ))}
      </ul>
    </>
  );
}

/** An external link to an official terms page, opened in a new tab. */
const Official = ({ to, children }: { to: string; children: ReactNode }) => (
  <a {...NEW_TAB} href={to}>
    {children}
  </a>
);

/**
 * What Monarch does with the platform vendor's SDK, icons and fonts, with
 * links to the official terms. A statement of fact, not legal advice; the
 * same text is in docs/architecture/NATIVE-PLATFORMS.md ("Platform terms").
 */
export function PlatformTerms({ platform }: { platform: Platform }) {
  const code = (text: string) => <code className="docs-code-inline">{text}</code>;
  return (
    <>
      <p className="docs-text">
        Monarch&apos;s own code is under the Apache License 2.0. An app built with it also uses{' '}
        {platform === 'ios' ? 'Apple' : 'Google'}&apos;s SDK, which comes with its own terms. This
        section says what Monarch does with that SDK. It is not legal advice; the official texts
        linked here are what apply.
      </p>
      {platform === 'ios' ? (
        <ul className="docs-list">
          <li>
            Monarch for iOS builds against Apple&apos;s SDKs (SwiftUI, Foundation) and ships no
            Apple code. Xcode and the SDKs are covered by the{' '}
            <Official to="https://www.apple.com/legal/sla/docs/xcode.pdf">
              Xcode and Apple SDKs Agreement
            </Official>
            ; App Store distribution by the{' '}
            <Official to="https://developer.apple.com/support/terms/apple-developer-program-license-agreement/">
              Apple Developer Program License Agreement
            </Official>{' '}
            and the{' '}
            <Official to="https://developer.apple.com/app-store/review/guidelines/">
              App Review Guidelines
            </Official>
            .
          </li>
          <li>
            SF Symbols are referred to by name and drawn by the system. The libraries ship no symbol
            files, uses no symbol that depicts an Apple product or mark, and uses SF Symbols on iOS
            only, as{' '}
            <Official to="https://developer.apple.com/sf-symbols/">Apple&apos;s terms</Official>{' '}
            limit them to app interfaces on Apple platforms. Screenshots show them only as part of
            an app&apos;s interface.
          </li>
          <li>
            San Francisco is the system font. Monarch bundles no{' '}
            <Official to="https://developer.apple.com/fonts/">Apple font</Official> file. The
            optional MonarchFontsPoppins product carries Poppins under the SIL Open Font License
            1.1, and the docs previews are set in it.
          </li>
          <li>
            {code('npm run legal:check')} fails when an Apple font file is in the repository, when
            SF Symbols are named outside the iOS library, or when a restricted symbol is named.
          </li>
        </ul>
      ) : (
        <ul className="docs-list">
          <li>
            Monarch for Android depends on AndroidX libraries (Jetpack Compose, Material 3, the
            adaptive layout artifacts and {code('activity-compose')}), which Google publishes under
            the{' '}
            <Official to="https://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</Official>
            . An app resolves them from Google&apos;s Maven repository; Monarch does not copy them.
          </li>
          <li>
            The Android SDK is covered by the{' '}
            <Official to="https://developer.android.com/studio/terms">
              Android Software Development Kit License Agreement
            </Official>
            , and Google Play distribution by the{' '}
            <Official to="https://play.google.com/about/developer-distribution-agreement.html">
              Google Play Developer Distribution Agreement
            </Official>{' '}
            and its{' '}
            <Official to="https://play.google.com/about/developer-content-policy/">
              policies
            </Official>
            .
          </li>
          <li>
            The libraries bundle no icons. The default typeface is the system font, Roboto, which is
            Google&apos;s. The optional monarch-fonts-poppins artifact carries Poppins under the SIL
            Open Font License 1.1, and the docs previews are set in it. The sample app and the
            previews use Material Icons (Outlined) under the Apache License 2.0 through a
            sample-only module that no library depends on.
          </li>
        </ul>
      )}
      <p className="docs-footnote">
        {platform === 'ios'
          ? 'Apple, iOS, SwiftUI and SF Symbols are trademarks of Apple Inc.'
          : 'Android and Google Play are trademarks of Google LLC.'}{' '}
        Monarch uses the names only to say what it runs on. Third-party code and data in Monarch
        itself are listed in{' '}
        <a {...NEW_TAB} href={`${REPO_FILE}THIRD_PARTY_NOTICES.md`}>
          THIRD_PARTY_NOTICES
        </a>
        .
      </p>
    </>
  );
}

/** Links between the Monarch overview, both platforms, foundations, components, guidelines and releases. */
export function Elsewhere({ platform }: { platform: Platform }) {
  const other: Platform = platform === 'android' ? 'ios' : 'android';
  const present = GUIDE_LABELS.filter(([id]) => hasGuide(id));
  return (
    <ul className="docs-list">
      <li>
        <a href={href('/')}>Monarch overview</a> and the{' '}
        <a href={href('/introduction')}>introduction</a>: what Monarch is on every platform.
      </li>
      <li>
        <a href={href(`/${other}`)}>Monarch for {PLATFORM_NAMES[other]}</a>: the same design
        language through the other platform's controls.
      </li>
      <li>
        Design foundations: <a href={href('/colours')}>colours</a>,{' '}
        <a href={href('/typography')}>typography</a>, <a href={href('/tokens')}>tokens</a> and{' '}
        <a href={href('/theming')}>theming</a>. The native tokens are generated from the same
        source.
      </li>
      <li>
        <a href={href('/components')}>Components</a>: every component, with its web, Android and iOS
        documentation on one page.
      </li>
      {present.length ? (
        <li>
          Guidelines:{' '}
          {present.map(([id, label], i) => (
            <span key={id}>
              {i > 0 ? ', ' : null}
              <a href={href(`/native/${id}`)}>{label}</a>
            </span>
          ))}
          .
        </li>
      ) : null}
      <li>
        Releases: <a href={href(`/${platform}/releases`)}>{PLATFORM_NAMES[platform]}</a>,{' '}
        <a href={href(`/${other}/releases`)}>{PLATFORM_NAMES[other]}</a>.
      </li>
    </ul>
  );
}
