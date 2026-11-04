import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolationsWithFrames as expectNoA11yViolations } from '../../../../test/a11y-frames.js';
import {
  APP_EMBED_PROTOCOL,
  EmbeddedAppFrame,
  decodeAppEmbedChildMessage,
  decodeAppEmbedHostMessage,
  encodeAppEmbedMessage,
  useEmbedReady,
} from './index.js';

const SRC = 'https://tickets.example.com/settings/email?embed=settings';
const ORIGIN = 'https://tickets.example.com';

function frame(): HTMLIFrameElement {
  return screen.getByTitle('Tickets: Email settings') as HTMLIFrameElement;
}

function fromFrame(
  data: unknown,
  { origin = ORIGIN, source }: { origin?: string; source?: unknown } = {},
) {
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

const msg = (type: string, extra: Record<string, unknown> = {}) => ({
  protocol: APP_EMBED_PROTOCOL,
  v: 1,
  type,
  ...extra,
});

describe('EmbeddedAppFrame', () => {
  it('renders a titled, sandboxed iframe with a loading status', async () => {
    render(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" />);
    const el = frame();
    expect(el).toHaveAttribute('src', SRC);
    expect(el.getAttribute('sandbox')).toBe('allow-scripts allow-same-origin allow-forms');
    expect(el).toHaveAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
    expect(el).not.toHaveAttribute('allow');
    expect(screen.getByRole('status')).toHaveTextContent('Loading…');
    await expectNoA11yViolations();
  });

  it('becomes ready on an exact ready message from the frame at an allowed origin', () => {
    const onReady = vi.fn();
    const onStatusChange = vi.fn();
    const { container } = render(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        onReady={onReady}
        onStatusChange={onStatusChange}
      />,
    );
    fromFrame(msg('ready'));
    expect(container.firstElementChild).toHaveAttribute('data-status', 'ready');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    fromFrame(msg('ready'));
    expect(onReady).toHaveBeenCalledTimes(1);
    expect(onStatusChange).toHaveBeenLastCalledWith('ready');
  });

  it('drops messages from other origins, other windows and with the wrong shape', () => {
    const onReady = vi.fn();
    render(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" onReady={onReady} />);
    fromFrame(msg('ready'), { origin: 'https://evil.example.net' });
    fromFrame(msg('ready'), { origin: 'https://tickets.example.com.evil.net' });
    fromFrame(msg('ready'), { source: window });
    fromFrame(msg('ready'), { source: null });
    fromFrame(msg('ready', { extra: true }));
    fromFrame({ ...msg('ready'), v: 2 });
    fromFrame({ ...msg('ready'), protocol: 'other' });
    fromFrame('ready');
    fromFrame([msg('ready')]);
    expect(onReady).not.toHaveBeenCalled();
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('accepts only the listed origins; `*` and paths never match', () => {
    const onReady = vi.fn();
    render(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        allowedOrigins={['*', 'https://hub.example.com/path', 'https://other.example.com']}
        onReady={onReady}
      />,
    );
    fromFrame(msg('ready'));
    fromFrame(msg('ready'), { origin: 'https://evil.example.net' });
    expect(onReady).not.toHaveBeenCalled();
    fromFrame(msg('ready'), { origin: 'https://other.example.com' });
    expect(onReady).toHaveBeenCalledTimes(1);
  });

  it('follows resize messages within min and max, and ignores them without autoHeight', () => {
    const { container, rerender } = render(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        minHeight={200}
        maxHeight={900}
      />,
    );
    const root = container.firstElementChild as HTMLElement;
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('480px');
    fromFrame(msg('resize', { height: 640.2 }));
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('641px');
    fromFrame(msg('resize', { height: 50 }));
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('200px');
    fromFrame(msg('resize', { height: 5000 }));
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('900px');
    fromFrame(msg('resize', { height: -1 }));
    fromFrame(msg('resize', { height: '700' }));
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('900px');

    rerender(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        autoHeight={false}
        height="70vh"
      />,
    );
    expect(root.style.getPropertyValue('--mn-embedded-app-frame-height')).toBe('70vh');
  });

  it('shows an error with Try again and an open link after the ready timeout', async () => {
    const user = userEvent.setup();
    render(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        readyTimeout={30}
        openHref="https://tickets.example.com/settings/email"
        messages={{ open: 'Open in Tickets' }}
      />,
    );
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('This page didn’t open');
    const open = screen.getByRole('link', { name: 'Open in Tickets (opens in a new tab)' });
    expect(open).toHaveAttribute('target', '_blank');
    expect(open).toHaveAttribute('rel', 'noopener noreferrer');
    const before = frame();
    expect(before).toHaveAttribute('aria-hidden', 'true');

    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(frame()).not.toBe(before);
    const status = screen.getByRole('status');
    expect(status).toHaveFocus();
    fromFrame(msg('ready'));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('pings the page on load, only at its own origin', () => {
    render(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" />);
    const win = frame().contentWindow as Window;
    const post = vi.spyOn(win, 'postMessage').mockImplementation(() => {});
    act(() => {
      frame().dispatchEvent(new Event('load'));
    });
    expect(post).toHaveBeenCalledWith(msg('ping'), ORIGIN);
  });

  it('moves focus into the page when ready with focusOnReady', () => {
    render(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" focusOnReady />);
    const win = frame().contentWindow as Window;
    const post = vi.spyOn(win, 'postMessage').mockImplementation(() => {});
    fromFrame(msg('ready'));
    expect(frame()).toHaveFocus();
    expect(post).toHaveBeenCalledWith(msg('focus', { direction: 'forward' }), ORIGIN);
  });

  it('hands focus back on focus-exit: next or previous control, or onFocusExit', () => {
    const { rerender } = render(
      <div>
        <button type="button">Before</button>
        <EmbeddedAppFrame src={SRC} title="Tickets: Email settings" />
        <button type="button">After</button>
      </div>,
    );
    fromFrame(msg('focus-exit', { direction: 'forward' }));
    expect(screen.getByRole('button', { name: 'After' })).toHaveFocus();
    fromFrame(msg('focus-exit', { direction: 'backward' }));
    expect(screen.getByRole('button', { name: 'Before' })).toHaveFocus();
    fromFrame(msg('focus-exit', { direction: 'sideways' }));
    expect(screen.getByRole('button', { name: 'Before' })).toHaveFocus();

    const onFocusExit = vi.fn();
    rerender(
      <div>
        <button type="button">Before</button>
        <EmbeddedAppFrame src={SRC} title="Tickets: Email settings" onFocusExit={onFocusExit} />
        <button type="button">After</button>
      </div>,
    );
    fromFrame(msg('focus-exit', { direction: 'forward' }));
    expect(onFocusExit).toHaveBeenCalledWith('forward');
  });

  it('keeps only known sandbox tokens and passes the allow list', () => {
    render(
      <EmbeddedAppFrame
        src={SRC}
        title="Tickets: Email settings"
        sandbox={['allow-scripts', 'allow-top-navigation' as never, 'allow-scripts']}
        allow={['clipboard-write', 'camera; geolocation']}
      />,
    );
    expect(frame().getAttribute('sandbox')).toBe('allow-scripts');
    expect(frame()).toHaveAttribute('allow', 'clipboard-write');
  });

  it('renders on the server', () => {
    const html = renderToString(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" />);
    expect(html).toContain('mn-embedded-app-frame');
    expect(html).toContain('sandbox="allow-scripts allow-same-origin allow-forms"');
  });
});

describe('embed protocol', () => {
  it('encodes and decodes both directions', () => {
    expect(encodeAppEmbedMessage({ type: 'ping' })).toEqual(msg('ping'));
    expect(decodeAppEmbedChildMessage(msg('ready'))).toEqual({ type: 'ready' });
    expect(decodeAppEmbedChildMessage(msg('resize', { height: 12.5 }))).toEqual({
      type: 'resize',
      height: 13,
    });
    expect(decodeAppEmbedChildMessage(msg('resize', { height: Infinity }))).toBeNull();
    expect(decodeAppEmbedChildMessage(msg('ping'))).toBeNull();
    expect(decodeAppEmbedHostMessage(msg('ping'))).toEqual({ type: 'ping' });
    expect(decodeAppEmbedHostMessage(msg('focus', { direction: 'backward' }))).toEqual({
      type: 'focus',
      direction: 'backward',
    });
    expect(decodeAppEmbedHostMessage(msg('ready'))).toBeNull();
    expect(decodeAppEmbedHostMessage(null)).toBeNull();
  });
});

describe('useEmbedReady', () => {
  const realParent = window.parent;
  afterEach(() => {
    Object.defineProperty(window, 'parent', { value: realParent, configurable: true });
    Object.defineProperty(document, 'referrer', { value: '', configurable: true });
  });

  function framedBy(origin: string) {
    const parent = { postMessage: vi.fn() };
    Object.defineProperty(window, 'parent', { value: parent, configurable: true });
    Object.defineProperty(document, 'referrer', { value: `${origin}/hub`, configurable: true });
    return parent;
  }

  it('is inert when not framed', () => {
    const { result } = renderHook(() =>
      useEmbedReady({ allowedParentOrigins: ['https://hub.example.com'] }),
    );
    expect(result.current.embedded).toBe(false);
  });

  it('announces ready to an allowed parent, answers ping and hands focus back', () => {
    const parent = framedBy('https://hub.example.com');
    const { result, rerender } = renderHook(
      ({ ready }) =>
        useEmbedReady({
          allowedParentOrigins: ['https://hub.example.com'],
          ready,
          autoHeight: false,
        }),
      { initialProps: { ready: false } },
    );
    expect(result.current.embedded).toBe(true);
    expect(parent.postMessage).not.toHaveBeenCalled();
    rerender({ ready: true });
    expect(parent.postMessage).toHaveBeenCalledWith(msg('ready'), 'https://hub.example.com');

    // jsdom's MessageEvent only takes real windows as `source`: fake the event.
    const fromParent = (origin: string) =>
      act(() => {
        window.dispatchEvent(
          Object.assign(new Event('message'), { data: msg('ping'), origin, source: parent }),
        );
      });
    parent.postMessage.mockClear();
    fromParent('https://evil.example.net');
    expect(parent.postMessage).not.toHaveBeenCalled();
    fromParent('https://hub.example.com');
    expect(parent.postMessage).toHaveBeenCalledWith(msg('ready'), 'https://hub.example.com');

    act(() => result.current.exitFocus('backward'));
    expect(parent.postMessage).toHaveBeenCalledWith(
      msg('focus-exit', { direction: 'backward' }),
      'https://hub.example.com',
    );
  });

  it('never talks to a parent that is not allowed', () => {
    const parent = framedBy('https://evil.example.net');
    const { result } = renderHook(() =>
      useEmbedReady({ allowedParentOrigins: ['https://hub.example.com'] }),
    );
    expect(result.current.embedded).toBe(false);
    act(() => result.current.exitFocus('forward'));
    expect(parent.postMessage).not.toHaveBeenCalled();
  });
});

describe('EmbeddedAppFrame waits', () => {
  it('does not time out while readyTimeout is 0', async () => {
    render(<EmbeddedAppFrame src={SRC} title="Tickets: Email settings" readyTimeout={0} />);
    await new Promise((r) => setTimeout(r, 40));
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
  });
});
