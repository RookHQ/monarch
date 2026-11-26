import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuSeparator,
  ContextMenuTrigger,
  type ContextMenuProps,
  type ContextMenuTriggerProps,
} from './index.js';

// jsdom has no PointerEvent, so `pointerType` would be dropped from fired events.
if (typeof window.PointerEvent === 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    pointerType: string;
    constructor(type: string, init: PointerEventInit = {}) {
      super(type, init);
      this.pointerType = init.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventPolyfill as unknown as typeof PointerEvent;
}

function FileMenu({
  onRename = () => {},
  onOpen = () => {},
  trigger,
  ...props
}: Partial<ContextMenuProps> & {
  onRename?: () => void;
  onOpen?: () => void;
  trigger?: ContextMenuTriggerProps;
}) {
  return (
    <ContextMenu {...props}>
      <ContextMenuTrigger data-testid="region" {...trigger}>
        <button type="button" onClick={onOpen}>
          Report.pdf
        </button>
      </ContextMenuTrigger>
      <ContextMenuContent>
        <ContextMenuItem onSelect={onRename}>Rename</ContextMenuItem>
        <ContextMenuItem>Duplicate</ContextMenuItem>
        <ContextMenuSeparator />
        <ContextMenuItem tone="danger">Delete</ContextMenuItem>
      </ContextMenuContent>
    </ContextMenu>
  );
}

describe('ContextMenu', () => {
  it('opens on right-click as a named menu; arrows move and Enter runs an item', async () => {
    const onRename = vi.fn();
    const onOpenChange = vi.fn();
    render(<FileMenu onRename={onRename} onOpenChange={onOpenChange} />);
    const region = screen.getByTestId('region');
    expect(region).toHaveClass('mn-context-menu');
    const notCancelled = fireEvent.contextMenu(region, { clientX: 120, clientY: 80 });
    expect(notCancelled).toBe(false);
    const menu = await screen.findByRole('menu', { name: 'Actions' });
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    expect(menu).toHaveClass('mn-menu', 'mn-context-menu__content');
    // Opened with the pointer, the menu itself takes focus; arrows then move through items.
    await waitFor(() => expect(menu).toHaveFocus());
    await expectNoA11yViolations(menu);
    const user = userEvent.setup();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Duplicate' })).toHaveFocus();
    await user.keyboard('{ArrowUp}{Enter}');
    expect(onRename).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('opens with Shift+F10 on focused content; Escape closes and returns focus', async () => {
    render(<FileMenu label="File actions" />);
    const user = userEvent.setup();
    const file = screen.getByRole('button', { name: 'Report.pdf' });
    file.focus();
    await user.keyboard('{Shift>}{F10}{/Shift}');
    expect(await screen.findByRole('menu', { name: 'File actions' })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus());
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    expect(file).toHaveFocus();
  });

  it('opens with the Menu key', async () => {
    render(<FileMenu />);
    screen.getByRole('button', { name: 'Report.pdf' }).focus();
    await userEvent.setup().keyboard('{ContextMenu}');
    expect(await screen.findByRole('menu')).toBeInTheDocument();
  });

  it('opens on a long touch press and swallows the click that follows', async () => {
    const onOpen = vi.fn();
    render(<FileMenu onOpen={onOpen} />);
    const file = screen.getByRole('button', { name: 'Report.pdf' });
    fireEvent.pointerDown(file, { pointerType: 'touch', clientX: 20, clientY: 20 });
    expect(screen.getByTestId('region')).toHaveAttribute('data-pressing', 'true');
    expect(await screen.findByRole('menu', {}, { timeout: 1500 })).toBeInTheDocument();
    fireEvent.pointerUp(file, { pointerType: 'touch' });
    fireEvent.click(file);
    expect(onOpen).not.toHaveBeenCalled();
  });

  it('does not open when the touch moves (a scroll)', async () => {
    render(<FileMenu />);
    const file = screen.getByRole('button', { name: 'Report.pdf' });
    // Fake timers, stepped well past the long-press delay, instead of a real wait.
    vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] });
    try {
      fireEvent.pointerDown(file, { pointerType: 'touch', clientX: 20, clientY: 20 });
      fireEvent.pointerMove(file, { pointerType: 'touch', clientX: 20, clientY: 60 });
      expect(screen.getByTestId('region')).not.toHaveAttribute('data-pressing');
      act(() => {
        vi.advanceTimersByTime(2000);
      });
      expect(screen.queryByRole('menu')).toBeNull();
    } finally {
      vi.useRealTimers();
    }
  });

  it('leaves the browser menu alone when disabled', () => {
    render(<FileMenu trigger={{ disabled: true }} />);
    const region = screen.getByTestId('region');
    expect(fireEvent.contextMenu(region, { clientX: 10, clientY: 10 })).toBe(true);
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('has no axe violations when closed', async () => {
    render(<FileMenu />);
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<FileMenu />);
    expect(html).toContain('mn-context-menu');
    expect(html).toContain('Report.pdf');
  });
});
