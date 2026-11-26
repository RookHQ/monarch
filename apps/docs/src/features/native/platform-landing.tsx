/**
 * The landing page of a native platform, `/ios/` and `/android/`: one layout,
 * the words in platform-landing-content.ts, the counts, install snippet and
 * code read at build time (plugins/native.ts), and pictures drawn by the
 * platform's sample app: the showcase screen, the category tiles and the
 * cards around the hero phone (scripts/capture-native-screens.sh). The long
 * form is on `/<platform>/get-started/` and `/<platform>/guide/`.
 */
import '../../styles/platform-landing.css';
import { useEffect, type ReactElement } from 'react';
import { Badge, Button } from '@rookcom/monarch-react';
import {
  CTABand,
  FAQ,
  FeatureGrid,
  MarketingSection,
  MarketingSectionHeader,
} from '@rookcom/monarch-react/marketing';
import type { NativeFacts } from '../../../plugins/native.js';
import { BASE, href, useLocation } from '../../lib/router.js';
import { movedSection } from './moved-sections.js';
import {
  ArrowRightIcon,
  ContrastIcon,
  GridIcon,
  LayersIcon,
  PaletteIcon,
  ShieldIcon,
  SlidersIcon,
  TypeIcon,
} from '../../ui/icons.js';
import { exampleCode } from './native-docs.js';
import { NativeCode } from './native-code.js';
import {
  LANDING,
  LANDING_TILES,
  type LandingIcon,
  type LandingScreen,
} from './platform-landing-content.js';
import SCREEN_SIZES from './native-screens.json';
import type { Platform } from './labels.js';

type Theme = 'light' | 'dark';

/** What `virtual:monarch-native-platform/<platform>` gives the landing page. */
export interface LandingData {
  groups: { group: string; count: number }[];
  install: { lang: string; code: string } | null;
  code: {
    id: string;
    title: string;
    example: { title: string; code: string; setup?: string; imports?: string[] };
  }[];
}

const SIZES: Readonly<Record<string, { width: number; height: number }>> = SCREEN_SIZES;

const ICONS: Readonly<Record<LandingIcon, () => ReactElement>> = {
  type: () => <TypeIcon />,
  shield: () => <ShieldIcon />,
  contrast: () => <ContrastIcon />,
  layers: () => <LayersIcon />,
  grid: () => <GridIcon />,
  palette: () => <PaletteIcon />,
  sliders: () => <SlidersIcon />,
};

/** The number of themes every component is drawn and tested in. */
const THEMES = 4;

/** The Android status bar the page draws over the space above each screenshot. */
function AndroidStatusBar() {
  return (
    <span className="docs-device__status" aria-hidden>
      <span className="docs-device__time">9:41</span>
      <span className="docs-device__camera" />
      <svg className="docs-device__icons" viewBox="0 0 52 14" focusable="false">
        {/* Wi-Fi */}
        <path d="M7 12.5 1 5.6a9.2 9.2 0 0 1 12 0Z" />
        {/* Signal */}
        <path d="M17 12.5 27 2.5v10Z" />
        {/* Battery */}
        <rect x="34" y="2" width="16" height="10.5" rx="2.4" />
      </svg>
    </span>
  );
}

/** A phone or tablet drawn in CSS around a real screenshot of the catalogue app. */
function Device({
  screen,
  theme,
  alt,
  priority = false,
  className,
}: {
  screen: LandingScreen;
  theme: Theme;
  alt: string;
  /** The page's largest image: loaded first, never lazily. */
  priority?: boolean;
  className?: string;
}) {
  const file = `${screen.file}.${theme}.webp`;
  const size = SIZES[file];
  const android = screen.device.startsWith('android');
  return (
    <div
      className={className ? `docs-device ${className}` : 'docs-device'}
      data-device={screen.device}
      data-theme={theme}
    >
      {/* Hardware is dark in both site themes; the screen shows its screenshot's theme. */}
      <div className="docs-device__body" data-mn-theme="dark">
        <div className="docs-device__screen" data-mn-theme={theme}>
          <img
            className="docs-device__img"
            src={`${BASE}native/${file}`}
            width={size?.width}
            height={size?.height}
            alt={alt}
            decoding="async"
            {...(priority ? { fetchPriority: 'high' as const } : { loading: 'lazy' as const })}
          />
          {android ? <AndroidStatusBar /> : null}
          {android ? <span className="docs-device__handle" aria-hidden /> : null}
        </div>
      </div>
    </div>
  );
}

/** A picture drawn by the sample app in both themes; the one matching the site theme is shown. */
function ThemedImage({ file, alt, className }: { file: string; alt: string; className: string }) {
  return (
    <span className={`docs-pl-themed ${className}`}>
      {(['light', 'dark'] as const).map((theme) => {
        const name = `${file}.${theme}.webp`;
        const size = SIZES[name];
        return (
          <img
            key={theme}
            data-theme={theme}
            src={`${BASE}native/${name}`}
            width={size?.width}
            height={size?.height}
            alt={alt}
            loading="lazy"
            decoding="async"
          />
        );
      })}
    </span>
  );
}

/** A small card from the showcase, beside the hero phone, in the phone's theme. */
function Float({ file, theme, className }: { file: string; theme: Theme; className: string }) {
  const name = `${file}.${theme}.webp`;
  const size = SIZES[name];
  return (
    <img
      className={`docs-pl-float ${className}`}
      src={`${BASE}native/${name}`}
      width={size ? size.width / 2 : undefined}
      height={size ? size.height / 2 : undefined}
      alt=""
      decoding="async"
      {...(theme === 'dark' ? { loading: 'lazy' as const } : {})}
    />
  );
}

export function PlatformLanding({
  platform,
  facts,
  data,
}: {
  platform: Platform;
  facts: NativeFacts[Platform];
  data: LandingData;
}) {
  const c = LANDING[platform];
  const { phone, tablet } = c.screens;
  const { anchor } = useLocation();
  // An old link to a section of the one-page `/<platform>/` opens that section's new page.
  useEffect(() => {
    const to = movedSection(platform, anchor);
    if (!to) return;
    window.history.replaceState(null, '', href(to, anchor ?? undefined));
    window.dispatchEvent(new PopStateEvent('popstate'));
  }, [platform, anchor]);
  const id = (section: string) => `${platform}-${section}`;
  const counts = new Map(data.groups.map((g) => [g.group, g.count]));
  const tileCount = (groups: readonly string[]) =>
    groups.length === 0
      ? `${THEMES} themes`
      : `${groups.reduce((n, g) => n + (counts.get(g) ?? 0), 0)} components`;
  const tileHref = (tile: (typeof LANDING_TILES)[number]) =>
    tile.groups[0]
      ? href(`/${platform}/components`, `family-${tile.groups[0]}`)
      : href(`/${platform}/get-started`, 'theme-setup');
  const subject =
    'The Overview screen of the demo workspace Acme Studio, built from Monarch components';

  return (
    <div className="docs-pl" data-platform={platform}>
      <section className="docs-pl-hero" aria-labelledby={id('title')}>
        <div className="docs-pl-hero__inner">
          <div className="docs-pl-hero__copy">
            <p className="docs-pl-hero__eyebrow">
              <span>{c.product}</span>
              {facts.version ? (
                <Badge size="sm" variant="outline">
                  {facts.version}
                </Badge>
              ) : null}
              {facts.unreleased ? (
                <Badge size="sm" tone="warning">
                  Unreleased
                </Badge>
              ) : null}
            </p>
            <h1 id={id('title')} className="docs-pl-hero__title">
              <span>{c.headline[0]}</span> <span>{c.headline[1]}</span>
            </h1>
            <p className="docs-pl-hero__lede">{c.lede}</p>
            <div className="docs-pl-hero__actions">
              <Button asChild variant="primary" trailingIcon={<ArrowRightIcon />}>
                <a href={href(`/${platform}/get-started`)}>Get started</a>
              </Button>
              <Button asChild>
                <a href={href(`/${platform}/components`)}>Browse components</a>
              </Button>
            </div>
          </div>
          <div className="docs-pl-hero__stage">
            {(['light', 'dark'] as const).map((theme) => (
              <div key={theme} className="docs-pl-hero__set" data-theme={theme}>
                <Device
                  className="docs-pl-hero__device"
                  screen={phone}
                  theme={theme}
                  priority={theme === 'light'}
                  alt={`${subject}, on ${phone.label} in the ${theme} theme.`}
                />
                <Float
                  file={`${platform}-float-toggle`}
                  theme={theme}
                  className="docs-pl-float--toggle"
                />
                <Float
                  file={`${platform}-float-progress`}
                  theme={theme}
                  className="docs-pl-float--progress"
                />
                <Float
                  file={`${platform}-float-badges`}
                  theme={theme}
                  className="docs-pl-float--badges"
                />
              </div>
            ))}
          </div>
        </div>
      </section>

      <MarketingSection spacing="md" aria-labelledby={id('features')} id="features">
        <MarketingSectionHeader
          titleId={id('features')}
          align="center"
          eyebrow="Built for the platform"
          title="Native by design."
        />
        <FeatureGrid
          className="docs-pl-features"
          columns={3}
          variant="card"
          headingAs="h3"
          items={c.features.map((f) => ({
            icon: ICONS[f.icon](),
            title: f.title,
            description: f.text,
          }))}
        />
      </MarketingSection>

      <MarketingSection
        tone="subtle"
        spacing="md"
        width="wide"
        aria-labelledby={id('devices')}
        id="devices"
      >
        <MarketingSectionHeader
          titleId={id('devices')}
          align="center"
          eyebrow="Light and dark"
          title={c.devicesTitle}
        />
        <div className="docs-pl-stages">
          {(['light', 'dark'] as const).map((theme) => (
            <figure key={theme} className="docs-pl-stage" data-theme={theme}>
              <div className="docs-pl-stage__devices">
                <Device
                  className="docs-pl-stage__tablet"
                  screen={tablet}
                  theme={theme}
                  alt={`${subject}, on ${tablet.label} in the ${theme} theme, with a sidebar.`}
                />
                <Device
                  className="docs-pl-stage__phone"
                  screen={phone}
                  theme={theme}
                  alt={`The same screen on ${phone.label} in the ${theme} theme, with a tab bar.`}
                />
              </div>
              <figcaption className="docs-pl-stage__caption">
                {theme === 'light' ? 'Light' : 'Dark'}
              </figcaption>
            </figure>
          ))}
        </div>
      </MarketingSection>

      <MarketingSection spacing="md" width="wide" aria-labelledby={id('catalogue')} id="catalogue">
        <MarketingSectionHeader
          titleId={id('catalogue')}
          eyebrow="Components"
          title={`${facts.covered} components.`}
          actions={
            <Button asChild trailingIcon={<ArrowRightIcon />}>
              <a href={href(`/${platform}/components`)}>Browse all</a>
            </Button>
          }
        />
        <ul className="docs-pl-tiles">
          {LANDING_TILES.map((tile) => (
            <li key={tile.id}>
              <a className="docs-pl-tile" href={tileHref(tile)}>
                <ThemedImage
                  className="docs-pl-tile__media"
                  file={`${platform}-tile-${tile.id}`}
                  alt={`${tile.shows}, on ${c.name}.`}
                />
                <span className="docs-pl-tile__text">
                  <span className="docs-pl-tile__title">{tile.title}</span>
                  <span className="docs-pl-tile__count">{tileCount(tile.groups)}</span>
                </span>
              </a>
            </li>
          ))}
        </ul>
      </MarketingSection>

      {data.code.length ? (
        <MarketingSection spacing="md" aria-labelledby={id('code')} id="code">
          <MarketingSectionHeader
            titleId={id('code')}
            align="center"
            eyebrow={platform === 'ios' ? 'Swift' : 'Kotlin'}
            title={c.codeTitle}
          />
          <div className="docs-pl-code">
            {data.code.map((e) => (
              <figure key={e.id} className="docs-pl-code__item">
                <figcaption className="docs-pl-code__caption">
                  <a href={href(`/components/${e.id}`, platform)}>{e.title}</a>
                </figcaption>
                <NativeCode
                  platform={platform}
                  code={exampleCode(e.example)}
                  label={`Code: ${e.title}, ${e.example.title}`}
                />
              </figure>
            ))}
          </div>
        </MarketingSection>
      ) : null}

      {data.install ? (
        <MarketingSection spacing="md" width="narrow" aria-labelledby={id('install')} id="install">
          <MarketingSectionHeader
            titleId={id('install')}
            align="center"
            eyebrow={platform === 'ios' ? 'Swift Package Manager' : 'Gradle'}
            title="Install."
          />
          <div className="docs-pl-install">
            <NativeCode
              platform={platform}
              code={data.install.code}
              label={`Code: install Monarch for ${c.name}`}
            />
            {facts.unreleased ? (
              <a
                className="docs-pl-install__note"
                href={href(`/${platform}/get-started`, 'installation')}
              >
                <Badge size="sm" tone="warning">
                  Unreleased
                </Badge>
                <span>Install from a local checkout</span>
              </a>
            ) : null}
          </div>
        </MarketingSection>
      ) : null}

      <MarketingSection spacing="md" aria-labelledby={id('faq')} id="faq">
        <div className="docs-pl-split">
          <MarketingSectionHeader titleId={id('faq')} eyebrow="Questions" title="Good to know." />
          <FAQ
            className="docs-pl-faq"
            multiple
            items={c.faq.map((f) => ({ question: f.question, answer: f.answer }))}
          />
        </div>
      </MarketingSection>

      <MarketingSection spacing="md" id="start">
        <CTABand
          tone="subtle"
          aria-labelledby={id('start')}
          title={<span id={id('start')}>Start building for {c.name}.</span>}
          actions={
            <>
              <Button asChild variant="primary" trailingIcon={<ArrowRightIcon />}>
                <a href={href(`/${platform}/get-started`)}>Get started</a>
              </Button>
              <Button asChild>
                <a href={href(`/${platform}/guide`)}>Read the guide</a>
              </Button>
            </>
          }
        />
        <p className="docs-pl-trademarks">
          {c.trademarks} <a href={href(`/${platform}/guide`, 'platform-terms')}>Platform terms</a>
        </p>
      </MarketingSection>
    </div>
  );
}
