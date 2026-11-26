import {
  AiFollowUpSuggestions,
  AiThread,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages }) => [
    ...textDeltas(
      `Here is a reply to “${messageText(messages[messages.length - 1] ?? { parts: [] })}”, streamed from a mock transport. Pick another follow-up below.`,
    ),
    { type: 'done' },
  ],
});

const SUGGESTIONS = ['Make it shorter', 'Add a timeline', 'Who owns each goal?'];

function Demo() {
  const chat = useAiConversation({ transport });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '40rem' }}>
      {chat.messages.length > 0 ? (
        <div style={{ height: 300 }}>
          <AiThread
            messages={chat.messages}
            status={chat.status}
            error={chat.error}
            onStop={chat.stop}
          />
        </div>
      ) : (
        <p style={{ margin: 0 }}>The quarterly plan has three goals and a launch in week 9.</p>
      )}
      <AiFollowUpSuggestions
        suggestions={SUGGESTIONS}
        disabled={chat.busy}
        onSelect={(text) => void chat.send(text)}
      />
    </div>
  );
}

export default {
  id: 'ai-follow-up-suggestions',
  title: 'AiFollowUpSuggestions',
  summary: 'Follow-up question chips after a reply, with arrow-key navigation.',
  sections: {
    purpose: {
      summary:
        'Offers likely next questions after an assistant reply, so people can go deeper with one choice instead of typing.',
      useWhen: [
        'After the latest assistant reply, when the model or product can suggest useful follow-ups.',
      ],
      avoidWhen: [
        'Starting a conversation: use AiStarterPrompts.',
        'Actions that change data: use AiSuggestedActions.',
      ],
    },
    designPrinciples: {
      items: [
        'Suggestions are complete prompts, shown exactly as they will be sent.',
        'One tab stop for the whole row, so they never slow down keyboard users moving past.',
        'Outlined chips in the AI colour, quieter than the reply itself.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the toolbar.',
        '`onSelect(text)` receives the suggestion. Send it, or place it in the prompt input.',
        'Renders nothing when `suggestions` is empty.',
        '`AI_FOLLOW_UP_SUGGESTIONS_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['There are no visual variants.'] },
    states: {
      items: [
        { name: 'Default', description: 'One chip is the tab stop (roving tabindex).' },
        { name: 'Hover / pressed', description: 'The chip takes the AI subtle tint.' },
        { name: 'Disabled', description: 'Every chip is disabled, e.g. while a reply streams.' },
      ],
    },
    interaction: {
      items: [
        'Choosing a chip calls `onSelect`; the component keeps only which chip is the tab stop.',
        'Focus wraps from the last chip to the first and back.',
        'If the list shrinks, the tab stop moves to a chip that still exists.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves into the row (to the last focused chip) and out of it.' },
        { keys: 'Arrow Right / Arrow Down', action: 'Next chip (Arrow Left in right-to-left).' },
        { keys: 'Arrow Left / Arrow Up', action: 'Previous chip (Arrow Right in right-to-left).' },
        { keys: 'Home / End', action: 'First or last chip.' },
        { keys: 'Enter / Space', action: 'Chooses the focused chip.' },
      ],
    },
    accessibility: {
      role: 'toolbar of buttons',
      name: '"Suggested follow-ups" (aria-label)',
      items: [
        'Each chip is a native button named by its text.',
        'Roving tabindex keeps a single tab stop; arrow keys follow the reading direction.',
      ],
    },
    responsive: {
      items: [
        'Chips wrap onto new lines and long text wraps inside a chip, so 390px has no horizontal scroll.',
        'On coarse pointers chips are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Chips: `--mn-color-ai-border`, `--mn-color-ai-strong` on `--mn-color-bg-surface`; hover `--mn-color-ai-subtle`.',
        'In forced colours chips use ButtonText and disabled chips GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'After a reply', stack: true, render: () => <Demo /> },
        {
          title: 'Disabled',
          render: () => (
            <AiFollowUpSuggestions suggestions={SUGGESTIONS} disabled onSelect={() => {}} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Show two to four suggestions under the latest reply only.',
        'Hide or disable them while a new reply streams.',
      ],
      dont: [
        'Do not put actions that change data in follow-ups.',
        'Do not keep stale suggestions under older replies.',
      ],
    },
    related: {
      items: [
        { id: 'ai-starter-prompts', reason: 'Suggestions for an empty conversation.' },
        { id: 'ai-suggested-actions', reason: 'Runnable actions rather than questions.' },
        { id: 'ai-thread', reason: 'The conversation the suggestions follow.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Named toolbar with one tab stop; props and ref forwarded.',
        'Arrow keys, Home and End with wrap-around; Tab returns to the last chip.',
        'onSelect from Enter, Space and click.',
        'Tab stop stays valid when suggestions shrink; disabled, messages, empty.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
