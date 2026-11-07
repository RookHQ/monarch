import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { AiLauncher } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function InlineDemo() {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', justifyItems: 'start' }}>
      <AiLauncher
        open={open}
        onOpenChange={setOpen}
        position="inline"
        shortcut={false}
        aria-controls="docs-ai-launcher-inline-panel"
      />
      <div
        id="docs-ai-launcher-inline-panel"
        hidden={!open}
        style={{
          padding: 'var(--mn-space-3)',
          border: '1px solid var(--mn-color-border-default)',
          borderRadius: 'var(--mn-radius-md)',
        }}
      >
        The assistant panel would be here.
      </div>
    </div>
  );
}

function ShortcutDemo() {
  const [open, setOpen] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', justifyItems: 'start' }}>
      <AiLauncher open={open} onOpenChange={setOpen} position="inline" />
      <p style={{ margin: 0 }}>
        Press ⌘J (Apple) or Ctrl+J anywhere on the page, except while typing in a field. The
        assistant is {open ? 'open' : 'closed'}.
      </p>
      <input
        aria-label="Typing here does not trigger the shortcut"
        placeholder="Typing here does not trigger the shortcut"
        style={{ width: '100%', maxWidth: '22rem' }}
      />
    </div>
  );
}

function FloatingDemo() {
  const [shown, setShown] = useState(false);
  const [dock, setDock] = useState(true);
  const [open, setOpen] = useState(false);
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-2)' }}>
      <Button onClick={() => setShown((s) => !s)}>
        {shown ? 'Hide floating launcher' : 'Show floating launcher'}
      </Button>
      {shown ? (
        <>
          <Button onClick={() => setDock((d) => !d)}>
            {dock ? 'Hide the dock' : 'Show the dock'}
          </Button>
          {dock ? (
            <div
              className="docs-ai-launcher-dock"
              style={{
                position: 'fixed',
                insetBlockEnd: 'var(--mn-space-4)',
                insetInlineEnd: 'var(--mn-space-4)',
                width: '14rem',
                padding: 'var(--mn-space-3)',
                border: '1px solid var(--mn-color-border-default)',
                borderRadius: 'var(--mn-radius-lg)',
                background: 'var(--mn-color-bg-surface)',
                boxShadow: 'var(--mn-shadow-lg)',
              }}
            >
              A chat dock in the same corner.
            </div>
          ) : null}
          <AiLauncher
            open={open}
            onOpenChange={setOpen}
            shortcut={false}
            badge="unread"
            avoidSelector=".docs-ai-launcher-dock"
          />
        </>
      ) : null}
    </div>
  );
}

export default {
  id: 'ai-launcher',
  title: 'AiLauncher',
  summary: 'Floating or rail button that opens the AI assistant, with ⌘/Ctrl+J and badges.',
  sections: {
    purpose: {
      summary:
        'Gives every page one consistent way into the assistant: a floating button in a corner (or an inline rail button), a global ⌘/Ctrl+J shortcut, an unread or working badge, and collision avoidance so it never covers another floating element such as a chat dock.',
      useWhen: [
        'An app mounts AiAssistantPanel and needs a button and shortcut to toggle it.',
        'The assistant can finish work in the background and should show "working" or unread replies.',
      ],
      avoidWhen: [
        'An inline AI action on a specific record (use a Button with the action’s name).',
        'More than one launcher on a page: there should be one shortcut owner.',
      ],
    },
    designPrinciples: {
      items: [
        'The launcher owns the shortcut; the panel only reacts to `open`.',
        'Never interrupt typing: the shortcut is ignored in inputs, text areas and editable regions unless `shortcutInEditable`.',
        'Badges are never colour alone: each is part of the accessible name.',
        'Identify AI with the Bot glyph and the AI intent colours, never sparkles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the `<button>`.',
        '`parseAiShortcut`, `matchesAiShortcut` and `ariaKeyShortcut` are exported for products that register the same shortcut elsewhere (a command palette entry, say).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'position',
          values: ['bottom-end', 'bottom-start', 'inline'],
          description:
            'Floating in the end or start corner (logical, so it follows RTL), or in the flow for a rail or toolbar. Inline shows the label and shortcut by default.',
        },
        {
          prop: 'badge',
          values: ['unread', 'working', 'number'],
          description:
            'A dot, a moving ring (static under reduced motion), or a count (99+ above 99).',
        },
        {
          prop: 'showLabel',
          values: ['true', 'false'],
          description: 'Shows the label and the shortcut as text next to the glyph.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Closed', description: 'Bot glyph; `aria-expanded="false"`.' },
        {
          name: 'Open',
          attribute: 'data-open, aria-expanded="true"',
          description: 'A floating launcher shows a close glyph.',
        },
        {
          name: 'Badge',
          attribute: 'data-badge="unread | working | count"',
          description: 'Dot, ring or count, also spoken in the name.',
        },
        {
          name: 'Avoiding',
          attribute: 'data-avoiding, --mn-ai-launcher-avoid',
          description: 'Moved up above an element matching `avoidSelector`.',
        },
      ],
    },
    interaction: {
      items: [
        'Click toggles: it calls `onOpenChange(!open)`. An `onClick` that calls `preventDefault()` stops the toggle.',
        'The shortcut toggles from anywhere on the page, except while focus is in an editable field (unless `shortcutInEditable`). Key repeats and IME composition are ignored.',
        'With `avoidSelector`, a ResizeObserver and a MutationObserver re-check the corner as elements appear, resize or hide, and the launcher moves above any visible match it would overlap.',
      ],
    },
    keyboard: {
      items: [
        { keys: '⌘J (Apple) / Ctrl+J', action: 'Toggles the assistant (the `shortcut` prop).' },
        { keys: 'Enter / Space', action: 'Toggles the assistant while the button has focus.' },
      ],
      notes: [
        '`shortcut` accepts `mod`, `ctrl`, `meta`, `alt` and `shift` joined with `+`, e.g. `mod+shift+k`; `mod` is ⌘ on Apple devices and Ctrl elsewhere. `false` turns it off.',
      ],
    },
    accessibility: {
      role: 'button with aria-expanded',
      name: '`messages.label` ("Assistant"), followed by the badge text ("Assistant, New messages").',
      items: [
        'Set `aria-controls` to the panel’s id.',
        '`aria-keyshortcuts` tells assistive technology the shortcut (Meta+J or Control+J, set after mount so server and client markup match).',
        'The floating button is at least 48px; inline it is at least 44px.',
        'Badges have no separate live region: the change is heard when the button is focused. Announce important background results in the panel or with a toast.',
      ],
    },
    responsive: {
      items: [
        'Floating positions sit 16px from the corner and use logical insets, so `bottom-end` is bottom left in RTL.',
        'At 390px it still clears a chat dock with `avoidSelector`.',
      ],
    },
    theme: {
      items: [
        'AI intent tokens: `--mn-color-ai-solid` (floating), `--mn-color-ai-subtle` and `--mn-color-ai-strong` (inline), `--mn-color-ai-thinking` for the working ring; the count uses danger tokens.',
        'Forced colours use ButtonFace / ButtonText and Highlight for badges.',
      ],
    },
    examples: {
      items: [
        { title: 'Inline (rail) launcher', render: () => <InlineDemo /> },
        { title: 'Keyboard shortcut', stack: true, render: () => <ShortcutDemo /> },
        {
          title: 'Badges',
          render: () => (
            <div style={{ display: 'flex', gap: 'var(--mn-space-4)', flexWrap: 'wrap' }}>
              <AiLauncher
                open={false}
                onOpenChange={() => {}}
                position="inline"
                shortcut={false}
                badge="unread"
              />
              <AiLauncher
                open={false}
                onOpenChange={() => {}}
                position="inline"
                shortcut={false}
                badge="working"
              />
              <AiLauncher
                open={false}
                onOpenChange={() => {}}
                position="inline"
                shortcut={false}
                badge={3}
              />
            </div>
          ),
        },
        { title: 'Floating, avoiding a dock', render: () => <FloatingDemo /> },
      ],
    },
    doDont: {
      do: [
        'Mount one launcher per page and give it `aria-controls` for the panel.',
        'Pass `avoidSelector` when the product has another floating element in the same corner.',
        'Use `badge="working"` while the assistant runs a background task.',
      ],
      dont: [
        'Do not bind ⌘/Ctrl+J yourself as well; the launcher already owns it.',
        'Do not use sparkle icons for the launcher.',
      ],
    },
    related: {
      items: [
        { id: 'ai-assistant-panel', reason: 'The panel it opens and closes.' },
        {
          id: 'ai-thinking-indicator',
          reason: 'In-panel progress while the badge says "working".',
        },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Named toggle button with aria-expanded and aria-keyshortcuts.',
        'Ctrl+J toggles from the page but not from a text field; `shortcutInEditable`, a custom shortcut and `shortcut={false}`.',
        'Shortcut parsing and matching per platform.',
        'Badges are part of the accessible name; counts cap at 99+.',
        'Inline variant shows the label and shortcut.',
        'onClick preventDefault stops the toggle.',
        'Moves above a visible element matching avoidSelector and returns when it goes away (mocked layout).',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
