import { Button } from '@rookcom/monarch-react';
import {
  CTABand,
  FAQ,
  FeatureGrid,
  MarketingSection,
  MarketingSectionHeader,
  ProductFrame,
  StatStrip,
} from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'marketing-section-blocks',
  title: 'Marketing sections',
  summary:
    'MarketingSection, MarketingSectionHeader, FeatureGrid, ProductFrame, StatStrip, CTABand and FAQ: the body of a public page.',
  sections: {
    purpose: {
      summary:
        'The building blocks between hero and footer. Each is a small, opinionated pattern so product sites share rhythm, type and colour without local CSS. Import from `@rookcom/monarch-react/marketing`.',
      useWhen: [
        'MarketingSection for every band after the hero; MarketingSectionHeader at its top.',
        'FeatureGrid for capabilities or benefits; ProductFrame for product illustrations.',
        'StatStrip for a few true figures; FAQ for buyer questions; CTABand once, at the end.',
      ],
      avoidWhen: [
        'Signed-in app screens: use Card, StatCard, SettingsSection and the app layouts.',
        'In-app section headings: use the planned `section-header`.',
      ],
    },
    designPrinciples: {
      items: [
        'Rhythm from tokens: sections pad with `--mn-section-{sm,md,lg}`, content sits in `--mn-width-{narrow,content,wide}`.',
        'One accent: eyebrows, feature icons and the CTA band use the product accent; gradients are derived from it, never hand-picked.',
        '`tone="inverse"` is the only dark band. Inside it Monarch remaps text, borders, surfaces, accent text and focus ring, all contrast-tested for every product. In dark themes it becomes an elevated surface instead of flipping to light.',
        'Illustrations are presentational: ProductFrame is one labelled image whose contents are inert.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'FeatureGrid renders a `<ul>`; items with `href` become one stretched link per card.',
        'StatStrip renders a `<dl>`: label (`dt`) first for screen readers, value shown first visually.',
        'FAQ wraps Accordion (`type="single" collapsible` unless `multiple`).',
        'CTABand on `tone="accent"` inverts primary and secondary Buttons inside it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone (MarketingSection)',
          values: ['default', 'subtle', 'inverse'],
          description: 'Canvas, quiet alternate band, or the one high-emphasis band.',
        },
        {
          prop: 'spacing (MarketingSection)',
          values: ['sm', 'md', 'lg'],
          description: 'Vertical padding from the section rhythm scale.',
        },
        {
          prop: 'width (MarketingSection)',
          values: ['narrow', 'content', 'wide'],
          description: '44rem, 72rem or 80rem content column.',
        },
        {
          prop: 'align / size (MarketingSectionHeader)',
          values: ['start | center', 'md | lg'],
          description: 'Alignment and display size of the title.',
        },
        {
          prop: 'columns / variant (FeatureGrid)',
          values: ['2 | 3 | 4', 'card | plain'],
          description: 'Columns at full width; bordered cards or a plain list.',
        },
        {
          prop: 'chrome / backdrop (ProductFrame)',
          values: ['window | browser | none', 'true | false'],
          description: 'App-window dots, a URL bar or nothing; optional soft accent panel behind.',
        },
        {
          prop: 'tone (CTABand)',
          values: ['accent', 'subtle', 'inverse'],
          description: 'Accent gradient (default), quiet panel, or dark panel.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Linked feature hover',
          attribute: 'data-linked',
          description: 'Border strengthens and a medium shadow appears.',
        },
        {
          name: 'FAQ item open',
          attribute: 'data-state="open"',
          description: 'From Accordion.',
        },
      ],
    },
    interaction: {
      items: [
        'Only linked FeatureGrid cards, FAQ questions and the actions you pass are interactive.',
        'ProductFrame contents never receive focus or pointer events (`inert`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Linked feature cards are one tab stop each; FAQ triggers follow.' },
        { keys: 'Enter / Space', action: 'Opens a linked card or toggles a FAQ answer.' },
        { keys: 'Arrow Up / Down, Home / End', action: 'Move between FAQ questions.' },
      ],
    },
    accessibility: {
      items: [
        'Label each MarketingSection with `aria-labelledby` pointing at its header `titleId`, so it becomes a named region.',
        'Headings come from props (`headingAs`) so the outline never skips: h2 for sections, h3 for features and questions.',
        'ProductFrame requires `label`; contents are `inert` so sample headings and inputs stay out of the outline and tab order.',
        'Feature icons are decorative (`aria-hidden`).',
        'Inverse sections keep 4.5:1 text contrast (7:1 more-contrast) for every Rook product, including accent text on cards within the band.',
      ],
    },
    responsive: {
      items: [
        'FeatureGrid: one column, two from 640px, three or four from 1024px.',
        'StatStrip: two columns, three or four from 1024px, with hairline separators that follow the grid.',
        'CTABand: stacked copy and actions; side by side from 1024px.',
      ],
    },
    theme: {
      items: [
        'Subtle bands use `--mn-color-bg-subtle`; inverse bands `--mn-gradient-inverse`; CTA `--mn-gradient-accent`; all follow the product accent.',
        'Verified in light, dark and more-contrast with the default, Dossier, Calendar and Base accents (visual baselines).',
      ],
    },
    examples: {
      items: [
        {
          title: 'Section with features',
          stack: true,
          render: () => (
            <MarketingSection spacing="sm" aria-labelledby="ex-features">
              <MarketingSectionHeader
                titleId="ex-features"
                eyebrow="The editor"
                title="Every idea gets the right shape."
                description="Type a slash and pick a block."
              />
              <FeatureGrid
                columns={2}
                items={[
                  {
                    title: 'Slash commands',
                    description: 'Tables, lists and callouts, one keystroke away.',
                  },
                  {
                    title: 'Version history',
                    description: 'Named snapshots you can restore safely.',
                  },
                ]}
              />
            </MarketingSection>
          ),
        },
        {
          title: 'Product frame, stats, FAQ and CTA',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 'var(--mn-space-10)' }}>
              <ProductFrame
                label="Share dialog with a copyable link"
                chrome="browser"
                title="example.rookhq.com/share"
              >
                <p style={{ margin: 0, padding: 'var(--mn-space-6)' }}>
                  Anyone with the link can view
                </p>
              </ProductFrame>
              <StatStrip
                items={[
                  { value: '3', label: 'Export formats' },
                  { value: '13', label: 'Interface languages' },
                ]}
              />
              <FAQ
                items={[
                  { question: 'Which formats can I export?', answer: 'HTML, Markdown and PDF.' },
                ]}
              />
              <CTABand
                headingAs="h3"
                title="Start with a blank page."
                actions={<Button variant="primary">Start writing</Button>}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Alternate default and subtle bands; use inverse at most once per page.',
        'Build ProductFrame contents from Monarch components so illustrations stay on-brand.',
        'Use only true, current numbers in StatStrip.',
      ],
      dont: [
        'Do not hand-pick colours or gradients (hex values, arbitrary colour classes, near-black section fills); use the tones.',
        'Do not use two CTA bands or more than one primary action per section.',
        'Do not use sparkle icons for AI features; use a plain Bot icon.',
        'Do not write FAQ entries that are internal notes ("Is this implemented?").',
      ],
    },
    related: {
      items: [
        { id: 'marketing-hero', reason: 'The section that opens the page.' },
        { id: 'marketing-site-shell', reason: 'Header and footer around the sections.' },
        { id: 'accordion', reason: 'The disclosure FAQ is built on.' },
        { id: 'stat-card', reason: 'Figures inside signed-in dashboards.' },
      ],
    },
    dependencies: {
      packages: [],
      components: ['accordion'],
      notes: [
        'Tokens: `--mn-display-*`, `--mn-section-*`, `--mn-width-*`, `--mn-gradient-{wash,accent,inverse}`, `--mn-color-*-inverse*`, `--mn-color-accent-strong-inverse`.',
      ],
    },
    testing: {
      unit: [
        'All blocks compose with no axe violations and render on the server.',
        'MarketingSection sets tone, spacing and width and is named by its h2.',
        'ProductFrame is one labelled image with inert contents.',
        'Linked FeatureGrid cards are one tab stop named by the title.',
        'StatStrip pairs dt labels with dd values.',
        'FAQ honours defaultOpen and toggles from the keyboard.',
        'CTABand heading level and tone.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__marketing in light, dark and more-contrast × 4 accents × 1280/390',
        'Three-browser axe: /__marketing',
      ],
    },
  },
} satisfies ComponentDocs;
