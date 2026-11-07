import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { KeyboardShortcutsSheet, describeShortcutKeys, type KeyboardShortcut } from './index.js';

const shortcuts: KeyboardShortcut[] = [
  { keys: 'mod+k', label: 'Search and commands' },
  { keys: 'c', label: 'Create task', group: 'Tasks' },
  { keys: ['j', 'arrowdown'], label: 'Next task', group: 'Tasks' },
  { keys: 'g i', label: 'Go to inbox', group: 'Navigation' },
  { keys: 'g p', label: 'Go to projects', group: 'Navigation' },
  { keys: '?', label: 'Show this list' },
];

describe('KeyboardShortcutsSheet', () => {
  it('opens from its trigger as a named dialog with grouped shortcuts', async () => {
    render(
      <KeyboardShortcutsSheet
        shortcuts={shortcuts}
        apple
        trigger={<Button>Shortcuts</Button>}
        description="Shortcuts work anywhere except in text fields."
      />,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Shortcuts' });
    await user.click(trigger);
    const sheet = await screen.findByRole('dialog', { name: 'Keyboard shortcuts' });
    expect(sheet).toHaveClass('mn-keyboard-shortcuts-sheet');
    expect(sheet).toHaveAccessibleDescription('Shortcuts work anywhere except in text fields.');
    // Ungrouped shortcuts come first, then the groups in order.
    expect(
      within(sheet)
        .getAllByRole('heading', { level: 3 })
        .map((h) => h.textContent),
    ).toEqual(['Tasks', 'Navigation']);
    expect(within(sheet).getByRole('region', { name: 'Navigation' })).toBeInTheDocument();
    const terms = within(sheet)
      .getAllByRole('term')
      .map((t) => t.textContent);
    expect(terms[0]).toBe('Search and commands');
    await expectNoA11yViolations(sheet);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('shows Apple symbols or names, and reads keys out by name', async () => {
    const { rerender } = render(<KeyboardShortcutsSheet shortcuts={shortcuts} defaultOpen apple />);
    const sheet = await screen.findByRole('dialog');
    const first = within(sheet).getAllByRole('definition')[0]!;
    expect(first.querySelector('[aria-hidden]')).toHaveTextContent('⌘K');
    expect(first).toHaveTextContent('Command K');
    rerender(<KeyboardShortcutsSheet shortcuts={shortcuts} defaultOpen apple={false} />);
    expect(first.querySelector('[aria-hidden]')).toHaveTextContent('CtrlK');
    expect(first).toHaveTextContent('Control K');
    const rows = within(sheet).getAllByRole('definition');
    expect(rows[4]).toHaveTextContent('G then I');
    expect(rows[3]).toHaveTextContent('J or Down arrow');
  });

  it('opens with "?" from the page but not while typing', async () => {
    const onOpenChange = vi.fn();
    render(
      <>
        <input aria-label="Notes" />
        <KeyboardShortcutsSheet shortcuts={shortcuts} onOpenChange={onOpenChange} />
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('?');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(document.body);
    await user.keyboard('?');
    expect(await screen.findByRole('dialog', { name: 'Keyboard shortcuts' })).toBeInTheDocument();
    expect(onOpenChange).toHaveBeenCalledWith(true);
  });

  it('takes a custom hotkey or none', async () => {
    const { unmount } = render(<KeyboardShortcutsSheet shortcuts={shortcuts} hotkey="mod+/" />);
    const user = userEvent.setup();
    await user.keyboard('{Control>}/{/Control}');
    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    unmount();
    render(<KeyboardShortcutsSheet shortcuts={shortcuts} hotkey={false} />);
    await user.keyboard('?');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('filters by label, group or key name and announces the count', async () => {
    render(<KeyboardShortcutsSheet shortcuts={shortcuts} defaultOpen filterable apple />);
    const user = userEvent.setup();
    const sheet = await screen.findByRole('dialog');
    const filter = within(sheet).getByRole('searchbox', { name: 'Filter shortcuts' });
    await user.type(filter, 'navigation');
    expect(
      within(sheet)
        .getAllByRole('term')
        .map((t) => t.textContent),
    ).toEqual(['Go to inbox', 'Go to projects']);
    expect(within(sheet).getByRole('status')).toHaveTextContent('2 shortcuts');
    await user.clear(filter);
    await user.type(filter, 'command');
    expect(
      within(sheet)
        .getAllByRole('term')
        .map((t) => t.textContent),
    ).toEqual(['Search and commands']);
    await user.clear(filter);
    await user.type(filter, 'zzz');
    expect(within(sheet).getByText('No shortcuts match', { selector: 'p' })).toBeInTheDocument();
    expect(within(sheet).getByRole('status')).toHaveTextContent('No shortcuts match');
  });

  it('hides the filter for short lists unless asked', async () => {
    render(<KeyboardShortcutsSheet shortcuts={shortcuts} defaultOpen />);
    const sheet = await screen.findByRole('dialog');
    expect(within(sheet).queryByRole('searchbox')).not.toBeInTheDocument();
  });

  it('translates its strings', async () => {
    render(
      <KeyboardShortcutsSheet
        shortcuts={[{ keys: 'g i', label: 'Posteingang' }]}
        defaultOpen
        apple
        messages={{ title: 'Tastenkürzel', then: 'dann' }}
      />,
    );
    const sheet = await screen.findByRole('dialog', { name: 'Tastenkürzel' });
    expect(within(sheet).getByRole('definition')).toHaveTextContent('G dann I');
  });

  it('describeShortcutKeys spells out keys', () => {
    expect(describeShortcutKeys('mod+shift+p', { apple: true })).toBe('Command Shift P');
    expect(describeShortcutKeys('mod+shift+p', { apple: false })).toBe('Control Shift P');
    expect(describeShortcutKeys(['alt+arrowup', 'alt+arrowdown'], { apple: false })).toBe(
      'Alt Up arrow or Alt Down arrow',
    );
  });

  it('renders closed on the server without touching the document', () => {
    const html = renderToString(
      <KeyboardShortcutsSheet shortcuts={shortcuts} trigger={<Button>Shortcuts</Button>} />,
    );
    expect(html).toContain('Shortcuts');
  });
});
