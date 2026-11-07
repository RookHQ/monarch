import { afterEach, describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  UnsavedChangesDialog,
  UnsavedChangesGuard,
  useUnsavedChangesGuard,
  type UnsavedChangesGuardHandle,
  type UseUnsavedChangesGuardOptions,
} from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const results: boolean[] = [];

function Editor({
  proceed,
  ...options
}: Partial<UseUnsavedChangesGuardOptions> & { proceed: () => void }) {
  const { guard, element } = useUnsavedChangesGuard({ when: true, ...options });
  return (
    <>
      <Button onClick={async () => results.push(await guard(proceed))}>Go to reports</Button>
      {element}
    </>
  );
}

const dialog = () => screen.findByRole('alertdialog', { name: 'Discard unsaved changes?' });

afterEach(() => {
  results.length = 0;
});

describe('useUnsavedChangesGuard', () => {
  it('proceeds at once when there are no unsaved changes', async () => {
    const proceed = vi.fn();
    render(<Editor when={false} proceed={proceed} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Go to reports' }));
    expect(proceed).toHaveBeenCalledOnce();
    expect(results).toEqual([true]);
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('asks first, with focus on Keep editing; Keep editing stays and restores focus', async () => {
    const proceed = vi.fn();
    render(<Editor proceed={proceed} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Go to reports' });
    await user.click(trigger);
    const box = await dialog();
    expect(box).toHaveAccessibleDescription(
      'Your changes have not been saved. If you leave now, they will be lost.',
    );
    expect(screen.getByRole('button', { name: 'Keep editing' })).toHaveFocus();
    expect(screen.queryByRole('button', { name: 'Save and leave' })).not.toBeInTheDocument();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Keep editing' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(proceed).not.toHaveBeenCalled();
    expect(results).toEqual([false]);
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('Escape stays; outside clicks do not close it', async () => {
    const proceed = vi.fn();
    render(<Editor proceed={proceed} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Go to reports' }));
    await dialog();
    fireEvent.pointerDown(document.body);
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(proceed).not.toHaveBeenCalled();
    expect(results).toEqual([false]);
  });

  it('Discard changes runs proceed and resolves true', async () => {
    const proceed = vi.fn();
    render(<Editor proceed={proceed} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Go to reports' }));
    await dialog();
    await user.click(screen.getByRole('button', { name: 'Discard changes' }));
    expect(proceed).toHaveBeenCalledOnce();
    expect(results).toEqual([true]);
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('Save and leave is pending while saving, then proceeds', async () => {
    const save = deferred();
    const proceed = vi.fn();
    render(<Editor proceed={proceed} onSave={() => save.promise} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Go to reports' }));
    const box = await dialog();
    const saveButton = screen.getByRole('button', { name: 'Save and leave' });
    await user.click(saveButton);
    expect(box).toHaveAttribute('aria-busy', 'true');
    expect(saveButton).toHaveAttribute('aria-busy', 'true');
    expect(saveButton).toHaveFocus();
    expect(screen.getByRole('button', { name: 'Keep editing' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Discard changes' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    save.resolve();
    await waitFor(() => expect(proceed).toHaveBeenCalledOnce());
    expect(results).toEqual([true]);
  });

  it('a failed save shows the error, stays open and can be retried', async () => {
    const proceed = vi.fn();
    const onSave = vi
      .fn()
      .mockRejectedValueOnce(new Error('The title is required.'))
      .mockResolvedValueOnce(undefined);
    render(<Editor proceed={proceed} onSave={onSave} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Go to reports' }));
    await dialog();
    await user.click(screen.getByRole('button', { name: 'Save and leave' }));
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('The title is required.');
    expect(screen.getByRole('button', { name: 'Save and leave' })).toHaveAccessibleDescription(
      'The title is required.',
    );
    expect(proceed).not.toHaveBeenCalled();
    await expectNoA11yViolations();
    await user.click(screen.getByRole('button', { name: 'Save and leave' }));
    await waitFor(() => expect(proceed).toHaveBeenCalledOnce());
  });

  it('uses the fallback message for a rejection without a message, and custom messages', async () => {
    render(
      <Editor
        proceed={() => {}}
        onSave={() => Promise.reject('nope')}
        messages={{ title: 'Leave the editor?', save: 'Save first', saveFailed: 'Not saved.' }}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Go to reports' }));
    await screen.findByRole('alertdialog', { name: 'Leave the editor?' });
    await user.click(screen.getByRole('button', { name: 'Save first' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Not saved.');
  });

  it('warns before unload only while there are unsaved changes', () => {
    const add = vi.spyOn(window, 'addEventListener');
    const remove = vi.spyOn(window, 'removeEventListener');
    function Form({ dirty }: { dirty: boolean }) {
      return useUnsavedChangesGuard({ when: dirty }).element;
    }
    const { rerender, unmount } = render(<Form dirty={false} />);
    expect(add.mock.calls.some(([type]) => type === 'beforeunload')).toBe(false);
    rerender(<Form dirty />);
    expect(add.mock.calls.some(([type]) => type === 'beforeunload')).toBe(true);
    const event = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(true);
    rerender(<Form dirty={false} />);
    expect(remove.mock.calls.some(([type]) => type === 'beforeunload')).toBe(true);
    const after = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(after);
    expect(after.defaultPrevented).toBe(false);
    unmount();
    add.mockRestore();
    remove.mockRestore();
  });

  it('beforeUnload={false} registers nothing; after Discard it no longer warns', async () => {
    function Form({ beforeUnload }: { beforeUnload: boolean }) {
      const { guard, element } = useUnsavedChangesGuard({ when: true, beforeUnload });
      return (
        <>
          <Button onClick={() => void guard(() => {})}>Leave</Button>
          {element}
        </>
      );
    }
    const { rerender } = render(<Form beforeUnload={false} />);
    const first = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(first);
    expect(first.defaultPrevented).toBe(false);
    rerender(<Form beforeUnload />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Leave' }));
    await dialog();
    await user.click(screen.getByRole('button', { name: 'Discard changes' }));
    const second = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(second);
    expect(second.defaultPrevented).toBe(false);
  });

  it('interceptLinks guards plain same-origin link clicks only', async () => {
    function Page() {
      const { element } = useUnsavedChangesGuard({ when: true, interceptLinks: true });
      return (
        <>
          <a href="/reports">Reports</a>
          <a href="/reports" target="_blank">
            Reports in a new tab
          </a>
          <a href="/export.csv" download>
            Export
          </a>
          <a href="https://example.com/help">Help</a>
          <a href="#section">Jump</a>
          <a href="/settings" data-unsaved-changes="ignore">
            Settings
          </a>
          {element}
        </>
      );
    }
    render(<Page />);
    const click = (name: string, init: MouseEventInit = {}) => {
      const event = new MouseEvent('click', { bubbles: true, cancelable: true, ...init });
      screen.getByRole('link', { name }).dispatchEvent(event);
      return event;
    };
    // Stop jsdom from trying to navigate for the links that are let through.
    const stop = (event: Event) => event.preventDefault();
    for (const name of ['Reports in a new tab', 'Export', 'Help', 'Jump', 'Settings']) {
      screen.getByRole('link', { name }).addEventListener('click', stop);
      const event = click(name);
      screen.getByRole('link', { name }).removeEventListener('click', stop);
      expect(event.defaultPrevented).toBe(true); // only by `stop`
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    }
    const link = screen.getByRole('link', { name: 'Reports' });
    link.addEventListener('click', stop);
    click('Reports', { metaKey: true });
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    link.removeEventListener('click', stop);
    const guarded = click('Reports');
    expect(guarded.defaultPrevented).toBe(true);
    await dialog();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Keep editing' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
  });

  it('does not intercept links while there are no unsaved changes', () => {
    function Page() {
      return useUnsavedChangesGuard({ when: false, interceptLinks: true }).element;
    }
    render(
      <>
        <a href="/reports">Reports</a>
        <Page />
      </>,
    );
    const event = new MouseEvent('click', { bubbles: true, cancelable: true });
    const link = screen.getByRole('link', { name: 'Reports' });
    let preventedByGuard = false;
    link.addEventListener('click', (e) => {
      preventedByGuard = e.defaultPrevented;
      e.preventDefault();
    });
    link.dispatchEvent(event);
    expect(preventedByGuard).toBe(false);
  });

  it('a second guard call settles the first as stayed', async () => {
    let guardFn!: (proceed: () => void) => Promise<boolean>;
    function Form() {
      const { guard, element } = useUnsavedChangesGuard({ when: true });
      guardFn = guard;
      return element;
    }
    render(<Form />);
    const first = guardFn(() => {});
    const proceed = vi.fn();
    const second = guardFn(proceed);
    await expect(first).resolves.toBe(false);
    await dialog();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Discard changes' }));
    await expect(second).resolves.toBe(true);
    expect(proceed).toHaveBeenCalledOnce();
  });
});

describe('UnsavedChangesGuard', () => {
  it('exposes guard through its ref and renders nothing until asked', async () => {
    const ref = createRef<UnsavedChangesGuardHandle>();
    const { container } = render(<UnsavedChangesGuard ref={ref} when />);
    expect(container).toBeEmptyDOMElement();
    const proceed = vi.fn();
    const result = ref.current!.guard(proceed);
    await dialog();
    await userEvent.setup().click(screen.getByRole('button', { name: 'Discard changes' }));
    await expect(result).resolves.toBe(true);
    expect(proceed).toHaveBeenCalledOnce();
  });
});

describe('UnsavedChangesDialog', () => {
  it('is controlled: onStay and onLeave report the choice', async () => {
    const onStay = vi.fn();
    const onLeave = vi.fn();
    function Blocker() {
      const [open, setOpen] = useState(true);
      return (
        <UnsavedChangesDialog
          open={open}
          onOpenChange={setOpen}
          onStay={onStay}
          onLeave={(reason) => {
            onLeave(reason);
            setOpen(false);
          }}
          onSave={() => {}}
        >
          3 fields changed.
        </UnsavedChangesDialog>
      );
    }
    const { rerender } = render(<Blocker />);
    const user = userEvent.setup();
    expect(await dialog()).toHaveTextContent('3 fields changed.');
    await user.click(screen.getByRole('button', { name: 'Keep editing' }));
    expect(onStay).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    rerender(<Blocker key="again" />);
    await user.click(await screen.findByRole('button', { name: 'Save and leave' }));
    expect(onLeave).toHaveBeenCalledWith('save');
  });

  it('shows a controlled error and pending state', async () => {
    render(
      <UnsavedChangesDialog
        open
        onLeave={() => {}}
        onSave={() => {}}
        pending
        error="The server is busy."
      />,
    );
    expect(await dialog()).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('alert')).toHaveTextContent('The server is busy.');
  });

  it('renders on the server, closed and open', () => {
    function Closed() {
      return useUnsavedChangesGuard({ when: true, interceptLinks: true }).element;
    }
    expect(() => renderToString(<Closed />)).not.toThrow();
    expect(() => renderToString(<UnsavedChangesGuard when />)).not.toThrow();
    expect(() =>
      renderToString(<UnsavedChangesDialog open onLeave={() => {}} onSave={() => {}} />),
    ).not.toThrow();
  });
});
