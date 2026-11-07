import { useState } from 'react';
import {
  AiAssistantPanel,
  AiLauncher,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 50,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant', role: 'Workspace assistant' } },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply streams from a mock transport. Press Escape while it streams to stop it, or Escape again to close the panel.`,
    ),
    { type: 'done' },
  ],
});

const STARTERS = ['Summarise this page', 'What changed this week?', 'Draft a status update'];

function LauncherDemo() {
  const chat = useAiConversation({ transport });
  const [open, setOpen] = useState(false);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '28rem' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--mn-space-3)' }}>
        <AiLauncher
          open={open}
          onOpenChange={setOpen}
          position="inline"
          aria-controls="docs-ai-assistant-panel"
          badge={chat.busy ? 'working' : undefined}
        />
        <span style={{ color: 'var(--mn-color-text-muted)' }}>or press ⌘J / Ctrl+J</span>
      </div>
      <div style={{ height: 520 }}>
        <AiAssistantPanel
          id="docs-ai-assistant-panel"
          placement="inline"
          conversation={chat}
          title="Assistant"
          open={open}
          onOpenChange={setOpen}
          starters={STARTERS}
          context={[
            { label: 'Page', items: 'Quarterly plan' },
            { label: 'Selected', items: '3 rows' },
          ]}
        />
      </div>
    </div>
  );
}

function FixedDemo() {
  const chat = useAiConversation({ transport });
  const [open, setOpen] = useState(false);
  const [minimized, setMinimized] = useState(false);
  return (
    <>
      <AiLauncher
        open={open}
        onOpenChange={setOpen}
        position="inline"
        shortcut={false}
        aria-controls="docs-ai-assistant-panel-fixed"
        messages={{ label: 'Open the docked assistant' }}
      />
      <AiAssistantPanel
        id="docs-ai-assistant-panel-fixed"
        conversation={chat}
        title="Assistant"
        open={open}
        onOpenChange={setOpen}
        minimized={minimized}
        onMinimizedChange={setMinimized}
        side="right"
        starters={STARTERS}
        footer={
          <span style={{ color: 'var(--mn-color-text-muted)', fontSize: 'var(--mn-text-xs)' }}>
            AI can make mistakes. Check important info.
          </span>
        }
      />
    </>
  );
}

export default {
  id: 'ai-assistant-panel',
  title: 'AiAssistantPanel',
  summary: 'The assistant rail any app mounts: header, page context, thread and prompt input.',
  sections: {
    purpose: {
      summary:
        'One ready-made assistant surface for every product: it wires `AiThread` and `AiPromptInput` to a `useAiConversation` result, shows what the assistant can see on the current page, and handles focus, Escape, minimizing and small screens, so products do not each rebuild a chat rail.',
      useWhen: [
        'A product adds a general assistant beside its pages.',
        'The assistant should keep its conversation while the person moves between pages (keep the hook above the router).',
      ],
      avoidWhen: [
        'A full-page chat product (compose AiThread and AiPromptInput in the page layout).',
        'A single AI action on a record (use the action’s own button and result).',
      ],
    },
    designPrinciples: {
      items: [
        'The panel is driven by the conversation hook: pass `useAiConversation()` in; the panel never owns the transport.',
        'Show context honestly: chips list what the assistant can see on this page.',
        'Never trap people on wide screens: beside the page it is a non-modal dialog; only the full-screen sheet below 640px is modal.',
        'The launcher owns the shortcut; the panel reacts to `open`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section role="dialog">`.',
        'Messages get regenerate, feedback, branch switching and retry wired to the conversation; override any of them with `messageProps`.',
        '`threadProps` and `promptProps` pass through to AiThread and AiPromptInput (e.g. `onAttach`, `maxLength`, `triggers`).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'placement',
          values: ['fixed', 'inline'],
          description:
            '`fixed` docks to the viewport edge above the page (and is a full-screen modal sheet below 640px). `inline` fills a column your layout reserves.',
        },
        {
          prop: 'side',
          values: ['right', 'left'],
          description: 'The viewport edge for `fixed`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Closed',
          attribute: 'hidden',
          description: 'Kept mounted (draft and scroll survive) unless `keepMounted={false}`.',
        },
        { name: 'Open', description: 'Header, context chips, thread and prompt input.' },
        {
          name: 'Minimized',
          attribute: 'data-minimized',
          description:
            'Only the header shows; the minimize button reports `aria-expanded="false"`.',
        },
        {
          name: 'Compact',
          attribute: 'data-compact, aria-modal="true"',
          description: 'Below 640px with `placement="fixed"`: full screen, Tab stays inside.',
        },
        {
          name: 'Running',
          attribute: 'data-status="submitted | streaming"',
          description: 'Send becomes Stop; Escape stops the reply.',
        },
        { name: 'Empty', description: 'A welcome with starter prompts, or your `empty` content.' },
      ],
    },
    interaction: {
      items: [
        'Opening moves focus to the prompt input; closing returns it to the element that had it before (usually the launcher).',
        'Expanding from minimized focuses the input again.',
        'Choosing a starter sends it and keeps focus in the input.',
        'Escape anywhere in the panel stops a running reply; when nothing runs it closes the panel (`closeOnEscape`). Escape inside an open menu or popup is left to it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Escape', action: 'Stops a running reply; otherwise closes the panel.' },
        { keys: 'Enter / Shift+Enter', action: 'Sends / adds a line in the prompt input.' },
        {
          keys: 'Tab',
          action: 'Moves through header, chips, thread and input; wraps inside below 640px.',
        },
        {
          keys: '⌘J / Ctrl+J',
          action: 'Toggles the panel, through AiLauncher (it owns the shortcut).',
        },
      ],
    },
    accessibility: {
      role: 'dialog (non-modal beside the page; aria-modal below 640px)',
      name: 'The `title` heading, through aria-labelledby.',
      items: [
        'Context chips are a list named "Context"; starters are a list named "Suggested prompts".',
        'The thread announces streamed text at sentence boundaries and "Response complete" (see AiThread); replies carry a visually hidden "Assistant said:" prefix.',
        'Minimize and close are labelled icon buttons; minimize sets `aria-expanded` and `aria-controls` on the body.',
        'Focus never moves while a reply streams.',
      ],
    },
    responsive: {
      items: [
        'Fixed: 24rem wide (`--mn-ai-assistant-panel-width`), never wider than the viewport minus 24px.',
        'Below 640px the fixed panel fills the screen as a modal sheet; minimized, it is a bar at the bottom.',
        'Header buttons and starters are 44px on coarse pointers; nothing scrolls horizontally at 390px.',
      ],
    },
    theme: {
      items: [
        'Surface, border and shadow tokens for the panel; chips use `--mn-color-ai-subtle`, `--mn-color-ai-border` and `--mn-color-ai-strong`.',
        'Forced colours draw the panel and dividers with CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With AiLauncher and ⌘/Ctrl+J (inline placement)',
          stack: true,
          render: () => <LauncherDemo />,
        },
        { title: 'Docked to the viewport, minimizable', render: () => <FixedDemo /> },
      ],
    },
    doDont: {
      do: [
        'Keep `useAiConversation` above routes so the conversation survives navigation.',
        'Pass context chips for what the assistant can actually see.',
        'Put an AiDisclaimer in `footer` for higher-risk assistants.',
      ],
      dont: [
        'Do not open the panel on page load; let people ask for it.',
        'Do not register a second shortcut; use AiLauncher.',
      ],
    },
    related: {
      items: [
        { id: 'ai-launcher', reason: 'The button and shortcut that toggle the panel.' },
        { id: 'ai-thread', reason: 'The conversation it shows.' },
        { id: 'ai-prompt-input', reason: 'The composer it shows.' },
        { id: 'ai-conversation', reason: 'The hook that drives it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-agent-avatar', 'ai-prompt-input', 'ai-thread', 'button'],
    },
    testing: {
      unit: [
        'Labelled non-modal dialog, hidden while closed, with context chips and the log.',
        'Focuses the input on open and returns focus to the launcher on close.',
        'Launcher Ctrl+J opens it; Escape closes it when idle.',
        'Sends through the conversation and shows the streamed reply and agent.',
        'Starters send their prompt.',
        'Escape stops a running reply instead of closing.',
        'Minimize / expand, with focus back to the input.',
        'Modal sheet with Tab wrap below 640px (mocked matchMedia).',
        '`keepMounted={false}`, `side` and strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
