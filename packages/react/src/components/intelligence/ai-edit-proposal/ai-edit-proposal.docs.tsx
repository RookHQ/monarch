import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiEditProposal,
  diffWords,
  type AiEditHunk,
  type AiEditProposalPart,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const HUNKS: AiEditHunk[] = [
  { id: 'title', label: 'Title', before: 'Quarterly plan', after: 'Quarterly roadmap' },
  {
    id: 'summary',
    label: 'Summary',
    before: 'The team will ship the new onboarding flow in May.',
    after: 'The team will ship the redesigned onboarding flow in early June.',
  },
  {
    id: 'owner',
    label: 'Owner',
    before: 'Sam',
    after: 'Sam Lee',
  },
];

function proposal(state: AiEditProposalPart['state']): AiEditProposalPart {
  return { type: 'edit-proposal', id: 'edit-1', target: 'doc-1', diff: null, state };
}

function ReviewDemo() {
  const [state, setState] = useState<AiEditProposalPart['state']>('proposed');
  const [decisions, setDecisions] = useState<Record<string, 'accepted' | 'rejected'>>({});
  const decide = (id: string, status: 'accepted' | 'rejected') =>
    setDecisions((d) => ({ ...d, [id]: status }));
  const hunks = HUNKS.map((h) => ({ ...h, status: decisions[h.id] }));
  const allDecided = HUNKS.every((h) => decisions[h.id]);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <AiEditProposal
        key={state}
        part={proposal(state)}
        hunks={hunks}
        onAcceptHunk={(id) => decide(id, 'accepted')}
        onRejectHunk={(id) => decide(id, 'rejected')}
        onAcceptAll={() => setState('applied')}
        onRejectAll={() => setState('rejected')}
        onRevert={() => setState('reverted')}
      />
      <div style={{ display: 'flex', gap: 'var(--mn-space-2)', flexWrap: 'wrap' }}>
        {state === 'proposed' && allDecided ? (
          <Button size="sm" variant="primary" onClick={() => setState('applied')}>
            Apply decisions
          </Button>
        ) : null}
        <Button
          size="sm"
          onClick={() => {
            setDecisions({});
            setState('proposed');
          }}
        >
          Reset demo
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'ai-edit-proposal',
  title: 'AiEditProposal',
  summary: 'A reviewable diff of AI edits, accepted or rejected per change or all at once.',
  sections: {
    purpose: {
      summary:
        'Shows the edits an AI proposes to a document or record as a word-level diff, and lets a person accept or reject each change, or all of them, before anything is written. Applied edits can be reverted. This replaces silently applied AI edits.',
      useWhen: [
        'An assistant proposes changes to existing content (a document, a record’s fields, a description) through an `edit-proposal` part.',
        'People need to see exactly which words change before they accept.',
        'Some changes may be wanted and others not: decide per hunk.',
      ],
      avoidWhen: [
        'The suggestions are inline in an editor the person is typing in: use AiInlineDiff.',
        'The change is code: use AiCodeBlock with `diff`.',
        'The AI already made the change and you only need to offer undo: use AiUndoBanner.',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is applied without a decision: the component only reports decisions; the app writes.',
        'Show the words that change, not two whole versions: removed text is struck through, added text underlined, and each is announced as "Deleted" or "Inserted".',
        'Decisions are reversible until applied, and applied edits can be reverted.',
        'Every hunk names what it changes (a field or section), so people know where the edit lands.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`diffWords(before, after)` returns `AiDiffSegment[]` (`equal`, `delete`, `insert`): a longest-common-subsequence diff over words, whitespace and punctuation, with adjacent changes grouped as one removal then one insertion. Very large inputs fall back to one removal and one insertion between the common start and end.',
        'Hunk decisions are kept inside the component unless a hunk has `status`, which always wins, so the app can own them.',
        'Accept all and Reject all decide only the hunks that are still undecided.',
        '`part.diff` is not read: pass the hunks you derived from it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. Which actions appear depends on the handlers passed and on `part.state`.',
      ],
    },
    states: {
      items: [
        {
          name: 'Proposed',
          attribute: 'data-state="proposed"',
          description:
            'Hunks show Accept and Reject; Accept all and Reject all decide the rest. The count reads "2 of 3 changes left".',
        },
        {
          name: 'Hunk accepted / rejected',
          attribute: 'data-status on the hunk',
          description:
            'The hunk shows "Accepted" or "Rejected" with an icon, its buttons go away, and the losing side of the diff is muted.',
        },
        {
          name: 'Applied',
          attribute: 'data-state="applied"',
          description:
            'Read-only. Hunks not rejected show as accepted. Revert appears when `onRevert` is passed.',
        },
        {
          name: 'Rejected',
          attribute: 'data-state="rejected"',
          description: 'Read-only; every hunk shows as rejected.',
        },
        {
          name: 'Reverted',
          attribute: 'data-state="reverted"',
          description: 'Read-only, after the app undid applied edits.',
        },
      ],
    },
    interaction: {
      items: [
        'Accept or Reject on a hunk records the decision, calls `onAcceptHunk(id)` or `onRejectHunk(id)` and announces it.',
        'Accept all and Reject all decide every undecided hunk and call `onAcceptAll` or `onRejectAll`; the app then applies the result and moves `part.state` on.',
        'Revert calls `onRevert`; the app undoes the edits and sets `part.state` to `reverted`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through each hunk’s Accept and Reject, then the bulk actions.',
        },
        { keys: 'Enter / Space', action: 'Runs the focused action.' },
      ],
      notes: [
        'After a hunk is decided, focus moves to the next undecided hunk’s Accept, or to the title when none are left, so it is never lost.',
      ],
    },
    accessibility: {
      role: 'group (the card), list of hunks, status',
      name: 'The title, "Suggested edits" by default; each hunk is named by its label or "Change 2 of 3"',
      items: [
        'Removed text is in `<del>` and added text in `<ins>`, each with a visually hidden "Deleted:" or "Inserted:" prefix, and they differ by strikethrough and underline as well as colour.',
        'The AI provenance badge says the edits are AI generated.',
        'Accept and Reject are described by the hunk label, so "Accept, Title" is unambiguous.',
        'Decisions are announced politely ("Change accepted. 2 left.").',
      ],
    },
    responsive: {
      items: [
        'Hunks stack and long words wrap, so there is no horizontal scroll at 390px.',
        'Actions wrap onto more rows as needed; on coarse pointers they are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Removed text: `--mn-color-ai-diff-remove` background and `--mn-color-ai-diff-remove-strong` text. Added text: `--mn-color-ai-diff-add` and `--mn-color-ai-diff-add-strong`.',
        'The card has an AI-coloured leading border (`--mn-color-ai-border`) on `--mn-color-bg-surface`; hunks sit on `--mn-color-bg-subtle`.',
        'In forced colours removed and added text use CanvasText, with strikethrough and a border on insertions.',
      ],
    },
    examples: {
      items: [
        { title: 'Review and apply', stack: true, render: () => <ReviewDemo /> },
        {
          title: 'Applied, with Revert',
          stack: true,
          render: () => (
            <AiEditProposal
              part={proposal('applied')}
              hunks={HUNKS.slice(0, 2)}
              onRevert={() => {}}
            />
          ),
        },
        {
          title: 'Word diff on its own',
          stack: true,
          render: () => (
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
              {JSON.stringify(diffWords('Ship in May.', 'Ship early in June.'), null, 2)}
            </pre>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Label each hunk with the field or section it changes.',
        'Apply only the accepted hunks, then set `part.state` to `applied`.',
        'Offer Revert after applying, and record the change in the audit log.',
      ],
      dont: [
        'Do not apply AI edits before the person decides.',
        'Do not split one sentence into many tiny hunks; let the word diff show the detail.',
        'Do not hide rejected hunks: the record of what was declined matters.',
      ],
    },
    related: {
      items: [
        { id: 'ai-inline-diff', reason: 'Suggestions inline in editable text.' },
        { id: 'ai-undo-banner', reason: 'Undo for changes the AI already made.' },
        { id: 'ai-code-block', reason: 'Line diffs for code.' },
        { id: 'ai-generated-badge', reason: 'The provenance badge in the header.' },
        { id: 'ai-message', reason: 'Renders edit-proposal parts through `renderPart`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'badge', 'ai-generated-badge'],
    },
    testing: {
      unit: [
        '`diffWords`: single words, whitespace joining, punctuation, empty and identical input, and both sides reconstructed for prose, Unicode, newlines and a large input.',
        'Group name, provenance, count, hunk names, `<del>`/`<ins>` with spoken prefixes, and described Accept buttons.',
        'Per-hunk accept and reject by click and keyboard, the announcements, and focus moving to the next hunk or the title.',
        'Accept all and Reject all decide only undecided hunks; controlled `status` wins.',
        'Applied, rejected and reverted are read-only; Revert; unchanged hunks; localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
