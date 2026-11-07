import { useState } from 'react';
import {
  AiChatPage,
  AiDisclaimer,
  AiStarterPrompts,
  AiWelcome,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
  type AiCanvasArtifact,
} from '@rookcom/monarch-react/ai';
import { Button } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant', role: 'Workspace assistant' } },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply streams from a mock transport. Use “Show artifact” in the header to open the canvas.`,
    ),
    { type: 'done' },
  ],
});

const ARTIFACT: AiCanvasArtifact = {
  id: 'summary',
  kind: 'document',
  title: 'Weekly summary',
  version: 1,
  content:
    '# Weekly summary\n\n- Three projects moved to review.\n- Two blockers were resolved.\n- Next step: confirm the launch date.',
};

const now = Date.now();
const day = 24 * 60 * 60 * 1000;

function Demo() {
  const chat = useAiConversation({ transport });
  const [conversations, setConversations] = useState([
    { id: 'c1', title: 'Weekly summary', updatedAt: new Date(now - 2 * 60 * 60 * 1000) },
    { id: 'c2', title: 'Release checklist', updatedAt: new Date(now - 2 * day), pinned: true },
    { id: 'c3', title: 'Interview questions', updatedAt: new Date(now - 20 * day) },
  ]);
  const [active, setActive] = useState('c1');
  const [showArtifact, setShowArtifact] = useState(false);
  const title = conversations.find((c) => c.id === active)?.title ?? '';
  return (
    <div
      style={{
        width: '100%',
        height: 600,
        border: '1px solid var(--mn-color-border-default)',
        borderRadius: 'var(--mn-radius-lg)',
        overflow: 'hidden',
      }}
    >
      <AiChatPage
        conversation={chat}
        conversations={conversations}
        activeId={active}
        onSelectConversation={(id) => {
          setActive(id);
          chat.reset();
        }}
        onNewConversation={() => {
          const id = `c${conversations.length + 1}`;
          setConversations((list) => [{ id, title: '', updatedAt: new Date() }, ...list]);
          setActive(id);
          chat.reset();
        }}
        historyProps={{
          onRename: (id, t) =>
            setConversations((list) => list.map((c) => (c.id === id ? { ...c, title: t } : c))),
          onPin: (id, pinned) =>
            setConversations((list) => list.map((c) => (c.id === id ? { ...c, pinned } : c))),
          onDelete: (id) => setConversations((list) => list.filter((c) => c.id !== id)),
        }}
        title={title}
        headerProps={{
          onRename: (t) =>
            setConversations((list) => list.map((c) => (c.id === active ? { ...c, title: t } : c))),
          actions: (
            <Button size="sm" onClick={() => setShowArtifact(true)}>
              Show artifact
            </Button>
          ),
        }}
        empty={
          <AiWelcome
            title="What can I help with?"
            description="Ask about your workspace, or start from a suggestion."
            starters={
              <AiStarterPrompts
                prompts={[
                  { id: 's1', title: 'Summarise my week', prompt: 'Summarise my week' },
                  { id: 's2', title: 'Draft a status update', prompt: 'Draft a status update' },
                ]}
                onSelect={(p) => chat.send(p)}
              />
            }
          />
        }
        footer={<AiDisclaimer />}
        artifact={showArtifact ? ARTIFACT : null}
        onArtifactOpenChange={(open) => {
          if (!open) setShowArtifact(false);
        }}
      />
    </div>
  );
}

export default {
  id: 'ai-chat-page',
  title: 'AiChatPage',
  summary: 'Full-page chat: history rail, conversation, prompt and an optional artifact canvas.',
  sections: {
    purpose: {
      summary:
        'Every full-page assistant needs the same frame: past conversations on the side, the conversation in the middle and room for an artifact. AiChatPage composes AiConversationList, AiConversationHeader, AiThread, AiPromptInput and AiArtifactCanvas around one `useAiConversation` result, with the phone layout handled.',
      useWhen: [
        'A dedicated assistant page or app.',
        'Conversations are stored and people return to them.',
      ],
      avoidWhen: [
        'An assistant beside another app’s content (use AiCopilotLayout or AiAssistantPanel).',
        'A single AI action inside a form.',
      ],
    },
    designPrinciples: {
      items: [
        'One conversation hook drives everything; loading a stored conversation is `reset(messages)`.',
        'The conversation stays central; history and canvas are on either side, never covering it on wide screens.',
        'Phones get one column: history in a sheet, the canvas as a full-screen sheet.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`.',
        '`historyProps`, `headerProps`, `threadProps`, `promptProps` and `artifactProps` pass through to the composed components.',
        'Messages get regenerate, feedback, branch switching and retry wired to the conversation; override with `messageProps`.',
        'The canvas opens whenever a new `artifact` (id or version) is set, unless `artifactOpen` is controlled.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. The header shows when `title` is given; the canvas when `artifact` is set.',
      ],
    },
    states: {
      items: [
        { name: 'Empty', description: 'Your `empty` content (e.g. AiWelcome) in the thread.' },
        {
          name: 'Running',
          attribute: 'data-status',
          description: 'Send becomes Stop; Escape stops.',
        },
        {
          name: 'Canvas open',
          attribute: 'data-canvas',
          description: 'A third column with the artifact.',
        },
        {
          name: 'Compact',
          attribute: 'data-compact',
          description: 'Below 768px: history in a sheet.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a conversation calls `onSelectConversation`; on phones the sheet closes.',
        '“New conversation” calls `onNewConversation` and focuses the prompt.',
        'Rename, pin and delete come from AiConversationList through `historyProps`.',
        'Closing the canvas returns focus to where it was (AiArtifactCanvas).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Shift+Enter', action: 'Sends / adds a line in the prompt.' },
        {
          keys: 'Escape',
          action: 'Stops a running reply; closes the canvas or history sheet when focus is in it.',
        },
        {
          keys: 'F2 / Delete',
          action: 'Rename / delete the focused conversation (AiConversationList).',
        },
        { keys: 'Tab', action: 'Moves from history to conversation to canvas.' },
      ],
    },
    accessibility: {
      role: 'none (layout) with a navigation, a region and the canvas region',
      name: 'History: “Conversation history”; chat: “Chat”; canvas: “Artifact: <title>”.',
      items: [
        'The current conversation has `aria-current="page"` in the history.',
        'The thread is a log with polite sentence-level announcements (AiThread).',
        'The phone history button reports `aria-expanded` and opens a labelled sheet.',
        'Focus is never moved while a reply streams.',
      ],
    },
    responsive: {
      items: [
        'From 768px: 16rem history rail, conversation, and a canvas of up to 40rem (half the width).',
        'Below 768px: one column, history in a left sheet; below 640px the canvas is a full-screen sheet.',
        'Nothing scrolls horizontally at 390px; the menu button is 44px on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Canvas background for the page, surface for the rail and conversation, subtle borders between them.',
        'Forced colours draw dividers with CanvasText.',
      ],
    },
    examples: {
      items: [{ title: 'Chat page with history and canvas', stack: true, render: () => <Demo /> }],
    },
    doDont: {
      do: [
        'Load a stored conversation with `conversation.reset(messages)` in `onSelectConversation`.',
        'Put an AiDisclaimer in `footer`.',
        'Use AiWelcome with starters as the empty state.',
      ],
      dont: [
        'Do not open the canvas for short answers; keep artifacts for documents, code and tables.',
        'Do not hide the history on wide screens.',
      ],
    },
    related: {
      items: [
        { id: 'ai-conversation-list', reason: 'The history rail.' },
        { id: 'ai-artifact-canvas', reason: 'The canvas pane.' },
        { id: 'ai-copilot-layout', reason: 'An assistant beside app content.' },
        { id: 'ai-welcome', reason: 'The empty state.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'ai-artifact-canvas',
        'ai-conversation-header',
        'ai-conversation-list',
        'ai-prompt-input',
        'ai-thread',
        'button',
        'sheet',
      ],
    },
    testing: {
      unit: [
        'History navigation with the current conversation, header, log and prompt.',
        'Selecting and starting conversations; new focuses the prompt.',
        'Empty state, then sending through the conversation.',
        'Artifact canvas opens beside the thread and closes.',
        'History in a sheet below 768px; choosing closes it (mocked matchMedia).',
        'Strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
