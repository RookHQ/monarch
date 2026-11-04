import { Button } from '@rookcom/monarch-react';
import { MarketingHero } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'marketing-hero',
  title: 'MarketingHero',
  summary: 'The opening section of a public page: eyebrow, the page h1, lead, actions and media.',
  sections: {
    purpose: {
      summary:
        'States what the product does for whom, offers the primary action, and shows the product. It owns the page’s single h1. Import from `@rookcom/monarch-react/marketing`.',
      useWhen: ['The top of a product site, launch page or solution page.'],
      avoidWhen: [
        'Signed-in app screens: use PageHeader.',
        'A second hero further down the page: use MarketingSection with MarketingSectionHeader.',
      ],
    },
    designPrinciples: {
      items: [
        'Type does the work: `--mn-display-xl` (Poppins 600, tight tracking), balanced line breaks, a muted lead capped at about 38rem.',
        'One accent moment: the eyebrow and primary action use the product accent; the background is a very subtle wash derived from it (`--mn-gradient-wash`).',
        'Hero actions are pill-shaped and one size up from the default, so the primary action is unmistakable.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Renders `<section class="mn-hero">`; place it first inside `<main>`.'],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['centered', 'split'],
          description:
            '`centered` stacks copy over media; `split` puts media beside the copy from 1024px.',
        },
        {
          prop: 'background',
          values: ['wash', 'none'],
          description: '`wash` (default) paints `--mn-gradient-wash`; `none` leaves the canvas.',
        },
        {
          prop: 'headingAs',
          values: ['h1', 'h2'],
          description: '`h2` only when the page title is rendered elsewhere.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Static',
          description: 'No interactive state of its own; the actions carry their own states.',
        },
      ],
    },
    interaction: {
      items: ['No interaction of its own. `actions` are ordinary Buttons or links.'],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive itself; focus moves through the actions in source order.'],
    },
    accessibility: {
      role: 'region (section)',
      items: [
        'Renders the page’s only h1 by default; give a separate `aria-label` only if the section needs a name other than the heading.',
        'Under 640px the actions stack full width, each at least 48px tall.',
        'Put product imagery in `media` as a ProductFrame so it is one labelled image, not a stream of fake headings and inputs.',
      ],
    },
    responsive: {
      items: [
        'Title, lead and section padding are fluid (`clamp()`) between 390px and 1440px.',
        '`split` becomes two columns at 1024px; below that media follows the copy.',
        'Actions stack full width below 640px.',
      ],
    },
    theme: {
      items: [
        'The wash, eyebrow and primary action follow the product accent (`data-mn-product`).',
        'Dark, light and more-contrast themes come from the semantic tokens; the wash stays subtle in each.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Centered',
          stack: true,
          render: () => (
            <MarketingHero
              headingAs="h2"
              eyebrow="Rook Example"
              title="Write it once. Find it whenever."
              lead="Structured documents for teams whose decisions need to stay connected."
              actions={
                <>
                  <Button variant="primary">Start writing</Button>
                  <Button variant="secondary">See how it works</Button>
                </>
              }
              note="Free for teams of up to 10."
            />
          ),
          code: `<MarketingHero
  eyebrow="Rook Example"
  title="Write it once. Find it whenever."
  lead="Structured documents for teams whose decisions need to stay connected."
  actions={<>
    <Button variant="primary">Start writing</Button>
    <Button variant="secondary">See how it works</Button>
  </>}
  note="Free for teams of up to 10."
  media={<ProductFrame label="…">…</ProductFrame>}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep the title under about eight words and the lead to one or two sentences.',
        'One primary action, at most one secondary.',
      ],
      dont: [
        'Do not override the title with light weights or arbitrary pixel font sizes.',
        'Do not add hand-picked gradients, glows or decorative shapes behind it.',
        'Do not invent proof (customer counts, ratings) for the note line.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-section-blocks', reason: 'Every section after the hero.' },
        { id: 'marketing-site-shell', reason: 'The header above and footer below.' },
        { id: 'page-header', reason: 'The equivalent for signed-in app screens.' },
      ],
    },
    dependencies: {
      packages: [],
      components: [],
      notes: ['Tokens: `--mn-display-xl*`, `--mn-lead*`, `--mn-section-*`, `--mn-gradient-wash`.'],
    },
    testing: {
      unit: [
        'Renders the h1 with eyebrow, lead, actions and note.',
        'layout, background and headingAs reach the DOM.',
        'No axe violations; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines and three-browser axe: /__marketing'],
    },
  },
} satisfies ComponentDocs;
