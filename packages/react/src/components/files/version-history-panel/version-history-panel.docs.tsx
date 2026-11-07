import { useEffect, useState } from 'react';
import { VersionHistoryPanel, type VersionHistoryItem } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = Date.now();

const CONTENT: Record<string, string[]> = {
  v4: [
    '# Onboarding guide',
    'Welcome to the team.',
    'Your first week: meet your buddy, set up your laptop, read the handbook.',
    'Ask questions in the help channel.',
    'Book a check-in with your manager on Friday.',
  ],
  v3: [
    '# Onboarding guide',
    'Welcome to the team.',
    'Your first week: meet your buddy and set up your laptop.',
    'Ask questions in the help channel.',
  ],
  v2: ['# Onboarding guide', 'Welcome!', 'Your first week: set up your laptop.'],
  v1: ['# Onboarding guide', 'Draft.'],
};

const VERSIONS: VersionHistoryItem[] = [
  { id: 'v4', createdAt: NOW - 5 * 60_000, author: { name: 'Robin Park' } },
  { id: 'v3', createdAt: NOW - 3 * 3_600_000, author: { name: 'Alex Chen' }, note: 'Reviewed' },
  { id: 'v2', createdAt: NOW - 26 * 3_600_000, author: { name: 'Robin Park' }, automatic: true },
  {
    id: 'v1',
    createdAt: NOW - 6 * 86_400_000,
    author: { name: 'Jordan Diaz' },
    note: 'First draft',
  },
];

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function Demo() {
  const [versions, setVersions] = useState(VERSIONS);
  const [current, setCurrent] = useState(CONTENT.v4 ?? []);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [lines, setLines] = useState<string[] | undefined>(undefined);

  useEffect(() => {
    setLines(undefined);
    if (!selectedId) return;
    // Stands in for loading the version's content.
    const timer = window.setTimeout(() => setLines(CONTENT[selectedId] ?? []), 500);
    return () => window.clearTimeout(timer);
  }, [selectedId]);

  return (
    <div style={{ maxWidth: 380, height: 520 }}>
      <VersionHistoryPanel
        versions={versions}
        selectedId={selectedId}
        onSelectedIdChange={setSelectedId}
        currentLines={current}
        selectedLines={lines}
        onSaveVersion={async (note) => {
          await wait(600);
          const id = `v${versions.length + 1}`;
          CONTENT[id] = current;
          setVersions([
            { id, createdAt: Date.now(), author: { name: 'You' }, note: note || undefined },
            ...versions,
          ]);
        }}
        onRestore={async (id) => {
          await wait(800);
          setCurrent(CONTENT[id] ?? []);
        }}
        onClose={() => setSelectedId(null)}
      />
    </div>
  );
}

export default {
  id: 'version-history-panel',
  title: 'VersionHistoryPanel',
  summary:
    'Saved versions of a document, a diff of any version against now, full text and restore.',
  sections: {
    purpose: {
      summary:
        'Lets people see how a document or record changed, name a version to mark a moment, compare any version with what is there now and restore it. Data and callbacks in: the product loads versions and their content. Needed by: Dossier.',
      useWhen: [
        'A side panel next to a document, page or long-form record that keeps versions.',
        'Anywhere "what changed since…?" matters more than who typed each character.',
      ],
      avoidWhen: [
        'An audit log of field changes on a record: use ActivityTimeline.',
        'Reviewing a proposed edit before it is applied: use AiEditProposal.',
      ],
    },
    designPrinciples: {
      items: [
        '"What changed" always means against the content now, so there is one comparison to understand.',
        'Compare blocks, not characters: the product turns its document into lines (one per paragraph or block), which is what readers recognise.',
        'Long unchanged stretches fold into "12 unchanged lines" with context around each change.',
        'Restore is confirmed in a dialog, and says what happens in the product’s words.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`VersionHistoryPanel` renders an `<aside>` named by its heading and forwards its ref there.',
        '`diffVersionLines(before, after)`, `versionDiffStats(lines)` and `collapseVersionDiff(lines, context)` are the pure helpers it uses; they are exported for products that show diffs elsewhere.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`selectedLines` stays undefined while the selected version loads; the panel shows skeletons.',
        '`onSaveVersion` and `onRestore` may return promises: saving shows a busy button; restore keeps the ConfirmDialog pending and shows its error on rejection.',
        'Migrating from the app copy (Dossier `VersionHistoryPanel`): `documentId` + `useVersions` → `versions` (map `createdBy` to `author`); `useVersion(selectedId)` + `snapshotToLines` → `selectedId`, `onSelectedIdChange` and `selectedLines`; `currentSnapshot` → `currentLines` (keep `snapshotToLines` in the product); `useCreateVersion().mutate({ note })` → `onSaveVersion(note)`; `useRestoreVersion().mutate(id)` → `onRestore(id)`; `onClose` is unchanged; the "current state is saved first" sentence → `messages.restoreDescription`; toasts → the built-in announcements; `diffLines`/`diffStats`/`collapseUnchanged` → `diffVersionLines`/`versionDiffStats`/`collapseVersionDiff`.',
      ],
    },
    variants: {
      items: [],
      notes: [
        'No visual variants. Saving and restoring appear only when their callbacks are passed.',
      ],
    },
    states: {
      items: [
        {
          name: 'List / detail',
          attribute: 'data-view',
          description: 'The version list, or one version with Changes and Full text tabs.',
        },
        {
          name: 'Loading',
          attribute: 'role="status"',
          description: 'Skeletons while the list or the selected version loads.',
        },
        { name: 'Empty', description: 'No versions yet: a hint.' },
        { name: 'Identical', description: 'The selected version matches the content now.' },
        {
          name: 'Save error',
          attribute: 'role="alert"',
          description: 'The save failed; the name is kept for another try.',
        },
      ],
    },
    interaction: {
      items: [
        'Choose a version to open it; Back returns to the list with focus on that version.',
        'Changes shows added and removed lines with counts; Full text shows the version as it was.',
        'Restore opens a confirmation; on success the panel goes back to the list and announces it.',
        'The current version cannot be restored onto itself, so its Restore button is hidden.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Version name, Save, then each version.' },
        { keys: 'Enter (in the name field)', action: 'Saves the version.' },
        { keys: 'ArrowLeft / ArrowRight', action: 'Between the Changes and Full text tabs.' },
        { keys: 'Enter / Space', action: 'Opens a version, goes back, or restores.' },
      ],
    },
    accessibility: {
      role: 'complementary (aside)',
      name: 'The heading (`title`, default "History"; the version’s time in detail view)',
      items: [
        'Added and removed lines are `<ins>` and `<del>` with hidden "Added:" and "Removed:" prefixes, so the change is not colour only.',
        'Counts are read as "2 added, 1 removed, compared with now".',
        'Focus moves to Back when a version opens and returns to the row afterwards.',
        'Save and restore results are announced in a polite live region.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; long lines wrap anywhere instead of scrolling sideways.',
        'Rows are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Added lines use `--mn-color-success-subtle`, removed `--mn-color-danger-subtle`; counts use the strong tokens.',
        'Forced colours: a solid start border marks added lines and a dashed one removed lines.',
      ],
    },
    examples: {
      items: [{ title: 'Browse, compare, save and restore', render: () => <Demo />, stack: true }],
    },
    doDont: {
      do: [
        'Produce one line per block or paragraph, with headings prefixed (for example "# Title").',
        'Save the current state before restoring, and say so in `messages.restoreDescription`.',
      ],
      dont: [
        'Do not diff serialised JSON; structural noise reads as content changes.',
        'Do not restore without the confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'The restore confirmation.' },
        { id: 'activity-timeline', reason: 'Field-level change history.' },
        { id: 'ai-artifact-versions', reason: 'Versions of AI-generated artifacts.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'relative-time',
        'skeleton',
        'text-field',
        'avatar',
        'tabs',
        'confirm-dialog',
      ],
    },
    testing: {
      unit: [
        'diffVersionLines, versionDiffStats and collapseVersionDiff.',
        'Lists versions with author, note, auto and current markers; axe passes.',
        'Opening a version: loading state, focus on Back, diff with ins/del, Full text tab, focus back on the row; axe passes.',
        'Identical versions; saving with Enter, trimming the name, and save errors.',
        'Restore needs confirmation and is hidden for the current version.',
        'Loading, empty and translated strings; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
