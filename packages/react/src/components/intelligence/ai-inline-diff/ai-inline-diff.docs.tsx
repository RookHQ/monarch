import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  AiInlineDiff,
  diffWords,
  resolveAiInlineDiff,
  type AiDiffSegment,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const BEFORE =
  'Our team meets every week to review the plan. We will ship the new flow in May, and collect feedback after.';
const AFTER =
  'Our team meets every Monday to review the roadmap. We will ship the redesigned flow in June, and collect feedback after launch.';

function ReviewDemo() {
  const [segments, setSegments] = useState<AiDiffSegment[]>(() => diffWords(BEFORE, AFTER));
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <AiInlineDiff
        segments={segments}
        onAccept={(id) => setSegments((s) => resolveAiInlineDiff(s, id, 'accept'))}
        onReject={(id) => setSegments((s) => resolveAiInlineDiff(s, id, 'reject'))}
      />
      <div>
        <Button size="sm" onClick={() => setSegments(diffWords(BEFORE, AFTER))}>
          Reset demo
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'ai-inline-diff',
  title: 'AiInlineDiff',
  summary: 'AI suggestions marked inline in text, with accept, reject and next / previous.',
  sections: {
    purpose: {
      summary:
        'Shows AI-suggested changes in place, as marked insertions and deletions inside the text, so a person can step through them and accept or reject each one without leaving the content.',
      useWhen: [
        'An assistant suggests rewording for a paragraph, a description or a message draft.',
        'The changes are small and best read in context.',
        'You need an editor-agnostic suggestion view now; an editor adapter (for example Tiptap) can map the same segments onto editor marks later.',
      ],
      avoidWhen: [
        'The edits span several fields or sections that each need a label: use AiEditProposal.',
        'The change is code: use AiCodeBlock with `diff`.',
        'The text is still streaming: wait until it is done.',
      ],
    },
    designPrinciples: {
      items: [
        'In context: the suggestion appears where the text will change.',
        'One change at a time: only the active change shows its controls, so the text stays readable.',
        'Controlled and editor-agnostic: it renders segments and reports decisions; the app (or an editor adapter) applies them.',
        'Never colour alone: deletions are struck through, insertions underlined, and both are announced.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'Segments use the `AiDiffSegment` shape from `diffWords` (exported with AiEditProposal).',
        'A change is a run of adjacent `delete` and `insert` segments that share an `id` (or all have none). Its id is that `id`, or the index of its first segment as a string; `aiInlineDiffChanges(segments)` returns them.',
        '`resolveAiInlineDiff(segments, id, "accept" | "reject")` returns the segments with that change applied or dropped; use it in `onAccept` and `onReject`.',
        'Editor adapters: map each change to a pair of suggestion marks and call `onAccept`/`onReject` from the editor’s own commands. No adapter ships yet.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'showNavigation',
          values: ['true', 'false'],
          description:
            'The bar with "Change 2 of 5" and previous / next buttons. Hide it for a single short suggestion.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Active change',
          attribute: 'data-active on the change',
          description: 'Outlined, in the tab order, and followed by its Accept and Reject buttons.',
        },
        {
          name: 'No changes',
          attribute: 'data-empty',
          description: 'The bar reads "No suggested changes" and the text is plain.',
        },
        {
          name: 'Read-only',
          description:
            'Without `onAccept` and `onReject` the changes can be navigated but not decided.',
        },
      ],
    },
    interaction: {
      items: [
        'Previous and next move to the neighbouring change, wrapping at the ends, and focus it.',
        'Clicking a change makes it active.',
        'Accept or Reject calls `onAccept(id)` or `onReject(id)`, announces it, and focus moves to the change that takes its place once the app updates `segments`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Enters at the active change, then its Accept and Reject.' },
        {
          keys: 'Arrow Down / Arrow Right',
          action: 'Next change (Arrow Left in right-to-left text).',
        },
        {
          keys: 'Arrow Up / Arrow Left',
          action: 'Previous change (Arrow Right in right-to-left text).',
        },
        { keys: 'Home / End', action: 'First / last change.' },
        { keys: 'Enter / Space', action: 'On Accept or Reject, decides the active change.' },
      ],
      notes: [
        'Arrow keys work when focus is on a change or its buttons; the changes use a roving tab index, so the diff is one tab stop plus the active change’s buttons.',
      ],
    },
    accessibility: {
      role: 'group, with a polite status',
      name: '"AI suggested changes" (messages.label), which also says the suggestions come from AI',
      items: [
        'Deletions are `<del>` and insertions `<ins>`, each with a visually hidden "Deleted:" or "Inserted:" prefix.',
        'Accept and Reject are icon buttons named "Accept change 2 of 5" and "Reject change 2 of 5".',
        'Decisions are announced politely ("Change accepted. 3 left.").',
        'When the last change is decided, focus moves to the text instead of being lost.',
      ],
    },
    responsive: {
      items: [
        'Text wraps, including long words, so there is no horizontal scroll at 390px.',
        'On coarse pointers the navigation and Accept / Reject buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Deletions use `--mn-color-ai-diff-remove` / `-remove-strong`, insertions `--mn-color-ai-diff-add` / `-add-strong`.',
        'The active change has an `--mn-color-ai-border` outline.',
        'In forced colours marks use CanvasText with strikethrough and a border, and the active change a Highlight outline.',
      ],
    },
    examples: {
      items: [
        { title: 'Review suggestions', stack: true, render: () => <ReviewDemo /> },
        {
          title: 'Read-only, single suggestion',
          stack: true,
          render: () => (
            <AiInlineDiff
              showNavigation={false}
              segments={diffWords('Thanks for the update.', 'Thanks for the quick update.')}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Update `segments` after every decision (`resolveAiInlineDiff`).',
        'Give changes stable `id`s when your editor or backend has them.',
        'Use AiEditProposal when changes need labels.',
      ],
      dont: [
        'Do not apply suggestions before the person decides.',
        'Do not render it while the suggestion is still streaming.',
        'Do not rely on colour to tell insertions from deletions.',
      ],
    },
    related: {
      items: [
        { id: 'ai-edit-proposal', reason: 'Labelled hunks of edits, and `diffWords`.' },
        { id: 'rich-text-editor', reason: 'Where an editor adapter would show the same marks.' },
        { id: 'ai-undo-banner', reason: 'Undo for changes the AI already made.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Groups segments into changes by adjacency and id; accept and reject merge the text.',
        'Group name, `<del>`/`<ins>` with spoken prefixes, position, and controls on the active change only.',
        'Previous / next buttons, arrow keys, Home / End, wrapping, right-to-left arrows, and clicking a change.',
        'Accept and reject by click and keyboard, the announcements, and focus after each decision and after the last.',
        'Read-only without handlers, hidden navigation and localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
