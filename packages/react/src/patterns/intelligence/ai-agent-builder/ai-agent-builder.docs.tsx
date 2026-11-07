import { useState } from 'react';
import {
  AiAgentBuilder,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiAgentDefinition,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages, body }) => {
    const agent = body?.agent as AiAgentDefinition | undefined;
    const name = agent?.name || 'The agent';
    const tools = agent?.tools.length ? agent.tools.join(', ') : 'no tools';
    return [
      ...textDeltas(
        `${name} received “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. It is running your unsaved settings with ${tools} and ${agent?.knowledge.length ?? 0} knowledge sources. This reply comes from a mock transport.`,
      ),
      { type: 'done' },
    ];
  },
});

const TOOLS = [
  { id: 'search', name: 'Search documents', description: 'Reads documents the agent can see.' },
  { id: 'tasks', name: 'Create tasks', description: 'Asks before creating anything.' },
  { id: 'email', name: 'Draft email', description: 'Drafts only; never sends.' },
];

const SOURCES = [
  {
    id: 'handbook',
    label: 'Team handbook',
    kind: 'space' as const,
    children: [
      { id: 'onboarding', label: 'Onboarding guide', kind: 'doc' as const },
      { id: 'policies', label: 'Policies', kind: 'doc' as const },
    ],
  },
  { id: 'tickets', label: 'Support tickets', kind: 'app' as const },
];

function Demo() {
  const chat = useAiConversation({ transport });
  const [saved, setSaved] = useState<AiAgentDefinition>({
    name: 'Onboarding guide',
    role: 'Helps new teammates find answers',
    instructions: 'Answer from the team handbook. Keep answers short and link the source.',
    tools: ['search'],
    knowledge: ['handbook'],
  });
  const [draft, setDraft] = useState(saved);
  return (
    <div style={{ width: '100%' }}>
      <AiAgentBuilder
        value={draft}
        onChange={setDraft}
        dirty={JSON.stringify(draft) !== JSON.stringify(saved)}
        tools={TOOLS}
        knowledgeSources={SOURCES}
        conversation={chat}
        onSave={async (d) => {
          await new Promise((r) => setTimeout(r, 500));
          setSaved(d);
        }}
      />
    </div>
  );
}

export default {
  id: 'ai-agent-builder',
  title: 'AiAgentBuilder',
  summary: 'Build an AI agent (persona, instructions, tools, knowledge) and test it side by side.',
  sections: {
    purpose: {
      summary:
        'Configuring an agent is a loop of edit and try. AiAgentBuilder puts the agent’s persona, instructions, allowed tools and knowledge sources in one form, and a test chat beside it that runs the current, unsaved definition, so people can see the effect of a change before saving it.',
      useWhen: [
        'Workspace admins or builders create and tune custom agents.',
        'You need a test-before-save loop for agent settings.',
      ],
      avoidWhen: [
        'Picking an existing agent (use AiAgentPicker).',
        'Workspace-wide AI switches and data settings (use AiSettingsSection).',
      ],
    },
    designPrinciples: {
      items: [
        'Try before you save: the test chat always uses the draft.',
        'Least privilege: tools and knowledge start unchecked; each tool says what it can do.',
        'An agent never looks like a person: the preview uses AiAgentAvatar.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`.',
        'Pass `conversation` (a `useAiConversation` result) to show the test pane; each test message carries the draft as `body.agent` (`definitionField`).',
        '`avatarField` is a slot for your upload control; set `avatar` on the definition from it.',
        'Builds on AiKnowledgeScope, AiThread, AiPromptInput and AiAgentAvatar.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. Leave out `conversation`, `knowledgeSources` or `onSave` to hide the test pane, the knowledge section or the Save footer.',
      ],
    },
    states: {
      items: [
        {
          name: 'Testing',
          attribute: 'data-testing',
          description: 'Two columns with a test chat.',
        },
        { name: 'Unsaved', description: '`dirty` shows “You have unsaved changes.”.' },
        {
          name: 'Saving / saved / error',
          attribute: 'data-status',
          description: 'Footer status and Save spinner.',
        },
        {
          name: 'Name missing',
          attribute: 'aria-invalid',
          description: 'Save focuses the name with an error.',
        },
      ],
    },
    interaction: {
      items: [
        'Every edit calls `onChange` with the full definition.',
        'Sending in the test pane uses the current draft; Restart test clears the test conversation.',
        'Save validates the name, then calls `onSave` and shows the result.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the form, then the test pane.' },
        { keys: 'Space', action: 'Toggles the focused tool or knowledge source.' },
        { keys: 'Enter / Shift+Enter', action: 'Sends / adds a line in the test input.' },
        { keys: 'Escape', action: 'Stops a running test reply.' },
      ],
    },
    accessibility: {
      role: 'region named “Agent builder”',
      name: '`messages.label`.',
      items: [
        'Each part is a section named by its heading; tools are a fieldset with a legend.',
        'The name is required and its error is linked to the field.',
        'The test pane is a region named “Test” with the AiThread log and announcements.',
        'The footer status is a polite live region.',
      ],
    },
    responsive: {
      items: [
        'From 1024px: form and a sticky test pane side by side.',
        'Below: the test pane follows the form; nothing scrolls horizontally at 390px.',
        'Buttons are 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'The test pane is outlined with `--mn-color-ai-border`; the rest uses surface and border tokens.',
        'Forced colours draw borders with CanvasText.',
      ],
    },
    examples: {
      items: [{ title: 'Builder with a test chat', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Run test messages with the draft on the server, never saving it implicitly.',
        'Describe every tool, especially ones that change data.',
        'Show a diff or confirmation before publishing an agent others use.',
      ],
      dont: ['Do not pre-select tools or knowledge.', 'Do not hide the instructions limit.'],
    },
    related: {
      items: [
        { id: 'ai-knowledge-scope', reason: 'The knowledge tree.' },
        { id: 'ai-agent-avatar', reason: 'The persona preview.' },
        { id: 'ai-agent-card', reason: 'Summarises a saved agent.' },
        { id: 'ai-thread', reason: 'The test conversation.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'ai-agent-avatar',
        'ai-knowledge-scope',
        'ai-prompt-input',
        'ai-thread',
        'button',
        'checkbox',
        'form-field',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Persona, instructions, tools, knowledge and test pane with accessible names.',
        'Edits report the full definition; the character counter updates.',
        'The test chat sends the draft as `body.agent`; Restart clears it.',
        'Save requires a name (focus and error), then saves and reports errors.',
        'Optional sections hide.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
