import { useState } from 'react';
import {
  AiCopilotLayout,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
  useAiSelectionContext,
} from '@rookcom/monarch-react/ai';
import { Checkbox } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages, body }) => {
    const selection = (body?.selection as { label: string; text?: string }[] | undefined) ?? [];
    const seen = selection.length
      ? `I can see your selection (${selection.map((s) => s.text ?? s.label).join(', ')}).`
      : 'Nothing is selected on the page.';
    return [
      { type: 'agent', agent: { name: 'Assistant', role: 'Copilot' } },
      ...textDeltas(
        `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. ${seen} This reply comes from a mock transport.`,
      ),
      { type: 'done' },
    ];
  },
});

const ROWS = ['Northwind renewal', 'Harbor expansion', 'Summit pilot', 'Atlas migration'];

function Table() {
  const { setSelection } = useAiSelectionContext();
  const [checked, setChecked] = useState<string[]>([]);
  const toggle = (row: string, on: boolean) => {
    const next = on ? [...checked, row] : checked.filter((r) => r !== row);
    setChecked(next);
    setSelection(
      next.length
        ? [{ label: 'Selected', items: `${next.length} rows`, text: next.join('; ') }]
        : null,
    );
  };
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', padding: 'var(--mn-space-4)' }}>
      <h3 style={{ margin: 0 }}>Opportunities</h3>
      <p style={{ margin: 0, color: 'var(--mn-color-text-muted)' }}>
        Select rows, then ask the assistant about them.
      </p>
      {ROWS.map((row) => (
        <Checkbox
          key={row}
          label={row}
          checked={checked.includes(row)}
          onCheckedChange={(on) => toggle(row, on === true)}
        />
      ))}
    </div>
  );
}

function Demo() {
  const chat = useAiConversation({ transport });
  return (
    <div
      style={{
        width: '100%',
        height: 520,
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
        overflow: 'hidden',
      }}
    >
      <AiCopilotLayout
        conversation={chat}
        title="Assistant"
        defaultOpen
        launcherProps={{ position: 'inline' }}
        context={[{ label: 'Page', items: 'Opportunities' }]}
        panelProps={{ starters: ['Summarise the selection', 'Which one is most at risk?'] }}
      >
        <Table />
      </AiCopilotLayout>
    </div>
  );
}

export default {
  id: 'ai-copilot-layout',
  title: 'AiCopilotLayout',
  summary: 'App content with a docked assistant that sees what the person selected.',
  sections: {
    purpose: {
      summary:
        'A copilot is most useful when it knows what the person is looking at. AiCopilotLayout docks an AiAssistantPanel beside the app’s content and shares a selection context between them: the page writes the selection, the panel shows it as context chips and sends it with the next prompt.',
      useWhen: [
        'An app page where the assistant should act on selected rows, text or records.',
        'You want the content to reflow beside the assistant instead of being covered.',
      ],
      avoidWhen: [
        'A full-page chat (use AiChatPage).',
        'An assistant that never needs page context (AiAssistantPanel with AiLauncher is enough).',
      ],
    },
    designPrinciples: {
      items: [
        'Context is explicit: what the assistant will receive is visible as chips before sending.',
        'The page owns the selection; the layout only carries it.',
        'Docked, not covering: content reflows on wide screens; phones get a full-screen sheet.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`useAiSelectionContext()` returns `{ selection, setSelection, available }`; outside the layout it is empty and inert.',
        'The selection is sent as `body.selection` (`selectionField`) with `{ label, text, data }` per item.',
        '`panelProps` and `launcherProps` pass through to AiAssistantPanel and AiLauncher. A `panelProps.onSend` replaces the built-in send.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        { prop: 'side', values: ['right', 'left'], description: 'Where the assistant docks.' },
        {
          prop: 'launcher',
          values: ['true', 'false'],
          description: 'Render the floating launcher with ⌘J / Ctrl+J, or open it yourself.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Closed', description: 'Content uses the full width; the launcher shows.' },
        {
          name: 'Open',
          attribute: 'data-open',
          description: 'The assistant takes a column (24rem, at most 45%); the launcher hides.',
        },
        {
          name: 'Compact',
          attribute: 'data-compact',
          description: 'Below 640px: the assistant is a full-screen modal sheet.',
        },
      ],
    },
    interaction: {
      items: [
        'The launcher or ⌘J / Ctrl+J opens the assistant; focus moves to its input.',
        'Closing returns focus to the launcher.',
        'Selecting in the page updates the chips at once; sending includes the selection.',
        '`clearSelectionOnSend` empties the selection after each prompt.',
      ],
    },
    keyboard: {
      items: [
        { keys: '⌘J / Ctrl+J', action: 'Opens or closes the assistant (AiLauncher).' },
        { keys: 'Escape', action: 'Stops a running reply, otherwise closes the assistant.' },
        { keys: 'Tab', action: 'Moves between the content and the assistant in reading order.' },
      ],
    },
    accessibility: {
      role: 'none (layout); the assistant is a labelled dialog',
      name: 'The assistant is named by `title`.',
      items: [
        'Context chips are a list named “Context”, read before the conversation.',
        'The launcher has `aria-controls` pointing at the panel.',
        'Focus is never moved while the person works in the page.',
      ],
    },
    responsive: {
      items: [
        'From 640px: two columns, content first in reading order.',
        'Below 640px: one column; the assistant opens as a full-screen sheet with Tab kept inside.',
        'Rail width: `--mn-ai-copilot-layout-rail-width`.',
      ],
    },
    theme: {
      items: [
        'The layout adds no colour; the panel and launcher use their own AI tokens.',
        'Forced colours: inherits from AiAssistantPanel.',
      ],
    },
    examples: {
      items: [
        { title: 'Selection shared with the assistant', stack: true, render: () => <Demo /> },
      ],
    },
    doDont: {
      do: [
        'Send plain `text` for each selection item so the model can read it.',
        'Clear the selection when the page’s own selection is cleared.',
        'Put the conversation hook above the layout so it survives navigation.',
      ],
      dont: [
        'Do not put secrets in `data`; it goes to the transport.',
        'Do not open the assistant on page load.',
      ],
    },
    related: {
      items: [
        { id: 'ai-assistant-panel', reason: 'The docked assistant.' },
        { id: 'ai-launcher', reason: 'The button and shortcut that open it.' },
        { id: 'ai-chat-page', reason: 'Full-page chat instead of a copilot.' },
        { id: 'ai-context-chips', reason: 'Richer removable context chips.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['ai-assistant-panel', 'ai-launcher'],
    },
    testing: {
      unit: [
        'Content and launcher render; closed by default.',
        'The launcher opens a docked inline panel and hides; closing returns focus.',
        'The page’s selection shows as chips and is sent in the request body.',
        '`clearSelectionOnSend` and `selectionField`.',
        'Full-screen sheet below 640px (mocked matchMedia).',
        'The hook is inert outside the layout.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
