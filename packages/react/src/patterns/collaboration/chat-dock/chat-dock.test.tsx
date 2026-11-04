import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolationsWithFrames as expectNoA11yViolations } from '../../../../test/a11y-frames.js';
import {
  CHAT_EMBED_PROTOCOL,
  ChatDock,
  createChatDockBridge,
  decodeChatEmbedMessage,
  encodeChatEmbedMessage,
  type ChatDockController,
  type ChatDockProps,
} from './index.js';

const CHAT = 'https://chat.example.com';
const SRC = `${CHAT}/embed/dock?ws=ws_1`;

const base: ChatDockProps = {
  src: SRC,
  allowedOrigins: [CHAT],
  appName: 'Rook Stride',
  workspaceId: 'ws_1',
  refreshSession: () => Promise.resolve(),
};

const env = (type: string, extra: Record<string, unknown> = {}) => ({
  protocol: CHAT_EMBED_PROTOCOL,
  v: 1,
  type,
  ...extra,
});

function frame(): HTMLIFrameElement {
  const el = document.querySelector<HTMLIFrameElement>('iframe.mn-chat-dock__frame');
  if (!el) throw new Error('no frame');
  return el;
}

function fromFrame(data: unknown, origin = CHAT, source?: unknown) {
  act(() => {
    window.dispatchEvent(
      new MessageEvent('message', {
        data,
        origin,
        source: (source === undefined ? frame().contentWindow : source) as Window | null,
      }),
    );
  });
}

function spyFrame() {
  return vi.spyOn(frame().contentWindow as Window, 'postMessage').mockImplementation(() => {});
}

describe('ChatDock', () => {
  it('shows a named launcher and creates no frame until it is wanted', async () => {
    render(<ChatDock {...base} />);
    const launcher = screen.getByRole('button', { name: 'Rook Chat' });
    expect(launcher).toHaveAttribute('aria-expanded', 'false');
    expect(launcher).not.toHaveAttribute('aria-controls');
    expect(document.querySelector('iframe')).toBeNull();
    await expectNoA11yViolations();
  });

  it('opens a dialog panel with a sandboxed frame and focuses it; Escape closes and returns focus', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<ChatDock {...base} onOpenChange={onOpenChange} />);
    const launcher = screen.getByRole('button', { name: 'Rook Chat' });
    await user.click(launcher);
    const panel = screen.getByRole('dialog', { name: 'Rook Chat' });
    expect(launcher).toHaveAttribute('aria-expanded', 'true');
    expect(launcher).toHaveAttribute('aria-controls', panel.id);
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    const el = frame();
    expect(el.getAttribute('sandbox')).toContain('allow-scripts');
    expect(el.getAttribute('sandbox')).not.toContain('top-navigation');
    expect(el.getAttribute('allow')).toContain('microphone');
    await waitFor(() => expect(el).toHaveFocus());
    await expectNoA11yViolations();

    // Inside a real (cross-origin) frame, Escape is Chat's own and comes back as
    // `rook-chat:close`; on the panel itself the dock handles it.
    fireEvent.keyDown(panel, { key: 'Escape' });
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    expect(panel).toHaveAttribute('aria-hidden', 'true');
    await waitFor(() => expect(launcher).toHaveFocus());
  });

  it('keeps the frame alive while closed: it never reloads on its own', async () => {
    const user = userEvent.setup();
    render(<ChatDock {...base} />);
    const launcher = screen.getByRole('button', { name: 'Rook Chat' });
    await user.click(launcher);
    const first = frame();
    await user.click(launcher);
    await user.click(launcher);
    expect(frame()).toBe(first);
  });

  it('mounts the frame on hover or focus (intent), unless mountOnIntent is false', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<ChatDock {...base} />);
    await user.hover(screen.getByRole('button', { name: 'Rook Chat' }));
    expect(document.querySelector('iframe')).not.toBeNull();
    unmount();
    render(<ChatDock {...base} mountOnIntent={false} />);
    await user.hover(screen.getByRole('button', { name: 'Rook Chat' }));
    expect(document.querySelector('iframe')).toBeNull();
  });

  it('handshakes on ready: init and context to the exact origin, then queued messages', async () => {
    const user = userEvent.setup();
    const controller = createRef<ChatDockController>();
    render(
      <ChatDock
        {...base}
        controllerRef={controller}
        context={() => ({
          app: 'Rook Stride',
          url: 'https://stride.example.com/t/1',
          title: 'Task 1',
        })}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Rook Chat' }));
    const post = spyFrame();
    act(() => controller.current?.post({ type: 'rook-chat:open', conversationId: 'cnv_1' }));
    expect(post).not.toHaveBeenCalled();
    fromFrame(env('rook-chat:ready'));
    const sent = post.mock.calls.map(([m, to]) => [(m as { type: string }).type, to]);
    expect(sent.slice(0, 3)).toEqual([
      ['rook-chat:init', CHAT],
      ['rook-chat:context', CHAT],
      ['rook-chat:open', CHAT],
    ]);
    expect(post.mock.calls[0]?.[0]).toEqual(
      encodeChatEmbedMessage({ type: 'rook-chat:init', app: 'Rook Stride', workspaceId: 'ws_1' }),
    );
    expect(post).toHaveBeenCalledWith(env('rook-chat:visibility', { open: true }), CHAT);
    expect(post.mock.calls.every((call) => (call as unknown[])[1] === CHAT)).toBe(true);
  });

  it('drops messages from another origin, another window or with a bad shape', async () => {
    const user = userEvent.setup();
    const onEmbedMessage = vi.fn();
    render(<ChatDock {...base} onEmbedMessage={onEmbedMessage} unread={2} />);
    await user.click(screen.getByRole('button', { name: 'Rook Chat, 2 unread' }));
    fromFrame(env('rook-chat:unread', { count: 9 }), 'https://evil.example.net');
    fromFrame(env('rook-chat:unread', { count: 9 }), 'https://chat.example.com.evil.net');
    fromFrame(env('rook-chat:unread', { count: 9 }), CHAT, window);
    fromFrame(env('rook-chat:unread', { count: -1 }));
    fromFrame(env('rook-chat:unread', { count: 1.5 }));
    fromFrame({ ...env('rook-chat:unread', { count: 9 }), v: 2 });
    fromFrame(env('rook-chat:init', { app: 'x', workspaceId: 'y' }));
    expect(onEmbedMessage).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Rook Chat, 2 unread' })).toBeInTheDocument();
  });

  it('shows the unread badge (count in the name, badge hidden) and follows the frame', async () => {
    const user = userEvent.setup();
    const onUnreadChange = vi.fn();
    render(<ChatDock {...base} unread={3} onUnreadChange={onUnreadChange} />);
    const launcher = screen.getByRole('button', { name: 'Rook Chat, 3 unread' });
    const badge = launcher.querySelector('.mn-chat-dock__badge');
    expect(badge).toHaveTextContent('3');
    expect(badge).toHaveAttribute('aria-hidden', 'true');
    await user.hover(launcher);
    fromFrame(env('rook-chat:unread', { count: 140 }));
    expect(onUnreadChange).toHaveBeenCalledWith(140);
    expect(screen.getByRole('button', { name: 'Rook Chat, 140 unread' })).toHaveTextContent('99+');
    fromFrame(env('rook-chat:unread', { count: 0 }));
    expect(
      screen.getByRole('button', { name: 'Rook Chat' }).querySelector('.mn-chat-dock__badge'),
    ).toBeNull();
  });

  it('answers refresh requests with the host refresh and never refreshes on its own', async () => {
    const user = userEvent.setup();
    const refreshSession = vi.fn(() => Promise.resolve());
    render(<ChatDock {...base} refreshSession={refreshSession} />);
    await user.click(screen.getByRole('button', { name: 'Rook Chat' }));
    expect(refreshSession).not.toHaveBeenCalled();
    const post = spyFrame();
    fromFrame(env('rook-chat:ready'));
    fromFrame(env('rook-chat:refresh-request', { id: 'r1' }));
    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        env('rook-chat:refresh-result', { id: 'r1', ok: true }),
        CHAT,
      ),
    );
    expect(refreshSession).toHaveBeenCalledTimes(1);

    refreshSession.mockImplementationOnce(() => Promise.reject(new Error('signed out')));
    fromFrame(env('rook-chat:refresh-request', { id: 'r2' }));
    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        env('rook-chat:refresh-result', { id: 'r2', ok: false }),
        CHAT,
      ),
    );
  });

  it('hides where the app has its own assistant, but keeps the frame and a call on screen', async () => {
    const user = userEvent.setup();
    const { rerender, container } = render(<ChatDock {...base} />);
    await user.click(screen.getByRole('button', { name: 'Rook Chat' }));
    const first = frame();
    rerender(<ChatDock {...base} hidden />);
    expect(screen.queryByRole('button', { name: 'Rook Chat' })).not.toBeInTheDocument();
    expect(frame()).toBe(first);
    expect(container.firstElementChild).toHaveAttribute('data-hidden');
    fromFrame(env('rook-chat:layout', { layout: 'call' }));
    expect(screen.getByRole('button', { name: 'Rook Chat' })).toBeInTheDocument();
    expect(container.firstElementChild).toHaveAttribute('data-layout', 'call');
  });

  it('closes from inside the frame and hides when Chat is unavailable', async () => {
    const user = userEvent.setup();
    const onStatusChange = vi.fn();
    render(<ChatDock {...base} defaultOpen onStatusChange={onStatusChange} />);
    expect(screen.getByRole('dialog', { name: 'Rook Chat' })).toHaveAttribute(
      'aria-hidden',
      'false',
    );
    fromFrame(env('rook-chat:close'));
    expect(screen.getByRole('button', { name: 'Rook Chat' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    fromFrame(env('rook-chat:status', { status: 'unavailable' }));
    expect(onStatusChange).toHaveBeenCalledWith('unavailable');
    expect(screen.queryByRole('button', { name: 'Rook Chat' })).not.toBeInTheDocument();
    void user;
  });

  it('shows an incoming call alert that opens the conversation', async () => {
    const user = userEvent.setup();
    render(<ChatDock {...base} />);
    await user.hover(screen.getByRole('button', { name: 'Rook Chat' }));
    const post = spyFrame();
    fromFrame(env('rook-chat:ready'));
    fromFrame(env('rook-chat:ring', { callId: 'call_1', conversationId: 'cnv_9', from: 'Ada' }));
    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Incoming call');
    await user.click(screen.getByRole('button', { name: 'Open' }));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(post).toHaveBeenCalledWith(env('rook-chat:open', { conversationId: 'cnv_9' }), CHAT);
  });

  it('drops unsafe open details and is controllable', async () => {
    const controller = createRef<ChatDockController>();
    const onOpenChange = vi.fn();
    const { rerender } = render(
      <ChatDock {...base} open={false} onOpenChange={onOpenChange} controllerRef={controller} />,
    );
    act(() =>
      controller.current?.open({
        conversationId: '../../etc',
        share: { title: 'Doc', url: 'javascript:alert(1)' },
      }),
    );
    expect(onOpenChange).toHaveBeenCalledWith(true);
    expect(screen.getByRole('button', { name: 'Rook Chat' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    rerender(<ChatDock {...base} open onOpenChange={onOpenChange} controllerRef={controller} />);
    expect(screen.getByRole('button', { name: 'Rook Chat' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    const post = spyFrame();
    fromFrame(env('rook-chat:ready'));
    expect(post.mock.calls.some(([m]) => (m as { type: string }).type === 'rook-chat:open')).toBe(
      false,
    );
  });

  it('translates', () => {
    render(
      <ChatDock
        {...base}
        unread={1}
        messages={{ label: 'Discussions', unread: (n) => `${n} non lus` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Discussions, 1 non lus' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<ChatDock {...base} unread={4} />);
    expect(html).toContain('mn-chat-dock');
    expect(html).toContain('Rook Chat, 4 unread');
    expect(html).not.toContain('<iframe');
  });
});

describe('chat embed protocol', () => {
  it('decodes only valid embed messages', () => {
    expect(decodeChatEmbedMessage(env('rook-chat:layout', { layout: 'expanded' }))).toEqual({
      type: 'rook-chat:layout',
      layout: 'expanded',
    });
    expect(decodeChatEmbedMessage(env('rook-chat:layout', { layout: 'huge' }))).toBeNull();
    expect(decodeChatEmbedMessage(env('rook-chat:location', { conversationId: null }))).toEqual({
      type: 'rook-chat:location',
      conversationId: null,
    });
    expect(decodeChatEmbedMessage(env('rook-chat:refresh-request', { id: 'a b' }))).toBeNull();
    expect(decodeChatEmbedMessage(env('rook-chat:visibility', { open: true }))).toBeNull();
    expect(decodeChatEmbedMessage(null)).toBeNull();
  });

  it('the bridge never posts to an origin outside the allow list', () => {
    const win = { postMessage: vi.fn() };
    const bridge = createChatDockBridge({
      frameWindow: () => win,
      allowedOrigins: new Set([CHAT]),
      targetOrigin: 'https://elsewhere.example.com',
      appName: () => 'App',
      workspaceId: () => 'ws',
      refresh: () => Promise.resolve(),
      context: () => ({ app: 'App', url: 'https://app.example.com/' }),
      on: () => {},
    });
    expect(bridge.handle({ origin: CHAT, source: win, data: env('rook-chat:ready') })).toBe(true);
    expect(win.postMessage).not.toHaveBeenCalled();
    expect(bridge.isReady()).toBe(true);
    bridge.reset();
    expect(bridge.isReady()).toBe(false);
  });
});
