import { describe, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import {
  agUiMapper,
  aiStreamEventMapper,
  applyStreamEvent,
  applyStreamEvents,
  bodyFromChunks,
  buildAgUiRunInput,
  buildChatCompletionsBody,
  buildContentBlocksBody,
  chunkBytes,
  classifyHttpStatus,
  collectStream,
  createStreamFixture,
  everySplit,
  finishParts,
  isRetryableErrorCode,
  normaliseSseChunk,
  parseRetryAfter,
  replayTransport,
  responsesMapper,
  runTurn,
  simulateConversation,
  sseFromEvents,
  sseText,
  toAgUiMessages,
  toChatCompletionsMessages,
  toContentBlockMessages,
  toResponsesInput,
  toUiMessages,
  uiMessageStreamMapper,
  unresolvedToolCalls,
  chatCompletionsMapper,
  contentBlockMessagesMapper,
  createSseTransport,
  isStreaming,
  manualNetworkSource,
  mapRookIntelligenceEvent,
  messageMemories,
  parseAiMemoryEvent,
  browserNetworkSource,
  messageText,
  mockTransport,
  readServerSentEvents,
  readSseFrames,
  rookIntelligenceTransport,
  textDeltas,
  toChatMessages,
  useAiConversation,
  type AiMemoryEvent,
  type AiMessage,
  type AiStreamEvent,
  type AiStreamMapper,
  type AiTransport,
  type AiTransportRequest,
  type SseFrame,
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
    // A service error without a code is not retried blindly.
    expect(mapRookIntelligenceEvent({ type: 'error', content: 'Unavailable' })).toMatchObject({
      type: 'error',
      message: 'Unavailable',
      retryable: false,
    });
    expect(
      mapRookIntelligenceEvent({ type: 'error', code: 'overloaded', content: 'Busy' }),
    ).toMatchObject({ code: 'overloaded', retryable: true });
    expect(mapRookIntelligenceEvent({ type: 'error' }, { unavailable: 'Pas dispo' })).toMatchObject(
      { message: 'Pas dispo' },
    );
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
      expect.objectContaining({ type: 'error', code: 'rate-limited', retryable: true }),
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
      // Slow enough that Stop always lands mid-reply, even on a loaded CI runner.
      delayMs: 250,
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
    // The raw exception text is never shown; the hook's own message is.
    expect(result.current.error).toEqual({
      code: 'network',
      message: 'The connection was lost.',
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

describe('SSE transport and stream formats', () => {
  const encoder = new TextEncoder();

  function sseBody(chunks: string[], fail?: Error): ReadableStream<Uint8Array> {
    let i = 0;
    // One chunk per pull, so a failure arrives after the chunks before it were read.
    return new ReadableStream({
      pull(controller) {
        const chunk = chunks[i++];
        if (chunk !== undefined) controller.enqueue(encoder.encode(chunk));
        else if (fail) controller.error(fail);
        else controller.close();
      },
    });
  }

  async function collect<T>(it: AsyncIterable<T>): Promise<T[]> {
    const out: T[] = [];
    for await (const x of it) out.push(x);
    return out;
  }

  const USER: AiMessage = {
    id: 'u1',
    role: 'user',
    createdAt: '2026-03-02T10:00:00.000Z',
    parts: [{ type: 'text', text: 'Hello', state: 'done' }],
  };

  const request = (extra: Partial<AiTransportRequest> = {}): AiTransportRequest => ({
    messages: [USER],
    trigger: 'send',
    signal: new AbortController().signal,
    ...extra,
  });

  /** Each frame's data is one JSON stream event; `[DONE]` ends the reply. */
  const jsonMapper = (): AiStreamMapper => ({
    map: (frame: SseFrame) =>
      frame.data === '[DONE]' ? [{ type: 'done' }] : [JSON.parse(frame.data) as AiStreamEvent],
  });

  const text = (t: string) => `data: ${JSON.stringify({ type: 'text-delta', text: t })}\n\n`;
  const NO_WAIT = { attempts: 2, baseDelayMs: 0, maxDelayMs: 0 };

  describe('readSseFrames', () => {
    it('reads event, id and multi-line data; skips comments; accepts CRLF and a last frame without a blank line', async () => {
      const body = sseBody([
        ': keep-alive\n\n',
        'event: delta\r\nid: 7\r\ndata: one\r\ndata: two\r\n\r\n',
        'data: split ',
        'across chunks\n\n',
        'data: tail',
      ]);
      expect(await collect(readSseFrames(body))).toEqual([
        { event: 'delta', id: '7', data: 'one\ntwo' },
        { event: '', data: 'split across chunks' },
        { event: '', data: 'tail' },
      ]);
    });

    it('gives up on a body that goes quiet for longer than idleTimeoutMs', async () => {
      const body = new ReadableStream<Uint8Array>({
        start(controller) {
          controller.enqueue(encoder.encode('data: first\n\n'));
          // Never closes and never sends again.
        },
      });
      const seen: string[] = [];
      await expect(
        (async () => {
          for await (const frame of readSseFrames(body, { idleTimeoutMs: 20 }))
            seen.push(frame.data);
        })(),
      ).rejects.toThrow('The assistant stopped responding.');
      expect(seen).toEqual(['first']);
    });
  });

  describe('createSseTransport', () => {
    it('posts JSON with the default body and stops at the terminal event', async () => {
      const fetchMock = vi.fn(
        async () => new Response(sseBody([text('Hi'), 'data: [DONE]\n\n', text('late')])),
      );
      const transport = createSseTransport({
        url: '/api/chat',
        headers: { Authorization: 'Bearer t' },
        createMapper: jsonMapper,
        fetch: fetchMock as unknown as typeof fetch,
      });
      const events = await collect(
        transport.stream(request({ conversationId: 'c1', body: { documentId: 'd1' } })),
      );
      expect(events).toEqual([{ type: 'text-delta', text: 'Hi' }, { type: 'done' }]);
      const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
      expect(url).toBe('/api/chat');
      expect(init.credentials).toBe('same-origin');
      expect(init.headers).toMatchObject({
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: 'Bearer t',
      });
      expect(JSON.parse(init.body as string)).toEqual({
        messages: [{ role: 'user', content: 'Hello' }],
        conversationId: 'c1',
        documentId: 'd1',
      });
      expect(transport.respondToTool).toBeUndefined();
    });

    it('toChatMessages keeps user and assistant text and drops empty turns', () => {
      const messages: AiMessage[] = [
        USER,
        { ...USER, id: 's', role: 'system' },
        {
          ...USER,
          id: 'a',
          role: 'assistant',
          parts: [{ type: 'reasoning', text: 'x', state: 'done' }],
        },
        {
          ...USER,
          id: 'b',
          role: 'assistant',
          parts: [{ type: 'text', text: 'Hi', state: 'done' }],
        },
      ];
      expect(toChatMessages(messages)).toEqual([
        { role: 'user', content: 'Hello' },
        { role: 'assistant', content: 'Hi' },
      ]);
    });

    it('maps HTTP failures: 429 is a retryable rate limit, 401 is not retryable', async () => {
      const make = (status: number) =>
        createSseTransport({
          url: '/api/chat',
          createMapper: jsonMapper,
          fetch: (async () => new Response(null, { status })) as unknown as typeof fetch,
        });
      expect(await collect(make(429).stream(request()))).toEqual([
        expect.objectContaining({ type: 'error', code: 'rate-limited', retryable: true }),
      ]);
      expect(await collect(make(401).stream(request()))).toEqual([
        expect.objectContaining({ type: 'error', code: 'auth', retryable: false }),
      ]);
    });

    it('retry: tries again before the first event, for a network failure and a 503', async () => {
      const fetchMock = vi
        .fn()
        .mockRejectedValueOnce(new TypeError('Failed to fetch'))
        .mockResolvedValueOnce(new Response(null, { status: 503 }))
        .mockResolvedValueOnce(new Response(sseBody([text('Hi'), 'data: [DONE]\n\n'])));
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: jsonMapper,
        retry: NO_WAIT,
        fetch: fetchMock as unknown as typeof fetch,
      });
      expect(await collect(transport.stream(request()))).toEqual([
        { type: 'text-delta', text: 'Hi' },
        { type: 'done' },
      ]);
      expect(fetchMock).toHaveBeenCalledTimes(3);
    });

    it('without retry, a failed request is not repeated', async () => {
      const fetchMock = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: jsonMapper,
        fetch: fetchMock as unknown as typeof fetch,
      });
      await expect(collect(transport.stream(request()))).rejects.toThrow('Failed to fetch');
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('resume: reconnects with Last-Event-ID after a drop and continues the same reply', async () => {
      const fetchMock = vi
        .fn()
        .mockResolvedValueOnce(
          new Response(sseBody([`id: 1\n${text('Hel')}`], new Error('socket closed'))),
        )
        .mockResolvedValueOnce(new Response(sseBody([`id: 2\n${text('lo')}`, 'data: [DONE]\n\n'])));
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: jsonMapper,
        resume: { attempts: 1 },
        fetch: fetchMock as unknown as typeof fetch,
      });
      const events = await collect(transport.stream(request()));
      expect(events).toEqual([
        { type: 'text-delta', text: 'Hel' },
        { type: 'text-delta', text: 'lo' },
        { type: 'done' },
      ]);
      const second = fetchMock.mock.calls[1] as unknown as [string, RequestInit];
      expect(second[1].headers).toMatchObject({ 'Last-Event-ID': '1' });
    }, 10_000);

    it('a drop without event ids is not resumed: it ends with a retryable network error', async () => {
      const fetchMock = vi
        .fn()
        .mockResolvedValue(new Response(sseBody([text('Hel')], new Error('socket closed'))));
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: jsonMapper,
        resume: true,
        fetch: fetchMock as unknown as typeof fetch,
      });
      expect(await collect(transport.stream(request()))).toEqual([
        { type: 'text-delta', text: 'Hel' },
        { type: 'error', code: 'network', message: 'The connection was lost.', retryable: true },
      ]);
      expect(fetchMock).toHaveBeenCalledTimes(1);
    });

    it('stops quietly when aborted, and flushes the mapper when the body just ends', async () => {
      const controller = new AbortController();
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: () => ({ map: () => [], flush: () => [{ type: 'done' }] }),
        fetch: (async () => new Response(sseBody([text('ignored')]))) as unknown as typeof fetch,
      });
      expect(await collect(transport.stream(request()))).toEqual([{ type: 'done' }]);
      controller.abort();
      expect(await collect(transport.stream(request({ signal: controller.signal })))).toEqual([]);
    });

    it('buildToolBody adds respondToTool on the same endpoint', async () => {
      const fetchMock = vi.fn(async () => new Response(sseBody(['data: [DONE]\n\n'])));
      const transport = createSseTransport({
        url: '/api/chat',
        createMapper: jsonMapper,
        buildToolBody: ({ toolCallId, approved }) => ({ toolCallId, approved }),
        fetch: fetchMock as unknown as typeof fetch,
      });
      const events = await collect(
        transport.respondToTool!({
          toolCallId: 't1',
          approved: true,
          messages: [USER],
          signal: new AbortController().signal,
        }),
      );
      expect(events).toEqual([{ type: 'done' }]);
      const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
      expect(JSON.parse(init.body as string)).toEqual({ toolCallId: 't1', approved: true });
    });
  });

  const frames = (payloads: unknown[]): SseFrame[] =>
    payloads.map((p) => ({ event: '', data: typeof p === 'string' ? p : JSON.stringify(p) }));

  const run = (mapper: AiStreamMapper, payloads: unknown[]) => [
    ...frames(payloads).flatMap((f) => [...mapper.map(f)]),
    ...(mapper.flush?.() ?? []),
  ];

  describe('chatCompletionsMapper', () => {
    it('maps text, reasoning, usage and [DONE]', () => {
      const events = run(chatCompletionsMapper(), [
        { model: 'm-1', choices: [{ delta: { role: 'assistant', content: '' } }] },
        { choices: [{ delta: { reasoning_content: 'Think. ' } }] },
        { choices: [{ delta: { content: 'Hel' } }] },
        { choices: [{ delta: { content: 'lo' }, finish_reason: 'stop' }] },
        { choices: [], usage: { prompt_tokens: 12, completion_tokens: 3 } },
        '[DONE]',
      ]);
      expect(events).toEqual([
        { type: 'reasoning-delta', text: 'Think. ' },
        { type: 'reasoning-end' },
        { type: 'text-delta', text: 'Hel' },
        { type: 'text-delta', text: 'lo' },
        { type: 'finish', reason: 'stop' },
        { type: 'usage', usage: { inputTokens: 12, outputTokens: 3 }, model: 'm-1' },
        { type: 'done' },
      ]);
    });

    it('assembles a tool call from argument pieces and fills its input at the end', () => {
      const events = run(chatCompletionsMapper(), [
        {
          choices: [
            {
              delta: {
                tool_calls: [
                  { index: 0, id: 'call_1', function: { name: 'search', arguments: '{"q":' } },
                ],
              },
            },
          ],
        },
        {
          choices: [{ delta: { tool_calls: [{ index: 0, function: { arguments: '"rook"}' } }] } }],
        },
        { choices: [{ delta: {}, finish_reason: 'tool_calls' }] },
        '[DONE]',
      ]);
      expect(events[0]).toMatchObject({
        type: 'part',
        part: { type: 'tool-call', id: 'call_1', name: 'search', state: 'running', input: {} },
      });
      expect(events.slice(1)).toEqual([
        { type: 'tool-input-delta', id: 'call_1', delta: '{"q":' },
        { type: 'tool-input-delta', id: 'call_1', delta: '"rook"}' },
        {
          type: 'tool-update',
          id: 'call_1',
          patch: { input: { q: 'rook' }, state: 'awaiting-result' },
        },
        { type: 'finish', reason: 'tool-use' },
        { type: 'done' },
      ]);
    });

    it('keeps broken arguments as text, reports an error payload, ignores junk and ends once', () => {
      const mapper = chatCompletionsMapper();
      const events = run(mapper, [
        'not json',
        {
          choices: [
            { delta: { tool_calls: [{ index: 0, function: { name: 'x', arguments: '{"a":' } }] } },
          ],
        },
      ]);
      expect(events.at(-2)).toEqual({
        type: 'tool-update',
        id: 'call-0',
        patch: { input: '{"a":' },
      });
      expect(events.at(-1)).toEqual({ type: 'done' });
      expect(mapper.flush?.()).toEqual([]);
      expect(
        run(chatCompletionsMapper(), [{ error: { message: 'Quota exceeded', code: 'quota' } }])[0],
      ).toEqual({
        type: 'error',
        code: 'quota',
        message: 'Quota exceeded',
        retryable: false,
      });
    });
  });

  describe('contentBlockMessagesMapper', () => {
    it('maps thinking, text, a tool-use block, usage and the stop', () => {
      const events = run(contentBlockMessagesMapper(), [
        { type: 'message_start', message: { model: 'm-2', usage: { input_tokens: 20 } } },
        { type: 'content_block_start', index: 0, content_block: { type: 'thinking' } },
        {
          type: 'content_block_delta',
          index: 0,
          delta: { type: 'thinking_delta', thinking: 'Plan. ' },
        },
        {
          type: 'content_block_delta',
          index: 0,
          delta: { type: 'signature_delta', signature: 's' },
        },
        { type: 'content_block_stop', index: 0 },
        { type: 'content_block_start', index: 1, content_block: { type: 'text', text: '' } },
        { type: 'content_block_delta', index: 1, delta: { type: 'text_delta', text: 'Hello' } },
        { type: 'content_block_stop', index: 1 },
        {
          type: 'content_block_start',
          index: 2,
          content_block: { type: 'tool_use', id: 'tu_1', name: 'search' },
        },
        {
          type: 'content_block_delta',
          index: 2,
          delta: { type: 'input_json_delta', partial_json: '{"q":"ro' },
        },
        {
          type: 'content_block_delta',
          index: 2,
          delta: { type: 'input_json_delta', partial_json: 'ok"}' },
        },
        { type: 'content_block_stop', index: 2 },
        { type: 'ping' },
        { type: 'message_delta', delta: { stop_reason: 'tool_use' }, usage: { output_tokens: 9 } },
        { type: 'message_stop' },
      ]);
      expect(events[3]).toMatchObject({
        type: 'part',
        part: { type: 'tool-call', id: 'tu_1', name: 'search', state: 'running' },
      });
      expect([...events.slice(0, 3), ...events.slice(4)]).toEqual([
        { type: 'reasoning-delta', text: 'Plan. ' },
        { type: 'reasoning-end' },
        { type: 'text-delta', text: 'Hello' },
        { type: 'tool-input-delta', id: 'tu_1', delta: '{"q":"ro' },
        { type: 'tool-input-delta', id: 'tu_1', delta: 'ok"}' },
        { type: 'tool-update', id: 'tu_1', patch: { input: { q: 'rook' } } },
        { type: 'tool-update', id: 'tu_1', patch: { state: 'awaiting-result' } },
        { type: 'finish', reason: 'tool-use' },
        { type: 'usage', usage: { inputTokens: 20, outputTokens: 9 }, model: 'm-2' },
        { type: 'done' },
      ]);
    });

    it('marks overload errors retryable and others not; a body that just ends is done', () => {
      const map = (payload: unknown) => run(contentBlockMessagesMapper(), [payload])[0];
      expect(map({ type: 'error', error: { type: 'overloaded_error', message: 'Busy' } })).toEqual({
        type: 'error',
        code: 'overloaded_error',
        message: 'Busy',
        retryable: true,
      });
      expect(
        map({ type: 'error', error: { type: 'invalid_request_error', message: 'Bad' } }),
      ).toMatchObject({
        retryable: false,
      });
      expect(run(contentBlockMessagesMapper(), [])).toEqual([{ type: 'done' }]);
    });
  });
});

// ------------------------------------------------------------------ hardening

/** A transport whose turns are driven by the test: each `stream` call gets its own gate. */
function gatedTransport() {
  const turns: {
    request: AiTransportRequest;
    push: (e: AiStreamEvent) => void;
    end: () => void;
  }[] = [];
  const transport: AiTransport = {
    stream(request) {
      const queue: AiStreamEvent[] = [];
      let wake: (() => void) | null = null;
      let ended = false;
      const turn = {
        request,
        push: (e: AiStreamEvent) => {
          queue.push(e);
          wake?.();
        },
        end: () => {
          ended = true;
          wake?.();
        },
      };
      turns.push(turn);
      request.signal.addEventListener('abort', () => wake?.());
      return (async function* gen() {
        while (true) {
          if (request.signal.aborted) return;
          const next = queue.shift();
          if (next) {
            yield next;
            continue;
          }
          if (ended) return;
          await new Promise<void>((r) => (wake = r));
          wake = null;
        }
      })();
    },
  };
  return { transport, turns };
}

describe('useAiConversation: superseded runs never touch the new run', () => {
  it('send during a stream: the old run is closed, the new reply keeps streaming and finishes', async () => {
    const { transport, turns } = gatedTransport();
    const { result } = renderHook(() => useAiConversation({ transport, generateId: ids }));
    act(() => result.current.send('first'));
    act(() => turns[0]!.push({ type: 'text-delta', text: 'Old ' }));
    await waitFor(() => expect(messageText(result.current.messages[1]!)).toBe('Old '));
    act(() => result.current.send('second'));
    expect(turns[0]!.request.signal.aborted).toBe(true);
    // The old reply is kept, closed.
    expect(isStreaming(result.current.messages[1]!)).toBe(false);
    act(() => turns[1]!.push({ type: 'text-delta', text: 'New' }));
    await waitFor(() => expect(result.current.status).toBe('streaming'));
    // The old generator wakes up and returns: nothing may change.
    act(() => turns[0]!.end());
    await new Promise((r) => setTimeout(r, 20));
    expect(result.current.status).toBe('streaming');
    expect(result.current.busy).toBe(true);
    act(() => {
      turns[1]!.push({ type: 'done' });
      turns[1]!.end();
    });
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages.map(messageText)).toEqual(['first', 'Old ', 'second', 'New']);
  });

  it('regenerate during a stream does not leave the new branch stopped', async () => {
    const { transport, turns } = gatedTransport();
    const { result } = renderHook(() => useAiConversation({ transport, generateId: ids }));
    act(() => result.current.send('Q'));
    act(() => turns[0]!.push({ type: 'text-delta', text: 'A1' }));
    await waitFor(() => expect(result.current.status).toBe('streaming'));
    act(() => result.current.regenerate());
    act(() => turns[0]!.end());
    act(() => turns[1]!.push({ type: 'text-delta', text: 'A2' }));
    await waitFor(() => expect(messageText(result.current.messages[1]!)).toBe('A2'));
    expect(result.current.status).toBe('streaming');
    act(() => turns[1]!.push({ type: 'done' }));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages[1]!.branch).toEqual({ index: 1, count: 2 });
  });

  it('reset during a stream: the old run cannot write into the new conversation', async () => {
    const { transport, turns } = gatedTransport();
    const { result } = renderHook(() => useAiConversation({ transport, generateId: ids }));
    act(() => result.current.send('Q'));
    act(() => turns[0]!.push({ type: 'text-delta', text: 'partial' }));
    await waitFor(() => expect(result.current.status).toBe('streaming'));
    act(() => result.current.reset());
    act(() => {
      turns[0]!.push({ type: 'text-delta', text: ' more' });
      turns[0]!.end();
    });
    await new Promise((r) => setTimeout(r, 20));
    expect(result.current.messages).toEqual([]);
    expect(result.current.status).toBe('idle');
  });

  it('stop finishes at once, even when the transport ignores the abort', async () => {
    const { transport, turns } = gatedTransport();
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Q'));
    act(() => turns[0]!.push({ type: 'text-delta', text: 'so far' }));
    await waitFor(() => expect(result.current.status).toBe('streaming'));
    act(() => result.current.stop());
    expect(result.current.status).toBe('stopped');
    expect(messageText(result.current.messages[1]!)).toBe('so far');
  });

  it('approving during a later stream continues the right reply', async () => {
    const tool: AiStreamEvent = {
      type: 'part',
      part: { type: 'tool-call', id: 't1', name: 'send', input: {}, state: 'pending-approval' },
    };
    const transport = replayTransport([
      {
        events: [tool, { type: 'done' }],
        toolEvents: [
          { type: 'tool-update', id: 't1', patch: { state: 'succeeded' } },
          { type: 'text-delta', text: 'Sent.' },
          { type: 'done' },
        ],
      },
    ]);
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Send it'));
    await waitFor(() => expect(result.current.status).toBe('awaiting-approval'));
    expect(result.current.canSend).toBe(false);
    // Sending is refused while an approval is pending.
    await act(async () => {
      await expect(result.current.submit('another')).rejects.toThrow();
    });
    expect(result.current.messages).toHaveLength(2);
    act(() => result.current.approveTool('t1'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(messageText(result.current.messages[1]!)).toBe('Sent.');
  });

  it('keeps the identity of finished messages while a reply streams', async () => {
    const { transport, turns } = gatedTransport();
    const initial: AiMessage[] = [
      {
        id: 'u0',
        role: 'user',
        parts: [{ type: 'text', text: 'hi', state: 'done' }],
        createdAt: '',
      },
      {
        id: 'a0',
        role: 'assistant',
        parts: [{ type: 'text', text: 'yo', state: 'done' }],
        createdAt: '',
      },
    ];
    const { result } = renderHook(() =>
      useAiConversation({ transport, initialMessages: initial, generateId: ids }),
    );
    act(() => result.current.send('next'));
    act(() => turns[0]!.push({ type: 'text-delta', text: 'a' }));
    await waitFor(() => expect(messageText(result.current.messages[3]!)).toBe('a'));
    const first = result.current.messages[0];
    const user = result.current.messages[2];
    act(() => turns[0]!.push({ type: 'text-delta', text: 'b' }));
    await waitFor(() => expect(messageText(result.current.messages[3]!)).toBe('ab'));
    expect(result.current.messages[0]).toBe(first);
    expect(result.current.messages[2]).toBe(user);
  });
});

describe('useAiConversation: client tools, retry, attachments, resume', () => {
  const awaitingTurn: AiStreamEvent[] = [
    {
      type: 'part',
      part: { type: 'tool-call', id: 'c1', name: 'lookup', input: {}, state: 'running' },
    },
    {
      type: 'part',
      part: { type: 'tool-call', id: 'c2', name: 'lookup', input: {}, state: 'running' },
    },
    { type: 'finish', reason: 'tool-use' },
    { type: 'done' },
  ];

  it('tool calls wait for results; submitToolResult continues once every parallel call has one', async () => {
    const transport = replayTransport([
      awaitingTurn,
      [...textDeltas('Both found.'), { type: 'done' }],
    ]);
    const onFinish = vi.fn();
    const { result } = renderHook(() => useAiConversation({ transport, onFinish }));
    act(() => result.current.send('Look up two'));
    await waitFor(() => expect(result.current.status).toBe('awaiting-tool-result'));
    expect(result.current.messages[1]!.parts.map((p) => p.type === 'tool-call' && p.state)).toEqual(
      ['awaiting-result', 'awaiting-result'],
    );
    expect(onFinish).not.toHaveBeenCalled();
    act(() => result.current.submitToolResult('c1', { output: { hit: 1 } }));
    expect(transport.requests).toHaveLength(1);
    act(() => result.current.submitToolResult('c2', { error: 'Not found' }));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(transport.requests[1]!.trigger).toBe('tool-result');
    const reply = result.current.messages[1]!;
    expect(reply.parts.slice(0, 2)).toMatchObject([
      { state: 'succeeded', output: { hit: 1 } },
      { state: 'failed', error: 'Not found' },
    ]);
    expect(messageText(reply)).toBe('Both found.');
    expect(onFinish).toHaveBeenCalledTimes(1);
  });

  it('onToolCall runs client tools and continues the reply', async () => {
    const transport = replayTransport([
      awaitingTurn,
      [{ type: 'text-delta', text: 'ok' }, { type: 'done' }],
    ]);
    const onToolCall = vi.fn(async (call: { id: string }) => ({ id: call.id }));
    const { result } = renderHook(() => useAiConversation({ transport, onToolCall }));
    act(() => result.current.send('Go'));
    await waitFor(() => expect(messageText(result.current.messages[1]!)).toBe('ok'));
    expect(onToolCall).toHaveBeenCalledTimes(2);
    expect(result.current.messages[1]!.parts[1]).toMatchObject({
      state: 'succeeded',
      output: { id: 'c2' },
    });
  });

  it('a new send cancels calls nobody answered, so the history stays consistent', async () => {
    const transport = replayTransport([awaitingTurn, [{ type: 'done' }]]);
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Go'));
    await waitFor(() => expect(result.current.status).toBe('awaiting-tool-result'));
    act(() => result.current.send('Never mind'));
    expect(result.current.messages[1]!.parts[0]).toMatchObject({ state: 'cancelled' });
  });

  it('toolPolicy can approve a call without asking', async () => {
    const transport = replayTransport([
      {
        events: [
          {
            type: 'part',
            part: {
              type: 'tool-call',
              id: 't',
              name: 'read',
              input: {},
              state: 'pending-approval',
            },
          },
          { type: 'done' },
        ],
        toolEvents: [
          { type: 'tool-update', id: 't', patch: { state: 'succeeded' } },
          { type: 'done' },
        ],
      },
    ]);
    const { result } = renderHook(() =>
      useAiConversation({
        transport,
        toolPolicy: (call) => (call.name === 'read' ? 'allow' : 'ask'),
      }),
    );
    act(() => result.current.send('Read'));
    await waitFor(() =>
      expect(result.current.messages[1]!.parts[0]).toMatchObject({ state: 'succeeded' }),
    );
    expect(transport.toolResponses[0]).toMatchObject({ toolCallId: 't', approved: true });
  });

  it('retry asks again in place, without adding a branch', async () => {
    let n = 0;
    const transport: AiTransport = {
      async *stream() {
        n += 1;
        if (n === 1) yield { type: 'error', code: 'http-503', message: 'Down' };
        else
          yield* [
            { type: 'text-delta', text: 'Back' } as AiStreamEvent,
            { type: 'done' } as AiStreamEvent,
          ];
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toMatchObject({ code: 'http-503', retryable: true });
    const id = result.current.messages[1]!.id;
    act(() => result.current.retry(id));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages).toHaveLength(2);
    expect(result.current.messages[1]!.id).toBe(id);
    expect(result.current.messages[1]!.branch).toBeUndefined();
    expect(messageText(result.current.messages[1]!)).toBe('Back');
  });

  it('error events carry retryAfterMs; unknown codes are not retryable', async () => {
    const transport: AiTransport = {
      async *stream() {
        yield { type: 'error', code: 'rate-limited', message: 'Slow', retryAfterMs: 3000 };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toEqual({
      code: 'rate-limited',
      message: 'Slow',
      retryable: true,
      retryAfterMs: 3000,
    });
    let m: AiMessage = { id: 'a', role: 'assistant', parts: [], createdAt: '' };
    m = applyStreamEvent(m, { type: 'error', code: 'weird', message: 'x' });
    expect(m.parts[0]).toMatchObject({ retryable: false });
  });

  it('a transport that throws when called ends in an error, never a stuck status', async () => {
    const transport: AiTransport = {
      stream() {
        throw new Error('boom');
      },
    };
    const { result } = renderHook(() =>
      useAiConversation({ transport, messages: { connectionLost: 'Perdu' } }),
    );
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error?.message).toBe('Perdu');
  });

  it('attachments: refused without uploadFile, nothing is sent and submit rejects', async () => {
    const transport = replayTransport([[{ type: 'done' }]]);
    const onError = vi.fn();
    const { result } = renderHook(() => useAiConversation({ transport, onError }));
    const file = new File(['abc'], 'a.txt', { type: 'text/plain' });
    await act(async () => {
      await expect(result.current.submit('see file', { files: [file] })).rejects.toThrow();
    });
    expect(result.current.messages).toEqual([]);
    expect(transport.requests).toHaveLength(0);
    expect(result.current.error).toMatchObject({
      code: 'attachments-unsupported',
      retryable: false,
    });
    expect(onError).toHaveBeenCalled();
  });

  it('attachments: uploadFile turns Files into parts and reports progress', async () => {
    const transport = replayTransport([[{ type: 'done' }]]);
    const progress: number[] = [];
    const uploadFile = vi.fn(async (f: File, ctx: { onProgress: (n: number) => void }) => {
      ctx.onProgress(0.5);
      return {
        type: 'file' as const,
        name: f.name,
        mediaType: f.type,
        url: `https://files.test/${f.name}`,
      };
    });
    const { result } = renderHook(() =>
      useAiConversation({ transport, uploadFile, onUploadProgress: (_f, n) => progress.push(n) }),
    );
    const file = new File(['abc'], 'a.png', { type: 'image/png' });
    await act(async () => {
      await result.current.submit('look', { files: [file] });
    });
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.messages[0]!.parts).toEqual([
      { type: 'text', text: 'look', state: 'done' },
      { type: 'file', name: 'a.png', mediaType: 'image/png', url: 'https://files.test/a.png' },
    ]);
    expect(progress).toEqual([0, 0.5, 1]);
    expect(transport.requests[0]!.messages[0]!.parts[1]).toMatchObject({ type: 'file' });
  });

  it('attachments: inlineFiles sends small files as data URLs only when allowed', async () => {
    const transport = replayTransport([[{ type: 'done' }]]);
    const { result } = renderHook(() =>
      useAiConversation({ transport, inlineFiles: { maxBytes: 10, mediaTypes: ['text/*'] } }),
    );
    await act(async () => {
      await result.current.submit('x', {
        files: [new File(['hi'], 'h.txt', { type: 'text/plain' })],
      });
    });
    expect(result.current.messages[0]!.parts[1]).toMatchObject({
      url: 'data:text/plain;base64,aGk=',
    });
    await waitFor(() => expect(result.current.status).toBe('idle'));
    await act(async () => {
      await expect(
        result.current.submit('y', {
          files: [new File(['0123456789AB'], 'big.txt', { type: 'text/plain' })],
        }),
      ).rejects.toThrow('big.txt is too large to attach.');
    });
  });

  it('resumeRun replays a run by its id into the same message', async () => {
    const resumed: string[] = [];
    const transport: AiTransport = {
      async *stream() {
        yield { type: 'run', runId: 'r1' };
        yield { type: 'text-delta', text: 'Par' };
        throw new Error('dropped');
      },
      async *resume({ runId }) {
        resumed.push(runId);
        yield { type: 'run', runId };
        yield { type: 'text-delta', text: 'Partial and done' };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.messages[1]!.runId).toBe('r1');
    act(() => result.current.resumeRun());
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(resumed).toEqual(['r1']);
    expect(messageText(result.current.messages[1]!)).toBe('Partial and done');
  });
});

describe('stream model: finish reasons, redaction, tool input', () => {
  it('text followed by reasoning closes the text; finish sets finishReason', () => {
    let m: AiMessage = { id: 'a', role: 'assistant', parts: [], createdAt: '' };
    m = applyStreamEvents(m, [
      { type: 'text-delta', text: 'A' },
      { type: 'reasoning-delta', text: 'hm' },
      { type: 'finish', reason: 'length' },
    ]);
    expect(m.parts[0]).toMatchObject({ type: 'text', state: 'done' });
    expect(m.finishReason).toBe('length');
  });

  it('tool-input-delta streams arguments; done with tool-use keeps calls awaiting results', () => {
    let m: AiMessage = { id: 'a', role: 'assistant', parts: [], createdAt: '' };
    m = applyStreamEvents(m, [
      {
        type: 'part',
        part: { type: 'tool-call', id: 't', name: 'x', input: {}, state: 'running' },
      },
      { type: 'tool-input-delta', id: 't', delta: '{"a"' },
      { type: 'tool-input-delta', id: 't', delta: ':1}' },
      { type: 'finish', reason: 'tool-use' },
      { type: 'done' },
    ]);
    expect(m.parts[0]).toMatchObject({ inputText: '{"a":1}', state: 'awaiting-result' });
    expect(unresolvedToolCalls(m)).toHaveLength(1);
    // Without tool-use, a call still running when the reply ends was cancelled.
    expect(
      finishParts(
        m.parts.map((p) => ({ ...p, state: 'running' }) as AiMessage['parts'][number]),
      )[0],
    ).toMatchObject({ state: 'cancelled' });
  });

  it('a redacted block becomes its own redacted reasoning part', () => {
    const events = [
      ...contentBlockMessagesMapper().map({
        event: '',
        data: JSON.stringify({
          type: 'content_block_start',
          index: 0,
          content_block: { type: 'redacted_thinking', data: 'x' },
        }),
      }),
    ];
    let m: AiMessage = {
      id: 'a',
      role: 'assistant',
      parts: [{ type: 'reasoning', text: 'seen', state: 'done' }],
      createdAt: '',
    };
    m = applyStreamEvents(m, events);
    expect(m.parts).toEqual([
      { type: 'reasoning', text: 'seen', state: 'done' },
      { type: 'reasoning', text: '', state: 'done', redacted: true },
    ]);
  });

  it('citations_delta becomes a source once and a citation per quote', () => {
    const mapper = contentBlockMessagesMapper();
    const cite = (text: string) =>
      mapper.map({
        event: '',
        data: JSON.stringify({
          type: 'content_block_delta',
          index: 0,
          delta: {
            type: 'citations_delta',
            citation: {
              type: 'web_search_result_location',
              url: 'https://a.test',
              title: 'A',
              cited_text: text,
            },
          },
        }),
      });
    expect([...cite('one'), ...cite('two')]).toEqual([
      {
        type: 'part',
        part: {
          type: 'source',
          id: 'https://a.test',
          title: 'A',
          kind: 'web',
          url: 'https://a.test',
        },
      },
      {
        type: 'part',
        part: { type: 'citation', id: 'citation-1', sourceId: 'https://a.test', quote: 'one' },
      },
      {
        type: 'part',
        part: { type: 'citation', id: 'citation-2', sourceId: 'https://a.test', quote: 'two' },
      },
    ]);
  });

  it('maps finish reasons from both vendor formats, and refusals', () => {
    const chat = (reason: string) =>
      chatCompletionsMapper().map({
        event: '',
        data: JSON.stringify({ choices: [{ delta: {}, finish_reason: reason }] }),
      });
    expect(chat('length')).toContainEqual({ type: 'finish', reason: 'length' });
    expect(chat('content_filter')).toContainEqual({ type: 'finish', reason: 'content-filter' });
    const refusing = chatCompletionsMapper();
    refusing.map({
      event: '',
      data: JSON.stringify({ choices: [{ delta: { refusal: 'I cannot.' } }] }),
    });
    expect(
      refusing.map({
        event: '',
        data: JSON.stringify({ choices: [{ delta: {}, finish_reason: 'stop' }] }),
      }),
    ).toContainEqual({ type: 'finish', reason: 'refusal' });
    const blocks = (reason: string) =>
      contentBlockMessagesMapper().map({
        event: '',
        data: JSON.stringify({ type: 'message_delta', delta: { stop_reason: reason } }),
      });
    expect(blocks('max_tokens')).toContainEqual({ type: 'finish', reason: 'length' });
    expect(blocks('refusal')).toContainEqual({ type: 'finish', reason: 'refusal' });
    expect(blocks('end_turn')).toContainEqual({ type: 'finish', reason: 'stop' });
  });

  it('chat completions: parallel tool calls stream separately', () => {
    const mapper = chatCompletionsMapper();
    const events = [
      {
        choices: [
          {
            delta: {
              tool_calls: [
                { index: 0, id: 'a', function: { name: 'x', arguments: '{}' } },
                { index: 1, id: 'b', function: { name: 'y', arguments: '{"k":' } },
              ],
            },
          },
        ],
      },
      { choices: [{ delta: { tool_calls: [{ index: 1, function: { arguments: '2}' } }] } }] },
      { choices: [{ delta: {}, finish_reason: 'tool_calls' }] },
    ].flatMap((c) => [...mapper.map({ event: '', data: JSON.stringify(c) })]);
    const msg = applyStreamEvents({ id: 'm', role: 'assistant', parts: [], createdAt: '' }, [
      ...events,
      { type: 'done' },
    ]);
    expect(msg.parts).toMatchObject([
      { id: 'a', input: {}, state: 'awaiting-result' },
      { id: 'b', input: { k: 2 }, state: 'awaiting-result' },
    ]);
  });
});

describe('SSE reader: chunk boundaries and resume', () => {
  const FIXTURE =
    'event: delta\r\nid: 7\r\ndata: one\r\ndata: twé\r\n\r\ndata: {"a":"€"}\r\n\r\n: ping\r\n\r\ndata: last\r\n\r\n';
  const EXPECTED = [
    { event: 'delta', id: '7', data: 'one\ntwé' },
    { event: '', data: '{"a":"€"}' },
    { event: '', data: 'last' },
  ];

  it('a \\r\\n split across chunks is one line end, not a frame boundary', async () => {
    expect(
      await collectStream(readSseFrames(bodyFromChunks(['data: a\r', '\ndata: b\r\n\r\n']))),
    ).toEqual([{ event: '', data: 'a\nb' }]);
    expect(normaliseSseChunk('', 'x\r')).toEqual({ text: 'x', carry: '\r' });
    expect(normaliseSseChunk('\r', '\ny')).toEqual({ text: '\ny', carry: '' });
  });

  it('gives the same frames for a split at every byte offset', async () => {
    for (const [head, tail] of everySplit(FIXTURE))
      expect(await collectStream(readSseFrames(bodyFromChunks([head, tail])))).toEqual(EXPECTED);
  });

  it('gives the same frames when cut into single bytes or random sizes', async () => {
    for (const lineEnding of ['\n', '\r\n', '\r'] as const) {
      const events: AiStreamEvent[] = [
        { type: 'text-delta', text: 'ünï' },
        { type: 'text-delta', text: 'code' },
      ];
      for (const chunking of ['byte', { seed: 1 }, { seed: 42, maxSize: 3 }] as const) {
        const frames = await collectStream(
          readSseFrames(sseFromEvents(events, { chunking, lineEnding })),
        );
        expect(frames.map((f) => JSON.parse(f.data))).toEqual(events);
      }
    }
  });

  it('every mapper fixture survives any two-chunk split', async () => {
    const payloads = [
      { choices: [{ delta: { content: 'Hé' } }] },
      { choices: [{ delta: { content: 'llo' }, finish_reason: 'stop' }] },
    ];
    const text = sseText([...payloads.map((p) => JSON.stringify(p)), '[DONE]'], {
      lineEnding: '\r\n',
    });
    const expected = await collectStream(
      createSseTransport({
        url: '/x',
        createMapper: chatCompletionsMapper,
        fetch: (async () => new Response(bodyFromChunks([text]))) as unknown as typeof fetch,
      }).stream({ messages: [], trigger: 'send', signal: new AbortController().signal }),
    );
    for (const [head, tail] of everySplit(text)) {
      const transport = createSseTransport({
        url: '/x',
        createMapper: chatCompletionsMapper,
        fetch: (async () => new Response(bodyFromChunks([head, tail]))) as unknown as typeof fetch,
      });
      expect(
        await collectStream(
          transport.stream({ messages: [], trigger: 'send', signal: new AbortController().signal }),
        ),
      ).toEqual(expected);
    }
  });

  it('resume: a server that replays from the start does not duplicate text', async () => {
    const frame = (id: number, t: string) =>
      `id: ${id}\ndata: ${JSON.stringify({ type: 'text-delta', text: t })}\n\n`;
    const failing = (chunks: string[]) => {
      let i = 0;
      return new ReadableStream<Uint8Array>({
        pull(c) {
          const ch = chunks[i++];
          if (ch === undefined) c.error(new Error('drop'));
          else c.enqueue(new TextEncoder().encode(ch));
        },
      });
    };
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(failing([frame(1, 'A'), frame(2, 'B')])))
      .mockResolvedValueOnce(
        new Response(
          bodyFromChunks([frame(1, 'A'), frame(2, 'B'), frame(3, 'C'), 'data: [DONE]\n\n']),
        ),
      );
    const transport = createSseTransport({
      url: '/x',
      createMapper: aiStreamEventMapper,
      resume: { attempts: 1 },
      fetch: fetchMock as unknown as typeof fetch,
    });
    const events = await collectStream(
      transport.stream({ messages: [], trigger: 'send', signal: new AbortController().signal }),
    );
    expect(events).toEqual([
      { type: 'text-delta', text: 'A' },
      { type: 'text-delta', text: 'B' },
      { type: 'text-delta', text: 'C' },
      { type: 'done' },
    ]);
  }, 10_000);

  it('HTTP errors: classified, localised, with retryAfterMs from Retry-After', async () => {
    const make = (status: number, headers?: Record<string, string>) =>
      createSseTransport({
        url: '/x',
        createMapper: aiStreamEventMapper,
        messages: { badRequest: 'Mauvaise requête' },
        fetch: (async () =>
          new Response(null, {
            status,
            ...(headers ? { headers } : {}),
          })) as unknown as typeof fetch,
      }).stream({ messages: [], trigger: 'send', signal: new AbortController().signal });
    expect(await collectStream(make(429, { 'Retry-After': '7' }))).toEqual([
      {
        type: 'error',
        code: 'rate-limited',
        message: 'Too many requests. Wait a moment and try again.',
        retryable: true,
        retryAfterMs: 7000,
      },
    ]);
    expect(await collectStream(make(403))).toMatchObject([{ code: 'auth', retryable: false }]);
    expect(await collectStream(make(422))).toMatchObject([
      { code: 'bad-request', retryable: false, message: 'Mauvaise requête' },
    ]);
    expect(await collectStream(make(502))).toMatchObject([{ code: 'http-502', retryable: true }]);
    expect(await collectStream(make(404))).toMatchObject([{ code: 'http-404', retryable: false }]);
    expect(classifyHttpStatus(500)).toEqual({
      code: 'http-500',
      retryable: true,
      message: 'unavailable',
    });
    expect(isRetryableErrorCode('auth')).toBe(false);
    expect(
      parseRetryAfter('Wed, 21 Oct 2015 07:28:10 GMT', Date.parse('Wed, 21 Oct 2015 07:28:00 GMT')),
    ).toBe(10_000);
  });

  it('respondToTool with a url function and no toolUrl fails when read, not when called', async () => {
    const transport = createSseTransport({
      url: () => '/x',
      createMapper: aiStreamEventMapper,
      buildToolBody: () => ({}),
    });
    const events = transport.respondToTool!({
      toolCallId: 't',
      approved: true,
      messages: [],
      signal: new AbortController().signal,
    });
    await expect(collectStream(events)).rejects.toThrow('toolUrl');
  });

  it('resumeUrl adds resume: a GET with Last-Event-ID', async () => {
    const fetchMock = vi.fn(async () => new Response(bodyFromChunks(['data: [DONE]\n\n'])));
    const transport = createSseTransport({
      url: '/x',
      createMapper: aiStreamEventMapper,
      resumeUrl: ({ runId }) => `/runs/${runId}`,
      fetch: fetchMock as unknown as typeof fetch,
    });
    await collectStream(
      transport.resume!({
        runId: 'r9',
        lastEventId: '4',
        messages: [],
        signal: new AbortController().signal,
      }),
    );
    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/runs/r9');
    expect(init.method).toBe('GET');
    expect(init.headers).toMatchObject({ 'Last-Event-ID': '4' });
  });

  it('the mock transport removes its abort listener after each wait', async () => {
    const controller = new AbortController();
    const remove = vi.spyOn(controller.signal, 'removeEventListener');
    await collectStream(
      mockTransport({ reply: textDeltas('a b'), delayMs: 1 }).stream({
        messages: [],
        trigger: 'send',
        signal: controller.signal,
      }),
    );
    expect(remove).toHaveBeenCalledTimes(2);
  });
});

describe('request builders', () => {
  const convo: AiMessage[] = [
    {
      id: 's',
      role: 'system',
      parts: [{ type: 'text', text: 'Be brief.', state: 'done' }],
      createdAt: '',
    },
    {
      id: 'u',
      role: 'user',
      parts: [
        { type: 'text', text: 'What is this?', state: 'done' },
        { type: 'file', name: 'a.png', mediaType: 'image/png', url: 'data:image/png;base64,QUJD' },
        { type: 'file', name: 'b.pdf', mediaType: 'application/pdf', url: 'https://f.test/b.pdf' },
      ],
      createdAt: '',
    },
    {
      id: 'a',
      role: 'assistant',
      parts: [
        { type: 'text', text: 'Checking.', state: 'done' },
        {
          type: 'tool-call',
          id: 't1',
          name: 'look',
          input: { q: 1 },
          state: 'succeeded',
          output: { ok: true },
        },
        { type: 'tool-call', id: 't2', name: 'skip', input: {}, state: 'cancelled' },
        { type: 'text', text: 'An image.', state: 'done' },
      ],
      createdAt: '',
    },
  ];

  it('chat completions: content parts, tool_calls and tool results', () => {
    expect(toChatCompletionsMessages(convo)).toEqual([
      { role: 'system', content: 'Be brief.' },
      {
        role: 'user',
        content: [
          { type: 'text', text: 'What is this?' },
          { type: 'image_url', image_url: { url: 'data:image/png;base64,QUJD' } },
          { type: 'text', text: '[b.pdf](https://f.test/b.pdf)' },
        ],
      },
      {
        role: 'assistant',
        content: 'Checking.',
        tool_calls: [
          { id: 't1', type: 'function', function: { name: 'look', arguments: '{"q":1}' } },
        ],
      },
      { role: 'tool', tool_call_id: 't1', content: '{"ok":true}' },
      { role: 'assistant', content: 'An image.' },
    ]);
    expect(
      buildChatCompletionsBody({
        messages: [],
        trigger: 'send',
        signal: new AbortController().signal,
        body: { x: 1 },
      }),
    ).toEqual({ messages: [], x: 1 });
  });

  it('content blocks: system apart, image and document blocks, tool_use then tool_result', () => {
    expect(toContentBlockMessages(convo)).toEqual({
      system: 'Be brief.',
      messages: [
        {
          role: 'user',
          content: [
            { type: 'image', source: { type: 'base64', media_type: 'image/png', data: 'QUJD' } },
            {
              type: 'document',
              source: { type: 'url', url: 'https://f.test/b.pdf' },
              title: 'b.pdf',
            },
            { type: 'text', text: 'What is this?' },
          ],
        },
        {
          role: 'assistant',
          content: [
            { type: 'text', text: 'Checking.' },
            { type: 'tool_use', id: 't1', name: 'look', input: { q: 1 } },
          ],
        },
        {
          role: 'user',
          content: [{ type: 'tool_result', tool_use_id: 't1', content: '{"ok":true}' }],
        },
        { role: 'assistant', content: [{ type: 'text', text: 'An image.' }] },
      ],
    });
    expect(
      buildContentBlocksBody({
        messages: convo,
        trigger: 'send',
        signal: new AbortController().signal,
      }),
    ).toHaveProperty('system', 'Be brief.');
  });

  it('responses input, UI messages and agent run input', () => {
    expect(toResponsesInput(convo)).toEqual([
      { role: 'system', content: [{ type: 'input_text', text: 'Be brief.' }] },
      {
        role: 'user',
        content: [
          { type: 'input_text', text: 'What is this?' },
          { type: 'input_image', image_url: 'data:image/png;base64,QUJD' },
          { type: 'input_file', file_url: 'https://f.test/b.pdf', filename: 'b.pdf' },
        ],
      },
      { role: 'assistant', content: [{ type: 'output_text', text: 'Checking.' }] },
      { type: 'function_call', call_id: 't1', name: 'look', arguments: '{"q":1}' },
      { type: 'function_call_output', call_id: 't1', output: '{"ok":true}' },
      { role: 'assistant', content: [{ type: 'output_text', text: 'An image.' }] },
    ]);
    expect(toUiMessages(convo)[2]!.parts[1]).toEqual({
      type: 'tool-look',
      toolCallId: 't1',
      state: 'output-available',
      input: { q: 1 },
      output: { ok: true },
    });
    const run = buildAgUiRunInput(
      {
        messages: convo,
        trigger: 'send',
        conversationId: 'th',
        signal: new AbortController().signal,
      },
      { runId: 'r' },
    );
    expect(run).toMatchObject({ threadId: 'th', runId: 'r' });
    expect(toAgUiMessages(convo).filter((m) => m.role === 'tool')).toEqual([
      { id: 'a-t1', role: 'tool', toolCallId: 't1', content: '{"ok":true}' },
    ]);
  });

  it("the Rook body carries the last message's attachments", async () => {
    const fetchMock = vi.fn(
      async () => new Response(bodyFromChunks(['data: {"type":"done"}\n\n'])),
    );
    const transport = rookIntelligenceTransport({
      url: '/chat',
      fetch: fetchMock as unknown as typeof fetch,
    });
    await collectStream(
      transport.stream({
        messages: convo.slice(0, 2),
        trigger: 'send',
        signal: new AbortController().signal,
      }),
    );
    const [, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(JSON.parse(String(init.body)).attachments).toEqual([
      { name: 'a.png', mediaType: 'image/png', url: 'data:image/png;base64,QUJD' },
      { name: 'b.pdf', mediaType: 'application/pdf', url: 'https://f.test/b.pdf' },
    ]);
  });
});

describe('more stream formats', () => {
  const feed = (mapper: AiStreamMapper, payloads: unknown[]) =>
    applyStreamEvents({ id: 'm', role: 'assistant', parts: [], createdAt: '' }, [
      ...payloads.flatMap((p) => [
        ...mapper.map({ event: '', data: typeof p === 'string' ? p : JSON.stringify(p) }),
      ]),
      ...(mapper.flush?.() ?? []),
    ]);

  it('UI message stream: text, reasoning, tool input streaming and output, sources, finish', () => {
    const m = feed(uiMessageStreamMapper(), [
      { type: 'start' },
      { type: 'reasoning-delta', id: 'r', delta: 'think' },
      { type: 'reasoning-end', id: 'r' },
      { type: 'text-delta', id: 't', delta: 'Hi' },
      { type: 'tool-input-start', toolCallId: 'c', toolName: 'search' },
      { type: 'tool-input-delta', toolCallId: 'c', inputTextDelta: '{"q":"x"}' },
      { type: 'tool-input-available', toolCallId: 'c', toolName: 'search', input: { q: 'x' } },
      { type: 'tool-output-available', toolCallId: 'c', output: [1] },
      { type: 'tool-input-available', toolCallId: 'd', toolName: 'ask_user', input: {} },
      { type: 'source-url', sourceId: 's', url: 'https://s.test', title: 'S' },
      { type: 'finish' },
      '[DONE]',
    ]);
    expect(m.parts).toMatchObject([
      { type: 'reasoning', text: 'think', state: 'done' },
      { type: 'text', text: 'Hi', state: 'done' },
      {
        type: 'tool-call',
        id: 'c',
        input: { q: 'x' },
        inputText: '{"q":"x"}',
        state: 'succeeded',
        output: [1],
      },
      { type: 'tool-call', id: 'd', state: 'awaiting-result' },
      { type: 'source', id: 's', url: 'https://s.test' },
    ]);
    expect(m.finishReason).toBe('tool-use');
  });

  it('responses events: run id, text, function calls, citations, incomplete', () => {
    const m = feed(responsesMapper(), [
      { type: 'response.created', response: { id: 'resp_1' } },
      { type: 'response.output_text.delta', delta: 'Hello' },
      {
        type: 'response.output_text.annotation.added',
        annotation: { type: 'url_citation', url: 'https://c.test', title: 'C' },
      },
      {
        type: 'response.output_item.added',
        item: { type: 'function_call', id: 'fc_1', call_id: 'call_1', name: 'f' },
      },
      { type: 'response.function_call_arguments.delta', item_id: 'fc_1', delta: '{"a":' },
      { type: 'response.function_call_arguments.done', item_id: 'fc_1', arguments: '{"a":1}' },
      {
        type: 'response.incomplete',
        response: {
          incomplete_details: { reason: 'max_output_tokens' },
          usage: { input_tokens: 3, output_tokens: 4 },
        },
      },
    ]);
    expect(m.runId).toBe('resp_1');
    expect(m.finishReason).toBe('length');
    expect(m.usage).toEqual({ inputTokens: 3, outputTokens: 4 });
    expect(m.parts).toMatchObject([
      { type: 'text', text: 'Hello' },
      { type: 'source', id: 'https://c.test' },
      { type: 'citation', sourceId: 'https://c.test' },
      { type: 'tool-call', id: 'call_1', input: { a: 1 }, inputText: '{"a":', state: 'cancelled' },
    ]);
    const failed = responsesMapper().map({
      event: '',
      data: JSON.stringify({
        type: 'response.failed',
        response: { error: { code: 'server_error', message: 'x' } },
      }),
    });
    expect(failed).toEqual([
      { type: 'error', code: 'server_error', message: 'x', retryable: true },
    ]);
  });

  it('agent events: run id, thinking, parallel tool calls with and without results', () => {
    const m = feed(agUiMapper(), [
      { type: 'RUN_STARTED', threadId: 't', runId: 'run-1' },
      { type: 'THINKING_TEXT_MESSAGE_CONTENT', delta: 'hmm' },
      { type: 'TEXT_MESSAGE_CONTENT', messageId: 'm', delta: 'Ok' },
      { type: 'TOOL_CALL_START', toolCallId: 'a', toolCallName: 'server' },
      { type: 'TOOL_CALL_START', toolCallId: 'b', toolCallName: 'confirm' },
      { type: 'TOOL_CALL_ARGS', toolCallId: 'a', delta: '{}' },
      { type: 'TOOL_CALL_END', toolCallId: 'a' },
      { type: 'TOOL_CALL_RESULT', toolCallId: 'a', content: '{"n":1}' },
      { type: 'TOOL_CALL_END', toolCallId: 'b' },
      { type: 'RUN_FINISHED', threadId: 't', runId: 'run-1' },
    ]);
    expect(m.runId).toBe('run-1');
    expect(m.parts).toMatchObject([
      { type: 'reasoning', text: 'hmm', state: 'done' },
      { type: 'text', text: 'Ok' },
      { type: 'tool-call', id: 'a', state: 'succeeded', output: { n: 1 } },
      { type: 'tool-call', id: 'b', state: 'awaiting-result' },
    ]);
    expect(feed(agUiMapper(), [{ type: 'RUN_ERROR', message: 'nope' }]).parts).toMatchObject([
      { type: 'error', message: 'nope', retryable: false },
    ]);
  });
});

describe('testing kit', () => {
  it('createStreamFixture, replayTransport, runTurn and simulateConversation', async () => {
    const fixture = createStreamFixture([...textDeltas('Hi you'), { type: 'done' }]);
    expect(fixture.text).toBe('Hi you');
    const transport = replayTransport([
      [{ type: 'text-delta', text: 'one' }, { type: 'done' }],
      [{ type: 'text-delta', text: 'two' }, { type: 'done' }],
    ]);
    const transcript = await simulateConversation(transport, ['a', 'b']);
    expect(transcript.map(messageText)).toEqual(['a', 'one', 'b', 'two']);
    expect(transport.requests).toHaveLength(2);
    const { message } = await runTurn(fixture.transport, []);
    expect(messageText(message)).toBe('Hi you');
    expect(chunkBytes(new Uint8Array([1, 2, 3, 4, 5]), { at: [2, 4] }).map((c) => [...c])).toEqual([
      [1, 2],
      [3, 4],
      [5],
    ]);
  });
});

describe('useAiConversation: offline, slow replies and dropped connections', () => {
  const echo = () =>
    replayTransport([
      [{ type: 'text-delta', text: 'one' }, { type: 'done' }],
      [{ type: 'text-delta', text: 'two' }, { type: 'done' }],
      [{ type: 'text-delta', text: 'three' }, { type: 'done' }],
    ]);

  it('queues messages sent offline, shows them as queued and sends them in order on reconnect', async () => {
    const network = manualNetworkSource(false);
    const transport = echo();
    const { result } = renderHook(() => useAiConversation({ transport, network, generateId: ids }));
    expect(result.current.offline).toBe(true);
    await act(async () => {
      await result.current.submit('first', { body: { doc: 1 } });
      await result.current.submit('second');
    });
    expect(transport.requests).toHaveLength(0);
    expect(result.current.status).toBe('idle');
    expect(result.current.queued.map((q) => q.text)).toEqual(['first', 'second']);
    expect(result.current.messages.map((m) => [m.role, messageText(m), m.queued])).toEqual([
      ['user', 'first', true],
      ['user', 'second', true],
    ]);
    const firstId = result.current.queued[0]!.id;

    act(() => network.set(true));
    expect(result.current.offline).toBe(false);
    await waitFor(() => expect(transport.requests).toHaveLength(2));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.queued).toEqual([]);
    expect(result.current.messages.map((m) => [m.role, messageText(m), m.queued])).toEqual([
      ['user', 'first', undefined],
      ['assistant', 'one', undefined],
      ['user', 'second', undefined],
      ['assistant', 'two', undefined],
    ]);
    // The queued message keeps its id when it is sent, and its request fields.
    expect(result.current.messages[0]!.id).toBe(firstId);
    expect(transport.requests[0]!.body).toEqual({ doc: 1 });
    expect(transport.requests[1]!.messages.map(messageText)).toEqual(['first', 'one', 'second']);
  });

  it('a queued message can be cancelled, and reset empties the queue', async () => {
    const network = manualNetworkSource(false);
    const transport = echo();
    const { result } = renderHook(() => useAiConversation({ transport, network }));
    act(() => {
      result.current.send('keep');
      result.current.send('drop');
    });
    const drop = result.current.queued[1]!.id;
    act(() => result.current.cancelQueued(drop));
    expect(result.current.queued.map((q) => q.text)).toEqual(['keep']);
    act(() => result.current.cancelQueued('unknown'));
    act(() => network.set(true));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    await waitFor(() => expect(transport.requests).toHaveLength(1));
    expect(result.current.messages.map(messageText)).toEqual(['keep', 'one']);

    act(() => network.set(false));
    act(() => result.current.send('later'));
    expect(result.current.queued).toHaveLength(1);
    act(() => result.current.reset());
    expect(result.current.queued).toEqual([]);
    expect(result.current.messages).toEqual([]);
  });

  it('queued attachments show as file parts and are uploaded when sent', async () => {
    const network = manualNetworkSource(false);
    const transport = echo();
    const uploadFile = vi.fn(async (file: File) => ({
      type: 'file' as const,
      name: file.name,
      mediaType: file.type,
      url: 'https://files.test/a.txt',
    }));
    const { result } = renderHook(() => useAiConversation({ transport, network, uploadFile }));
    act(() =>
      result.current.send('see', { files: [new File(['hi'], 'a.txt', { type: 'text/plain' })] }),
    );
    expect(uploadFile).not.toHaveBeenCalled();
    expect(result.current.messages[0]!.parts).toEqual([
      { type: 'text', text: 'see', state: 'done' },
      { type: 'file', name: 'a.txt', mediaType: 'text/plain', size: 2 },
    ]);
    act(() => network.set(true));
    await waitFor(() => expect(transport.requests).toHaveLength(1));
    expect(uploadFile).toHaveBeenCalledTimes(1);
    expect(transport.requests[0]!.messages[0]!.parts[1]).toMatchObject({
      url: 'https://files.test/a.txt',
    });
  });

  it('a message sent while others still wait goes behind them', async () => {
    const network = manualNetworkSource(false);
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => {
      release = resolve;
    });
    const asked: string[] = [];
    const transport: AiTransport = {
      async *stream({ messages }) {
        asked.push(messageText(messages[messages.length - 1]!));
        if (asked.length === 1) await gate;
        yield { type: 'text-delta', text: 'ok' };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport, network }));
    act(() => {
      result.current.send('a');
      result.current.send('b');
    });
    act(() => network.set(true));
    await waitFor(() => expect(asked).toEqual(['a']));
    // Online again, but "b" has not gone yet: "c" must not overtake it.
    act(() => result.current.send('c'));
    expect(result.current.queued.map((q) => q.text)).toEqual(['b', 'c']);
    release();
    await waitFor(() => expect(asked).toEqual(['a', 'b', 'c']));
    await waitFor(() => expect(result.current.status).toBe('idle'));
  });

  it('a queued message that fails holds the rest until the person retries', async () => {
    const network = manualNetworkSource(false);
    let fail = true;
    const asked: string[] = [];
    const transport: AiTransport = {
      async *stream({ messages }) {
        asked.push(messageText(messages.filter((m) => m.role === 'user').pop()!));
        if (fail) throw new Error('down');
        yield { type: 'text-delta', text: 'ok' };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport, network }));
    act(() => {
      result.current.send('a');
      result.current.send('b');
    });
    act(() => network.set(true));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(asked).toEqual(['a']);
    expect(result.current.queued.map((q) => q.text)).toEqual(['b']);
    fail = false;
    act(() => result.current.retry());
    await waitFor(() => expect(asked).toEqual(['a', 'a', 'b']));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(result.current.queued).toEqual([]);
  });

  it('offlineQueue: false sends at once; the browser source follows online and offline events', async () => {
    const network = manualNetworkSource(false);
    const transport = echo();
    const { result } = renderHook(() =>
      useAiConversation({ transport, network, offlineQueue: false }),
    );
    act(() => result.current.send('now'));
    await waitFor(() => expect(transport.requests).toHaveLength(1));
    expect(result.current.queued).toEqual([]);

    const source = browserNetworkSource();
    const seen: boolean[] = [];
    const off = source.subscribe((online) => seen.push(online));
    window.dispatchEvent(new Event('offline'));
    window.dispatchEvent(new Event('online'));
    off();
    window.dispatchEvent(new Event('offline'));
    expect(seen).toEqual([false, true]);
    expect(typeof source.isOnline()).toBe('boolean');
  });

  it('reports a slow reply after the delay and clears it with the next event', async () => {
    vi.useFakeTimers();
    try {
      const steps: (() => void)[] = [];
      const next = () => new Promise<void>((resolve) => steps.push(resolve));
      const transport: AiTransport = {
        async *stream() {
          await next();
          yield { type: 'text-delta', text: 'a' };
          await next();
          yield { type: 'done' };
        },
      };
      const { result } = renderHook(() => useAiConversation({ transport, slowAfterMs: 2000 }));
      act(() => result.current.send('Q'));
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1999);
      });
      expect(result.current.slow).toBe(false);
      await act(async () => {
        await vi.advanceTimersByTimeAsync(1);
      });
      expect(result.current.slow).toBe(true);
      await act(async () => {
        steps.shift()?.();
        await vi.advanceTimersByTimeAsync(20);
      });
      expect(result.current.slow).toBe(false);
      await act(async () => {
        await vi.advanceTimersByTimeAsync(2000);
      });
      expect(result.current.slow).toBe(true);
      act(() => result.current.stop());
      expect(result.current.slow).toBe(false);
      await act(async () => {
        await vi.advanceTimersByTimeAsync(5000);
      });
      expect(result.current.slow).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });

  it('slowAfterMs: 0 never reports a slow reply', async () => {
    vi.useFakeTimers();
    try {
      const transport: AiTransport = {
        async *stream({ signal }) {
          await new Promise<void>((resolve) => signal.addEventListener('abort', () => resolve()));
          yield { type: 'done' };
        },
      };
      const { result } = renderHook(() => useAiConversation({ transport, slowAfterMs: 0 }));
      act(() => result.current.send('Q'));
      await act(async () => {
        await vi.advanceTimersByTimeAsync(60000);
      });
      expect(result.current.slow).toBe(false);
      act(() => result.current.stop());
    } finally {
      vi.useRealTimers();
    }
  });

  it('a connection lost mid-reply is a resumable error when the run has an id and the transport can resume', async () => {
    const onError = vi.fn();
    const transport: AiTransport = {
      async *stream() {
        yield { type: 'run', runId: 'r1' };
        yield { type: 'text-delta', text: 'Par' };
        throw new Error('dropped');
      },
      async *resume() {
        yield { type: 'text-delta', text: 'Partial and done' };
        yield { type: 'done' };
      },
    };
    const { result } = renderHook(() => useAiConversation({ transport, onError }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('error'));
    expect(result.current.error).toMatchObject({
      code: 'network',
      retryable: true,
      resumable: true,
    });
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({ resumable: true }));
    expect(result.current.messages[1]!.parts.at(-1)).toMatchObject({
      type: 'error',
      resumable: true,
    });
    // What arrived before the drop stays on screen.
    expect(messageText(result.current.messages[1]!)).toBe('Par');
    act(() => result.current.resumeRun());
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(messageText(result.current.messages[1]!)).toBe('Partial and done');
  });

  it('a dropped connection without a run id, or without resume, is not resumable', async () => {
    const noRun: AiTransport = {
      async *stream() {
        yield { type: 'text-delta', text: 'Par' };
        throw new Error('dropped');
      },
      async *resume() {
        yield { type: 'done' };
      },
    };
    const first = renderHook(() => useAiConversation({ transport: noRun }));
    act(() => first.result.current.send('Q'));
    await waitFor(() => expect(first.result.current.status).toBe('error'));
    expect(first.result.current.error?.resumable).toBeUndefined();

    const noResume: AiTransport = {
      async *stream() {
        yield { type: 'run', runId: 'r1' };
        yield { type: 'error', code: 'network', message: 'gone' };
      },
    };
    const second = renderHook(() => useAiConversation({ transport: noResume }));
    act(() => second.result.current.send('Q'));
    await waitFor(() => expect(second.result.current.status).toBe('error'));
    expect(second.result.current.error?.resumable).toBeUndefined();
    expect(second.result.current.messages[1]!.parts.at(-1)).not.toHaveProperty('resumable');
  });

  it('regenerate and retry send the request fields they are given', async () => {
    const transport = echo();
    const { result } = renderHook(() => useAiConversation({ transport, body: { base: 1 } }));
    act(() => result.current.send('Q'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    act(() => result.current.regenerate(undefined, { body: { draft: 2 } }));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    act(() => result.current.retry(undefined, { body: { draft: 3 } }));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(transport.requests.map((r) => r.body)).toEqual([
      { base: 1 },
      { base: 1, draft: 2 },
      { base: 1, draft: 3 },
    ]);
  });
});

describe('memory in the stream', () => {
  const blank = (): AiMessage => ({ id: 'm', role: 'assistant', parts: [], createdAt: '' });

  it('a memory event adds a memory part, and the same id and action again updates it', () => {
    const m = applyStreamEvents(blank(), [
      { type: 'text-delta', text: 'Noted.' },
      { type: 'memory', memory: { id: 'm1', action: 'write', text: 'Likes tea', scope: 'user' } },
      { type: 'memory', memory: { id: 'm2', action: 'read', text: 'Lives in Leeds' } },
      { type: 'memory', memory: { id: 'm1', action: 'write', text: 'Likes green tea' } },
      { type: 'memory', memory: { id: 'm2', action: 'forget', text: '' } },
      { type: 'done' },
    ]);
    expect(m.parts).toEqual([
      { type: 'text', text: 'Noted.', state: 'done' },
      { type: 'memory', id: 'm1', action: 'write', text: 'Likes green tea' },
      { type: 'memory', id: 'm2', action: 'read', text: 'Lives in Leeds' },
      { type: 'memory', id: 'm2', action: 'forget', text: '' },
    ]);
    expect(messageMemories(m).map((p) => p.action)).toEqual(['write', 'read', 'forget']);
    expect(messageText(m)).toBe('Noted.');
  });

  it('parseAiMemoryEvent reads loose shapes and rejects what is not a memory', () => {
    expect(parseAiMemoryEvent({ action: 'write', id: 'a', text: 'T', scope: 'project' })).toEqual({
      id: 'a',
      action: 'write',
      text: 'T',
      scope: 'project',
    });
    expect(parseAiMemoryEvent({ op: 'Recall', memoryId: 'b', content: 'C' })).toEqual({
      id: 'b',
      action: 'read',
      text: 'C',
    });
    expect(parseAiMemoryEvent({ operation: 'delete', id: 'c' })).toEqual({
      id: 'c',
      action: 'forget',
      text: '',
    });
    expect(parseAiMemoryEvent({ action: 'save', memory: 'M' }, 'fallback')).toMatchObject({
      id: 'fallback',
      action: 'write',
    });
    expect(parseAiMemoryEvent({ action: 'write', id: 'a' })).toBeNull();
    expect(parseAiMemoryEvent({ action: 'explode', id: 'a', text: 'x' })).toBeNull();
    expect(parseAiMemoryEvent({ action: 'write', text: 'x' })).toBeNull();
    expect(parseAiMemoryEvent('write')).toBeNull();
    expect(parseAiMemoryEvent(null)).toBeNull();
  });

  it('the UI message stream and agent–user interaction mappers map their custom events', () => {
    const frame = (payload: unknown): SseFrame => ({ event: '', data: JSON.stringify(payload) });
    expect(
      uiMessageStreamMapper().map(
        frame({ type: 'data-memory', id: 'd1', data: { action: 'write', text: 'Likes tea' } }),
      ),
    ).toEqual([{ type: 'memory', memory: { id: 'd1', action: 'write', text: 'Likes tea' } }]);
    expect(uiMessageStreamMapper().map(frame({ type: 'data-memory', data: {} }))).toEqual([]);
    expect(uiMessageStreamMapper().map(frame({ type: 'data-other', data: {} }))).toEqual([]);
    expect(
      agUiMapper().map(
        frame({ type: 'CUSTOM', name: 'memory', value: { action: 'forget', id: 'm9' } }),
      ),
    ).toEqual([{ type: 'memory', memory: { id: 'm9', action: 'forget', text: '' } }]);
    expect(agUiMapper().map(frame({ type: 'CUSTOM', name: 'other', value: {} }))).toEqual([]);
  });

  it('useAiConversation calls onMemoryEvent for memory events and memory parts', async () => {
    const seen: [AiMemoryEvent, string][] = [];
    const transport = replayTransport([
      [
        { type: 'memory', memory: { id: 'm1', action: 'write', text: 'Likes tea' } },
        {
          type: 'part',
          part: { type: 'memory', id: 'm2', action: 'read', text: 'Lives in Leeds' },
        },
        { type: 'text-delta', text: 'Done' },
        { type: 'done' },
      ],
    ]);
    const { result } = renderHook(() =>
      useAiConversation({
        transport,
        onMemoryEvent: (memory, message) => seen.push([memory, message.role]),
      }),
    );
    act(() => result.current.send('Remember I like tea'));
    await waitFor(() => expect(result.current.status).toBe('idle'));
    expect(seen).toEqual([
      [{ id: 'm1', action: 'write', text: 'Likes tea' }, 'assistant'],
      [{ id: 'm2', action: 'read', text: 'Lives in Leeds' }, 'assistant'],
    ]);
    expect(messageMemories(result.current.messages[1]!)).toHaveLength(2);
  });
});

describe('usage details', () => {
  const frame = (payload: unknown, event = ''): SseFrame => ({
    event,
    data: JSON.stringify(payload),
  });
  const usageOf = (events: readonly AiStreamEvent[]) =>
    events.flatMap((e) => (e.type === 'usage' ? [e.usage] : []));

  it('chat completions: cached and reasoning tokens', () => {
    const events = chatCompletionsMapper().map(
      frame({
        choices: [],
        usage: {
          prompt_tokens: 100,
          completion_tokens: 40,
          prompt_tokens_details: { cached_tokens: 60 },
          completion_tokens_details: { reasoning_tokens: 25 },
        },
      }),
    );
    expect(usageOf(events)).toEqual([
      { inputTokens: 100, outputTokens: 40, cacheReadTokens: 60, reasoningTokens: 25 },
    ]);
    const plain = chatCompletionsMapper().map(
      frame({ choices: [], usage: { prompt_tokens: 5, completion_tokens: 2 } }),
    );
    expect(usageOf(plain)).toEqual([{ inputTokens: 5, outputTokens: 2 }]);
  });

  it('content blocks: cache reads and writes count towards the input total', () => {
    const mapper = contentBlockMessagesMapper();
    mapper.map(
      frame({
        type: 'message_start',
        message: {
          model: 'm',
          usage: { input_tokens: 10, cache_read_input_tokens: 70, cache_creation_input_tokens: 20 },
        },
      }),
    );
    const events = mapper.map(
      frame({ type: 'message_delta', delta: {}, usage: { output_tokens: 8 } }),
    );
    expect(usageOf(events)).toEqual([
      { inputTokens: 100, outputTokens: 8, cacheReadTokens: 70, cacheWriteTokens: 20 },
    ]);
  });

  it('responses: cached and reasoning tokens', () => {
    const events = responsesMapper().map(
      frame({
        type: 'response.completed',
        response: {
          model: 'm',
          usage: {
            input_tokens: 50,
            output_tokens: 30,
            input_tokens_details: { cached_tokens: 10 },
            output_tokens_details: { reasoning_tokens: 12 },
          },
        },
      }),
    );
    expect(usageOf(events)).toEqual([
      { inputTokens: 50, outputTokens: 30, cacheReadTokens: 10, reasoningTokens: 12 },
    ]);
  });
});
