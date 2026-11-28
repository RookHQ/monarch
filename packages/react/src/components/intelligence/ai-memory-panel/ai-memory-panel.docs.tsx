import { useState } from 'react';
import { AiMemoryPanel, type AiMemoryItem } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-memory-panel.fixture.json';

const MEMORIES = fixture.data.memories as AiMemoryItem[];

function PanelDemo({ initial = MEMORIES, label }: { initial?: AiMemoryItem[]; label?: string }) {
  const [memories, setMemories] = useState(initial);
  return (
    <div style={{ maxWidth: '32rem' }}>
      <AiMemoryPanel
        memories={memories}
        locale={fixture.data.locale}
        timeZone={fixture.data.timeZone}
        {...(label ? { messages: { label } } : {})}
        onEdit={(id, text) =>
          setMemories((all) =>
            all.map((x) => (x.id === id ? { ...x, text, updatedAt: new Date() } : x)),
          )
        }
        onForget={(id) => setMemories((all) => all.filter((x) => x.id !== id))}
        onForgetMany={(ids) => setMemories((all) => all.filter((x) => !ids.includes(x.id)))}
      />
    </div>
  );
}

export default {
  id: 'ai-memory-panel',
  title: 'AiMemoryPanel',
  summary: 'Everything the AI remembers, grouped by entity, with search, edit and bulk forget.',
  sections: {
    purpose: {
      summary:
        'Gives people one place to review, correct and remove what the AI keeps between conversations, grouped by the person, organization or project each memory is about.',
      useWhen: [
        'A settings page or side panel lists the AI’s saved memories.',
        'People need to forget several memories at once, for example everything about one person.',
      ],
      avoidWhen: [
        'You show one memory in a thread as it is saved: use AiMemoryChip.',
        'The AI keeps no memory between conversations.',
      ],
    },
    designPrinciples: {
      items: [
        'Organised by who or what the memory is about, because that is how people look for it.',
        'Single changes are quick (inline edit, one-click forget); bulk forget is deliberate and confirmed.',
        'Controlled: the app owns the memories and the panel reports what people decided.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<section>` attributes pass through and the ref is forwarded to the root, a region named by `messages.label`.',
        '`onForgetMany` may return a promise: the confirmation stays open and pending until it settles and shows the error if it rejects.',
        '`groupAiMemories(memories)` is exported: people, then organizations, projects and other, each by name, newest memory first.',
        'Selection survives searching; "Forget selected" acts on every selected memory, shown or not, and its count says how many.',
        '`applyAiMemoryEvent(memories, event, options?)` keeps the list in step with a conversation: pass it the events from `useAiConversation({ onMemoryEvent })`. `write` updates the memory with that id (text and time) or adds it, `forget` removes it and `read` changes nothing; it returns the same array when nothing changed, so it works as a state updater. `options.entity(event)` names who or what a new memory is about (default: an `other` entity named by the event’s `scope`, or `defaultEntityName`, "General"); `options.now` sets the time. `applyAiMemoryEvents` applies several.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. `headingLevel` sets the level of the group headings (default 3).',
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: 'No memories: an empty message only.',
        },
        { name: 'No matches', description: 'The search matched nothing: says so with the query.' },
        {
          name: 'Selection',
          description:
            'Group and select-all checkboxes are checked, unchecked or mixed; "Forget selected (n)" is enabled while anything is selected.',
        },
        { name: 'Editing', description: 'One row shows a text field with Save and Cancel.' },
        { name: 'Confirming', description: 'The bulk-forget confirmation dialog is open.' },
      ],
    },
    interaction: {
      items: [
        'Typing in search filters by memory text and entity name; the match count is announced after a short pause.',
        'Edit works in place (Enter saves, Escape cancels) and announces "Memory updated".',
        'Forget on a row removes it straight away, announces "Memory forgotten" and moves focus to the row that took its place (or the search field).',
        'Forget selected opens a danger confirmation; confirming forgets, clears the selection, announces the count and focuses the search field.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through search, select all, Forget selected and each group and row.',
        },
        { keys: 'Space', action: 'Toggles the focused checkbox.' },
        { keys: 'Enter / Escape (editing)', action: 'Saves or cancels the edit.' },
        { keys: 'Escape (search)', action: 'Clears the search.' },
      ],
    },
    accessibility: {
      role: 'region',
      name: '`messages.label`, default "Memories"',
      items: [
        'Each group has a heading (entity, kind and count) that names its list.',
        'Every checkbox and button has a unique name that includes the memory text or entity name.',
        'Search results, edits and forgets are announced through a polite status region.',
        'Bulk forget uses the shared confirmation dialog, which returns focus sensibly when it closes.',
      ],
    },
    responsive: {
      items: [
        'The toolbar wraps and rows wrap their text, so nothing scrolls sideways at 390px.',
        'On coarse pointers the buttons grow to 44px.',
      ],
    },
    theme: {
      items: [
        'Neutral surface tokens: `--mn-color-border-subtle` dividers, `--mn-color-text-muted` metadata.',
        'In forced colours dividers and the empty outline use CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Memories', stack: true, render: () => <PanelDemo /> },
        {
          title: 'Empty',
          stack: true,
          render: () => <PanelDemo initial={[]} label="Project memories" />,
        },
      ],
    },
    doDont: {
      do: [
        'Link to the panel from wherever the AI says it remembered something.',
        'Delete forgotten memories on the server, not only from the view.',
      ],
      dont: [
        'Do not skip the confirmation for bulk forget.',
        'Do not show memories about other people to someone who cannot manage them.',
      ],
    },
    related: {
      items: [
        { id: 'ai-memory-chip', reason: 'One memory, shown in a thread as it is saved.' },
        { id: 'ai-settings-section', reason: 'Turns memory on or off for the workspace.' },
        { id: 'confirm-dialog', reason: 'The confirmation used for bulk forget.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'checkbox', 'confirm-dialog', 'search-input', 'ai-memory-chip'],
    },
    testing: {
      unit: [
        'Groups by entity with headings that name their lists; group and item order.',
        'Search filters by text and entity, announces the match count and shows a no-match message.',
        'Inline edit saves, announces and returns focus; single forget announces and moves focus.',
        'Row, group and select-all checkboxes with mixed states; bulk forget confirms, cancels, calls back, announces and focuses search.',
        'Empty state and localised strings.',
        '`applyAiMemoryEvent`: write updates in place or adds, entity and time options, forget removes, read and no-op events return the same array; `applyAiMemoryEvents` in order, rendered by the panel.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
