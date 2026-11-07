import { Button } from '@rookcom/monarch-react';
import {
  AiStarterPrompts,
  AiThread,
  AiWelcome,
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
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply comes from a mock transport, so the demo works offline.`,
    ),
    { type: 'done' },
  ],
});

const AGENT = { name: 'Assistant', role: 'Workspace assistant' };

function Demo() {
  const chat = useAiConversation({ transport });
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <div style={{ height: 460 }}>
        <AiThread
          messages={chat.messages}
          status={chat.status}
          error={chat.error}
          onStop={chat.stop}
          empty={
            <AiWelcome
              agent={AGENT}
              title="How can I help?"
              description="I can read the page you are on and the documents you share with me."
              starters={
                <AiStarterPrompts
                  prompts={[
                    { id: 'sum', title: 'Summarise this page', prompt: 'Summarise this page.' },
                    {
                      id: 'next',
                      title: 'Suggest next steps',
                      prompt: 'What should I do next on this project?',
                    },
                  ]}
                  onSelect={(prompt) => void chat.send(prompt)}
                />
              }
            />
          }
        />
      </div>
      {chat.messages.length > 0 ? (
        <Button size="sm" onClick={() => chat.reset()} style={{ justifySelf: 'start' }}>
          Start over
        </Button>
      ) : null}
    </div>
  );
}

export default {
  id: 'ai-welcome',
  title: 'AiWelcome',
  summary: 'Empty state for a new conversation: who the assistant is and where to start.',
  sections: {
    purpose: {
      summary:
        'Fills the empty thread of a new conversation with a greeting, the assistant’s identity, what it can help with and starting points, so people know what to ask before they type.',
      useWhen: [
        'A conversation has no messages yet (pass it as AiThread’s `empty`).',
        'A chat page or assistant panel opens fresh.',
      ],
      avoidWhen: [
        'A non-AI empty list or page: use EmptyState.',
        'Explaining AI features across a product: use AiOnboardingTour.',
      ],
    },
    designPrinciples: {
      items: [
        'Clearly AI: the Bot mark in the AI colour and the agent’s name, never a sparkle.',
        'Concrete over clever: capabilities are short, factual statements.',
        'Starting points are real prompts the person can send with one choice.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<section>` named by the title; native attributes pass through and the ref is forwarded.',
        '`starters` is a slot: usually AiStarterPrompts, but any content works.',
        'An `agent.avatar` image is shown only when it passes the safe image URL check; otherwise the Bot mark is used.',
        '`AI_WELCOME_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'There are no variant props; omit `agent`, `description` or `capabilities` to simplify it.',
      ],
    },
    states: {
      items: [{ name: 'Static', description: 'Not interactive itself; the starters slot may be.' }],
    },
    interaction: {
      items: ['Not interactive itself. Starters inside it handle their own selection.'],
    },
    keyboard: {
      items: [],
      notes: ['Nothing to focus in the welcome itself; Tab reaches the starters.'],
    },
    accessibility: {
      role: 'region (section with an accessible name)',
      name: 'The title (aria-labelledby)',
      items: [
        'The agent line starts with a visually hidden "AI assistant:" so it is identified as AI.',
        'Capabilities are a list named "What I can help with".',
        'The Bot mark and avatar are decorative (aria-hidden).',
      ],
    },
    responsive: {
      items: [
        'Centred, up to 40rem wide; text wraps, so there is no horizontal scroll at 390px.',
        'Below 30rem the padding and title size shrink.',
      ],
    },
    theme: {
      items: [
        'Mark: `--mn-color-ai-subtle`, `--mn-color-ai-border`, `--mn-color-ai-strong`.',
        'Description and agent role: `--mn-color-text-muted`.',
        'In forced colours the mark and checks use CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'In an empty thread', stack: true, render: () => <Demo /> },
        {
          title: 'With capabilities',
          stack: true,
          render: () => (
            <AiWelcome
              agent={AGENT}
              title="Ask about your projects"
              capabilities={[
                'Summarise documents and threads',
                'Draft replies and status updates',
                'Find records across your workspace',
              ]}
              footer="AI can make mistakes. Check important information."
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say what the assistant can see, so people can judge its answers.',
        'Offer two to six starters that work with the current page.',
      ],
      dont: [
        'Do not promise capabilities the assistant does not have.',
        'Do not keep the welcome visible once the conversation starts.',
      ],
    },
    related: {
      items: [
        { id: 'ai-starter-prompts', reason: 'The usual content of the starters slot.' },
        { id: 'ai-thread', reason: 'Shows the welcome as its empty state.' },
        { id: 'ai-disclaimer', reason: 'A footer note about checking AI output.' },
        { id: 'empty-state', reason: 'For empty states that are not about AI.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Region named by its heading with agent, description, capabilities, starters and footer; props and ref forwarded.',
        'Bot mark by default; avatar only for safe URLs.',
        'Heading level and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
