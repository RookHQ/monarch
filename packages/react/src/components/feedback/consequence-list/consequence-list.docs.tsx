import { useEffect, useState } from 'react';
import {
  Button,
  ConfirmDialog,
  ConsequenceList,
  InlineErrorRetry,
  type ConsequenceItem,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

const projectImpact: ConsequenceItem[] = [
  { id: 'tasks', kind: 'removed', count: 48, label: 'tasks and their comments will be deleted' },
  { id: 'files', kind: 'removed', count: 1312, label: 'files will be deleted' },
  { id: 'access', kind: 'changed', count: 6, label: 'people lose access immediately' },
  {
    id: 'invoices',
    kind: 'kept',
    label: 'Invoices are kept for 7 years',
    detail: 'They stay available to workspace owners under Billing.',
  },
  { id: 'links', kind: 'warning', label: 'Shared links stop working' },
];

function DeleteProject() {
  return (
    <ConfirmDialog
      trigger={<Button variant="danger">Delete project</Button>}
      tone="danger"
      title="Delete this project?"
      description="This cannot be undone."
      confirmLabel="Delete project"
      onConfirm={() => wait(1000)}
    >
      <ConsequenceList items={projectImpact} />
    </ConfirmDialog>
  );
}

function LoadedImpact() {
  const [state, setState] = useState<'loading' | 'error' | 'ready'>('loading');
  const [retrying, setRetrying] = useState(false);
  useEffect(() => {
    const timer = window.setTimeout(() => setState('error'), 1500);
    return () => window.clearTimeout(timer);
  }, []);
  return (
    <ConsequenceList
      heading="What happens"
      items={projectImpact}
      loading={state === 'loading'}
      error={
        state === 'error' ? (
          <InlineErrorRetry
            retrying={retrying}
            onRetry={async () => {
              setRetrying(true);
              await wait(800);
              setRetrying(false);
              setState('ready');
            }}
          >
            Could not work out what will change.
          </InlineErrorRetry>
        ) : undefined
      }
    />
  );
}

export default {
  id: 'consequence-list',
  title: 'ConsequenceList',
  summary:
    'States plainly what an action will do: what is deleted, kept, changed, and what to know first, with counts.',
  sections: {
    purpose: {
      summary:
        'Says concretely what an action will do before the person commits to it: "48 tasks will be deleted", "Invoices are kept for 7 years", "Members lose access immediately". Each line has a kind (removed, kept, changed, warning) shown by an icon, a tone and, for screen readers, a spoken word, so the meaning never rests on colour.',
      useWhen: [
        'Inside a ConfirmDialog or a destructive flow, to list the effects of deleting, removing or transferring something.',
        'When some things are lost and others are kept, and the person needs to see both.',
        'When impact counts come from an API: show `loading` while they arrive and `error` if they fail.',
      ],
      avoidWhen: [
        'A single sentence says it all: use the ConfirmDialog `description`.',
        'General status or validation messages: use Alert or InlineNotice.',
      ],
    },
    designPrinciples: {
      items: [
        'Concrete over vague: counts and named things, not "some data may be lost".',
        'Never colour alone: every item has an icon and text, and ungrouped items start with a spoken kind ("Removed: …").',
        'Calm: the danger tone is on the icon and the count only, never a red block of text.',
        'What is kept matters as much as what is lost; say both.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref forwarded) holding an optional heading and one `<ul>`, or one `<ul>` per kind when `groupBy="kind"`. Native div attributes pass through.',
        'Each list is named by its heading (`aria-labelledby`) when there is one.',
        '`CONSEQUENCE_LIST_MESSAGES` exports the English defaults for translation tables.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'An item is `{ id?, kind, label, detail?, count? }`. `count` is written before `label`, so phrase the label to follow a number ("tasks will be deleted").',
        'Every built-in string (group headings, spoken kinds, loading announcement) is in `messages`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'groupBy',
          values: ['undefined', 'kind'],
          description:
            'Without it, items keep the given order. With `kind`, they are grouped under "Will be deleted", "Will change", "Before you continue" and "Will be kept", in that order; empty groups are left out. Exposed as `data-grouped`.',
        },
        {
          prop: 'items[].kind',
          values: ['removed', 'kept', 'changed', 'warning'],
          description:
            'Icon and tone of the line: danger minus-circle, success check-circle, neutral arrows, warning triangle. Exposed as `data-kind` on the `<li>`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Ready',
          description: 'The items, grouped or not.',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading", aria-busy',
          description:
            'Skeleton rows (`loadingRows`, default 3) replace the items, and a visually hidden status announces `messages.loading`.',
        },
        {
          name: 'Error',
          attribute: 'data-state="error"',
          description:
            'The `error` node replaces the items, usually an InlineErrorRetry. Ignored while loading.',
        },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. Anything inside the `error` slot (a Retry button) keeps its own behaviour.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable. Tab reaches only controls passed in `error`, `label` or `detail`.'],
    },
    accessibility: {
      role: 'list (one per group)',
      name: '`heading` names the list; with `groupBy="kind"`, each group heading names its list.',
      items: [
        'Icons are decorative (`aria-hidden`); the kind is spoken as a visually hidden prefix ("Removed: 48 tasks…") or by the group heading.',
        'Headings use `headingLevel` (default h3); group headings are one level lower, or the same level when there is no `heading`.',
        'Loading sets `aria-busy` and announces a polite status; skeleton rows are hidden from assistive technology.',
        'Tone tokens meet text contrast in every theme; forced colours use system text colour.',
      ],
    },
    responsive: {
      items: [
        'Single column at every width; labels and details wrap, long words break, so nothing scrolls sideways at 390px.',
        'The icon stays aligned with the first line of a wrapped label.',
      ],
    },
    theme: {
      items: [
        'Icons use `--mn-color-danger-strong`, `--mn-color-success-strong`, `--mn-color-warning-strong` and `--mn-color-text-muted`; removed counts use the danger tone.',
        'Counts use tabular numerals so columns of figures line up.',
        'Light, dark and more-contrast themes apply automatically; skeleton shimmer stops under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Mixed consequences',
          stack: true,
          render: () => <ConsequenceList heading="What happens" items={projectImpact} />,
        },
        {
          title: 'Grouped by kind',
          stack: true,
          render: () => (
            <ConsequenceList heading="What happens" groupBy="kind" items={projectImpact} />
          ),
        },
        {
          title: 'Inside a ConfirmDialog',
          render: () => <DeleteProject />,
        },
        {
          title: 'Loading, then an error with retry',
          stack: true,
          render: () => <LoadedImpact />,
        },
        {
          title: 'Locale-aware counts',
          stack: true,
          render: () => (
            <ConsequenceList
              locale="de-DE"
              items={[{ kind: 'removed', count: 12500, label: 'Einträge werden gelöscht' }]}
              messages={{ kindRemoved: 'Entfernt' }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give real counts when you have them, and show `loading` while they arrive.',
        'List what is kept, so people are not afraid of losing more than they will.',
        'Phrase labels as outcomes ("lose access immediately"), in plain words.',
      ],
      dont: [
        'Do not use it for reversible actions; do them and offer Undo instead.',
        'Do not hide uncertainty: if counts failed to load, show the error, not zero.',
        'Do not mark everything `warning`; keep the kinds honest.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'Where it usually lives, as the dialog body.' },
        { id: 'danger-zone', reason: 'The settings section whose actions lead to it.' },
        { id: 'inline-error-retry', reason: 'The usual content of the `error` slot.' },
        { id: 'skeleton', reason: 'Used for the loading rows.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['skeleton'],
      notes: [
        'Glyphs for kept and warning come from the internal icon set; the others are drawn inline.',
      ],
    },
    testing: {
      unit: [
        'Renders a list named by its heading; forwards ref and props; axe passes.',
        'Every item has a decorative icon and a spoken kind, plus label, count and detail text.',
        'Counts are formatted with Intl.NumberFormat in the given locale.',
        'groupBy="kind" renders sub-headings in a fixed order, skips empty groups, names each list and takes translated labels; axe passes.',
        'Loading shows skeleton rows, aria-busy, data-state and a status; axe passes.',
        'Error replaces the items; a Retry button inside is reachable and works by keyboard; axe passes.',
        'The list itself is not focusable.',
        'Renders on the server (ready and loading).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
