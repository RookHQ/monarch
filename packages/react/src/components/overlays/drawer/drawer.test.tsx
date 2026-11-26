import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  Drawer,
  DrawerBody,
  DrawerClose,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
  DrawerTrigger,
  shouldCloseDrawer,
  type DrawerContentProps,
  type DrawerProps,
} from './index.js';

function Share({ content, ...props }: DrawerProps & { content?: DrawerContentProps }) {
  return (
    <Drawer {...props}>
      <DrawerTrigger asChild>
        <button type="button">Share</button>
      </DrawerTrigger>
      <DrawerContent {...content}>
        <DrawerHeader>
          <DrawerTitle>Share link</DrawerTitle>
          <DrawerDescription>Anyone with the link can view.</DrawerDescription>
        </DrawerHeader>
        <DrawerBody>
          <label>
            Link <input defaultValue="https://example.com/doc" />
          </label>
        </DrawerBody>
        <DrawerFooter>
          <DrawerClose asChild>
            <button type="button">Done</button>
          </DrawerClose>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  );
}

/** jsdom has no layout; dispatch pointer events with the fields the drawer reads. */
function pointer(el: Element, type: string, clientY: number, timeStamp: number) {
  const event = new Event(type, { bubbles: true, cancelable: true });
  Object.assign(event, { pointerId: 1, button: 0, clientY });
  Object.defineProperty(event, 'timeStamp', { value: timeStamp });
  fireEvent(el, event);
}

describe('Drawer', () => {
  it('forwards refs from the header, body and footer', () => {
    const header = createRef<HTMLDivElement>();
    const body = createRef<HTMLDivElement>();
    const footer = createRef<HTMLDivElement>();
    render(
      <Drawer defaultOpen>
        <DrawerContent>
          <DrawerHeader ref={header}>
            <DrawerTitle>Refs</DrawerTitle>
          </DrawerHeader>
          <DrawerBody ref={body}>Body</DrawerBody>
          <DrawerFooter ref={footer}>Footer</DrawerFooter>
        </DrawerContent>
      </Drawer>,
    );
    expect(header.current).toHaveClass('mn-drawer__header');
    expect(body.current).toBeInstanceOf(HTMLDivElement);
    expect(footer.current).toBeInstanceOf(HTMLDivElement);
  });

  it('opens as a modal bottom sheet with a handle; Escape closes and returns focus', async () => {
    render(<Share />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Share' });
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Share link' });
    expect(dialog).toHaveClass('mn-drawer', 'mn-sheet');
    expect(dialog).toHaveAttribute('data-side', 'bottom');
    expect(dialog).toHaveAccessibleDescription('Anyone with the link can view.');
    expect(dialog.querySelector('.mn-drawer__handle')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations(dialog);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('closes from the close button and DrawerClose, keeping focus inside while open', async () => {
    const onOpenChange = vi.fn();
    render(<Share defaultOpen onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    const dialog = await screen.findByRole('dialog');
    for (let i = 0; i < 4; i++) {
      await user.tab();
      expect(dialog.contains(document.activeElement)).toBe(true);
    }
    await user.click(screen.getByRole('button', { name: 'Done' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenLastCalledWith(false);
    await user.click(screen.getByRole('button', { name: 'Share' }));
    await user.click(await screen.findByRole('button', { name: 'Close' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('closes when the handle is dragged down far enough', async () => {
    const onOpenChange = vi.fn();
    render(<Share defaultOpen onOpenChange={onOpenChange} />);
    const dialog = await screen.findByRole('dialog');
    const handle = dialog.querySelector('.mn-drawer__handle')!;
    pointer(handle, 'pointerdown', 100, 0);
    pointer(handle, 'pointermove', 160, 400);
    expect(dialog).toHaveAttribute('data-dragging');
    expect(dialog.style.transform).toBe('translateY(60px)');
    pointer(handle, 'pointerup', 200, 800);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('snaps back after a short, slow drag from the header', async () => {
    render(<Share defaultOpen />);
    const dialog = await screen.findByRole('dialog');
    const title = screen.getByText('Share link');
    pointer(title, 'pointerdown', 100, 0);
    pointer(title, 'pointermove', 130, 500);
    pointer(title, 'pointerup', 130, 1000);
    expect(screen.getByRole('dialog')).toBe(dialog);
    expect(dialog).not.toHaveAttribute('data-dragging');
    expect(dialog.style.transform).toBe('');
  });

  it('closes on a quick flick, and ignores drags from the body or while busy', async () => {
    const { unmount } = render(<Share defaultOpen content={{ busy: true }} />);
    let dialog = await screen.findByRole('dialog');
    let handle = dialog.querySelector('.mn-drawer__handle')!;
    expect(dialog).not.toHaveAttribute('data-drag');
    pointer(handle, 'pointerdown', 0, 0);
    pointer(handle, 'pointermove', 300, 100);
    pointer(handle, 'pointerup', 300, 100);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    unmount();

    render(<Share defaultOpen />);
    dialog = await screen.findByRole('dialog');
    const input = screen.getByRole('textbox', { name: 'Link' });
    pointer(input, 'pointerdown', 0, 0);
    pointer(input, 'pointermove', 300, 50);
    expect(dialog).not.toHaveAttribute('data-dragging');
    pointer(input, 'pointerup', 300, 50);
    expect(screen.getByRole('dialog')).toBeInTheDocument();

    handle = dialog.querySelector('.mn-drawer__handle')!;
    pointer(handle, 'pointerdown', 0, 0);
    pointer(handle, 'pointermove', 40, 40);
    pointer(handle, 'pointerup', 40, 50);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('decides when a drag closes', () => {
    expect(shouldCloseDrawer(0, 100, 400)).toBe(false);
    expect(shouldCloseDrawer(-50, 100, 400)).toBe(false);
    expect(shouldCloseDrawer(99, 1000, 400)).toBe(false);
    expect(shouldCloseDrawer(100, 1000, 400)).toBe(true);
    expect(shouldCloseDrawer(80, 1000, 0)).toBe(true);
    expect(shouldCloseDrawer(30, 40, 400)).toBe(true);
    expect(shouldCloseDrawer(8, 1, 400)).toBe(false);
  });

  it('renders the trigger on the server', () => {
    const html = renderToString(<Share />);
    expect(html).toContain('Share');
    expect(html).toContain('aria-haspopup="dialog"');
  });
});
