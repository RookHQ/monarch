import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { fireEvent, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SplitInboxContextToggle, SplitInboxLayout, useSplitInboxLayout } from './index.js';

// jsdom has no PointerEvent; a MouseEvent subclass carries clientX and button.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventStub extends MouseEvent {
    pointerId: number;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerId = init.pointerId ?? 1;
    }
  }
  window.PointerEvent = PointerEventStub as unknown as typeof PointerEvent;
}

const original = window.matchMedia;
/** Fakes the two layout breakpoints: `single` below 768px, `drawer` below 1280px. */
function mockScreen({ single = false, drawer = false }: { single?: boolean; drawer?: boolean }) {
  window.matchMedia = ((query: string) => ({
    matches: query.includes('767') ? single : query.includes('1279') ? drawer : false,
    media: query,
    onchange: null,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia;
}
beforeEach(() => mockScreen({}));
afterEach(() => {
  window.matchMedia = original;
});

function Inbox(props: Partial<Parameters<typeof SplitInboxLayout>[0]>) {
  return (
    <SplitInboxLayout
      listLabel="Conversations"
      detailLabel="Conversation"
      contextLabel="Ticket details"
      list={
        <ul>
          <li>
            <button type="button">Printer offline</button>
          </li>
          <li>
            <button type="button">Invoice question</button>
          </li>
        </ul>
      }
      detail={
        <div>
          <h2>Printer offline</h2>
          <SplitInboxContextToggle />
          <button type="button">Reply</button>
        </div>
      }
      context={<button type="button">Assignee</button>}
      {...props}
    />
  );
}

describe('SplitInboxLayout', () => {
  it('renders the three panes as labelled landmarks', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<Inbox ref={ref} data-testid="root" />);
    expect(ref.current).toBe(screen.getByTestId('root'));
    expect(ref.current).toHaveAttribute('data-layout', 'split');
    expect(screen.getByRole('complementary', { name: 'Conversations' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Conversation' })).toBeInTheDocument();
    expect(screen.getByRole('complementary', { name: 'Ticket details' })).toBeInTheDocument();
    expect(screen.getAllByRole('separator')).toHaveLength(2);
    await expectNoA11yViolations();
  });

  it('resizes the list with the keyboard and collapses it with Enter (uncontrolled)', async () => {
    const onListSizeChange = vi.fn();
    const onListCollapsedChange = vi.fn();
    render(
      <Inbox
        onListSizeChange={onListSizeChange}
        onListCollapsedChange={onListCollapsedChange}
        context={undefined}
      />,
    );
    const splitter = screen.getByRole('separator', { name: 'Resize list' });
    const list = screen.getByRole('complementary', { name: 'Conversations' });
    expect(splitter).toHaveAttribute('aria-controls', list.id);
    expect(splitter).toHaveAttribute('aria-orientation', 'vertical');
    expect(splitter).toHaveAttribute('aria-valuenow', '360');
    expect(splitter).toHaveAttribute('aria-valuemin', '240');
    expect(splitter).toHaveAttribute('aria-valuemax', '560');
    const user = userEvent.setup();
    splitter.focus();
    await user.keyboard('{ArrowRight}');
    expect(splitter).toHaveAttribute('aria-valuenow', '376');
    await user.keyboard('{Shift>}{ArrowLeft}{/Shift}');
    expect(splitter).toHaveAttribute('aria-valuenow', '312');
    await user.keyboard('{End}');
    expect(splitter).toHaveAttribute('aria-valuenow', '560');
    await user.keyboard('{ArrowRight}');
    expect(splitter).toHaveAttribute('aria-valuenow', '560');
    await user.keyboard('{Home}');
    expect(onListSizeChange).toHaveBeenLastCalledWith(240);

    await user.keyboard('{Enter}');
    const root = list.parentElement as HTMLElement;
    expect(root).toHaveAttribute('data-list-collapsed');
    expect(splitter).toHaveAttribute('data-collapsed');
    expect(onListCollapsedChange).toHaveBeenLastCalledWith(true);
    expect(splitter).toHaveFocus();
    // An arrow key on a collapsed splitter expands it again.
    await user.keyboard('{ArrowRight}');
    expect(root).not.toHaveAttribute('data-list-collapsed');
    expect(splitter).toHaveAttribute('aria-valuenow', '256');
  });

  it('controlled sizes only change through the parent', async () => {
    const onListSizeChange = vi.fn();
    render(<Inbox listSize={300} onListSizeChange={onListSizeChange} />);
    const splitter = screen.getByRole('separator', { name: 'Resize list' });
    splitter.focus();
    await userEvent.setup().keyboard('{ArrowRight}');
    expect(onListSizeChange).toHaveBeenCalledWith(316);
    expect(splitter).toHaveAttribute('aria-valuenow', '300');
    expect(splitter.parentElement?.style.getPropertyValue('--_list')).toBe('300px');
  });

  it('resizes with the pointer, clamped to the limits', () => {
    render(<Inbox resizeStep={10} />);
    const splitter = screen.getByRole('separator', { name: 'Resize list' });
    fireEvent.pointerDown(splitter, { button: 0, clientX: 100, pointerId: 1 });
    expect(splitter).toHaveFocus();
    fireEvent.pointerMove(splitter, { clientX: 140, pointerId: 1 });
    expect(splitter).toHaveAttribute('aria-valuenow', '400');
    fireEvent.pointerMove(splitter, { clientX: 1000, pointerId: 1 });
    expect(splitter).toHaveAttribute('aria-valuenow', '560');
    fireEvent.pointerUp(splitter, { pointerId: 1 });
    fireEvent.pointerMove(splitter, { clientX: 0, pointerId: 1 });
    expect(splitter).toHaveAttribute('aria-valuenow', '560');

    // The context splitter grows its pane when dragged towards the start.
    const ctx = screen.getByRole('separator', { name: 'Resize details' });
    fireEvent.pointerDown(ctx, { button: 0, clientX: 500, pointerId: 2 });
    fireEvent.pointerMove(ctx, { clientX: 460, pointerId: 2 });
    expect(ctx).toHaveAttribute('aria-valuenow', '380');
  });

  it('the context splitter: ArrowLeft widens, Enter closes and focus moves to the detail', async () => {
    const onContextOpenChange = vi.fn();
    render(<Inbox onContextOpenChange={onContextOpenChange} />);
    const splitter = screen.getByRole('separator', { name: 'Resize details' });
    const user = userEvent.setup();
    splitter.focus();
    await user.keyboard('{ArrowLeft}');
    expect(splitter).toHaveAttribute('aria-valuenow', '356');
    await user.keyboard('{Enter}');
    expect(onContextOpenChange).toHaveBeenLastCalledWith(false);
    expect(screen.queryByRole('complementary', { name: 'Ticket details' })).toBeNull();
    expect(screen.getByRole('region', { name: 'Conversation' })).toHaveFocus();
  });

  it('SplitInboxContextToggle shows and hides the context column on wide screens', async () => {
    render(<Inbox defaultContextOpen={false} />);
    const toggle = screen.getByRole('button', { name: 'Show details' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('complementary', { name: 'Ticket details' })).toBeNull();
    const user = userEvent.setup();
    await user.click(toggle);
    const pane = screen.getByRole('complementary', { name: 'Ticket details' });
    expect(toggle).toHaveAccessibleName('Hide details');
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(toggle).toHaveAttribute('aria-controls', pane.id);
    await user.click(toggle);
    expect(screen.queryByRole('complementary', { name: 'Ticket details' })).toBeNull();
  });

  it('below 1280px the context pane is a modal drawer, closed at first', async () => {
    mockScreen({ drawer: true });
    render(<Inbox defaultContextOpen />);
    expect(screen.queryByRole('complementary', { name: 'Ticket details' })).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getAllByRole('separator')).toHaveLength(1);
    const user = userEvent.setup();
    const toggle = screen.getByRole('button', { name: 'Show details' });
    await user.click(toggle);
    const dialog = await screen.findByRole('dialog', { name: 'Ticket details' });
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(dialog).toContainElement(screen.getByRole('button', { name: 'Assignee' }));
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    expect(toggle).toHaveFocus();
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
  });

  it('below 768px shows one pane, with back navigation and focus moving between panes', async () => {
    mockScreen({ single: true, drawer: true });
    function Single() {
      const [open, setOpen] = useState(false);
      return (
        <Inbox
          detailOpen={open}
          onBack={() => setOpen(false)}
          list={
            <button type="button" onClick={() => setOpen(true)}>
              Printer offline
            </button>
          }
        />
      );
    }
    render(<Single />);
    const root = document.querySelector('.mn-split-inbox') as HTMLElement;
    expect(root).toHaveAttribute('data-layout', 'single');
    const user = userEvent.setup();
    const item = screen.getByRole('button', { name: 'Printer offline' });
    await user.click(item);
    expect(root).toHaveAttribute('data-detail-open');
    const detail = screen.getByRole('region', { name: 'Conversation' });
    await waitFor(() => expect(detail).toHaveFocus());
    await user.click(screen.getByRole('button', { name: 'Back to list' }));
    expect(root).not.toHaveAttribute('data-detail-open');
    // Focus returns to the item that opened the detail.
    await waitFor(() => expect(item).toHaveFocus());
  });

  it('F6 and Shift+F6 move focus between the visible panes, back to the last focused element', async () => {
    render(<Inbox />);
    const user = userEvent.setup();
    const reply = screen.getByRole('button', { name: 'Reply' });
    await user.click(reply);
    const first = screen.getByRole('button', { name: 'Printer offline' });
    await user.click(first);
    await user.keyboard('{F6}');
    expect(reply).toHaveFocus();
    await user.keyboard('{F6}');
    expect(screen.getByRole('complementary', { name: 'Ticket details' })).toHaveFocus();
    await user.keyboard('{F6}');
    expect(first).toHaveFocus();
    await user.keyboard('{Shift>}{F6}{/Shift}');
    expect(screen.getByRole('complementary', { name: 'Ticket details' })).toHaveFocus();
  });

  it('collapsed list (focus mode) leaves list out of F6; useSplitInboxLayout exposes the state', async () => {
    let state: ReturnType<typeof useSplitInboxLayout> = null;
    function Probe() {
      state = useSplitInboxLayout();
      return <button type="button">Probe</button>;
    }
    render(<Inbox listCollapsed resizable={false} context={undefined} detail={<Probe />} />);
    expect(screen.queryByRole('separator')).toBeNull();
    expect(state).toMatchObject({ layout: 'split', contextMode: 'inline', listCollapsed: true });
    const probe = screen.getByRole('button', { name: 'Probe' });
    probe.focus();
    await userEvent.setup().keyboard('{F6}');
    expect(probe).toHaveFocus();
    expect(renderHook(() => useSplitInboxLayout()).result.current).toBeNull();
  });

  it('renders on the server in the wide layout', () => {
    const html = renderToString(<Inbox detailOpen onBack={() => {}} />);
    expect(html).toContain('mn-split-inbox');
    expect(html).toContain('data-layout="split"');
    expect(html).toContain('Back to list');
  });
});
