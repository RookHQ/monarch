import { describe, expect, it, vi } from 'vitest';
import { useRef, useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FocusTrap, getFocusableElements, useFocusTrap, type FocusTrapProps } from './index.js';

function Harness(props: Omit<FocusTrapProps, 'active'>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open panel
      </button>
      <button type="button">Outside</button>
      {open ? (
        <FocusTrap role="group" aria-label="Filters panel" {...props}>
          <button type="button">First</button>
          <input aria-label="Search" />
          <button type="button" disabled>
            Disabled
          </button>
          <button type="button" onClick={() => setOpen(false)}>
            Close
          </button>
        </FocusTrap>
      ) : null}
    </>
  );
}

describe('FocusTrap', () => {
  it('focuses the first focusable element and wraps Tab and Shift+Tab', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));
    expect(screen.getByRole('button', { name: 'First' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'Search' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Close' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'First' })).toHaveFocus();
    await user.tab({ shift: true });
    expect(screen.getByRole('button', { name: 'Close' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('returns focus to the opener when it unmounts', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    const opener = screen.getByRole('button', { name: 'Open panel' });
    await user.click(opener);
    await user.click(screen.getByRole('button', { name: 'Close' }));
    await waitFor(() => expect(opener).toHaveFocus());
  });

  it('does not return focus when returnFocus is false', async () => {
    const user = userEvent.setup();
    render(<Harness returnFocus={false} />);
    const opener = screen.getByRole('button', { name: 'Open panel' });
    await user.click(opener);
    screen.getByRole('button', { name: 'Close' }).click();
    await Promise.resolve();
    expect(opener).not.toHaveFocus();
  });

  it('brings escaped focus back inside', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));
    await user.tab();
    const search = screen.getByRole('textbox', { name: 'Search' });
    expect(search).toHaveFocus();
    screen.getByRole('button', { name: 'Outside' }).focus();
    expect(search).toHaveFocus();
  });

  it('leaves focus in a popup opened from inside the trap but portalled elsewhere', async () => {
    const user = userEvent.setup();
    const menu = document.createElement('div');
    menu.id = 'trap-menu';
    menu.setAttribute('role', 'menu');
    menu.innerHTML = '<button role="menuitem">Rename</button>';
    const popper = document.createElement('div');
    popper.setAttribute('data-radix-popper-content-wrapper', '');
    popper.innerHTML = '<button>In popper</button>';
    document.body.append(menu, popper);
    try {
      render(
        <FocusTrap role="group" aria-label="Panel">
          <button type="button" aria-haspopup="menu" aria-controls="trap-menu">
            More
          </button>
          <button type="button">Last</button>
        </FocusTrap>,
      );
      const item = screen.getByRole('menuitem', { name: 'Rename' });
      item.focus();
      expect(item).toHaveFocus();
      // Tab inside the popup is the popup's business, not wrapped to the trap's first element.
      await user.tab();
      expect(screen.getByRole('button', { name: 'More' })).not.toHaveFocus();
      const inPopper = screen.getByRole('button', { name: 'In popper' });
      inPopper.focus();
      expect(inPopper).toHaveFocus();
    } finally {
      menu.remove();
      popper.remove();
    }
  });

  it('calls onEscape and leaves closing to the caller', async () => {
    const user = userEvent.setup();
    const onEscape = vi.fn();
    render(<Harness onEscape={onEscape} />);
    await user.click(screen.getByRole('button', { name: 'Open panel' }));
    await user.keyboard('{Escape}');
    expect(onEscape).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('group', { name: 'Filters panel' })).toBeInTheDocument();
  });

  it('can focus the container itself, or a given element', async () => {
    const { unmount } = render(
      <FocusTrap aria-label="Editor" role="region" initialFocus="container">
        <button type="button">Bold</button>
      </FocusTrap>,
    );
    const regionEl = screen.getByRole('region', { name: 'Editor' });
    expect(regionEl).toHaveFocus();
    expect(regionEl).toHaveAttribute('tabindex', '-1');
    unmount();

    function WithRef() {
      const ref = useRef<HTMLButtonElement>(null);
      return (
        <FocusTrap initialFocus={ref}>
          <button type="button">One</button>
          <button type="button" ref={ref}>
            Two
          </button>
        </FocusTrap>
      );
    }
    render(<WithRef />);
    expect(screen.getByRole('button', { name: 'Two' })).toHaveFocus();
  });

  it('keeps focus on the container when nothing inside can take it', async () => {
    const user = userEvent.setup();
    render(
      <FocusTrap role="region" aria-label="Notice">
        <p>Nothing to focus</p>
      </FocusTrap>,
    );
    const el = screen.getByRole('region', { name: 'Notice' });
    expect(el).toHaveFocus();
    await user.tab();
    expect(el).toHaveFocus();
  });

  it('does nothing while inactive and marks the active state', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <>
        <button type="button">Before</button>
        <FocusTrap data-testid="trap" active={false}>
          <button type="button">Inside</button>
        </FocusTrap>
      </>,
    );
    expect(screen.getByTestId('trap')).not.toHaveAttribute('data-active');
    expect(document.body).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Before' })).toHaveFocus();
    rerender(
      <>
        <button type="button">Before</button>
        <FocusTrap data-testid="trap" active>
          <button type="button">Inside</button>
        </FocusTrap>
      </>,
    );
    expect(screen.getByTestId('trap')).toHaveAttribute('data-active');
    expect(screen.getByRole('button', { name: 'Inside' })).toHaveFocus();
  });

  it('nests: only the innermost trap acts', async () => {
    const user = userEvent.setup();
    function Nested() {
      const [inner, setInner] = useState(false);
      return (
        <FocusTrap aria-label="Outer" role="group">
          <button type="button" onClick={() => setInner(true)}>
            Open inner
          </button>
          {inner ? (
            <FocusTrap aria-label="Inner" role="group">
              <button type="button">Inner A</button>
              <button type="button" onClick={() => setInner(false)}>
                Inner B
              </button>
            </FocusTrap>
          ) : null}
        </FocusTrap>
      );
    }
    render(<Nested />);
    await user.click(screen.getByRole('button', { name: 'Open inner' }));
    expect(screen.getByRole('button', { name: 'Inner A' })).toHaveFocus();
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Inner A' })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Inner B' }));
    await waitFor(() => expect(screen.getByRole('button', { name: 'Open inner' })).toHaveFocus());
  });

  it('works as a hook on any element', () => {
    function Hooked() {
      const ref = useRef<HTMLElement>(null);
      useFocusTrap(ref);
      return (
        <section ref={ref} aria-label="Drawer">
          <a href="#a">Link</a>
        </section>
      );
    }
    render(<Hooked />);
    expect(screen.getByRole('link', { name: 'Link' })).toHaveFocus();
  });

  it('lists focusable elements in tab order, skipping disabled, hidden and negative tabindex', () => {
    render(
      <div data-testid="box">
        <button type="button">A</button>
        <button type="button" disabled>
          B
        </button>
        <span tabIndex={-1}>C</span>
        <div hidden>
          <button type="button">D</button>
        </div>
        {/* Positive tabindex on purpose: the helper must order it first. */}
        {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex, jsx-a11y/tabindex-no-positive */}
        <span tabIndex={2}>E</span>
        <input type="hidden" />
        <textarea aria-label="F" />
      </div>,
    );
    const names = getFocusableElements(screen.getByTestId('box')).map(
      (el) => el.textContent || el.getAttribute('aria-label'),
    );
    expect(names).toEqual(['E', 'A', 'F']);
  });

  it('calls a callback ref once, not again on every render', () => {
    const ref = vi.fn();
    const { rerender } = render(
      <FocusTrap ref={ref} active={false}>
        <button type="button">One</button>
      </FocusTrap>,
    );
    rerender(
      <FocusTrap ref={ref} active={false}>
        <button type="button">Two</button>
      </FocusTrap>,
    );
    expect(ref).toHaveBeenCalledTimes(1);
    expect(ref).toHaveBeenLastCalledWith(expect.any(HTMLDivElement));
  });

  it('renders on the server', () => {
    expect(renderToString(<FocusTrap>Body</FocusTrap>)).toContain('mn-focus-trap');
  });
});
