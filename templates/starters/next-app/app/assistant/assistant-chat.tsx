'use client';

import { Text } from '@rookcom/monarch-react';
import {
  AiPromptInput,
  AiThread,
  messageText,
  mockTransport,
  textDeltas,
  useAiConversation,
} from '@rookcom/monarch-react/ai';

// Streams a fixed reply and calls no model. Replace it with a transport that
// points at your own service, for example createSseTransport. A model key never
// belongs in the browser.
const transport = mockTransport({
  delayMs: 40,
  reply: ({ messages }) => [
    { type: 'agent', agent: { name: 'Assistant' } },
    ...textDeltas(
      `You asked: “${messageText(messages[messages.length - 1] ?? { parts: [] })}”. This reply is streamed word by word from a mock transport.`,
    ),
    { type: 'done' },
  ],
});

export function AssistantChat() {
  const chat = useAiConversation({ transport });
  return (
    <div className="grid gap-4">
      <div className="h-96 rounded-lg border border-mn-line bg-mn-surface">
        <AiThread
          messages={chat.messages}
          status={chat.status}
          error={chat.error}
          onStop={chat.stop}
          empty={<Text tone="muted">Ask a question to start.</Text>}
          messageProps={{
            agent: chat.agent,
            onRegenerate: (message) => chat.regenerate(message.id),
          }}
        />
      </div>
      <AiPromptInput
        label="Message the assistant"
        placeholder="Ask something"
        status={chat.status}
        onStop={chat.stop}
        onSubmit={(text) => chat.submit(text)}
      />
    </div>
  );
}
