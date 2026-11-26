import { useState } from 'react';
import {
  DocumentRelations,
  type DocumentRelation,
  type DocumentRelationTarget,
} from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const HIRING: DocumentRelationTarget = { id: 'd1', title: 'Hiring plan', description: 'People' };
const HANDBOOK: DocumentRelationTarget = {
  id: 'd2',
  title: 'Company handbook',
  description: 'Workspace root',
};
const TRAVEL: DocumentRelationTarget = {
  id: 'd3',
  title: 'Travel policy (2025)',
  description: 'Operations',
};
const DOCS: DocumentRelationTarget[] = [
  HIRING,
  HANDBOOK,
  TRAVEL,
  { id: 'd4', title: 'Budget 2027', description: 'Finance' },
  { id: 'd5', title: 'Brand guidelines', description: 'Marketing' },
  { id: 'd6', title: 'Security checklist', description: 'Engineering' },
];

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function Editable() {
  const [relations, setRelations] = useState<DocumentRelation[]>([
    { id: 'r1', type: 'parent-of', target: { ...HIRING, href: '#hiring-plan' } },
    {
      id: 'r2',
      type: 'parent-of',
      direction: 'incoming',
      target: { ...HANDBOOK, href: '#handbook' },
    },
    { id: 'r3', type: 'supersedes', target: { ...TRAVEL, href: '#travel-policy' } },
  ]);
  return (
    <DocumentRelations
      relations={relations}
      candidates={DOCS}
      onAdd={async (targetId, type) => {
        await wait(500);
        const target = DOCS.find((d) => d.id === targetId);
        if (target)
          setRelations((list) => [
            ...list,
            { id: `r${Date.now()}`, type, target: { ...target, href: `#${targetId}` } },
          ]);
      }}
      onRemove={async (id) => {
        await wait(300);
        setRelations((list) => list.filter((r) => r.id !== id));
      }}
    />
  );
}

export default {
  id: 'document-relations',
  title: 'DocumentRelations',
  summary: 'Typed links between documents (parent, supersedes, implements…) with add and remove.',
  sections: {
    purpose: {
      summary:
        'Shows how a document connects to others, read correctly from this side ("Child of Company handbook" when the handbook is the parent), and lets people add a relation by searching or remove one. Data and callbacks in; routing goes through `renderLink`. Needed by: Dossier.',
      useWhen: [
        'Under a document title or in its side panel, to link related, parent, replacing or implementing documents.',
        'Any record type with typed links (specs and tasks, policies and versions).',
      ],
      avoidWhen: [
        'Untyped "linked docs" lists: use LinkedDocsPanel.',
        'Tags or categories: use TagInput or Badge.',
      ],
    },
    designPrinciples: {
      items: [
        'One relation, two readings: store it once and let `direction` pick the label (Parent of ↔ Child of).',
        'Types are data: the defaults cover common document links, and products pass their own with inverses.',
        'Removing a link is light and reversible (add it again), so it has no confirmation; it is announced and focus stays in the list.',
        'Adding keeps the dialog open on failure with the reason, so nothing is lost.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`DocumentRelations` renders a labelled `role="group"` and forwards its ref there.',
        '`DEFAULT_DOCUMENT_RELATION_TYPES` lists the default types and inverses; spread it to add your own.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`candidates` are filtered by the search text and already-related documents are left out; with `onSearch` the product filters instead.',
        '`renderLink(target, children)` wraps the title, for example in the router’s Link; otherwise `target.href` makes an `<a>`.',
        'Migrating from the app copy (Dossier `DocumentRelations`): `documentId` + `useDocumentRelations` → `relations` (set `direction: "incoming"` when `sourceDocumentId` is not this document, and use the other side as `target`); `useDocuments(workspaceId)` → `candidates` (space name as `description`); `useCreateDocumentRelation().mutate({ targetDocumentId, relationType })` → `onAdd(targetId, type)`; `useDeleteDocumentRelation().mutate(id)` → `onRemove(id)`; `slug` + `next/link` → `renderLink` or `target.href`; the `PARENT_OF`-style enum → `types` with `value`s matching your API (or map to the kebab-case defaults).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showEmpty',
          values: ['false', 'true'],
          description: 'Say "No relations yet." instead of showing only the Add button.',
        },
      ],
      notes: ['Add and remove appear only when `onAdd` and `onRemove` are passed.'],
    },
    states: {
      items: [
        {
          name: 'Adding',
          attribute: 'aria-busy (result), busy (dialog)',
          description:
            'Results are disabled and the dialog cannot be dismissed while `onAdd` runs.',
        },
        {
          name: 'Removing',
          attribute: 'aria-busy, disabled',
          description: 'The chip’s remove button while `onRemove` runs.',
        },
        {
          name: 'No matches',
          attribute: 'role="status"',
          description: 'The search found nothing.',
        },
        { name: 'Error', attribute: 'role="alert"', description: 'Adding or removing failed.' },
      ],
    },
    interaction: {
      items: [
        'Add relation opens a dialog: pick the relation, search, choose a document.',
        'The dialog closes on success and focus returns to Add relation.',
        'Removing a relation moves focus to the next relation’s remove button, or to Add relation.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Each link and remove button, then Add relation.' },
        {
          keys: 'ArrowDown / ArrowUp (in the dialog)',
          action: 'From the search field into the results and back.',
        },
        { keys: 'Enter', action: 'Adds the focused result.' },
        { keys: 'Escape', action: 'Closes the dialog (not while adding).' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '`messages.label` ("Relations")',
      items: [
        'Each relation is a list item read as "Parent of Hiring plan"; the arrow icon is decorative.',
        'Remove buttons are named "Remove relation to Hiring plan".',
        'The dialog has a title and description; the search field controls the results list.',
        'Additions and removals are announced politely.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap; long titles truncate inside the chip.',
        'Remove buttons and chips are 44px on coarse pointers; the dialog is full width on phones.',
      ],
    },
    theme: {
      items: [
        'Chips use `--mn-color-bg-surface` and `--mn-color-border-default`; links `--mn-color-text-link`; remove hovers to the danger subtle colour.',
        'Forced colours: CanvasText chip borders, LinkText links and Highlight focus.',
      ],
    },
    examples: {
      items: [
        { title: 'Add, read both ways, remove', render: () => <Editable />, stack: true },
        {
          title: 'Read-only, custom types',
          render: () => (
            <DocumentRelations
              types={[
                { value: 'blocks', label: 'Blocks', inverse: 'blocked-by' },
                { value: 'blocked-by', label: 'Blocked by', inverse: 'blocks' },
              ]}
              relations={[
                { id: 'a', type: 'blocks', target: { id: 'x', title: 'Launch checklist' } },
                {
                  id: 'b',
                  type: 'blocks',
                  direction: 'incoming',
                  target: { id: 'y', title: 'Vendor contract' },
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Store each relation once and pass `direction` from the reader’s side.',
        'Give candidates a second line (space or folder) so similar titles can be told apart.',
      ],
      dont: [
        'Do not offer the document itself or ones already related as candidates (they are filtered out locally).',
        'Do not invent relation types per document; keep one list per product.',
      ],
    },
    related: {
      items: [
        { id: 'linked-docs-panel', reason: 'A simple list of linked documents without types.' },
        { id: 'dialog', reason: 'The add dialog.' },
        { id: 'page-tree', reason: 'Pages inside one document.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'text-field', 'dialog'],
    },
    testing: {
      unit: [
        'Reads relations from this side (inverse labels), links and remove buttons; axe passes.',
        'renderLink routing.',
        'Adds by type and search with keyboard into the results; dialog passes axe; announces and returns focus.',
        'Failed add keeps the dialog open with an alert; onSearch and the no-match status.',
        'Remove moves focus to the next relation and announces it.',
        'Empty text, custom types and translations; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
