import { LogoCloud } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const placeholderNames = [
  'Company A',
  'Company B',
  'Company C',
  'Company D',
  'Company E',
  'Company F',
];

export default {
  id: 'logo-cloud',
  title: 'LogoCloud',
  summary: 'A titled row or grid of organisation logos, with safe URLs and a wordmark fallback.',
  sections: {
    purpose: {
      summary:
        'Shows the organisations a product can name (customers, partners, integrations) on a public page, without each site re-building the strip. Needed by: Recruit (`rookrecruit-frontend/src/components/marketing/shared/LogoCloud.tsx`, a names-only strip), and any product marketing page with a partner or integration strip.',
      useWhen: [
        'A strip of organisations under a hero, with permission to show them.',
        'A grid of integration or partner logos.',
      ],
      avoidWhen: [
        'Testimonials with quotes: use a card layout with the quote and attribution.',
        'Organisations you do not have permission to name.',
        'Navigation to integrations with descriptions: use a FeatureGrid of cards.',
      ],
    },
    designPrinciples: {
      items: [
        'Only list real organisations with permission; docs and demos use placeholders like "Company A".',
        'Logos are quiet by default (monochrome), so the strip supports the page rather than competing with it.',
        'Every logo has a text equivalent: the image alt, or the name itself when the image is missing.',
        'URLs from data are checked; unsafe image or link URLs are dropped, never rendered.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<section>`.',
        '`defaultLogoCloudMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the Recruit copy: `items: string[]` → `logos={items.map((name) => ({ name }))}`, `label` → `title` (use `titleAs="p"` to keep it out of the heading outline, as the copy did).',
        'The Recruit copy is not a shadcn/ui component; there is no shadcn prop mapping.',
        'Images go through `isSafeImageSrc` (http, https, same-origin, blob) and links through `isSafeUrl`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['row', 'grid'],
          description: 'Centred wrapping row, or equal bordered cells.',
        },
        { prop: 'size', values: ['sm', 'md', 'lg'], description: 'Logo height: 24, 32 or 44px.' },
        {
          prop: 'monochrome',
          values: ['true', 'false'],
          description: 'Greyscale, slightly faded logos that regain colour on hover or focus.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Layout', attribute: 'data-layout', description: '`row` or `grid`.' },
        { name: 'Size', attribute: 'data-size', description: '`sm`, `md` or `lg`.' },
        { name: 'Monochrome', attribute: 'data-monochrome', description: 'Logos are greyscale.' },
        {
          name: 'Wordmark fallback',
          description: 'No image, an unsafe image URL or a failed load: the name is shown as text.',
        },
      ],
    },
    interaction: {
      items: [
        'Logos with an `href` are links; others are not interactive.',
        'External links open in a new tab and say so to screen readers.',
      ],
    },
    keyboard: {
      items: [{ keys: 'Tab', action: 'Moves between linked logos.' }],
      notes: ['Unlinked logos are not focusable.'],
    },
    accessibility: {
      role: 'region (section) with a list',
      name: 'The `title`, or `messages.listLabel` ("Organisations") without one.',
      items: [
        'Each logo is a list item; images use the organisation name as alt text.',
        'Links that open a new tab add hidden "(opens in a new tab)" text.',
        'Linked logos have a 44px minimum height for touch.',
      ],
    },
    responsive: {
      items: [
        'The row wraps and stays centred; gaps tighten below 480px.',
        'The grid fits as many 144px cells as the width allows, down to one column at 390px.',
        'Images keep their aspect ratio at a fixed height, never wider than their cell.',
      ],
    },
    theme: {
      items: [
        'Text, border and focus tokens only; works in light, dark and every product accent.',
        '`srcDark` swaps in a dark-theme logo through `<picture>`.',
        'Reduced motion removes the colour transition; forced colours keep borders and text in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Wordmarks in a row',
          render: () => (
            <LogoCloud
              title="Placeholder organisations"
              logos={placeholderNames.map((name) => ({ name }))}
            />
          ),
        },
        {
          title: 'Grid with links',
          render: () => (
            <LogoCloud
              title="Integrations"
              titleAs="h3"
              layout="grid"
              size="sm"
              logos={placeholderNames.slice(0, 4).map((name, i) => ({
                name,
                href: `#integration-${i + 1}`,
              }))}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Get permission before showing an organisation’s logo.',
        'Give image `width` and `height` to avoid layout shift.',
        'Provide `srcDark` for logos that disappear on dark backgrounds.',
      ],
      dont: [
        'Do not invent or imply customers in demos; use placeholders.',
        'Do not add numbers or claims ("trusted by 10,000 teams") to the title without a source.',
        'Do not link every logo to the organisation’s home page; link only to your own stories.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-section-blocks', reason: 'Wrap the cloud in a MarketingSection band.' },
        { id: 'marketing-hero', reason: 'The hero the strip usually follows.' },
        { id: 'comparison-section', reason: 'Comparisons with other tools.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Section named by the title; list of image logos and wordmarks; axe clean.',
        'Unsafe image and link URLs dropped; names shown instead.',
        'Image load failure falls back to the wordmark.',
        'Links reachable by Tab; external links say they open a new tab; axe clean.',
        'Fallback list name, layout and size data attributes, ref forwarding.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
