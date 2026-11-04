import { Button } from '@rookcom/monarch-react';
import { SiteFooter, SiteHeader } from '@rookcom/monarch-react/marketing';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'marketing-site-shell',
  title: 'SiteHeader and SiteFooter',
  summary:
    'The frame of a public page: wordmark, navigation with disclosure panels, actions, and a footer of link columns.',
  sections: {
    purpose: {
      summary:
        'SiteHeader and SiteFooter give every Rook public site the same navigation model and landmarks. Import from `@rookcom/monarch-react/marketing`.',
      useWhen: ['Product sites, launch pages, docs landing pages and other public pages.'],
      avoidWhen: [
        'Signed-in app screens: use the Rook app shell (RookRail).',
        'Dialog-like flows such as sign-in, where navigation would distract.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet chrome: a translucent canvas bar with a hairline border, muted links that darken on hover; the wordmark and one primary action carry the brand.',
        'Group items open a panel of links with descriptions, not an application menu: they are navigation, so they stay links.',
        'No artwork ships in the package. `brand` is a slot for the product wordmark from the Rook brand kit.',
        'Footer column titles are real headings and links meet the 24px target size.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`nav` takes `SiteLink` (`label`, `href`, `description?`, `current?`, `external?`) and `SiteNavGroup` (`label`, `items`).',
        '`linkAs` renders every link with your router (for example Next.js `Link`); it receives `href`, `className`, `aria-current`, and `target`/`rel` for external links.',
        'Group panels use Radix Popover, portalled to the body.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'sticky (SiteHeader)',
          values: ['true', 'false'],
          description: 'Stick to the top while scrolling (default true).',
        },
        {
          prop: 'headingAs (SiteFooter)',
          values: ['h2', 'h3', 'h4'],
          description: 'Heading level of the column titles.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current page',
          attribute: 'aria-current="page"',
          description: 'Set `current` on a link; it renders in the default text colour.',
        },
        {
          name: 'Group open',
          attribute: 'aria-expanded="true"',
          description: 'Trigger tinted, chevron rotated, panel shown.',
        },
        {
          name: 'Mobile menu open',
          attribute: 'aria-expanded="true" (toggle)',
          description: 'Below 768px the toggle shows a close icon and the panel lists every link.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a group trigger opens its panel; clicking outside, Escape or choosing a link closes it.',
        'Below 768px, links and actions move into a disclosure panel under the bar; Escape closes it and returns focus to the toggle.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through wordmark, links, group triggers and actions.' },
        { keys: 'Enter / Space', action: 'Opens a group panel or the mobile menu.' },
        { keys: 'Escape', action: 'Closes the open panel and returns focus to its trigger.' },
      ],
    },
    accessibility: {
      role: 'banner and contentinfo landmarks, each with a labelled navigation',
      name: '`navLabel` (default "Main" in the header, "Footer" in the footer).',
      items: [
        'The mobile toggle names its action ("Open menu" / "Close menu") and has `aria-expanded` and `aria-controls`.',
        'External links get target and rel attributes and a visually hidden "(opens in a new tab)".',
        'Header targets are at least 36px tall, mobile panel links 44px, footer links 32px (WCAG 2.5.8 needs 24px).',
      ],
    },
    responsive: {
      items: [
        'From 768px: inline navigation and actions. Below: wordmark plus a 44px menu toggle.',
        'Footer: brand above the columns until 1024px, beside them after; columns go from two to auto-fit.',
      ],
    },
    theme: {
      items: [
        'All colours are semantic tokens; the bar is `--mn-color-bg-canvas` at 86% with a backdrop blur.',
        'Works unchanged in light, dark and more-contrast themes and every product accent.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Header and footer',
          stack: true,
          render: () => (
            <div>
              <SiteHeader
                sticky={false}
                brand={<a href="#/components/marketing-site-shell">Rook Example</a>}
                nav={[
                  {
                    label: 'Product',
                    items: [
                      {
                        label: 'Editor',
                        href: '#/components/marketing-site-shell',
                        description: 'Pages and blocks',
                      },
                      {
                        label: 'Search',
                        href: '#/components/marketing-site-shell',
                        description: 'Find any page',
                      },
                    ],
                  },
                  { label: 'Pricing', href: '#/components/marketing-site-shell' },
                ]}
                actions={
                  <>
                    <Button variant="ghost" size="sm">
                      Log in
                    </Button>
                    <Button variant="primary" size="sm">
                      Start free
                    </Button>
                  </>
                }
              />
              <SiteFooter
                columns={[
                  {
                    title: 'Product',
                    links: [{ label: 'Editor', href: '#/components/marketing-site-shell' }],
                  },
                  {
                    title: 'Company',
                    links: [{ label: 'About Rook', href: '#/components/marketing-site-shell' }],
                  },
                ]}
                legal="© 2026 Rook"
                legalLinks={[{ label: 'Privacy', href: '#/components/marketing-site-shell' }]}
              />
            </div>
          ),
          code: `<SiteHeader
  brand={<Link href="/"><img src="/brand/wordmark.svg" alt="Rook Example" /></Link>}
  linkAs={Link}
  nav={[
    { label: 'Product', items: [{ label: 'Editor', href: '/editor', description: 'Pages and blocks' }] },
    { label: 'Pricing', href: '/pricing' },
  ]}
  actions={<>
    <Button variant="ghost" size="sm">Log in</Button>
    <Button variant="primary" size="sm">Start free</Button>
  </>}
/>
…
<SiteFooter
  columns={[{ title: 'Product', links: [{ label: 'Editor', href: '/editor' }] }]}
  legal="© 2026 Rook"
  legalLinks={[{ label: 'Privacy', href: '/privacy' }]}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use the product wordmark from the brand kit as `brand`, linked home, with the product name as alt text.',
        'Keep actions to "Log in" (ghost) and one primary.',
        'Pass `linkAs` so navigation uses client-side routing.',
      ],
      dont: [
        'Do not put application menus (actions, checkboxes) in nav groups.',
        'Do not add banners, crowns, gold or decorative artwork to the bar.',
        'Do not list pages that do not exist yet.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-hero', reason: 'Directly below the header.' },
        { id: 'marketing-section-blocks', reason: 'The page body between header and footer.' },
        { id: 'popover', reason: 'The primitive behind group panels.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-popover', 'react'],
      components: [],
    },
    testing: {
      unit: [
        'Group triggers open a panel of links; current page is marked.',
        'Mobile toggle has aria-expanded, closes on Escape and restores focus.',
        'Footer column titles are headings; external links announce the new tab.',
        'Landmarks are present with no axe violations; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines and three-browser axe: /__marketing'],
    },
  },
} satisfies ComponentDocs;
