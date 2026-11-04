import { useRef, useState } from 'react';
import { LinkedDocsPanel, type LinkedDocItem } from '@rookcom/monarch-react/shell';
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
    kind: 'doc',
    docId: 'd2',
    title: 'Launch brief',
    href: '#doc-launch-brief',
    meta: 'Updated yesterday',
    status: 'Draft',
    space: { id: 's-mkt', name: 'Marketing' },
  },
  {
    id: 'l3',
    kind: 'link',
    title: 'Design file',
    href: 'https://design.example.com/file/123',
  },
];

const DOCS = [
  { value: 'd1', label: 'Release runbook', description: 'Engineering' },
  { value: 'd3', label: 'Incident review template', description: 'Engineering' },
  { value: 'd4', label: 'Pricing notes', description: 'Sales' },
];

function Basic() {
  const [items, setItems] = useState(START);
  const counter = useRef(10);
  const nextId = () => `l${(counter.current += 1)}`;
  return (
    <div style={{ width: '100%', maxWidth: '26rem' }}>
      <LinkedDocsPanel
        items={items}
        docOptions={DOCS}
        onLinkDoc={(docId) => {
          const doc = DOCS.find((d) => d.value === docId);
          if (doc)
            setItems((l) => [
              ...l,
              { id: nextId(), kind: 'doc', docId, title: doc.label, href: `#doc-${docId}` },
            ]);
        }}
        onCreateDoc={() => {}}
        onAddLink={({ url, title }) =>
          setItems((l) => [...l, { id: nextId(), kind: 'link', title, href: url }])
        }
        onUnlink={(it) => setItems((l) => l.filter((x) => x.id !== it.id))}
      />
    </div>
  );
}

function States() {
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-6)', width: '100%', maxWidth: '26rem' }}>
      <LinkedDocsPanel items={[]} onAddLink={() => {}} messages={{ title: 'Docs (empty)' }} />
      <LinkedDocsPanel items={null} messages={{ title: 'Docs (loading)' }} />
      <LinkedDocsPanel
        items={START.slice(0, 1)}
        readOnly
        messages={{ title: 'Docs (read-only)' }}
      />
    </div>
  );
}

export default {
  id: 'linked-docs-panel',
  title: 'LinkedDocsPanel',
  summary:
    'Docs and links attached to a record: list, open, unlink, link a doc, add a link, filter by Space.',
  sections: {
    purpose: {
      summary:
        'Records in Rook apps (tickets, tasks, campaigns, audits) link to Rook Dossier docs and to outside pages. LinkedDocsPanel is that panel on Monarch tokens, with the data and the linking left to the product.',
      useWhen: [
        'On a record’s detail page, in a side column or a "Docs" tab.',
        'Read-only (`readOnly`) for people who may see but not change links.',
      ],
      avoidWhen: [
        'Attachments uploaded to the record itself: use FileUpload and UploadQueue.',
        'A general list of documents: use a table or list page.',
      ],
    },
    designPrinciples: {
      items: [
        'Data in, intent out: `items` comes from the product; link, create, add and unlink are callbacks. The panel never fetches.',
        'Both kinds in one list: Dossier docs (status, Space, last update) and external links (the host name shows by default).',
        'Filter, don’t hide: Space chips appear only when items span several Spaces, and a filtered list says how many more exist.',
        'Promise-aware: a returned promise shows progress on the row or button; a rejection becomes an alert.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the `section`. "Link doc" uses EntityPicker from `@rookcom/monarch-react`.',
        'Also exported: `parseLinkUrl` (the add-link validator: full http(s) URLs only).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`items={null}` is loading. Each item has a row id (`id`); doc rows can carry `docId` so linked docs are left out of the picker.',
        '"Link doc" appears with `onLinkDoc` and either `docOptions` or `onSearchDocs`; "New doc" with `onCreateDoc`; "Add link" with `onAddLink`.',
        '`filter`: a Space id, `""` for all, `null` for items without a Space.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'readOnly',
          values: ['false', 'true'],
          description: 'No link, add or unlink controls.',
        },
        {
          prop: 'spaceFilter',
          values: ['true', 'false'],
          description: 'Space chips when items span more than one Space.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'aria-busy="true"',
          description: 'Skeleton rows and a status.',
        },
        { name: 'Empty', description: 'A short hint (different when read-only).' },
        {
          name: 'Filtered',
          attribute: 'aria-pressed="true"',
          description: 'On the active chip; "N more in other Spaces. Show all".',
        },
        {
          name: 'Unlinking',
          attribute: 'data-busy',
          description: 'The row dims and its button is busy.',
        },
        {
          name: 'Notice',
          description: 'A product message replaces the list, e.g. no access to Dossier.',
        },
        { name: 'Error', description: 'An alert, with Try again when `onRetry` is given.' },
      ],
    },
    interaction: {
      items: [
        'Rows open in a new tab (unless `external: false`); `onOpen` runs first and may prevent it.',
        'Unlink removes the row once the product updates `items`; focus moves to the next row, the previous one, or the header.',
        'Add link opens a small form with the URL focused; a URL that is not full http(s) is refused with a message. The title defaults to the host name.',
        'Link doc opens a search list without the docs already linked; "New doc" sits under it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Through chips, rows, unlink buttons and actions.' },
        {
          keys: 'Enter / Space',
          action: 'Press a chip, open a row, unlink, open the picker or form.',
        },
        { keys: '↓ / ↑, Enter', action: 'Choose a doc in the picker (EntityPicker).' },
        { keys: 'Enter', action: 'Add the link from the form.' },
        {
          keys: 'Escape',
          action: 'Cancel the form or close the picker; focus returns to its button.',
        },
      ],
    },
    accessibility: {
      role: 'region (section labelled by its heading) with a list',
      name: 'The heading (`messages.title`, default "Docs") plus the count.',
      items: [
        'Chips are toggle buttons (`aria-pressed`) in a labelled group.',
        'Unlink buttons are named "Unlink <title>"; new-tab rows say so.',
        'The URL field reports errors through FieldError (`aria-invalid`, `aria-describedby`).',
        'Errors are alerts; loading is a status inside an `aria-busy` region.',
        'Security: only http(s) URLs are accepted from the add-link form (no `javascript:` or `data:`); items are rendered as text, and new-tab links get `rel="noopener noreferrer"`. The product must still validate URLs and permissions on the server.',
      ],
    },
    responsive: {
      items: [
        'Fills its column; long titles and meta truncate with an ellipsis.',
        'Chips wrap; chips and rows are 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Surface, border and muted text tokens; the active chip uses the accent subtle colours.',
        'Transitions are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Docs and links on a record',
          stack: true,
          render: () => <Basic />,
          code: `<LinkedDocsPanel
  items={links}            // null while loading
  onSearchDocs={(q, { signal }) => dossier.search(q, { signal })}
  onLinkDoc={(docId) => link(docId)}
  onCreateDoc={() => createDocInSpace(record.spaceId)}
  onAddLink={({ url, title }) => addExternalLink(url, title)}
  onUnlink={(item) => unlink(item.id)}
/>`,
        },
        {
          title: 'Empty, loading and read-only',
          stack: true,
          render: () => <States />,
          code: `<LinkedDocsPanel items={[]} onAddLink={addLink} />
<LinkedDocsPanel items={null} />
<LinkedDocsPanel items={links} readOnly />`,
        },
      ],
    },
    doDont: {
      do: [
        'Return the promise from your callbacks so the panel can show progress and errors.',
        'Pass `docId` on doc rows so the picker leaves linked docs out.',
        'Pass a `notice` when the person has no access to Dossier in this workspace.',
      ],
      dont: [
        'Do not fetch inside the callbacks without handling errors: reject, and the panel shows a message.',
        'Do not put file uploads here.',
      ],
    },
    related: {
      items: [
        { id: 'entity-picker', reason: 'The doc picker.' },
        { id: 'file-upload', reason: 'Attachments stored on the record.' },
        { id: 'space-switcher', reason: 'The Space the person works in.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'badge',
        'button',
        'combobox',
        'entity-picker',
        'form-field',
        'skeleton',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Labelled region with a list; new-tab links named with meta; host name for links; status chip.',
        'Space chips (pressed), filtered count and Show all; a stale filter shows all; no chips for one Space or when off.',
        'Unlink with a labelled button, focus to the next row; failed unlink alert.',
        'Link doc from the picker without linked docs; New doc; server search drops linked docs.',
        'Add link: validation, title from the host, focus back; Escape cancels.',
        'Loading, empty, read-only, notice and error with Try again; onOpen; URL parser; SSR.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/linked-docs-panel',
        'Cross-browser axe: /components/linked-docs-panel',
      ],
    },
  },
} satisfies ComponentDocs;
