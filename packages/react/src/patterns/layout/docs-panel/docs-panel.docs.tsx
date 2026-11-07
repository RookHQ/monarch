import { useState } from 'react';
import { DocsPanel, type LinkedDocItem } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const START: LinkedDocItem[] = [
  {
    id: 'l1',
    kind: 'doc',
    docId: 'd1',
    title: 'Release runbook',
    href: '#doc-release-runbook',
    meta: 'Updated 2 hours ago',
    status: 'Published',
    space: { id: 's-eng', name: 'Engineering' },
  },
  {
    id: 'l2',
    kind: 'link',
    title: 'Design file',
    href: 'https://design.example.com/file/123',
  },
];

function OnARecord() {
  const [items, setItems] = useState(START);
  return (
    <div style={{ maxWidth: '26rem' }}>
      <DocsPanel
        items={items}
        onUnlink={(item) => setItems((list) => list.filter((i) => i.id !== item.id))}
        onAddLink={({ url, title }) =>
          setItems((list) => [
            ...list,
            { id: `l${list.length + 10}`, kind: 'link', title: title || url, href: url },
          ])
        }
      />
    </div>
  );
}

function InAList() {
  const [items, setItems] = useState<LinkedDocItem[] | null>(null);
  return (
    <div style={{ maxWidth: '26rem' }}>
      <DocsPanel
        frame="none"
        collapsible
        count={START.length}
        items={items}
        readOnly
        onOpenChange={(open) => {
          if (open && !items) setTimeout(() => setItems(START), 600);
        }}
      />
    </div>
  );
}

export default {
  id: 'docs-panel',
  title: 'DocsPanel',
  summary:
    'The record-page block for linked Rook Dossier docs and links, framed or behind a Docs toggle.',
  sections: {
    purpose: {
      summary:
        'Every Rook app shows the Dossier docs and links attached to a record (an issue, a ticket, a project, a status page) in the same block. DocsPanel is that block: LinkedDocsPanel in a card or divider frame, or behind a "Docs" toggle that loads nothing until opened. Needed by: Stride, Tickets, Dots (projects and clients), Pulse, Signal, Ranker, Base, GTM, Recruit.',
      useWhen: [
        'A record page or side panel lists the docs linked to that record.',
        'Each card in a long list may have docs, and loading them for every card would be wasteful (`collapsible`).',
      ],
      avoidWhen: [
        'You need the list without any frame inside your own layout: use LinkedDocsPanel directly.',
        'Attachments uploaded to the record: use FileUpload and the file list.',
      ],
    },
    designPrinciples: {
      items: [
        'One panel: the list, picker, Space filter and unlink are LinkedDocsPanel’s; DocsPanel only adds the frame and the lazy toggle.',
        'Data in, callbacks out: the Dossier API calls, session refresh and record URL stay in the app.',
        'Lazy by default in lists: a closed panel mounts nothing, so the app fetches on `onOpenChange(true)`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Takes every LinkedDocsPanel prop. The ref goes to the inner LinkedDocsPanel `<section>`; `className` and `style` go to the frame.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`messages` takes every LinkedDocsPanel message plus `toggle` and `toggleCount`.',
        'Migrating from the app copies (`components/rook-docs/DocsPanel.tsx` and `LinkedDocs.tsx` in each app, Dots `ClientDocsPanel`, Tickets `detail/integrations/DocsPanel`): keep the Dossier client in the app (a hook that loads links for `{ app, entityType, entityId }` and links, unlinks and creates docs with `refreshSession`). Pass its results as `items`, `onLinkDoc`, `onSearchDocs`, `onCreateDoc` (with `spaceId` and the record `url` and `title` captured in the callback), `onAddLink` and `onUnlink`; `readOnly` stays `readOnly`. The wrapping `<section className="rounded-xl border …">` becomes `frame="card"`, Tickets’ `border-b` wrapper becomes `frame="divider"`, and Signal’s Docs toggle becomes `collapsible` with the load in `onOpenChange`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'frame',
          values: ['card', 'divider', 'none'],
          description: 'Bordered surface, a rule below, or no frame.',
        },
        {
          prop: 'collapsible',
          values: ['false', 'true'],
          description: 'Hide the panel behind a "Docs" toggle and mount it only when open.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Frame', attribute: 'data-frame', description: 'The frame in use.' },
        {
          name: 'Open / closed',
          attribute: 'data-state',
          description: 'With `collapsible`; also `aria-expanded` on the toggle.',
        },
        {
          name: 'Loading, error, notice, read-only',
          description: 'LinkedDocsPanel’s states (`items={null}`, `error`, `notice`, `readOnly`).',
        },
      ],
    },
    interaction: {
      items: [
        'The toggle opens and closes the panel and calls `onOpenChange`; the panel mounts on open.',
        'Everything inside (open, unlink, link a doc, add a link, Space filter) is LinkedDocsPanel’s.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'On the toggle: open or close the panel.' },
        { keys: 'Tab', action: 'Move through the toggle and the panel’s controls.' },
      ],
    },
    accessibility: {
      role: 'disclosure button (collapsible) controlling a region',
      name: 'The toggle reads "Docs, 2 linked"; the panel is a region named by its heading.',
      items: [
        '`aria-expanded` and `aria-controls` connect the toggle and the panel.',
        'The panel is `aria-busy` while its items load.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; the panel wraps its actions at narrow widths.',
        'The toggle is 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Card frame: `--mn-color-bg-surface`, `--mn-color-border-subtle` and `--mn-radius-lg`.',
        'Forced colours draw the frame in CanvasText; reduced motion removes the chevron turn.',
      ],
    },
    examples: {
      items: [
        { title: 'On a record page', render: () => <OnARecord /> },
        { title: 'Collapsible in a list, loaded on open', render: () => <InAList /> },
      ],
    },
    doDont: {
      do: [
        'Load the items when the collapsible panel opens, not before.',
        'Give new docs the record’s Space so they appear in the right place in Dossier.',
      ],
      dont: [
        'Do not check Dossier permissions in the UI only; the API decides.',
        'Do not put two DocsPanels for the same record on one page.',
      ],
    },
    related: {
      items: [
        { id: 'linked-docs-panel', reason: 'The panel inside.' },
        { id: 'section-card', reason: 'Other record-page blocks.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['linked-docs-panel'] },
    testing: {
      unit: [
        'Card frame around a named region with the items.',
        'Callbacks and messages reach the panel; divider frame.',
        'Collapsible: closed panel is not mounted; keyboard toggles; aria-expanded and aria-controls; count on the toggle.',
        'Controlled open; translation.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
