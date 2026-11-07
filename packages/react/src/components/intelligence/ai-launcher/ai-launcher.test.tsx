import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiLauncher,
  ariaKeyShortcut,
  matchesAiShortcut,
  parseAiShortcut,
  type AiLauncherProps,
} from './index.js';

function Controlled(props: Partial<AiLauncherProps>) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <AiLauncher open={open} onOpenChange={setOpen} {...props} />
      <input aria-label="Notes" />
      <p data-testid="state">{open ? 'open' : 'closed'}</p>
    </>
  );
}

function rect(r: Partial<DOMRect>): DOMRect {
  const base = { x: 0, y: 0, top: 0, left: 0, right: 0, bottom: 0, width: 0, height: 0 };
  return { ...base, ...r, toJSON: () => ({}) } as DOMRect;
}

describe('AiLauncher', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    document.body.innerHTML = '';
  });

  it('is a named toggle button that reports its expanded state', async () => {
    const user = userEvent.setup();
    render(
      <>
        <Controlled aria-controls="panel" />
        <div id="panel" />
      </>,
    );
    const button = screen.getByRole('button', { name: 'Assistant' });
    expect(button).toHaveClass('mn-ai-launcher');
    expect(button).toHaveAttribute('aria-expanded', 'false');
    expect(button).toHaveAttribute('aria-keyshortcuts', 'Control+J');
    expect(button).toHaveAttribute('data-position', 'bottom-end');
    await user.click(button);
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(button).toHaveAttribute('data-open');
    await expectNoA11yViolations();
  });

  it('toggles with Ctrl+J anywhere except while typing', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByTestId('state')).toHaveTextContent('open');
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByTestId('state')).toHaveTextContent('closed');
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByTestId('state')).toHaveTextContent('closed');
  });

  it('can take the shortcut inside text fields, use another shortcut, or none', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<Controlled shortcutInEditable shortcut="alt+shift+a" />);
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByTestId('state')).toHaveTextContent('closed');
    await user.keyboard('{Alt>}{Shift>}A{/Shift}{/Alt}');
    expect(screen.getByTestId('state')).toHaveTextContent('open');
    expect(screen.getByRole('button', { name: 'Assistant' })).toHaveAttribute(
      'aria-keyshortcuts',
      'Alt+Shift+A',
    );
    unmount();

    render(<Controlled shortcut={false} />);
    await user.keyboard('{Control>}j{/Control}');
    expect(screen.getByTestId('state')).toHaveTextContent('closed');
    expect(screen.getByRole('button', { name: 'Assistant' })).not.toHaveAttribute(
      'aria-keyshortcuts',
    );
  });

  it('parses and matches shortcuts per platform', () => {
    const sc = parseAiShortcut('Mod + J');
    expect(sc).toMatchObject({ key: 'j', mod: true, shift: false });
    const ev = { key: 'j', ctrlKey: false, metaKey: true, altKey: false, shiftKey: false };
    expect(matchesAiShortcut(ev, sc, true)).toBe(true);
    expect(matchesAiShortcut(ev, sc, false)).toBe(false);
    expect(matchesAiShortcut({ ...ev, metaKey: false, ctrlKey: true }, sc, false)).toBe(true);
    expect(ariaKeyShortcut(sc, true)).toBe('Meta+J');
    expect(ariaKeyShortcut(sc, false)).toBe('Control+J');
  });

  it('includes the badge in the accessible name', () => {
    const { rerender } = render(<AiLauncher open={false} onOpenChange={() => {}} badge="unread" />);
    expect(screen.getByRole('button', { name: 'Assistant, New messages' })).toHaveAttribute(
      'data-badge',
      'unread',
    );
    rerender(<AiLauncher open={false} onOpenChange={() => {}} badge="working" />);
    expect(screen.getByRole('button', { name: 'Assistant, Working' })).toBeInTheDocument();
    rerender(<AiLauncher open={false} onOpenChange={() => {}} badge={120} />);
    const button = screen.getByRole('button', { name: 'Assistant, 120 unread messages' });
    expect(button).toHaveTextContent('99+');
    rerender(
      <AiLauncher
        open={false}
        onOpenChange={() => {}}
        badge={1}
        messages={{ label: 'Helper', count: (n) => `${n} neue` }}
      />,
    );
    expect(screen.getByRole('button', { name: 'Helper, 1 neue' })).toBeInTheDocument();
  });

  it('shows the label and shortcut inline', async () => {
    render(<AiLauncher open={false} onOpenChange={() => {}} position="inline" />);
    const button = screen.getByRole('button', { name: 'Assistant' });
    expect(button).toHaveAttribute('data-label');
    expect(button.querySelector('kbd')).toHaveTextContent('Ctrl+J');
    await expectNoA11yViolations();
  });

  it('a click handler can prevent the toggle', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(
      <AiLauncher open={false} onOpenChange={onOpenChange} onClick={(e) => e.preventDefault()} />,
    );
    await user.click(screen.getByRole('button'));
    expect(onOpenChange).not.toHaveBeenCalled();
  });

  it('moves above a visible element matching avoidSelector at the same corner', async () => {
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
      cb(0);
      return 1;
    });
    const dock = document.createElement('div');
    dock.className = 'chat-dock';
    document.body.appendChild(dock);
    vi.spyOn(dock, 'getBoundingClientRect').mockReturnValue(
      rect({ top: 600, bottom: 760, left: 900, right: 1000, width: 100, height: 160 }),
    );
    vi.spyOn(HTMLButtonElement.prototype, 'getBoundingClientRect').mockReturnValue(
      rect({ top: 700, bottom: 748, left: 940, right: 988, width: 48, height: 48 }),
    );
    render(<AiLauncher open={false} onOpenChange={() => {}} avoidSelector=".chat-dock" />);
    const button = screen.getByRole('button');
    // 748 - 600 + 12 (gap)
    expect(button).toHaveAttribute('data-avoiding');
    expect(button.style.getPropertyValue('--mn-ai-launcher-avoid')).toBe('160px');

    // The dock goes away: the launcher returns to its corner.
    vi.spyOn(dock, 'getBoundingClientRect').mockReturnValue(rect({}));
    act(() => {
      fireEvent(window, new Event('resize'));
    });
    expect(button).not.toHaveAttribute('data-avoiding');
  });

  it('renders on the server', () => {
    const html = renderToString(<AiLauncher open={false} onOpenChange={() => {}} badge={3} />);
    expect(html).toContain('mn-ai-launcher');
    expect(html).toContain('aria-expanded="false"');
  });
});
