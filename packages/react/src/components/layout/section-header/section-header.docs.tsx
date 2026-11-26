import { useEffect, useState } from 'react';
import { Button, SectionHeader, SectionHeaderLink } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PlusIcon } from '../../../internal/docs-demo.js';

function BusyDemo() {
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    if (!busy) return;
    const t = setTimeout(() => setBusy(false), 1500);
    return () => clearTimeout(t);
  }, [busy]);
  return (
    <SectionHeader
      title="Traffic sources"
      meta="Last 7 days"
      busy={busy}
      actions={
        <Button size="xs" onClick={() => setBusy(true)}>
          Refresh
        </Button>
      }
    >
      <ul style={{ margin: 0, paddingInlineStart: '1.25rem' }}>
        <li>Search</li>
        <li>Direct</li>
        <li>Referral</li>
      </ul>
    </SectionHeader>
  );
}

export default {
  id: 'section-header',
  title: 'SectionHeader',
  summary: 'Unboxed section title with description, meta, actions, a trailing link and busy state.',
  sections: {
    purpose: {
      summary:
        'Titles a section of a page or dashboard without drawing a box around it: a heading, an optional description and meta line, and trailing actions or a "View all" link. With content it becomes the labelled section itself.',
      useWhen: [
        'A dashboard or detail page is split into sections, each with a title and maybe a count or "View all" link.',
        'The header row inside a Card needs actions or a link next to the title.',
        'A section reloads in place and should say so (`busy`).',
      ],
      avoidWhen: [
        'The page title: use PageHeader.',
        'A titled group of settings with a footer: use SettingsSection.',
      ],
    },
    designPrinciples: {
      items: [
        'Unboxed: it adds no border or background, so it works on the page, inside a Card or in a split pane.',
        'The heading level is chosen by the page outline (`headingLevel`), never by size; size is `size`.',
        'Secondary text is quiet: description in muted text, meta smaller still.',
        'Trailing content stays on the title row while it fits and wraps under it on narrow screens.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Without `children` it renders `<div class="mn-section-header">` with the header row; with `children` it renders `<section aria-labelledby>` with the row and a `.mn-section-header__body`. The ref goes to the root either way.',
        '`SectionHeaderLink` renders an `<a>` (or your router link with `asChild`) with a decorative arrow.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description:
            '`sm` (default, 14px semibold) for dashboard sections; `md` (16px) for page sections. Exposed as `data-size`.',
        },
        {
          prop: 'headingLevel',
          values: ['h2', 'h3', 'h4', 'h5', 'h6'],
          description: 'The heading element. Default `h2`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Busy',
          attribute: 'data-busy, aria-busy="true"',
          description:
            'A small spinner named by `busyLabel` ("Loading") follows the title, and the section content dims to 60% opacity.',
        },
      ],
    },
    interaction: {
      items: [
        'No behaviour of its own. Actions and links are ordinary controls in reading order after the title.',
        'Busy is driven by the product; content stays mounted while it dims.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the actions and then the trailing link, in source order.',
        },
      ],
      notes: ['SectionHeader adds no key handling; the slotted controls keep their own.'],
    },
    accessibility: {
      role: 'region (with children, via `<section aria-labelledby>`), otherwise none',
      name: 'The heading. Pass `headingId` to label another element with it.',
      items: [
        'Choose `headingLevel` so the outline does not skip levels.',
        'With `busy`, `aria-busy` is set on the root and the spinner is a named status.',
        'The link arrow is `aria-hidden`; write link text that makes sense alone ("View all invoices").',
      ],
    },
    responsive: {
      items: [
        'The row wraps: when the title and trailing controls do not fit, the controls move under the text (`flex-wrap`).',
        'Long titles wrap instead of overflowing (`overflow-wrap: anywhere`), so there is no horizontal scrolling at 320px.',
      ],
    },
    theme: {
      items: [
        'Text uses `--mn-color-text-default` and `--mn-color-text-muted`; no surfaces of its own, so it inherits the page or card behind it in every theme.',
        'The link focus ring uses `--mn-color-focus-ring`; the dim transition uses duration tokens, which are zero under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Meta and trailing link',
          stack: true,
          render: () => (
            <SectionHeader
              title="Recent invoices"
              meta="12 issued this month"
              link={
                <SectionHeaderLink href="#/components/section-header">
                  View all invoices
                </SectionHeaderLink>
              }
            />
          ),
        },
        {
          title: 'Description and actions',
          stack: true,
          render: () => (
            <SectionHeader
              size="md"
              title="Team members"
              description="People who can sign in to this workspace."
              actions={
                <Button size="sm" variant="primary" leadingIcon={<PlusIcon />}>
                  Invite
                </Button>
              }
            />
          ),
        },
        {
          title: 'Section with busy state',
          stack: true,
          render: () => <BusyDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Pick `headingLevel` from the page outline: h2 under the page h1, h3 inside an h2 section.',
        'Pass `children` when the header titles a whole section, so the section is a labelled region.',
        'Keep meta short: a count, a period, a freshness note.',
      ],
      dont: [
        'Do not use it as the page title.',
        'Do not use `size="md"` to fake a higher heading level, or a lower level to make text smaller.',
        'Do not put more than two or three trailing controls; move the rest into a menu.',
      ],
    },
    related: {
      items: [
        { id: 'page-header', reason: 'The page title, with breadcrumbs and page actions.' },
        { id: 'settings-section', reason: 'A titled card of settings rows with a footer.' },
        { id: 'card', reason: 'Boxes a section; put a SectionHeader inside it for actions.' },
        { id: 'section-card', reason: 'Planned card with a standard header and actions.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-slot', 'react'],
      components: ['spinner'],
    },
    testing: {
      unit: [
        'Renders title (h2), description, meta, actions and the trailing link; Tab order is actions then link; no region without children.',
        'With children it is a region labelled by its heading, honouring `headingLevel` and `headingId`.',
        'Busy sets aria-busy and data-busy and shows a named spinner, and clears when done.',
        'Forwards the ref, passes attributes through and exposes `size`.',
        'SectionHeaderLink renders a router link with `asChild`, with a hidden arrow.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/section-header'],
    },
  },
} satisfies ComponentDocs;
