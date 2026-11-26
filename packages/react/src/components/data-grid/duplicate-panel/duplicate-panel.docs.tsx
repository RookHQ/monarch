import { useState } from 'react';
import {
  DuplicatePanel,
  MergeDialog,
  type DuplicateMatch,
  type MergeRecord,
} from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const MATCHES: DuplicateMatch[] = [
  {
    id: 'c1',
    title: 'Example Ltd',
    kind: 'Client',
    reasons: ['Same email'],
    href: '#client-c1',
  },
  {
    id: 'l7',
    title: 'Example Group',
    kind: 'Open lead',
    reasons: ['Same company domain'],
    meta: 'Created last week',
    href: '#lead-l7',
  },
];

const CURRENT: MergeRecord = {
  id: 'c9',
  title: 'Example Limited',
  values: { name: 'Example Limited', email: 'hello@example.com', city: 'York' },
};

function OnRecordPage() {
  const [matches, setMatches] = useState(MATCHES);
  const [merging, setMerging] = useState<DuplicateMatch | null>(null);
  return (
    <>
      <DuplicatePanel
        variant="panel"
        matches={matches}
        openInNewTab={false}
        onMerge={setMerging}
        onDismiss={(m) => setMatches((all) => all.filter((x) => x.id !== m.id))}
      />
      <MergeDialog
        open={merging !== null}
        onOpenChange={(open) => !open && setMerging(null)}
        records={[
          CURRENT,
          {
            id: merging?.id ?? 'other',
            title: merging?.title ?? '',
            values: { name: merging?.title ?? '', email: 'hello@example.com', city: 'Leeds' },
          },
        ]}
        fields={[
          { id: 'name', label: 'Name' },
          { id: 'email', label: 'Email' },
          { id: 'city', label: 'City' },
        ]}
        onMerge={() => {
          setMatches((all) => all.filter((x) => x.id !== merging?.id));
          return new Promise((resolve) => setTimeout(resolve, 600));
        }}
      />
    </>
  );
}

export default {
  id: 'duplicate-panel',
  title: 'DuplicatePanel',
  summary: 'Likely duplicates of a record, why they matched, and open / merge / dismiss actions.',
  sections: {
    purpose: {
      summary:
        'Warns that a record being created or viewed may already exist, says why each candidate matched, and offers the next step: open it, merge, or mark it as not a duplicate. Needed by: Dots (client and lead create forms, account pages).',
      useWhen: [
        'Under a create form, as the person types (`hint`): matching runs in the product and passes `matches` in.',
        'On a record page, listing candidates to merge (`panel`).',
      ],
      avoidWhen: [
        'An exact uniqueness rule that must block saving: use a field error.',
        'Duplicate rows in a file being imported: PreviewRows marks them.',
      ],
    },
    designPrinciples: {
      items: [
        'Advice, not a gate: it never blocks saving and stays silent when there is nothing to say.',
        'Say why: every match names the reason in words (“Same email”).',
        'Open links default to a new tab so a half-filled form is not lost.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<section>` (ref), hidden while `matches` is empty. In `hint` it is a polite `role="status"` region, so new matches are announced.',
        'Matching, debouncing and fetching stay in the product.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Dots `DuplicatePanel`): the component fetched `/clients/duplicates` itself; Dots now runs that query (with its debounce) and passes `matches`: clients → `{ id, title: name, kind: "Client", reasons: matchedOn.map(label), meta: archived ? "Archived" : undefined, href }`, leads → `kind: "Open lead"`. The closing sentence is `footer`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['hint', 'panel'],
          description:
            '`hint` (default) is a warning-toned box announced politely; `panel` is a plain section with a heading.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'hidden',
          description:
            'Nothing visible; the live region stays mounted to announce the first matches.',
        },
        { name: 'One match', description: 'Singular heading (“Possible duplicate”).' },
        { name: 'Several matches', description: 'Plural heading and one row per match.' },
      ],
    },
    interaction: {
      items: [
        'Open follows the match’s `href` (a new tab by default).',
        'Merge calls `onMerge(match)`; open a MergeDialog from it.',
        'Not a duplicate calls `onDismiss(match)`; remove the match from `matches` and remember the choice.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through Open, Merge and Not a duplicate for each match.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
    },
    accessibility: {
      role: 'status (hint) or region with a heading (panel)',
      name: 'The heading “Possible duplicates”.',
      items: [
        'Every action names its record (“Merge with Example Ltd”, “Open Example Ltd (opens in a new tab)”).',
        'The warning icon is decorative; the heading says what it is.',
        'Unsafe `href` values are ignored.',
      ],
    },
    responsive: {
      items: [
        'Actions wrap under the match text on narrow screens.',
        'On touch screens actions are at least 44 px tall.',
      ],
    },
    theme: {
      items: [
        '`hint` uses `--mn-color-warning-subtle`, `--mn-color-warning-border` and `--mn-color-warning-strong`.',
        'Forced colours: CanvasText border and text, LinkText links.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Hint under a create form',
          stack: true,
          render: () => (
            <DuplicatePanel
              matches={MATCHES}
              footer="You can still create it. Merge duplicates later from the record page."
            />
          ),
        },
        { title: 'On a record page, with merge', stack: true, render: () => <OnRecordPage /> },
      ],
    },
    doDont: {
      do: [
        'Debounce the lookup and only show matches you are confident about.',
        'Remember “Not a duplicate” so the same pair is not suggested again.',
      ],
      dont: [
        'Do not block saving because of a possible duplicate.',
        'Do not show raw match codes; write the reason.',
      ],
    },
    related: {
      items: [
        { id: 'merge-dialog', reason: 'Merging the records once confirmed.' },
        { id: 'preview-rows', reason: 'Duplicates inside an import file.' },
        { id: 'inline-notice', reason: 'A single advisory line without actions.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Hidden while empty; a status region that shows matches, reasons, kind, score, meta and footer.',
        'Open links are named, open in a new tab by default and drop unsafe URLs.',
        'Merge and Not a duplicate work by keyboard; panel variant heading.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
