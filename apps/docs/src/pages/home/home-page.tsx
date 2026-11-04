import { Fragment, type CSSProperties } from 'react';
import { Badge, Button, CopyField, Progress, Switch } from '@rookcom/monarch-react';
import {
  CTABand,
  FeatureGrid,
  MarketingHero,
  MarketingSection,
  MarketingSectionHeader,
} from '@rookcom/monarch-react/marketing';
import { CrownMark } from '../../features/brand/crown-mark.js';
import { ComponentCard } from '../../features/examples/component-card.js';
import { ThemeSwitcher } from '../../features/appearance/theme-switcher.js';
import { useAppearance } from '../../features/appearance/appearance-provider.js';
import { REPO_URL } from '../../layout/top-bar.js';
import { pageById } from '../../lib/docs.js';
import { facts, INSTALL_COMMAND } from '../../lib/facts.js';
import { DEFAULT_PRODUCT } from '../../lib/appearance.js';
import '../../styles/home.css';
import { products } from '../../lib/registry.js';
import { href } from '../../lib/router.js';
import {
  ArrowRightIcon,
  ContrastIcon,
  GridIcon,
  PaletteIcon,
  RepoIcon,
  TypeIcon,
} from '../../ui/icons.js';

/** Entrance delay for `.docs-rise` / `.docs-fade` (seconds). */
const delay = (s: number) => ({ '--docs-delay': `${s}s` }) as CSSProperties;

/** Components shown live on the home page; each links to its page. */
const SHOWCASE = ['button', 'segmented-control', 'tabs', 'status-badge', 'progress-bar', 'avatar'];

/** Product accents offered in the theming demo (the first few registered products). */
const DEMO_ACCENTS = products.slice(0, 5);

/** Crown and wordmark in a frosted pill with a soft four-colour glow. */
function Lockup() {
  return (
    <span className="docs-lockup docs-rise" style={delay(0.05)}>
      <CrownMark animated className="docs-lockup__mark" />
      <span className="docs-lockup__name">Monarch</span>
    </span>
  );
}

/** Tokens → Components → Conformance: the three packages, one Rook colour each. */
function StepFlow() {
  const steps = [
    ['Tokens', 'info'],
    ['Components', 'success'],
    ['Conformance', 'warning'],
  ] as const;
  return (
    <ol className="docs-steps-flow docs-rise" style={delay(0.5)} aria-label="What Monarch includes">
      {steps.map(([label, tone], i) => (
        <Fragment key={label}>
          {i > 0 ? (
            <li className="docs-steps-flow__arrow" aria-hidden>
              <svg viewBox="0 0 44 16" focusable="false">
                <path d="M0 8h41M34 1l7 7-7 7" />
              </svg>
            </li>
          ) : null}
          <li className="docs-steps-flow__step" data-tone={tone}>
            {label}
          </li>
        </Fragment>
      ))}
    </ol>
  );
}

/** The hill, ridge and flag from the Monarch launch page, drawn in theme tokens. */
function Horizon() {
  const ridge = 'M0 260 C 260 70, 520 20, 700 20 C 880 20, 1140 70, 1400 260';
  const road =
    'M716 22 C 690 30, 640 40, 660 56 C 690 78, 800 82, 790 112 C 778 150, 620 170, 560 260 ' +
    'L 700 260 C 760 180, 870 150, 860 108 C 850 70, 730 70, 712 52 C 700 40, 720 30, 724 22 Z';
  return (
    <div className="docs-horizon docs-fade" style={delay(0.6)} aria-hidden>
      <svg viewBox="0 0 1400 260" preserveAspectRatio="none" focusable="false">
        <defs>
          <linearGradient id="docs-hill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" className="docs-horizon__hill-top" />
            <stop offset="1" className="docs-horizon__hill-bottom" />
          </linearGradient>
          <linearGradient id="docs-ridge" x1="0" y1="0" x2="1" y2="0">
            <stop offset="0" className="docs-horizon__stop" data-tone="info" data-fade />
            <stop offset="0.2" className="docs-horizon__stop" data-tone="info" />
            <stop offset="0.4" className="docs-horizon__stop" data-tone="success" />
            <stop offset="0.62" className="docs-horizon__stop" data-tone="warning" />
            <stop offset="0.82" className="docs-horizon__stop" data-tone="danger" />
            <stop offset="1" className="docs-horizon__stop" data-tone="danger" data-fade />
          </linearGradient>
          <filter id="docs-glow" x="-5%" y="-50%" width="110%" height="200%">
            <feGaussianBlur stdDeviation="6" />
          </filter>
        </defs>
        <path d={`${ridge} Z`} fill="url(#docs-hill)" />
        <path d={road} className="docs-horizon__road" />
        <path
          d={ridge}
          fill="none"
          stroke="url(#docs-ridge)"
          strokeWidth="6"
          opacity="0.45"
          filter="url(#docs-glow)"
          vectorEffect="non-scaling-stroke"
        />
        <path
          d={ridge}
          fill="none"
          stroke="url(#docs-ridge)"
          strokeWidth="1.5"
          opacity="0.9"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
      <div className="docs-flag">
        <svg viewBox="0 0 70 80" focusable="false">
          <line x1="5" y1="5" x2="5" y2="78" className="docs-flag__pole" />
          <circle cx="5" cy="4" r="2.4" className="docs-flag__tip" />
          <g className="docs-flag__cloth">
            <path d="M6 8 C 22 4, 38 12, 60 8 L 60 38 C 38 42, 22 34, 6 38 Z" />
          </g>
        </svg>
      </div>
    </div>
  );
}

function Hero() {
  return (
    <section className="docs-hero" aria-labelledby="home-title">
      <div className="docs-hero__spectrum" aria-hidden />
      <div className="docs-hero__beam" aria-hidden />
      <div className="docs-hero__grid" aria-hidden />
      <MarketingHero
        id="home-hero"
        className="docs-hero__copy"
        background="none"
        headingAs="h1"
        eyebrow={<Lockup />}
        title={
          <span id="home-title" className="docs-hero__block docs-rise" style={delay(0.15)}>
            <span className="docs-hero__title">Meet Monarch.</span>
            <span className="docs-hero__tagline">Rook’s open-source design system.</span>
          </span>
        }
        lead={
          <span className="docs-hero__block docs-rise" style={delay(0.28)}>
            Design tokens, accessible React components and conformance tooling, built for every Rook
            product and free to use under the MIT License.
          </span>
        }
        actions={
          <>
            <Button asChild variant="primary" trailingIcon={<ArrowRightIcon />}>
              <a href={href('/getting-started')}>Get started</a>
            </Button>
            <Button asChild leadingIcon={<RepoIcon />}>
              <a href={REPO_URL}>GitHub</a>
            </Button>
          </>
        }
      />
      <div className="docs-hero__extras">
        <div className="docs-hero__install docs-rise" style={delay(0.4)}>
          <CopyField label="Install command" value={INSTALL_COMMAND} mono />
        </div>
        <StepFlow />
      </div>
      <Horizon />
    </section>
  );
}

function Foundations() {
  return (
    <MarketingSection spacing="md" aria-labelledby="home-foundations">
      <MarketingSectionHeader
        titleId="home-foundations"
        eyebrow="Foundations"
        title="One set of tokens under everything"
        description="Colour, type, spacing, radius, elevation and motion are CSS variables. Components use nothing else, so a theme or accent change reaches every control."
      />
      <FeatureGrid
        columns={4}
        items={[
          {
            icon: <PaletteIcon size={20} />,
            title: 'Colours',
            description: 'Brand colours and semantic roles, with live contrast ratios.',
            href: href('/colours'),
          },
          {
            icon: <TypeIcon size={20} />,
            title: 'Typography',
            description: 'A fixed text scale for apps and a fluid display scale for pages.',
            href: href('/typography'),
          },
          {
            icon: <GridIcon size={20} />,
            title: 'Tokens',
            description: 'Spacing on a 4px grid, radius, elevation, motion and layout.',
            href: href('/tokens'),
          },
          {
            icon: <ContrastIcon size={20} />,
            title: 'Theming',
            description: 'Light, dark and high contrast, and an accent per product.',
            href: href('/theming'),
          },
        ]}
      />
    </MarketingSection>
  );
}

function Components() {
  const showcase = SHOWCASE.map((id) => pageById[id]).filter((p) => p !== undefined);
  return (
    <MarketingSection spacing="md" tone="subtle" aria-labelledby="home-components">
      <MarketingSectionHeader
        titleId="home-components"
        eyebrow="Components"
        title="Accessible components, documented by their code"
        description={`${facts.components} components and ${facts.patterns} patterns with ${facts.examples} live examples. Every page is generated from the component’s docs file and TypeScript declarations.`}
        actions={
          <Button asChild trailingIcon={<ArrowRightIcon />}>
            <a href={href('/components')}>Browse all</a>
          </Button>
        }
      />
      <ul className="docs-cards docs-cards--home">
        {showcase.map((p) => (
          <ComponentCard key={p.id} page={p} />
        ))}
      </ul>
    </MarketingSection>
  );
}

function Theming() {
  const { prefs, update } = useAppearance();
  return (
    <MarketingSection spacing="md" aria-labelledby="home-theming">
      <div className="docs-split">
        <div className="docs-split__text">
          <MarketingSectionHeader
            titleId="home-theming"
            eyebrow="Accessibility and theming"
            title="Light, dark, high contrast, and your accent"
            description="Every product accent is adjusted at build time until text, focus rings and states meet WCAG 2.2 AA in each theme. Try it: these controls change this whole site."
          />
          <ul className="docs-checks">
            <li>Keyboard support, focus management and accessible names in every component</li>
            <li>Component tests run axe and server rendering</li>
            <li>This site is checked with axe in Chromium, Firefox and WebKit</li>
            <li>Motion collapses under reduced-motion settings</li>
          </ul>
        </div>
        <div className="docs-lab" role="group" aria-label="Try the appearance settings">
          <div className="docs-lab__row">
            <span className="docs-lab__label">Theme</span>
            <ThemeSwitcher />
          </div>
          <div className="docs-lab__row">
            <Switch
              label="More contrast"
              checked={prefs.contrast === 'more'}
              onCheckedChange={(on) => update({ contrast: on ? 'more' : 'standard' })}
            />
          </div>
          <div className="docs-lab__row">
            <span className="docs-lab__label" id="home-accent-label">
              Accent
            </span>
            <div className="docs-lab__accents" role="group" aria-labelledby="home-accent-label">
              {[{ id: DEFAULT_PRODUCT, name: 'Rook' }, ...DEMO_ACCENTS].map((p) => (
                <button
                  key={p.id}
                  type="button"
                  className="docs-lab__accent"
                  data-mn-product={p.id}
                  aria-pressed={prefs.product === p.id}
                  onClick={() => update({ product: p.id })}
                >
                  <span className="docs-lab__dot" aria-hidden />
                  {p.name.replace(/^Rook /, '')}
                </button>
              ))}
            </div>
          </div>
          <div className="docs-lab__sample">
            <div className="docs-lab__sample-row">
              <Button variant="primary">Save changes</Button>
              <Button>Cancel</Button>
              <Badge tone="accent">New</Badge>
            </div>
            <Progress value={64} label="Upload progress" />
          </div>
        </div>
      </div>
    </MarketingSection>
  );
}

function OpenSource() {
  return (
    <MarketingSection spacing="md">
      <CTABand
        tone="inverse"
        aria-labelledby="home-open-source"
        title={<span id="home-open-source">Open source, MIT-licensed</span>}
        description="Read the code, report issues and contribute on GitHub. Use Monarch in your own projects under the MIT License."
        actions={
          <>
            <Button asChild variant="primary" leadingIcon={<RepoIcon />}>
              <a href={REPO_URL}>View on GitHub</a>
            </Button>
            <Button asChild>
              <a href={`${REPO_URL}/blob/main/CONTRIBUTING.md`}>How to contribute</a>
            </Button>
          </>
        }
      />
    </MarketingSection>
  );
}

export function HomePage() {
  return (
    <div className="docs-home">
      <Hero />
      <Foundations />
      <Components />
      <Theming />
      <OpenSource />
    </div>
  );
}
