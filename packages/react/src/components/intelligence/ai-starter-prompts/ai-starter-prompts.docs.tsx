import { useState } from 'react';
import { AiStarterPrompts, type AiStarterPrompt } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PROMPTS: AiStarterPrompt[] = [
  {
    id: 'summary',
    title: 'Summarise a document',
    prompt: 'Summarise the open document in five bullet points.',
  },
  {
    id: 'reply',
    title: 'Draft a reply',
    prompt: 'Draft a short, friendly reply to the latest message.',
  },
  {
    id: 'plan',
    title: 'Plan the week',
    prompt: 'Turn my open tasks into a plan for this week.',
  },
  {
    id: 'explain',
    title: 'Explain a chart',
    prompt: 'Explain what the chart on this page shows and what stands out.',
  },
];

function Demo({ layout }: { layout: 'grid' | 'list' }) {
  const [chosen, setChosen] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', width: '100%', maxWidth: '40rem' }}>
      <AiStarterPrompts prompts={PROMPTS} layout={layout} onSelect={setChosen} />
      <p style={{ margin: 0, color: 'var(--mn-color-text-muted)' }}>
        {chosen ? `Would send: ${chosen}` : 'Choose a prompt.'}
      </p>
    </div>
  );
}

export default {
  id: 'ai-starter-prompts',
  title: 'AiStarterPrompts',
  summary: 'Suggested ways to start a conversation, as a grid of cards or a list.',
  sections: {
    purpose: {
      summary:
        'Shows a few ready-made prompts so people can start a conversation with one choice and learn what the assistant is good at.',
      useWhen: [
        'An empty conversation (usually inside AiWelcome).',
        'A launcher or panel that should suggest useful first questions for the current page.',
      ],
      avoidWhen: [
        'Suggestions after a reply: use AiFollowUpSuggestions.',
        'Reusable templates with fields to fill in: use AiPromptTemplatePicker.',
      ],
    },
    designPrinciples: {
      items: [
        'Show the actual prompt, so people know exactly what will be sent.',
        'Few and relevant: four to six starters that fit the context.',
        'Hover and press use the AI tint; icons are decorative and never sparkles.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<ul>`; native list attributes pass through and the ref is forwarded.',
        '`onSelect(prompt, starter)` receives the prompt text first. Send it (`useAiConversation().send`) or place it in the input for editing.',
        'Renders nothing when `prompts` is empty.',
        '`AI_STARTER_PROMPTS_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'layout',
          values: ['grid', 'list'],
          description:
            '`grid` fits as many 13rem cards per row as the width allows; `list` stacks full-width rows. Exposed as `data-layout`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Default', description: 'Every starter is a button.' },
        { name: 'Hover / pressed', description: 'The card takes the AI subtle tint.' },
        { name: 'Disabled', description: 'Every button is disabled, e.g. while a reply streams.' },
      ],
    },
    interaction: {
      items: ['Choosing a starter calls `onSelect` with its prompt; the component keeps no state.'],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between starters.' },
        { keys: 'Enter / Space', action: 'Chooses the focused starter.' },
      ],
    },
    accessibility: {
      role: 'list of buttons',
      name: '"Suggested prompts" (aria-label)',
      items: [
        'Each button is named by its title and described by its prompt text.',
        'With `hidePrompt` the prompt is neither shown nor read.',
        'Icons are aria-hidden.',
      ],
    },
    responsive: {
      items: [
        'The grid drops to one column when there is no room for two 13rem cards, so 390px has no horizontal scroll.',
        'Prompt text is clamped to two lines; buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Cards: `--mn-color-bg-surface`, `--mn-color-border-default`; hover `--mn-color-ai-subtle` and `--mn-color-ai-border`.',
        'Icon colour: `--mn-color-ai-strong`.',
        'In forced colours borders use ButtonText and disabled starters GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Grid', stack: true, render: () => <Demo layout="grid" /> },
        { title: 'List', stack: true, render: () => <Demo layout="list" /> },
        {
          title: 'Titles only, disabled',
          stack: true,
          render: () => (
            <AiStarterPrompts prompts={PROMPTS} hidePrompt disabled onSelect={() => {}} />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write titles as short verb phrases and prompts as complete requests.',
        'Tailor the starters to what the assistant can see on the current page.',
      ],
      dont: [
        'Do not show more than six starters.',
        'Do not send a starter with hidden extra instructions the person cannot see.',
      ],
    },
    related: {
      items: [
        { id: 'ai-welcome', reason: 'The empty state that usually holds the starters.' },
        { id: 'ai-follow-up-suggestions', reason: 'Suggestions after a reply.' },
        { id: 'ai-prompt-input', reason: 'Where an edited starter can be placed.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Named list of buttons named by title and described by prompt; decorative icons; props and ref forwarded.',
        'onSelect from mouse and keyboard.',
        'hidePrompt, disabled and messages; nothing rendered when empty.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
