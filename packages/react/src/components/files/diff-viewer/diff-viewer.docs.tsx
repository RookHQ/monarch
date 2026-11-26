import { DiffViewer } from '@rookcom/monarch-react/editor';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const BEFORE = `# Refund policy
Refunds are available within 14 days.
Contact support to start a refund.
Refunds go back to the original payment method.
Processing takes up to 10 days.`;

const AFTER = `# Refund policy
Refunds are available within 30 days.
Contact support or use the billing page to start a refund.
Refunds go back to the original payment method.
Processing takes up to 5 business days.
Partial refunds are possible for annual plans.`;

export default {
  id: 'diff-viewer',
  title: 'DiffViewer',
  summary:
    'Shows what changed between two texts or document versions, unified or side by side, with word highlights and change navigation.',
  sections: {
    purpose: {
      summary:
        'Compares two versions of a text or document so people can review edits before restoring, approving or publishing. Needed by: Rook Dossier (src/lib/tiptap/diff.ts and src/components/document/VersionHistoryPanel.tsx); shares the LCS core in `src/internal/diff.ts` with VersionHistoryPanel, AiArtifactVersions and AiEditProposal.',
      useWhen: [
        'Reviewing two versions of a document, template, policy or configuration text.',
        'A side-by-side view helps on wide screens; a unified view on narrow ones.',
      ],
      avoidWhen: [
        'Accepting or rejecting a single AI suggestion inline: use AiEditProposal.',
        'Browsing a document’s saved versions: use VersionHistoryPanel (which shows a compact diff itself).',
      ],
    },
    designPrinciples: {
      items: [
        'Never colour alone: changes are `<ins>` and `<del>` elements with a spoken "Added:" or "Removed:" prefix and a +/− marker.',
        'Lines first, words second: lines say what changed, word highlights inside paired lines say how.',
        'Quiet context: long unchanged runs fold to a button, keeping a few lines around each change.',
        'Documents are lists of blocks: pass arrays (one entry per paragraph) to compare documents rather than raw markup.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-diff-viewer[data-mode]` > `__header` (`__stats`, `__controls`) and `__scroll` > `table.__table` with `tr.__row[data-kind]`, `td.__number`, `td.__marker`, `td.__text`, and `tr.__gap` with `__expand`.',
        'Helpers: `diffTextLines(before, after)` (numbered lines), `diffTextWords(a, b)` (segments), `diffStats(lines)`.',
        'Import from `@rookcom/monarch-react/editor`. It does not need Tiptap.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`before` and `after` are strings (split on newlines) or arrays of lines or blocks.',
        'Over a million line pairs the line diff falls back to remove-all/add-all; word diffs over 250,000 token pairs do the same for that line.',
        '`context={Infinity}` shows every unchanged line.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'mode',
          values: ['unified', 'split'],
          description: 'One column with both sides interleaved, or before and after side by side.',
        },
        {
          prop: 'granularity',
          values: ['word', 'line'],
          description: 'Highlight changed words inside changed lines, or whole lines only.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Mode', attribute: 'data-mode', description: '`unified` or `split`.' },
        {
          name: 'Line kind',
          attribute: 'data-kind on rows and cells',
          description: '`same`, `added`, `removed`; `change` and `empty` in split mode.',
        },
        {
          name: 'Current change',
          attribute: 'data-current',
          description: 'Accent bar on the change reached with Previous/Next.',
        },
        { name: 'No changes', description: '`messages.noChanges` instead of the table.' },
      ],
    },
    interaction: {
      items: [
        'Previous and Next move focus to the first line of each change (wrapping) and announce "Change 2 of 5".',
        'The fold button shows the hidden unchanged lines.',
        'The layout switch changes between unified and side by side.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the buttons, the layout switch and the scrollable diff.',
        },
        { keys: 'Arrow keys (layout switch)', action: 'Choose unified or side by side.' },
        { keys: 'Arrow keys (diff region)', action: 'Scroll a wide diff.' },
      ],
    },
    accessibility: {
      role: 'table inside a named region; radiogroup for the layout',
      name: 'Table caption "Changes from <before> to <after>" (visually hidden) names the table and its scroll region.',
      items: [
        'Stats are read as "3 added, 1 removed"; the coloured +3 −1 is hidden from screen readers.',
        'Column headers are visible in split mode and visually hidden in unified mode.',
        'Position changes are announced in a polite status region.',
      ],
    },
    responsive: {
      items: [
        'Unified mode wraps long lines and fits 390px.',
        'Side-by-side keeps a 40rem minimum and scrolls sideways inside its region; prefer unified on phones.',
        'Buttons grow to 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        '`--mn-color-success-subtle` / `-border` for additions, `--mn-color-danger-subtle` / `-border` for removals, `--mn-font-mono` for text.',
        'Forced colours drop the tints and keep underline and strike-through.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Unified',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 720 }}>
              <DiffViewer
                before={BEFORE}
                after={AFTER}
                beforeLabel="Version 3"
                afterLabel="Version 4"
              />
            </div>
          ),
        },
        {
          title: 'Side by side, document blocks',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 720 }}>
              <DiffViewer
                defaultMode="split"
                hideLineNumbers
                before={['Kick-off on Monday.', 'Agenda: goals and owners.']}
                after={['Kick-off on Tuesday.', 'Agenda: goals, owners and risks.']}
              />
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: ['Name both sides (`beforeLabel`, `afterLabel`).', 'Compare document blocks as arrays.'],
      dont: [
        'Do not diff raw HTML; compare text or blocks.',
        'Do not rely on colour to explain the diff in surrounding copy.',
      ],
    },
    related: {
      items: [
        { id: 'version-history-panel', reason: 'Browse and restore versions.' },
        { id: 'ai-edit-proposal', reason: 'Accept or reject one suggested edit.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'segmented-control'],
      notes: ['Uses the shared LCS in `src/internal/diff.ts`.'],
    },
    testing: {
      unit: [
        '`diffTextLines` numbering, `diffStats`, `diffTextWords`.',
        'Unified rows with ins/del, spoken labels, word highlights, stats.',
        'Folding and expanding unchanged runs.',
        'Layout radio group, side-by-side pairing, column headers.',
        'Previous/next focus and announcement; no-changes message.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
