import { describe, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import {
  applyStreamEvent,
  isStreaming,
  mapRookIntelligenceEvent,
  messageText,
  mockTransport,
  readServerSentEvents,
  rookIntelligenceTransport,
  textDeltas,
  useAiConversation,
  type AiMessage,
  type AiStreamEvent,
  type AiTransport,
} from './index.js';

let n = 0;
const ids = () => `id-${++n}`;

function sseBody(chunks: string[]): ReadableStream<Uint8Array> {
  const encoder = new TextEncoder();
  return new ReadableStream({
    start(controller) {
      for (const c of chunks) controller.enqueue(encoder.encode(c));
      controller.close();
    },
  });
}

async function collect<T>(it: AsyncIterable<T>): Promise<T[]> {
  const out: T[] = [];
  for await (const x of it) out.push(x);
  return out;
}

describe('message helpers', () => {
  it('messageText joins text parts; isStreaming looks at text and reasoning', () => {
    const m: Pick<AiMessage, 'parts'> = {
      parts: [
        { type: 'reasoning', text: 'hmm', state: 'done' },
        { type: 'text', text: 'Hello ', state: 'done' },
        { type: 'text', text: 'there', state: 'streaming' },
      ],
    };
    expect(messageText(m)).toBe('Hello there');
    expect(isStreaming(m)).toBe(true);
  });

  it('applyStreamEvent appends deltas, closes reasoning when text starts, and finishes on done', () => {
    let m: AiMessage = { id: 'a', role: 'assistant', parts: [], createdAt: '' };
    const events: AiStreamEvent[] = [
      { type: 'reasoning-delta', text: 'Looking ' },
      { type: 'reasoning-delta', text: 'up.' },
      { type: 'text-delta', text: 'Here ' },
      { type: 'text-delta', text: 'it is.' },
      {
        type: 'part',
        part: { type: 'tool-call', id: 't1', name: 'search', input: {}, state: 'running' },
      },
      { type: 'tool-update', id: 't1', patch: { state: 'succeeded', output: { hits: 2 } } },
      { type: 'usage', usage: { inputTokens: 10, outputTokens: 4 }, model: 'test-model' },
      { type: 'done' },
    ];
    for (const e of events) m = applyStreamEvent(m, e);
    expect(m.parts).toEqual([
      { type: 'reasoning', text: 'Looking up.', state: 'done' },
      { type: 'text', text: 'Here it is.', state: 'done' },
      {
        type: 'tool-call',
        id: 't1',
        name: 'search',
        input: {},
        state: 'succeeded',
        output: { hits: 2 },
      },
    ]);
    expect(m.usage).toEqual({ inputTokens: 10, outputTokens: 4 });
    expect(m.model).toBe('test-model');
  });

  it('an error event finishes streaming parts and adds an error part', () => {
    let m: AiMessage = { id: 'a', role: 'assistant', parts: [], createdAt: '' };
    m = applyStreamEvent(m, { type: 'text-delta', text: 'Part' });
    m = applyStreamEvent(m, { type: 'error', code: 'rate-limit', message: 'Slow down' });
    expect(m.parts).toEqual([
      { type: 'text', text: 'Part', state: 'done' },
      { type: 'error', code: 'rate-limit', message: 'Slow down', retryable: true },
    ]);
  });

  it('textDeltas splits words and keeps spacing', () => {
    expect(textDeltas('Hi there  you')).toEqual([
      { type: 'text-delta', text: 'Hi ' },
      { type: 'text-delta', text: 'there  ' },
      { type: 'text-delta', text: 'you' },
    ]);
  });
});

describe('Rook Intelligence transport', () => {
  it('reads SSE frames split across chunks and stops at [DONE]', async () => {
    const body = sseBody([
      'data: {"a":',
      '1}\n\ndata: {"b":2}\n',
      '\ndata: [DONE]\n\ndata: {"c":3}\n\n',
    ]);
    expect(await collect(readServerSentEvents(body))).toEqual(['{"a":1}', '{"b":2}']);
  });

  it('maps agent, chunk, tool, done and error payloads', () => {
    expect(
      mapRookIntelligenceEvent({
        type: 'agent',
        agent: { name: 'Intelligence', codename: 'IN', role: 'Assistant', avatar: '/a.png' },
      }),
    ).toEqual({
      type: 'agent',
      agent: { name: 'Intelligence', codename: 'IN', role: 'Assistant', avatar: '/a.png' },
    });
    expect(mapRookIntelligenceEvent({ type: 'chunk', content: 'Hi' })).toEqual({
      type: 'text-delta',
      text: 'Hi',
    });
    const tool = mapRookIntelligenceEvent({ type: 'tool', tool: { type: 'insert_block', at: 3 } });
    expect(tool).toMatchObject({
      type: 'part',
      part: {
        type: 'tool-call',
        name: 'insert_block',
        state: 'succeeded',
        input: { type: 'insert_block', at: 3 },
      },
    });
    expect(mapRookIntelligenceEvent({ type: 'done', conversationId: 'c1' })).toEqual({
      type: 'done',
      conversationId: 'c1',
    });
    expect(mapRookIntelligenceEvent({ type: 'error', content: 'Unavailable' })).toMatchObject({
      type: 'error',
      message: 'Unavailable',
      retryable: true,
    });
    expect(mapRookIntelligenceEvent({ type: 'unknown' })).toBeNull();
  });

  it('posts the message, conversation id, trimmed history and body', async () => {
    const fetchMock = vi.fn(
      async () =>
        new Response(sseBody(['data: {"type":"chunk","content":"OK"}\n\n', 'data: [DONE]\n\n']), {
          status: 200,
        }),
    );
    const transport = rookIntelligenceTransport({
      url: 'https://example.test/v1/agents/a/chat',
      headers: () => ({ Authorization: 'Bearer t' }),
      historyLimit: 1,
      fetch: fetchMock as unknown as typeof fetch,
    });
    const messages: AiMessage[] = [
      {
        id: '1',
        role: 'user',
        parts: [{ type: 'text', text: 'first', state: 'done' }],
        createdAt: '',
      },
      {
        id: '2',
        role: 'assistant',
        parts: [{ type: 'text', text: 'reply', state: 'done' }],
        createdAt: '',
      },
      {
        id: '3',
        role: 'user',
        parts: [{ type: 'text', text: 'second', state: 'done' }],
        createdAt: '',
      },
    ];
    const events = await collect(
      transport.stream({
        messages,
        trigger: 'send',
        conversationId: 'c9',
        signal: new AbortController().signal,
        body: { documentId: 'd1' },
      }),
    );
    expect(events).toEqual([{ type: 'text-delta', text: 'OK' }]);
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('https://example.test/v1/agents/a/chat');
    expect(init.credentials).toBe('include');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer t');
    expect(JSON.parse(String(init.body))).toEqual({
      message: 'second',
      conversationId: 'c9',
      history: [{ role: 'assistant', content: 'reply' }],
      documentId: 'd1',
    });
  });

  it('turns an HTTP 429 into a retryable rate-limit error', async () => {
    const transport = rookIntelligenceTransport({
      url: '/chat',
      fetch: (async () => new Response('', { status: 429 })) as unknown as typeof fetch,
    });
    const events = await collect(
      transport.stream({ messages: [], trigger: 'send', signal: new AbortController().signal }),
    );
    expect(events).toEqual([
      expect.objectContaining({ type: 'error', code: 'rate-limit', retryable: true }),
    ]);
  });
});

describe('useAiConversation', () => {
  const reply = (text: string) =>
    mockTransport({ reply: [...textDeltas(text), { type: 'done' }], delayMs: 0 });

  it('sends, streams and finishes; calls onFinish with the reply', async () => {
    const onFinish = vi.fn();
    const { result } = renderHook(() =>
      useAiConversation({ transport: reply('Hello there'), generateId: ids, onFinish }),
    );
    act(() => result.current.send('Hi'));
    expect(result.current.status).toBe('submitted');
    expect(result.current.busy).toBe(true);
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages.map((m) => [m.role, messageText(m)])).toEqual([
      ['user', 'Hi'],
      ['assistant', 'Hello there'],
    ]);
    expect(onFinish).toHaveBeenCalledWith(expect.objectContaining({ role: 'assistant' }));
  });

  it('ignores empty prompts', () => {
    const { result } = renderHook(() => useAiConversation({ transport: reply('x') }));
    act(() => result.current.send('   '));
    expect(result.current.messages).toEqual([]);
  });

  it('stop keeps what arrived and reports stopped', async () => {
    const transport = mockTransport({
      reply: [...textDeltas('one two three four five'), { type: 'done' }],
      delayMs: 20,
    });
    const onFinish = vi.fn();
    const { result } = renderHook(() => useAiConversation({ transport, onFinish }));
    act(() => result.current.send('Count'));
    await waitFor(() => expect(messageText(result.current.messages[1]!)).not.toBe(''));
    act(() => result.current.stop());
    await waitFor(() => expect(result.current.status).toBe('stopped'));
    const answer = result.current.messages[1]!;
    expect(isStreaming(answer)).toBe(false);
    expect(messageText(answer).length).toBeLessThan('one two three four five'.length);
    expect(onFinish).not.toHaveBeenCalled();
  });

  it('regenerate adds a branch; branchTo switches between them', async () => {
    let i = 0;
    const transport = mockTransport({
      reply: () => [...textDeltas(`Answer ${++i}`), { type: 'done' }],
      delayMs: 0,
    });
    const { result } = renderHook(() => useAiConversation({ transport, generateId: ids }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    act(() => result.current.regenerate());
    await waitFor(() => expect(messageText(result.current.messages[1]!)).toBe('Answer 2'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages[1]!.branch).toEqual({ index: 1, count: 2 });
    act(() => result.current.branchTo(result.current.messages[1]!.id, 0));
    expect(messageText(result.current.messages[1]!)).toBe('Answer 1');
    expect(result.current.messages[1]!.branch).toEqual({ index: 0, count: 2 });
  });

  it('editAndResend forks the user message and sends only the visible history', async () => {
    const seen: string[][] = [];
    const transport: AiTransport = {
      async *stream({ messages }) {
        seen.push(messages.map(messageText));
        yield { type: 'text-delta', text: `re: ${messageText(messages[messages.length - 1]!)}` };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport, generateId: ids }));
    act(() => result.current.send('first'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    act(() => result.current.send('second'));
    await waitFor(() => expect(result.current.messages).toHaveLength(4));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    const second = result.current.messages[2]!;
    act(() => result.current.editAndResend(second.id, 'second, edited'));
    await waitFor(() =>
      expect(messageText(result.current.messages[3]!)).toBe('re: second, edited'),
    );
    expect(seen[2]).toEqual(['first', 're: first', 'second, edited']);
    expect(result.current.messages[2]!.branch).toEqual({ index: 1, count: 2 });
  });

  it('tool approval: waits, then continues the same reply through respondToTool', async () => {
    const transport = mockTransport({
      delayMs: 0,
      reply: [
        { type: 'text-delta', text: 'I can send that email.' },
        {
          type: 'part',
          part: {
            type: 'tool-call',
            id: 'mail',
            name: 'send_email',
            input: { to: 'a@b.test' },
            state: 'pending-approval',
          },
        },
        { type: 'done' },
      ],
    });
    const onToolResponse = vi.fn();
    const { result } = renderHook(() => useAiConversation({ transport, onToolResponse }));
    act(() => result.current.send('Email them'));
    await waitFor(() => expect(result.current.status).toBe('awaiting-approval'));
    act(() => result.current.approveTool('mail', { to: 'c@d.test' }));
    expect(onToolResponse).toHaveBeenCalledWith({
      toolCallId: 'mail',
      approved: true,
      input: { to: 'c@d.test' },
    });
    await waitFor(() => expect(result.current.status).toBe('idle'));
    const answer = result.current.messages[1]!;
    expect(answer.parts.find((p) => p.type === 'tool-call')).toMatchObject({
      state: 'succeeded',
      input: { to: 'c@d.test' },
    });
    expect(messageText(answer)).toBe('I can send that email.Done.');
  });

  it('denying a tool marks it denied', async () => {
    const transport: AiTransport = {
      async *stream() {
        yield {
          type: 'part',
          part: {
            type: 'tool-call',
            id: 'del',
            name: 'delete_records',
            input: { count: 12 },
            state: 'pending-approval',
          },
        };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Clean up'));
    await waitFor(() => expect(result.current.status).toBe('awaiting-approval'));
    act(() => result.current.denyTool('del', 'Not now'));
    expect(result.current.messages[1]!.parts[0]).toMatchObject({ state: 'denied' });
    expect(result.current.status).toBe('idle');
  });

  it('a transport error sets status error and keeps a retryable error part', async () => {
    const onError = vi.fn();
    const transport: AiTransport = {
      async *stream() {
        yield { type: 'text-delta', text: 'Partial' };
        throw new Error('Socket closed');
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport, onError }));
    act(() => result.current.send('Go'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toEqual({
      code: 'network',
      message: 'Socket closed',
      retryable: true,
    });
    expect(onError).toHaveBeenCalledTimes(1);
    expect(result.current.messages[1]!.parts.at(-1)).toMatchObject({
      type: 'error',
      code: 'network',
    });
  });

  it('remembers the agent and conversation id, sends the id on the next turn', async () => {
    const requests: (string | undefined)[] = [];
    const transport: AiTransport = {
      async *stream({ conversationId }) {
        requests.push(conversationId);
        yield { type: 'agent', agent: { name: 'Intelligence' } };
        yield { type: 'text-delta', text: 'ok' };
        yield { type: 'done', conversationId: 'conv-1' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('a'));
    await waitFor(() => expect(result.current.conversationId).toBe('conv-1'));
    expect(result.current.agent).toEqual({ name: 'Intelligence' });
    expect(result.current.messages[1]!.agent).toEqual({ name: 'Intelligence' });
    await waitFor(() => expect(result.current.status).toBe('idle'));
    act(() => result.current.send('b'));
    await waitFor(() => expect(requests).toEqual([undefined, 'conv-1']));
  });

  it('feedback, reset and initial messages', () => {
    const initial: AiMessage[] = [
      { id: 'u', role: 'user', parts: [{ type: 'text', text: 'q', state: 'done' }], createdAt: '' },
      {
        id: 'a',
        role: 'assistant',
        parts: [{ type: 'text', text: 'r', state: 'done' }],
        createdAt: '',
      },
    ];
    const { result } = renderHook(() =>
      useAiConversation({ transport: reply('x'), initialMessages: initial }),
    );
    expect(result.current.messages).toHaveLength(2);
    act(() => result.current.setFeedback('a', 'up'));
    expect(result.current.messages[1]!.feedback).toBe('up');
    act(() => result.current.setFeedback('a', undefined));
    expect(result.current.messages[1]!.feedback).toBeUndefined();
    act(() => result.current.reset());
    expect(result.current.messages).toEqual([]);
  });

  it('renders on the server without starting anything', () => {
    function Probe() {
      const { status, messages } = useAiConversation({ transport: reply('x') });
      return (
        <p>
          {status} {messages.length}
        </p>
      );
    }
    expect(renderToString(<Probe />)).toContain('idle');
  });
});
