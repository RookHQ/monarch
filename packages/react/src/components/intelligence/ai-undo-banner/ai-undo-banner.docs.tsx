import { useState } from 'react';
import { Button, ToastProvider } from '@rookcom/monarch-react';
import { AiUndoBanner, useAiUndoToast, type AiChange } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const BEFORE = { status: 'Open', owner: 'Unassigned', priority: 'Normal' };
const AFTER = { status: 'In progress', owner: 'Sam Lee', priority: 'High' };

function changesOf(from: typeof BEFORE, to: typeof BEFORE): AiChange[] {
  return [
    { label: 'Status', before: from.status, after: to.status },
    { label: 'Owner', before: from.owner, after: to.owner },
    { label: 'Priority', before: from.priority, after: to.priority },
  ];
}

function BannerDemo() {
  const [record, setRecord] = useState(BEFORE);
  const [key, setKey] = useState(0);
  const changed = record !== BEFORE;
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)' }}>
      <div>
        <Button
          size="sm"
          onClick={() => {
            setRecord(AFTER);
            setKey((k) => k + 1);
          }}
        >
          Let the assistant triage this ticket
        </Button>
      </div>
      <p style={{ margin: 0 }}>
        Status: {record.status} · Owner: {record.owner} · Priority: {record.priority}
      </p>
      {changed ? (
        <AiUndoBanner
          key={key}
          title="AI changed 3 fields"
          changes={changesOf(BEFORE, AFTER)}
          auditHref="#"
          onUndo={() =>
            new Promise<void>((resolve) =>
              setTimeout(() => {
                setRecord(BEFORE);
                resolve();
              }, 500),
            )
          }
        />
      ) : null}
    </div>
  );
}

function ToastDemo() {
  const [record, setRecord] = useState(BEFORE);
  const { undoable } = useAiUndoToast();
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <div>
        <Button
          size="sm"
          onClick={() => {
            setRecord(AFTER);
            void undoable({
              title: 'AI changed 3 fields',
              changes: changesOf(BEFORE, AFTER),
              onUndo: () => setRecord(BEFORE),
            });
          }}
        >
          Apply the suggestion
        </Button>
      </div>
      <p style={{ margin: 0 }}>
        Status: {record.status} · Owner: {record.owner} · Priority: {record.priority}
      </p>
    </div>
  );
}

export default {
  id: 'ai-undo-banner',
  title: 'AiUndoBanner',
  summary: 'An inline banner after an AI change, with what changed, Undo and an audit link.',
  sections: {
    purpose: {
      summary:
        'Makes AI edits visible and reversible: after the assistant changes fields on a record, the banner says so, lists each field before and after, and offers Undo and a link to the audit log. `useAiUndoToast()` is the timed toast variant.',
      useWhen: [
        'The assistant applied a change on the person’s behalf (autofill, triage, bulk update) and it should never be silent.',
        'The change can be reverted and the person may want to check it first.',
      ],
      avoidWhen: [
        'The change has not been applied yet: propose it with AiEditProposal instead.',
        'The person made the change themselves: use useUndoToast.',
        'The tool created something new: use AiToolResultCard.',
      ],
    },
    designPrinciples: {
      items: [
        'Every AI change is announced and attributable: the banner is identified as an AI change and links to the audit log.',
        'Show before and after, not just “updated”.',
        'Undo stays until the person leaves unless you pass `duration`; a timed Undo pauses while it is being read.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`onUndo` may return a promise: Undo shows a spinner, then “Changes undone” or an error with Undo still available.',
        'More than three `changes` collapse into a “Show N changes” disclosure.',
        '`useAiUndoToast({ messages? })` returns `{ undoable(options) }`; it wraps `useUndoToast` and needs `ToastProvider`. Its default description is the comma-separated labels of `changes`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No visual variants. Use `useAiUndoToast()` for the toast form.'],
    },
    states: {
      items: [
        { name: 'Active', attribute: 'data-state="active"', description: 'Undo available.' },
        {
          name: 'Undoing',
          attribute: 'data-state="undoing"',
          description: 'Undo shows a spinner.',
        },
        {
          name: 'Undone',
          attribute: 'data-state="undone"',
          description: '“Changes undone” shown and announced; Undo removed.',
        },
        {
          name: 'Failed',
          attribute: 'data-state="failed"',
          description: '“Couldn’t undo. Try again.” and Undo again.',
        },
        {
          name: 'Expired',
          attribute: 'data-state="expired"',
          description:
            'With `duration`: Undo removed, “Undo is no longer available.” The audit link stays.',
        },
      ],
    },
    interaction: {
      items: [
        'Undo calls `onUndo` once per press.',
        'With `duration`, the countdown pauses while the pointer is over the banner or focus is inside it, and `onExpire` runs once when it ends.',
        'Dismiss (with `onDismiss`) is the app’s cue to remove the banner.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the changes disclosure, Undo, the audit link and Dismiss.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused control.' },
      ],
      notes: ['When Undo disappears (undone or expired) focus moves to the banner itself.'],
    },
    accessibility: {
      role: 'group',
      name: 'The title with a hidden “AI change:” prefix, via aria-labelledby',
      items: [
        'Each change reads “Status changed from Open to In progress” using `<del>` and `<ins>` with hidden connecting words.',
        'Outcomes are announced through a polite `role="status"`.',
        'The Bot glyph is decorative; the hidden prefix is what identifies the change as AI-made.',
        'The timed Undo pauses on hover and focus (WCAG 2.2.1).',
      ],
    },
    responsive: {
      items: [
        'Below 480px the actions move under the content; values wrap; no horizontal scroll at 390px.',
        'The disclosure summary and audit link are at least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Banner on `--mn-color-ai-subtle` with `--mn-color-ai-border`; the Bot glyph in `--mn-color-ai-strong`.',
        'Undone and expired banners fall back to `--mn-color-bg-subtle`.',
        'Forced colours: CanvasText border, LinkText audit link.',
      ],
    },
    examples: {
      items: [
        { title: 'After an AI change', stack: true, render: () => <BannerDemo /> },
        {
          title: 'Timed, with more than three changes',
          stack: true,
          render: () => (
            <AiUndoBanner
              title="AI filled 4 fields from the email"
              changes={[
                { label: 'Company', before: '—', after: 'Northwind Traders' },
                { label: 'Phone', before: '—', after: '+1 555 0100' },
                { label: 'City', before: '—', after: 'Lisbon' },
                { label: 'Stage', before: 'Lead', after: 'Prospect' },
              ]}
              duration={15000}
              auditHref="#"
              onUndo={() => {}}
              onDismiss={() => {}}
            />
          ),
        },
        {
          title: 'Toast variant',
          stack: true,
          render: () => (
            <ToastProvider label="AI change notifications">
              <ToastDemo />
            </ToastProvider>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show the banner every time the AI writes to a record, next to the record.',
        'Link to the audit log entry so the change can be traced later.',
        'Use the toast variant only for small changes in a list or table.',
      ],
      dont: [
        'Do not apply AI edits silently.',
        'Do not set a short `duration` for many changes; people need time to read them.',
      ],
    },
    related: {
      items: [
        { id: 'undo-toast', reason: 'The toast that `useAiUndoToast` builds on.' },
        { id: 'ai-edit-proposal', reason: 'Propose AI edits before applying them.' },
        { id: 'ai-tool-result-card', reason: 'Report something the AI created.' },
        { id: 'audit-log-viewer', reason: 'Where the audit link should lead.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'undo-toast'] },
    testing: {
      unit: [
        'Title with AI prefix, before/after with del/ins, safe audit link; prop and ref forwarding.',
        'More than three changes collapse into a disclosure; unsafe audit links dropped.',
        'Undo success announces and keeps focus; failure keeps Undo.',
        'Timed Undo expires, pausing while hovered; Dismiss.',
        'useAiUndoToast shows the toast with the changed labels and resolves `undone`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
