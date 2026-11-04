import { afterEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  CommandPalette,
  defaultCommandFilter,
  formatHotkey,
  isTypingTarget,
  matchesHotkey,
  type CommandPaletteItem,
  type CommandPaletteSource,
} from './index.js';

const onCreate = vi.fn();
const ITEMS: CommandPaletteItem[] = [
  { id: 'new', label: 'New issue', group: 'Actions', shortcut: 'c', onSelect: onCreate },
  { id: 'archive', label: 'Archive', group: 'Actions', keywords: ['delete'], tone: 'danger' },
  { id: 'locked', label: 'Export', group: 'Actions', disabled: true },
  { id: 'inbox', label: 'Inbox', group: 'Go to', shortcut: ['G', 'I'] },
  { id: 'settings', label: 'Settings', group: 'Go to', description: 'Workspace' },
  { id: 'move', label: 'Move to…', group: 'Actions', page: 'move' },
];
const PAGES = {
  move: {
    title: 'Move to',
    placeholder: 'Choose a project…',
    items: [
      { id: 'p-web', label: 'Website' },
      { id: 'p-app', label: 'Mobile app' },
    ],
  },
};

const input = () => screen.getByRole('combobox');
const active = () => {
  const id = input().getAttribute('aria-activedescendant');
  return id ? document.getElementById(id) : null;
};
const optionNames = () => screen.queryAllByRole('option').map((o) => o.textContent);

afterEach(() => {
  vi.useRealTimers();
  onCreate.mockReset();
});

describe('CommandPalette', () => {
  it('opens with the hotkey and implements the combobox + listbox pattern', async () => {
    const user = userEvent.setup();
    render(<CommandPalette items={ITEMS} pages={PAGES} />);
    expect(screen.queryByRole('dialog')).toBeNull();
    await user.keyboard('{Control>}k{/Control}');
    const dialog = screen.getByRole('dialog', { name: 'Command palette' });
    expect(input()).toHaveFocus();
    expect(input()).toHaveAttribute('aria-expanded', 'true');
    const listbox = screen.getByRole('listbox', { name: 'Results' });
    expect(input()).toHaveAttribute('aria-controls', listbox.id);
    // Grouped results with headings.
    expect(within(listbox).getByRole('group', { name: 'Actions' })).toBeInTheDocument();
    expect(within(listbox).getByRole('group', { name: 'Go to' })).toBeInTheDocument();
    // The first enabled option is active and selected.
    expect(active()).toHaveTextContent('New issue');
    expect(active()).toHaveAttribute('aria-selected', 'true');
    expect(dialog).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves with the arrow keys, skips disabled items, wraps, and runs a command with Enter', async () => {
    const user = userEvent.setup();
    const onItemSelect = vi.fn();
    render(<CommandPalette items={ITEMS} pages={PAGES} defaultOpen onItemSelect={onItemSelect} />);
    await user.keyboard('{ArrowDown}');
    expect(active()).toHaveTextContent('Archive');
    await user.keyboard('{ArrowDown}');
    // "Export" is disabled and skipped.
    expect(active()).toHaveTextContent('Move to');
    await user.keyboard('{ArrowUp}{ArrowUp}{ArrowUp}');
    // Wrapped from the first option to the last.
    expect(active()).toHaveTextContent('Settings');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onCreate).toHaveBeenCalledTimes(1);
    expect(onItemSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 'new' }), {
      page: null,
    });
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('filters by label, description and keywords, ranking label prefixes first', async () => {
    const user = userEvent.setup();
    render(<CommandPalette items={ITEMS} defaultOpen />);
    await user.type(input(), 'delete');
    expect(optionNames()).toEqual(['Archive']);
    await user.clear(input());
    await user.type(input(), 'work');
    expect(optionNames()).toEqual(['SettingsWorkspace']);
    await user.clear(input());
    await user.type(input(), 'zzz');
    expect(screen.queryAllByRole('option')).toHaveLength(0);
    expect(screen.getByText('No results')).toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('No results'));
    await expectNoA11yViolations();
  });

  it('shows recent items only while the query is empty', async () => {
    const user = userEvent.setup();
    render(
      <CommandPalette
        items={ITEMS}
        recent={[{ id: 'r1', label: 'Quarterly report' }]}
        defaultOpen
      />,
    );
    const recent = screen.getByRole('group', { name: 'Recent' });
    expect(within(recent).getByRole('option', { name: /Quarterly report/ })).toBeInTheDocument();
    expect(active()).toHaveTextContent('Quarterly report');
    await user.type(input(), 'inb');
    expect(screen.queryByRole('group', { name: 'Recent' })).toBeNull();
  });

  it('shows shortcut hints with Kbd but keeps them out of the option name', () => {
    render(<CommandPalette items={ITEMS} defaultOpen />);
    const inbox = screen.getByRole('option', { name: 'Inbox' });
    const keys = inbox.querySelectorAll('kbd');
    expect([...keys].map((k) => k.textContent)).toEqual(['G', 'I']);
    const create = screen.getByRole('option', { name: 'New issue' });
    expect(create.querySelector('kbd')).toHaveTextContent('C');
    // The default footer explains the keys.
    expect(screen.getByText('to navigate')).toBeInTheDocument();
  });

  it('opens a nested page, and Backspace or Escape goes back before closing', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<CommandPalette items={ITEMS} pages={PAGES} defaultOpen onOpenChange={onOpenChange} />);
    await user.type(input(), 'move');
    await user.keyboard('{Enter}');
    expect(input()).toHaveAttribute('placeholder', 'Choose a project…');
    expect(input()).toHaveValue('');
    expect(input()).toHaveAccessibleName('Command palette: Move to');
    expect(optionNames()).toEqual(['Website', 'Mobile app']);
    expect(screen.getByRole('button', { name: 'Back from Move to' })).toBeInTheDocument();
    await expectNoA11yViolations();
    await user.keyboard('{Backspace}');
    expect(screen.getByRole('option', { name: 'Inbox' })).toBeInTheDocument();
    // Open the page again and leave with Escape.
    await user.type(input(), 'move');
    await user.keyboard('{Enter}');
    await user.keyboard('{Escape}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Inbox' })).toBeInTheDocument();
    expect(onOpenChange).not.toHaveBeenCalled();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('runs a page item, reporting the page it was on', async () => {
    const user = userEvent.setup();
    const onItemSelect = vi.fn();
    render(<CommandPalette items={ITEMS} pages={PAGES} defaultOpen onItemSelect={onItemSelect} />);
    await user.click(screen.getByRole('option', { name: 'Move to…' }));
    await user.click(screen.getByRole('option', { name: 'Mobile app' }));
    expect(onItemSelect).toHaveBeenLastCalledWith(expect.objectContaining({ id: 'p-app' }), {
      page: 'move',
    });
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('debounces async sources, aborts stale requests, and shows their results in a group', async () => {
    // No delay between keystrokes, so the test does not depend on machine speed.
    const user = userEvent.setup({ delay: null });
    const signals: AbortSignal[] = [];
    const search = vi.fn((q: string, signal: AbortSignal) => {
      signals.push(signal);
      return new Promise<CommandPaletteItem[]>((resolve) =>
        setTimeout(() => resolve([{ id: `doc-${q}`, label: `Doc about ${q}` }]), 300),
      );
    });
    const source: CommandPaletteSource = {
      id: 'docs',
      group: 'Documents',
      search,
      minQueryLength: 2,
      debounceMs: 150,
    };
    render(<CommandPalette items={[]} sources={[source]} defaultOpen />);
    await user.type(input(), 'r');
    await new Promise((r) => setTimeout(r, 250));
    // Below the minimum length: no request.
    expect(search).not.toHaveBeenCalled();
    await user.type(input(), 'oa');
    const group = await screen.findByRole('group', { name: 'Documents' }, { timeout: 4000 });
    // One request for the settled query, not one per keystroke.
    expect(search).toHaveBeenCalledTimes(1);
    expect(search).toHaveBeenLastCalledWith('roa', expect.any(AbortSignal));
    expect(within(group).getByRole('option', { name: 'Doc about roa' })).toBeInTheDocument();
    // While the next query loads, the old results stay and the search icon is a spinner.
    await user.type(input(), 'd');
    expect(screen.getByRole('option', { name: 'Doc about roa' })).toBeInTheDocument();
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2));
    // Typing again before the response arrives aborts that request.
    await user.type(input(), 's');
    expect(signals[1]?.aborted).toBe(true);
    expect(
      await screen.findByRole('option', { name: 'Doc about roads' }, { timeout: 4000 }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Doc about road' })).toBeNull();
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('1 result'));
  }, 15_000);

  it('aborts the request in flight when the palette closes', async () => {
    const user = userEvent.setup();
    let signal: AbortSignal | undefined;
    const search = vi.fn((_q: string, s: AbortSignal) => {
      signal = s;
      return new Promise<CommandPaletteItem[]>(() => {});
    });
    render(
      <CommandPalette
        items={[]}
        sources={[{ id: 'docs', group: 'Documents', search, debounceMs: 0 }]}
        defaultOpen
      />,
    );
    await user.type(input(), 'q');
    await waitFor(() => expect(search).toHaveBeenCalled());
    await user.keyboard('{Escape}');
    expect(signal?.aborted).toBe(true);
  });

  it('shows a source error with a retry that searches again', async () => {
    const user = userEvent.setup();
    let fail = true;
    const search = vi.fn(() =>
      fail
        ? Promise.reject(new Error('offline'))
        : Promise.resolve([{ id: 'x', label: 'Found it' }]),
    );
    render(
      <CommandPalette
        items={[]}
        sources={[{ id: 'docs', group: 'Documents', search, debounceMs: 0 }]}
        defaultOpen
      />,
    );
    await user.type(input(), 'q');
    expect(await screen.findByText('Could not load documents.')).toBeInTheDocument();
    fail = false;
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    expect(await screen.findByRole('option', { name: 'Found it' })).toBeInTheDocument();
    expect(input()).toHaveFocus();
  });

  it('ignores a single-key hotkey while typing in another field, but not a modified one', async () => {
    const user = userEvent.setup();
    render(
      <>
        <input aria-label="Notes" />
        <CommandPalette items={ITEMS} hotkey={['/', 'mod+k']} />
      </>,
    );
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('/');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(screen.getByRole('textbox', { name: 'Notes' })).toHaveValue('/');
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    // The hotkey toggles it closed again.
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.queryByRole('dialog')).toBeNull();
    // Outside fields the single key works.
    await user.click(document.body);
    await user.keyboard('/');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('can keep modified hotkeys out of fields, or turn the hotkey off', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <>
        <input aria-label="Notes" />
        <CommandPalette items={ITEMS} hotkeyInFields={false} />
      </>,
    );
    await user.click(screen.getByRole('textbox', { name: 'Notes' }));
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.queryByRole('dialog')).toBeNull();
    rerender(
      <>
        <input aria-label="Notes" />
        <CommandPalette items={ITEMS} hotkey={false} />
      </>,
    );
    await user.click(document.body);
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('works controlled, with a trigger and a controlled query', async () => {
    const user = userEvent.setup();
    function Controlled() {
      const [open, setOpen] = useState(false);
      const [q, setQ] = useState('');
      return (
        <>
          <CommandPalette
            items={ITEMS}
            open={open}
            onOpenChange={setOpen}
            search={q}
            onSearchChange={setQ}
            trigger={<button type="button">Search</button>}
          />
          <output data-testid="q">{q}</output>
        </>
      );
    }
    render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(input()).toHaveFocus();
    await user.type(input(), 'set');
    expect(screen.getByTestId('q')).toHaveTextContent('set');
    expect(optionNames()).toEqual(['SettingsWorkspace']);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    // Reopening starts from an empty query.
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(input()).toHaveValue('');
  });

  it('keeps the palette open for keepOpen items and follows the pointer', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();
    render(
      <CommandPalette
        items={[
          { id: 'a', label: 'Toggle theme', keepOpen: true, onSelect },
          { id: 'b', label: 'Copy link' },
        ]}
        defaultOpen
      />,
    );
    fireEvent.pointerMove(screen.getByRole('option', { name: 'Copy link' }));
    expect(active()).toHaveTextContent('Copy link');
    await user.click(screen.getByRole('option', { name: 'Toggle theme' }));
    expect(onSelect).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    // Clicking a disabled option does nothing; Page keys jump within the list.
    await user.keyboard('{PageDown}');
    expect(active()).toHaveTextContent('Copy link');
    await user.keyboard('{PageUp}');
    expect(active()).toHaveTextContent('Toggle theme');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <CommandPalette items={ITEMS} trigger={<button type="button">Search</button>} />,
    );
    expect(html).toContain('Search');
  });
});

describe('hotkey helpers', () => {
  it('formats hotkeys per platform', () => {
    expect(formatHotkey('mod+k', { apple: true })).toEqual(['⌘', 'K']);
    expect(formatHotkey('mod+shift+p', { apple: false })).toEqual(['Ctrl', 'Shift', 'P']);
    expect(formatHotkey('enter', { apple: true })).toEqual(['↵']);
    expect(formatHotkey('shift+f2', { apple: false })).toEqual(['Shift', 'F2']);
  });

  it('matches events, letting symbols carry their own Shift', () => {
    const ev = (init: KeyboardEventInit) => new KeyboardEvent('keydown', init);
    expect(matchesHotkey(ev({ key: 'k', ctrlKey: true }), 'mod+k')).toBe(true);
    expect(matchesHotkey(ev({ key: 'k' }), 'mod+k')).toBe(false);
    expect(matchesHotkey(ev({ key: 'K', ctrlKey: true, shiftKey: true }), 'mod+k')).toBe(false);
    expect(matchesHotkey(ev({ key: '?', shiftKey: true }), '?')).toBe(true);
    expect(matchesHotkey(ev({ key: '/' }), ['x', '/'])).toBe(true);
  });

  it('knows typing targets', () => {
    const text = document.createElement('input');
    const box = document.createElement('input');
    box.type = 'checkbox';
    const area = document.createElement('textarea');
    expect(isTypingTarget(text)).toBe(true);
    expect(isTypingTarget(box)).toBe(false);
    expect(isTypingTarget(area)).toBe(true);
    expect(isTypingTarget(document.body)).toBe(false);
  });

  it('ranks with the default filter', () => {
    const item = { id: '1', label: 'Create project', keywords: ['new'] };
    expect(defaultCommandFilter(item, 'cre')).toBeGreaterThan(defaultCommandFilter(item, 'proj'));
    expect(defaultCommandFilter(item, 'proj')).toBeGreaterThan(defaultCommandFilter(item, 'new'));
    expect(defaultCommandFilter(item, 'create zzz')).toBe(0);
    expect(defaultCommandFilter({ id: '2', label: 'Café' }, 'cafe')).toBeGreaterThan(0);
  });
});
