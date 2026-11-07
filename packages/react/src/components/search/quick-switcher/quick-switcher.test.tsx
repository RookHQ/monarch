import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { QuickSwitcher, quickSwitcherFilter, type QuickSwitcherDestination } from './index.js';

const destinations: QuickSwitcherDestination[] = [
  { id: 'c-general', label: '#general', group: 'Channels', href: '/c/general' },
  { id: 'c-design', label: '#design-review', group: 'Channels', href: '/c/design' },
  {
    id: 'p-ana',
    label: 'Ana Silva',
    description: 'ana@example.com',
    group: 'People',
    href: '/dm/ana',
  },
];
const recent: QuickSwitcherDestination[] = [destinations[1]!];

const input = () => screen.getByRole('combobox');
const options = () => screen.queryAllByRole('option').map((o) => o.textContent);

describe('QuickSwitcher', () => {
  it('opens with ⌘K / Ctrl+K as a "Jump to" combobox and shows recent destinations', async () => {
    render(<QuickSwitcher destinations={destinations} recent={recent} />);
    const user = userEvent.setup();
    await user.keyboard('{Control>}k{/Control}');
    const dialog = await screen.findByRole('dialog', { name: 'Jump to' });
    expect(dialog).toHaveClass('mn-quick-switcher');
    expect(input()).toHaveFocus();
    expect(input()).toHaveAttribute('placeholder', 'Jump to…');
    const listbox = screen.getByRole('listbox');
    expect(within(listbox).getByRole('group', { name: 'Recent' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('matches loosely and navigates with Enter, then closes', async () => {
    const onNavigate = vi.fn();
    render(<QuickSwitcher destinations={destinations} onNavigate={onNavigate} defaultOpen />);
    const user = userEvent.setup();
    await user.type(input(), 'gnl');
    expect(options()).toEqual(['#general']);
    await user.keyboard('{Enter}');
    expect(onNavigate).toHaveBeenCalledWith(destinations[0]);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('matches descriptions and keywords through the CommandPalette filter', async () => {
    render(<QuickSwitcher destinations={destinations} defaultOpen />);
    const user = userEvent.setup();
    await user.type(input(), 'example.com');
    expect(options()[0]).toContain('Ana Silva');
  });

  it('offers "Search for …" once the query is long enough', async () => {
    const onSearchAll = vi.fn();
    const onNavigate = vi.fn();
    render(
      <QuickSwitcher
        destinations={destinations}
        onSearchAll={onSearchAll}
        onNavigate={onNavigate}
        defaultOpen
      />,
    );
    const user = userEvent.setup();
    await user.type(input(), 'r');
    expect(options().some((o) => o?.includes('Search for'))).toBe(false);
    await user.type(input(), 'oadmap');
    expect(options()).toEqual(['Search for “roadmap”']);
    await user.keyboard('{Enter}');
    expect(onSearchAll).toHaveBeenCalledWith('roadmap');
    expect(onNavigate).not.toHaveBeenCalled();
  });

  it('passes source results to onNavigate', async () => {
    const onNavigate = vi.fn();
    render(
      <QuickSwitcher
        defaultOpen
        onNavigate={onNavigate}
        sources={[
          {
            id: 'files',
            group: 'Files',
            debounceMs: 0,
            search: async (q) => [{ id: 'f-1', label: `${q} notes.pdf` }],
          },
        ]}
      />,
    );
    const user = userEvent.setup();
    await user.type(input(), 'plan');
    await screen.findByRole('option', { name: /plan notes\.pdf/ });
    await user.keyboard('{Enter}');
    expect(onNavigate).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'f-1', label: 'plan notes.pdf' }),
    );
  });

  it('opens from a trigger and translates strings', async () => {
    render(
      <QuickSwitcher
        destinations={destinations}
        hotkey={false}
        trigger={<Button>Jump</Button>}
        messages={{ title: 'Springen zu', placeholder: 'Name eingeben…' }}
      />,
    );
    const user = userEvent.setup();
    await user.keyboard('{Control>}k{/Control}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Jump' }));
    expect(await screen.findByRole('dialog', { name: 'Springen zu' })).toBeInTheDocument();
    expect(input()).toHaveAttribute('placeholder', 'Name eingeben…');
  });

  it('quickSwitcherFilter ranks word matches above letter matches', () => {
    const general = { id: 'g', label: 'general' };
    expect(quickSwitcherFilter(general, 'gen')).toBeGreaterThan(
      quickSwitcherFilter(general, 'gnl'),
    );
    expect(quickSwitcherFilter(general, 'gnl')).toBe(1);
    expect(quickSwitcherFilter(general, 'xyz')).toBe(0);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <QuickSwitcher destinations={destinations} trigger={<Button>Jump</Button>} />,
    );
    expect(html).toContain('Jump');
  });
});
