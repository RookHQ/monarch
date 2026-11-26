import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiComputerView,
  aiComputerViewActionDetail,
  aiComputerViewActionLabel,
  aiComputerViewFramePoint,
  aiComputerViewPointPosition,
  type AiComputerAction,
  type AiComputerFrame,
  type AiComputerViewProps,
} from './index.js';

const frame: AiComputerFrame = {
  src: 'https://example.com/frames/12.png',
  width: 1280,
  height: 800,
  capturedAt: '2026-01-01T12:00:09Z',
  alt: 'A sign-in page',
};

const actions: AiComputerAction[] = [
  {
    id: 'a1',
    kind: 'navigate',
    label: 'Opened the supplier portal',
    at: '2026-01-01T12:00:01Z',
    detail: 'https://example.com/portal',
  },
  {
    id: 'a2',
    kind: 'type',
    label: 'Typed in the search box',
    at: '2026-01-01T12:00:04Z',
    point: { x: 320, y: 200 },
    detail: 'invoice 4417',
  },
  {
    id: 'a3',
    kind: 'type',
    label: 'Typed in the password field',
    at: '2026-01-01T12:00:06Z',
    detail: 'hunter2',
    sensitive: true,
  },
  {
    id: 'a4',
    kind: 'click',
    label: 'Clicked Sign in',
    at: '2026-01-01T12:00:08Z',
    point: { x: 640, y: 600 },
  },
];

const base = { frame, actions, status: 'working', timeZone: 'UTC', locale: 'en-GB' } as const;

function setup(props: Partial<AiComputerViewProps> = {}) {
  const user = userEvent.setup();
  const view = render(<AiComputerView data-testid="root" {...base} {...props} />);
  return { user, ...view, root: screen.getByTestId('root') };
}

afterEach(() => {
  vi.useRealTimers();
});

describe('aiComputerViewPointPosition', () => {
  it('turns frame pixels into percentages of the frame', () => {
    expect(aiComputerViewPointPosition({ x: 320, y: 200 }, frame)).toEqual({ left: 25, top: 25 });
    expect(aiComputerViewPointPosition({ x: 0, y: 800 }, frame)).toEqual({ left: 0, top: 100 });
    expect(aiComputerViewPointPosition({ x: 1, y: 1 }, { width: 3, height: 3 })).toEqual({
      left: 33.33,
      top: 33.33,
    });
  });

  it('clamps points outside the frame and rejects unusable input', () => {
    expect(aiComputerViewPointPosition({ x: -50, y: 9999 }, frame)).toEqual({ left: 0, top: 100 });
    expect(aiComputerViewPointPosition(undefined, frame)).toBeNull();
    expect(aiComputerViewPointPosition({ x: 1, y: 1 }, null)).toBeNull();
    expect(aiComputerViewPointPosition({ x: 1, y: 1 }, { width: 0, height: 800 })).toBeNull();
    expect(aiComputerViewPointPosition({ x: Number.NaN, y: 1 }, frame)).toBeNull();
  });
});

describe('aiComputerViewActionDetail', () => {
  it('withholds typed text unless asked, and sensitive detail always', () => {
    const typed = { kind: 'type', detail: 'invoice 4417' } as const;
    expect(aiComputerViewActionDetail(typed)).toEqual({ text: undefined, hidden: true });
    expect(aiComputerViewActionDetail(typed, true)).toEqual({
      text: 'invoice 4417',
      hidden: false,
    });
    const secret = { kind: 'type', detail: 'hunter2', sensitive: true } as const;
    expect(aiComputerViewActionDetail(secret, true)).toEqual({ text: undefined, hidden: true });
    expect(aiComputerViewActionDetail({ kind: 'click', sensitive: true })).toEqual({
      text: undefined,
      hidden: true,
    });
    expect(aiComputerViewActionDetail({ kind: 'navigate', detail: '/portal' })).toEqual({
      text: '/portal',
      hidden: false,
    });
    expect(aiComputerViewActionDetail({ kind: 'click' })).toEqual({
      text: undefined,
      hidden: false,
    });
  });
});

describe('AiComputerView', () => {
  it('renders a named region with status, banner, window bar, screenshot and log', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiComputerView
        ref={ref}
        data-testid="root"
        className="extra"
        {...base}
        windowTitle="Supplier portal"
        currentUrl="https://user:secret@example.com/portal"
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-computer-view', 'extra');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'Assistant browser view' })).toBe(root);
    expect(root).toHaveAttribute('data-status', 'working');
    expect(root).toHaveAttribute('data-controller', 'agent');
    expect(root).toHaveTextContent('Working');
    expect(root).toHaveTextContent('The assistant is controlling this browser');
    expect(root).toHaveTextContent('Supplier portal');
    expect(root).toHaveTextContent('Address: https://example.com/portal');
    expect(root).not.toHaveTextContent('secret@');
    expect(root).toHaveTextContent('Screenshot from 12:00:09');

    const log = screen.getByRole('log', { name: 'Actions' });
    const items = within(log).getAllByRole('listitem');
    expect(items.map((li) => li.querySelector('.mn-ai-computer-view__label')?.textContent)).toEqual(
      [
        'Navigate: Opened the supplier portal',
        'Type: Typed in the search box',
        // A sensitive action is never named.
        'Type: Private action',
        'Click: Clicked Sign in',
      ],
    );
    expect(items[3]).toHaveTextContent('12:00:08');
    await expectNoA11yViolations();
  });

  it('reserves the frame box and gives the screenshot a text alternative', () => {
    const { root, rerender } = setup();
    const box = root.querySelector<HTMLElement>('.mn-ai-computer-view__frame')!;
    expect(box.style.aspectRatio).toBe('1280 / 800');
    const img = screen.getByRole('img');
    expect(img).toHaveAttribute('src', frame.src);
    expect(img).toHaveAttribute('width', '1280');
    expect(img).toHaveAttribute('height', '800');
    expect(img).toHaveAccessibleName('A sign-in page. Latest action: Clicked Sign in');

    rerender(
      <AiComputerView
        data-testid="root"
        {...base}
        frame={{ ...frame, alt: undefined }}
        actions={[]}
      />,
    );
    expect(screen.getByRole('img')).toHaveAccessibleName(
      'Screenshot of the browser the assistant is using',
    );
  });

  it('never renders an unsafe screenshot URL and still describes the frame', () => {
    const { root, rerender } = setup({ frame: { ...frame, src: 'javascript:alert(1)' } });
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(root.querySelector('.mn-ai-computer-view__overlay')).toBeNull();
    expect(root).toHaveTextContent('Screenshot unavailable');
    expect(root).toHaveTextContent('A sign-in page. Latest action: Clicked Sign in');

    rerender(
      <AiComputerView data-testid="root" {...base} frame={{ ...frame, src: 'data:image/png,x' }} />,
    );
    expect(screen.queryByRole('img')).not.toBeInTheDocument();

    rerender(<AiComputerView data-testid="root" {...base} status="starting" frame={null} />);
    expect(root).toHaveTextContent('No screenshot yet');
    expect(root.querySelector<HTMLElement>('.mn-ai-computer-view__frame')!.style.aspectRatio).toBe(
      '16 / 10',
    );
  });

  it('marks the last action point and the pointer in a decorative overlay', () => {
    const { root } = setup({ pointer: { x: 960, y: 400 } });
    const overlay = root.querySelector('.mn-ai-computer-view__overlay')!;
    expect(overlay).toHaveAttribute('aria-hidden', 'true');
    const last = overlay.querySelector<HTMLElement>('[data-marker="last"]')!;
    expect(last.style.left).toBe('50%');
    expect(last.style.top).toBe('75%');
    const pointer = overlay.querySelector<HTMLElement>('.mn-ai-computer-view__pointer')!;
    expect(pointer.style.left).toBe('75%');
    expect(pointer.style.top).toBe('50%');
    expect(overlay.querySelector('[data-marker="selected"]')).toBeNull();
  });

  it('hides sensitive detail and typed text by default', () => {
    const { root } = setup();
    expect(root).not.toHaveTextContent('hunter2');
    expect(root).not.toHaveTextContent('invoice 4417');
    expect(root).toHaveTextContent('https://example.com/portal');
    const hidden = root.querySelectorAll('.mn-ai-computer-view__detail[data-hidden]');
    expect(hidden).toHaveLength(2);
    expect(hidden[0]).toHaveTextContent('Hidden');
  });

  it('shows typed text with showTypedText but never a sensitive detail', () => {
    const { root } = setup({ showTypedText: true });
    expect(root).toHaveTextContent('invoice 4417');
    expect(root).not.toHaveTextContent('hunter2');
    expect(root.querySelectorAll('.mn-ai-computer-view__detail[data-hidden]')).toHaveLength(1);
  });

  it('pauses, resumes and takes over from the keyboard', async () => {
    const onPauseChange = vi.fn();
    const onTakeOver = vi.fn();
    const { user, rerender } = setup({ onPauseChange, onTakeOver });
    await user.tab();
    expect(screen.getByRole('button', { name: 'Take over' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onTakeOver).toHaveBeenCalledTimes(1);
    await user.tab();
    expect(screen.getByRole('button', { name: 'Pause' })).toHaveFocus();
    await user.keyboard(' ');
    expect(onPauseChange).toHaveBeenLastCalledWith(true);

    rerender(
      <AiComputerView
        data-testid="root"
        {...base}
        status="paused"
        onPauseChange={onPauseChange}
        onTakeOver={onTakeOver}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Pause' })).not.toBeInTheDocument();
    expect(screen.getByTestId('root')).not.toHaveTextContent('The assistant is controlling');
    await user.click(screen.getByRole('button', { name: 'Resume' }));
    expect(onPauseChange).toHaveBeenLastCalledWith(false);
  });

  it('offers Hand back while taken over and says who is in control', async () => {
    const onHandBack = vi.fn();
    const onTakeOver = vi.fn();
    const { user, root } = setup({
      status: 'taken-over',
      onHandBack,
      onTakeOver,
      onPauseChange: vi.fn(),
    });
    expect(root).toHaveAttribute('data-controller', 'you');
    expect(root).toHaveTextContent('You are controlling this browser');
    expect(screen.queryByRole('button', { name: 'Take over' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Pause' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Hand back' }));
    expect(onHandBack).toHaveBeenCalledTimes(1);
  });

  it('stops only after the inline confirmation, and Escape or Keep going backs out', async () => {
    const onStop = vi.fn();
    const { user } = setup({ onStop, onPauseChange: vi.fn() });
    await user.click(screen.getByRole('button', { name: 'Stop' }));
    expect(onStop).not.toHaveBeenCalled();
    const group = screen.getByRole('group', {
      name: 'Stop the assistant? It will not finish the task.',
    });
    expect(within(group).getByRole('button', { name: 'Stop now' })).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Pause' })).not.toBeInTheDocument();
    await expectNoA11yViolations();

    await user.keyboard('{Escape}');
    expect(
      screen.queryByRole('group', { name: 'Stop the assistant? It will not finish the task.' }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Stop' })).toHaveFocus();

    await user.keyboard('{Enter}');
    await user.click(screen.getByRole('button', { name: 'Keep going' }));
    expect(onStop).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Stop' })).toHaveFocus();

    await user.keyboard('{Enter}');
    await user.keyboard('{Enter}');
    expect(onStop).toHaveBeenCalledTimes(1);
    expect(
      screen.queryByRole('group', { name: 'Stop the assistant? It will not finish the task.' }),
    ).not.toBeInTheDocument();
  });

  it('keeps Stop until the session has finished, then removes every control', () => {
    const handlers = {
      onStop: vi.fn(),
      onPauseChange: vi.fn(),
      onTakeOver: vi.fn(),
      onHandBack: vi.fn(),
    };
    const { rerender } = setup(handlers);
    for (const status of [
      'starting',
      'working',
      'paused',
      'waiting-for-you',
      'taken-over',
    ] as const) {
      rerender(<AiComputerView {...base} {...handlers} status={status} />);
      expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();
    }
    for (const status of ['done', 'failed', 'stopped'] as const) {
      rerender(<AiComputerView {...base} {...handlers} status={status} />);
      expect(screen.queryByRole('button', { name: /Stop|Pause|Resume|Take over|Hand back/ })).toBe(
        null,
      );
    }
  });

  it('explains why it is waiting and makes Take over the primary action', async () => {
    const { root, rerender } = setup({
      status: 'waiting-for-you',
      waitingReason: 'Sign in to the supplier portal to continue.',
      onTakeOver: vi.fn(),
      onPauseChange: vi.fn(),
    });
    expect(root).toHaveTextContent('Waiting for you');
    expect(root).toHaveTextContent('The assistant needs you to continue');
    expect(root).toHaveTextContent('Sign in to the supplier portal to continue.');
    expect(root).not.toHaveAttribute('data-controller');
    expect(screen.getByRole('button', { name: 'Take over' })).toHaveAttribute(
      'data-variant',
      'primary',
    );
    expect(screen.queryByRole('button', { name: 'Pause' })).not.toBeInTheDocument();
    await expectNoA11yViolations();

    rerender(
      <AiComputerView data-testid="root" {...base} status="waiting-for-you" onTakeOver={vi.fn()} />,
    );
    expect(root).toHaveTextContent('This step is one the assistant must not do itself.');
  });

  it('selects an action (uncontrolled) and highlights its point', async () => {
    const onSelectAction = vi.fn();
    const { user, root } = setup({ onSelectAction });
    const row = screen.getByRole('button', { name: /Typed in the search box/ });
    expect(row).toHaveAttribute('aria-pressed', 'false');
    await user.click(row);
    expect(onSelectAction).toHaveBeenCalledWith('a2');
    expect(row).toHaveAttribute('aria-pressed', 'true');
    const selected = root.querySelector<HTMLElement>('[data-marker="selected"]')!;
    expect(selected.style.left).toBe('25%');
    expect(selected.style.top).toBe('25%');
    // The last action keeps its own marker.
    expect(root.querySelector('[data-marker="last"]')).not.toBeNull();
  });

  it('highlights only the selectedActionId the product passes (controlled)', async () => {
    const onSelectAction = vi.fn();
    const { user, root } = setup({ onSelectAction, selectedActionId: 'a4' });
    expect(screen.getByRole('button', { name: /Clicked Sign in/ })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    // The selected action is also the last one: one marker, not two.
    expect(root.querySelectorAll('.mn-ai-computer-view__marker')).toHaveLength(1);
    await user.click(screen.getByRole('button', { name: /Typed in the search box/ }));
    expect(onSelectAction).toHaveBeenCalledWith('a2');
    expect(screen.getByRole('button', { name: /Typed in the search box/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('keeps the log at the newest action unless the person scrolled up', () => {
    const { rerender } = setup();
    const log = screen.getByRole('log');
    Object.defineProperty(log, 'scrollHeight', { configurable: true, value: 900 });
    Object.defineProperty(log, 'clientHeight', { configurable: true, value: 200 });
    const more = (n: number): AiComputerAction[] => [
      ...actions,
      ...Array.from({ length: n }, (_, i) => ({
        id: `n${i}`,
        kind: 'scroll' as const,
        label: `Scrolled ${i}`,
        at: '2026-01-01T12:00:10Z',
      })),
    ];
    rerender(<AiComputerView data-testid="root" {...base} actions={more(1)} />);
    expect(log.scrollTop).toBe(900);

    log.scrollTop = 100;
    act(() => {
      log.dispatchEvent(new Event('scroll', { bubbles: true }));
    });
    rerender(<AiComputerView data-testid="root" {...base} actions={more(2)} />);
    expect(log.scrollTop).toBe(100);

    log.scrollTop = 690;
    act(() => {
      log.dispatchEvent(new Event('scroll', { bubbles: true }));
    });
    rerender(<AiComputerView data-testid="root" {...base} actions={more(3)} />);
    expect(log.scrollTop).toBe(900);
  });

  it('announces new actions politely and throttled, never the first list', () => {
    vi.useFakeTimers();
    const { root, rerender } = setup({ announceInterval: 4000 });
    const [statusRegion, actionRegion] = Array.from(
      root.querySelectorAll<HTMLElement>(':scope > [role="status"]'),
    );
    expect(screen.getByRole('log')).toHaveAttribute('aria-live', 'off');
    expect(actionRegion).toHaveAttribute('aria-live', 'polite');
    expect(actionRegion).toBeEmptyDOMElement();
    const add = (...labels: string[]) =>
      labels.map((label) => ({ id: label, kind: 'click' as const, label, at: actions[3]!.at }));

    let list = [...actions, ...add('Clicked Next')];
    rerender(<AiComputerView data-testid="root" {...base} actions={list} />);
    expect(actionRegion).toHaveTextContent('Clicked Next');

    list = [...list, ...add('Scrolled down')];
    rerender(<AiComputerView data-testid="root" {...base} actions={list} />);
    list = [...list, ...add('Opened Invoices', 'Read the table')];
    rerender(<AiComputerView data-testid="root" {...base} actions={list} />);
    expect(actionRegion).toHaveTextContent('Clicked Next');

    act(() => {
      vi.advanceTimersByTime(4000);
    });
    expect(actionRegion).toHaveTextContent('3 new actions. Latest: Read the table');
    expect(statusRegion).toBeEmptyDOMElement();
  });

  it('announces each status change once, urgent ones as an alert', () => {
    const { root, rerender } = setup();
    const status = root.querySelector<HTMLElement>(':scope > [role="status"]')!;
    const alert = screen.getByRole('alert');
    expect(status).toBeEmptyDOMElement();
    expect(alert).toBeEmptyDOMElement();

    rerender(<AiComputerView data-testid="root" {...base} status="paused" />);
    expect(status).toHaveTextContent('Assistant status: Paused');
    rerender(<AiComputerView data-testid="root" {...base} status="paused" actions={[]} />);
    expect(status).toHaveTextContent('Assistant status: Paused');

    rerender(<AiComputerView data-testid="root" {...base} status="waiting-for-you" />);
    expect(alert).toHaveTextContent('Assistant status: Waiting for you');
    expect(status).toBeEmptyDOMElement();

    rerender(<AiComputerView data-testid="root" {...base} status="done" />);
    expect(status).toHaveTextContent('Assistant status: Done');
    expect(alert).toBeEmptyDOMElement();
  });

  it('hides the log behind a disclosure when compact (uncontrolled and controlled)', async () => {
    const onLogOpenChange = vi.fn();
    const { user, root, unmount } = setup({ compact: true, onLogOpenChange });
    expect(root).toHaveAttribute('data-compact', 'true');
    const toggle = screen.getByRole('button', { name: 'Actions (4)' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('log')).not.toBeInTheDocument();
    await user.click(toggle);
    expect(onLogOpenChange).toHaveBeenLastCalledWith(true);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('log')).toBeVisible();
    expect(toggle).toHaveAttribute('aria-controls', screen.getByRole('log').id);
    await expectNoA11yViolations();
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(onLogOpenChange).toHaveBeenLastCalledWith(false);
    expect(screen.queryByRole('log')).not.toBeInTheDocument();
    unmount();

    function Controlled() {
      const [open, setOpen] = useState(true);
      return <AiComputerView {...base} compact logOpen={open} onLogOpenChange={setOpen} />;
    }
    render(<Controlled />);
    expect(screen.getByRole('log')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Actions (4)' }));
    expect(screen.queryByRole('log')).not.toBeInTheDocument();
  });

  it('uses translated strings and the computer wording', () => {
    const { root } = setup({
      surface: 'computer',
      status: 'working',
      onStop: vi.fn(),
      actions: [],
      messages: {
        statuses: { working: 'Aan het werk' },
        agentInControl: (surface) => `De assistent bestuurt deze ${surface}`,
        stop: 'Stoppen',
        empty: 'Nog geen acties.',
        logLabel: 'Acties',
      },
    });
    expect(screen.getByRole('group', { name: 'Assistant computer view' })).toBe(root);
    expect(root).toHaveTextContent('Aan het werk');
    expect(root).toHaveTextContent('De assistent bestuurt deze computer');
    expect(screen.getByRole('button', { name: 'Stoppen' })).toBeInTheDocument();
    expect(screen.getByRole('log', { name: 'Acties' })).toHaveTextContent('Nog geen acties.');
  });

  it('formats times with the locale and time zone', () => {
    const { root } = setup({ locale: 'en-US', timeZone: 'America/New_York' });
    expect(root).toHaveTextContent(/7:00:08\sAM/);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiComputerView {...base} pointer={{ x: 10, y: 10 }} onStop={() => {}} />,
    );
    expect(html).toContain('mn-ai-computer-view');
    expect(html).toContain('role="log"');
    expect(html).toContain('The assistant is controlling this browser');
    expect(html).not.toContain('hunter2');
  });

  it('hides the screenshot when the frame or the latest action is private', () => {
    const { rerender } = render(<AiComputerView {...base} frame={{ ...frame, sensitive: true }} />);
    expect(screen.queryByRole('img')).toBeNull();
    expect(
      document.querySelector('.mn-ai-computer-view__placeholder[data-hidden]'),
    ).toHaveTextContent(/Screenshot hidden/);
    rerender(<AiComputerView {...base} actions={actions.slice(0, 3)} />);
    expect(screen.queryByRole('img')).toBeNull();
    rerender(<AiComputerView {...base} />);
    expect(screen.getByRole('img')).toBeInTheDocument();
    expect(aiComputerViewActionLabel({ label: 'Typed card number', sensitive: true })).toBe(
      'Private action',
    );
  });

  it('never announces a sensitive label', () => {
    vi.useFakeTimers();
    const { rerender } = render(
      <AiComputerView {...base} actions={actions.slice(0, 2)} announceInterval={0} />,
    );
    rerender(<AiComputerView {...base} actions={actions.slice(0, 3)} announceInterval={0} />);
    const live = document.querySelectorAll('[role="status"]');
    const text = Array.from(live)
      .map((n) => n.textContent)
      .join(' ');
    expect(text).toContain('Private action');
    expect(text).not.toContain('password');
    vi.useRealTimers();
  });

  it('renders only the newest actions of a long session until asked', async () => {
    const user = userEvent.setup();
    const many: AiComputerAction[] = Array.from({ length: 30 }, (_, i) => ({
      id: `x${i}`,
      kind: 'click',
      label: `Step ${i}`,
      at: '2026-01-01T12:00:00Z',
    }));
    render(<AiComputerView {...base} actions={many} visibleActions={10} />);
    const log = screen.getByRole('log', { name: 'Actions' });
    expect(within(log).getAllByRole('listitem')).toHaveLength(10);
    expect(within(log).getByText('Step 29')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Show 20 earlier actions' }));
    expect(within(log).getAllByRole('listitem')).toHaveLength(30);
  });

  it('renders only the stream while it has a live video track and falls back to the frame', async () => {
    const { stream, track } = fakeStream();
    const { root, rerender } = setup({ stream });
    const video = root.querySelector('video')!;
    expect(video).toBeInTheDocument();
    expect(video.muted).toBe(true);
    expect((video as HTMLVideoElement & { srcObject: unknown }).srcObject).toBe(stream);
    expect(video).toHaveAttribute('aria-label', 'A sign-in page. Latest action: Clicked Sign in');
    expect(root.querySelector('img')).not.toBeInTheDocument();
    expect(root).toHaveAttribute('data-live', 'true');
    expect(root).toHaveTextContent('Live');
    expect(root).not.toHaveTextContent('Screenshot from');
    await expectNoA11yViolations();

    // The track ends: the screenshot takes over again.
    act(() => {
      track.readyState = 'ended';
      track.dispatchEvent(new Event('ended'));
    });
    expect(root.querySelector('video')).not.toBeInTheDocument();
    expect(root.querySelector('img')).toHaveAttribute('src', frame.src);
    expect(root).not.toHaveAttribute('data-live');

    rerender(<AiComputerView data-testid="root" {...base} stream={null} />);
    expect(root.querySelector('img')).toBeInTheDocument();
  });

  it('detaches the stream on unmount without stopping its tracks', () => {
    const { stream, track } = fakeStream();
    const { root, unmount } = setup({ stream });
    const video = root.querySelector('video') as HTMLVideoElement & { srcObject: unknown };
    unmount();
    expect(video.srcObject).toBeNull();
    expect(track.stop).not.toHaveBeenCalled();
    expect(track.listeners).toBe(0);
  });

  it('never shows a private live picture, nor one after a private action', () => {
    const { stream } = fakeStream();
    const { root, rerender } = setup({ stream, streamSensitive: true });
    expect(root.querySelector('video')).not.toBeInTheDocument();
    expect(root.querySelector('img')).not.toBeInTheDocument();
    expect(root).toHaveTextContent('Screenshot hidden: it may show private information');

    rerender(
      <AiComputerView data-testid="root" {...base} stream={stream} actions={actions.slice(0, 3)} />,
    );
    expect(root.querySelector('video')).not.toBeInTheDocument();
    rerender(<AiComputerView data-testid="root" {...base} frame={null} stream={stream} />);
    expect(root.querySelector('video')).toHaveAttribute(
      'aria-label',
      'Live view of the browser the assistant is using. Latest action: Clicked Sign in',
    );
  });

  it('converts page positions to frame pixels', () => {
    const box = { left: 100, top: 50, width: 640, height: 400 };
    const size = { width: 1280, height: 800 };
    expect(aiComputerViewFramePoint({ clientX: 420, clientY: 250 }, box, size)).toEqual({
      x: 640,
      y: 400,
    });
    expect(aiComputerViewFramePoint({ clientX: 0, clientY: 9999 }, box, size)).toEqual({
      x: 0,
      y: 800,
    });
    expect(aiComputerViewFramePoint({ clientX: 1, clientY: 1 }, { ...box, width: 0 }, size)).toBe(
      null,
    );
    expect(aiComputerViewFramePoint({ clientX: 1, clientY: 1 }, box, { width: 0, height: 1 })).toBe(
      null,
    );
    expect(aiComputerViewFramePoint({ clientX: Number.NaN, clientY: 1 }, box, size)).toBe(null);
  });

  it('forwards pointer and key input only while the person is in control', async () => {
    const onPointer = vi.fn();
    const onKey = vi.fn();
    const { root, rerender } = setup({ onPointer, onKey });
    const frameBox = () => root.querySelector<HTMLElement>('.mn-ai-computer-view__frame')!;
    // The agent holds control: the screen is not an input surface.
    expect(frameBox()).not.toHaveAttribute('tabindex');
    expect(screen.queryByRole('application')).not.toBeInTheDocument();
    pointer('pointerdown', frameBox(), { clientX: 10, clientY: 10 });
    fireEvent.keyDown(frameBox(), { key: 'a' });
    expect(onPointer).not.toHaveBeenCalled();
    expect(onKey).not.toHaveBeenCalled();

    rerender(
      <AiComputerView
        data-testid="root"
        {...base}
        status="taken-over"
        onPointer={onPointer}
        onKey={onKey}
      />,
    );
    const surface = screen.getByRole('application', {
      name: 'The browser. You are in control: your keys and pointer are sent to it',
    });
    expect(surface).toBe(frameBox());
    expect(surface).toHaveAttribute('tabindex', '0');
    expect(surface).toHaveAccessibleDescription(
      'Your keys and pointer go to this screen. Press Shift+Escape to leave it.',
    );
    surface.getBoundingClientRect = () =>
      ({
        left: 0,
        top: 0,
        width: 640,
        height: 400,
        right: 640,
        bottom: 400,
        x: 0,
        y: 0,
      }) as DOMRect;

    pointer('pointerdown', surface, { clientX: 320, clientY: 200, button: 0, buttons: 1 });
    expect(surface).toHaveFocus();
    expect(onPointer).toHaveBeenLastCalledWith({
      type: 'down',
      x: 640,
      y: 400,
      button: 0,
      buttons: 1,
      deltaX: 0,
      deltaY: 0,
      modifiers: { alt: false, ctrl: false, meta: false, shift: false },
    });
    pointer('pointermove', surface, { clientX: 64, clientY: 40 });
    expect(onPointer).toHaveBeenLastCalledWith(
      expect.objectContaining({ type: 'move', x: 128, y: 80, button: -1 }),
    );
    pointer('pointerup', surface, { clientX: 64, clientY: 40, button: 0 });
    expect(onPointer).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'up', button: 0 }));
    fireEvent.wheel(surface, { clientX: 64, clientY: 40, deltaY: 120 });
    expect(onPointer).toHaveBeenLastCalledWith(
      expect.objectContaining({ type: 'wheel', deltaY: 120, x: 128, y: 80 }),
    );

    const tab = fireEvent.keyDown(surface, { key: 'Tab', code: 'Tab', ctrlKey: true });
    // Forwarded keys do not also act on the page.
    expect(tab).toBe(false);
    expect(onKey).toHaveBeenLastCalledWith({
      type: 'down',
      key: 'Tab',
      code: 'Tab',
      repeat: false,
      modifiers: { alt: false, ctrl: true, meta: false, shift: false },
    });
    fireEvent.keyUp(surface, { key: 'Tab', code: 'Tab' });
    expect(onKey).toHaveBeenLastCalledWith(expect.objectContaining({ type: 'up', key: 'Tab' }));
    await expectNoA11yViolations();

    // A private picture is not shown, so nothing can be sent to it blind.
    onPointer.mockClear();
    rerender(
      <AiComputerView
        data-testid="root"
        {...base}
        frame={{ ...frame, sensitive: true }}
        status="taken-over"
        onPointer={onPointer}
        onKey={onKey}
      />,
    );
    expect(screen.queryByRole('application')).not.toBeInTheDocument();
    pointer('pointerdown', frameBox(), { clientX: 10, clientY: 10 });
    expect(onPointer).not.toHaveBeenCalled();
  });

  it('never forwards Shift+Escape: it moves focus out of the screen to Hand back', () => {
    const onKey = vi.fn();
    setup({ status: 'taken-over', onKey, onRelease: vi.fn(), onStop: vi.fn() });
    const surface = screen.getByRole('application');
    surface.focus();
    fireEvent.keyDown(surface, { key: 'Escape', code: 'Escape', shiftKey: true });
    fireEvent.keyUp(surface, { key: 'Escape', code: 'Escape', shiftKey: true });
    expect(onKey).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Hand back' })).toHaveFocus();
    // Stop stays available while the person is in control.
    expect(screen.getByRole('button', { name: 'Stop' })).toBeInTheDocument();
  });

  it('moves focus with control and announces who holds it', async () => {
    const onTakeOver = vi.fn();
    const onRelease = vi.fn();
    const onHandBack = vi.fn();
    function Harness() {
      const [status, setStatus] = useState<AiComputerViewProps['status']>('working');
      return (
        <AiComputerView
          data-testid="root"
          {...base}
          status={status}
          onStop={() => undefined}
          onTakeOver={() => {
            onTakeOver();
            setStatus('taken-over');
          }}
          onHandBack={onHandBack}
          onRelease={() => {
            onRelease();
            setStatus('working');
          }}
        />
      );
    }
    const user = userEvent.setup();
    render(<Harness />);
    const root = screen.getByTestId('root');
    const status = root.querySelector<HTMLElement>(':scope > [role="status"]')!;

    await user.click(screen.getByRole('button', { name: 'Take over' }));
    expect(onTakeOver).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Hand back' })).toHaveFocus();
    expect(root).toHaveAttribute('data-controller', 'you');
    expect(root).toHaveTextContent('You are in control');
    expect(status).toHaveTextContent(
      'You are in control of this browser. The assistant has stopped acting.',
    );

    await user.keyboard('{Enter}');
    expect(onRelease).toHaveBeenCalledTimes(1);
    expect(onHandBack).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Take over' })).toHaveFocus();
    expect(status).toHaveTextContent('The assistant is in control of this browser again.');
  });

  it('offers Hand back from onRelease alone and does not steal focus on outside changes', () => {
    const { rerender } = setup({ onRelease: vi.fn(), onTakeOver: vi.fn() });
    rerender(
      <AiComputerView
        data-testid="root"
        {...base}
        status="taken-over"
        onRelease={vi.fn()}
        onTakeOver={vi.fn()}
      />,
    );
    expect(screen.getByRole('button', { name: 'Hand back' })).not.toHaveFocus();
    expect(document.body).toHaveFocus();
  });
});

/** jsdom has no PointerEvent; a MouseEvent of the same type carries the same fields. */
function pointer(type: string, target: Element, init: MouseEventInit) {
  fireEvent(target, new MouseEvent(type, { bubbles: true, cancelable: true, ...init }));
}

function fakeStream() {
  class FakeTrack extends EventTarget {
    readyState: 'live' | 'ended' = 'live';
    listeners = 0;
    stop = vi.fn();
    override addEventListener(...args: Parameters<EventTarget['addEventListener']>) {
      this.listeners += 1;
      super.addEventListener(...args);
    }
    override removeEventListener(...args: Parameters<EventTarget['removeEventListener']>) {
      this.listeners -= 1;
      super.removeEventListener(...args);
    }
  }
  const track = new FakeTrack();
  class FakeStream extends EventTarget {
    getVideoTracks() {
      return [track];
    }
  }
  return { stream: new FakeStream() as unknown as MediaStream, track };
}
