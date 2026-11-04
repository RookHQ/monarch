import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FilterBar, FilterChip, FilterMenu, readFilterParams, writeFilterParams } from './index.js';

describe('FilterBar', () => {
  it('labels remove buttons and clears all', async () => {
    const onRemove = vi.fn();
    const onClearAll = vi.fn();
    render(
      <FilterBar onClearAll={onClearAll}>
        <FilterChip name="Status" value="Open" onRemove={onRemove} />
        <FilterChip name="Owner" value="Me" />
      </FilterBar>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove Status filter' }));
    expect(onRemove).toHaveBeenCalledOnce();
    await user.click(screen.getByRole('button', { name: 'Clear all' }));
    expect(onClearAll).toHaveBeenCalledOnce();
    expect(screen.getByRole('group', { name: 'Active filters' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <FilterBar onClearAll={() => {}}>
        <FilterChip name="Stage" value="Won" onRemove={() => {}} />
      </FilterBar>,
    );
    expect(html).toContain('mn-filter-bar');
    expect(html).toContain('mn-filter-chip');
  });

  it('toggle chips expose aria-pressed and a count (controlled and uncontrolled)', async () => {
    const onPressedChange = vi.fn();
    render(
      <FilterBar label="Quick filters" scrollable>
        <FilterChip
          name="Unread"
          count={4}
          defaultPressed={false}
          onPressedChange={onPressedChange}
        />
        <FilterChip name="Mine" pressed />
      </FilterBar>,
    );
    const user = userEvent.setup();
    const unread = screen.getByRole('button', { name: /Unread/ });
    expect(unread).toHaveAttribute('aria-pressed', 'false');
    expect(unread).toHaveTextContent('Unread4');
    await user.click(unread);
    expect(unread).toHaveAttribute('aria-pressed', 'true');
    expect(onPressedChange).toHaveBeenCalledWith(true);
    const mine = screen.getByRole('button', { name: 'Mine' });
    await user.click(mine);
    expect(mine).toHaveAttribute('aria-pressed', 'true');
    await expectNoA11yViolations();
  });

  it('edits a chip value in a popover', async () => {
    render(
      <FilterBar>
        <FilterChip
          name="Status"
          value="Open"
          onRemove={() => {}}
          editor={<button type="button">Closed</button>}
        />
      </FilterBar>,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: /Status.*Open/ });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger);
    const dialog = await screen.findByRole('dialog', { name: 'Status filter' });
    expect(within(dialog).getByRole('button', { name: 'Closed' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(trigger).toHaveFocus();
  });

  it('renders search, active count, result count and trailing slots; landmark mode', () => {
    render(
      <FilterBar
        landmark
        label="Filter deals"
        search={<input aria-label="Search deals" />}
        activeCount={2}
        resultCount="24 results"
        end={<button type="button">Sort</button>}
        onClearAll={() => {}}
      >
        <FilterChip name="Stage" value="Won" />
      </FilterBar>,
    );
    const bar = screen.getByRole('search', { name: 'Filter deals' });
    expect(within(bar).getByText('2 filters')).toBeInTheDocument();
    expect(within(bar).getByRole('status')).toHaveTextContent('24 results');
    expect(within(bar).getByRole('button', { name: 'Sort' })).toBeInTheDocument();
    expect(within(bar).getByRole('textbox', { name: 'Search deals' })).toBeInTheDocument();
  });

  it('collapses into a sheet below collapseBelow', async () => {
    const spy = vi
      .spyOn(HTMLElement.prototype, 'getBoundingClientRect')
      .mockReturnValue({ width: 320 } as DOMRect);
    const onClearAll = vi.fn();
    render(
      <FilterBar collapseBelow={600} activeCount={1} onClearAll={onClearAll}>
        <FilterChip name="Owner" value="Me" onRemove={() => {}} />
      </FilterBar>,
    );
    spy.mockRestore();
    expect(screen.queryByText('Owner')).toBeNull();
    const user = userEvent.setup();
    const button = screen.getByRole('button', { name: 'Filters (1)' });
    await user.click(button);
    const sheet = await screen.findByRole('dialog', { name: 'Filters' });
    expect(within(sheet).getByRole('button', { name: 'Remove Owner filter' })).toBeInTheDocument();
    await user.click(within(sheet).getByRole('button', { name: 'Clear all' }));
    expect(onClearAll).toHaveBeenCalledOnce();
    await user.click(within(sheet).getByRole('button', { name: 'Done' }));
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(button).toHaveFocus();
  });

  it('FilterMenu adds a filter field by keyboard', async () => {
    function Demo() {
      const [added, setAdded] = useState<string[]>([]);
      return (
        <FilterBar
          add={
            <FilterMenu
              options={[
                { value: 'stage', label: 'Stage' },
                { value: 'owner', label: 'Owner' },
              ]}
              onSelect={(v) => setAdded((a) => [...a, v])}
            />
          }
        >
          {added.map((a) => (
            <FilterChip key={a} name={a} value="Any" />
          ))}
        </FilterBar>
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Add filter' }).focus();
    await user.keyboard('{Enter}');
    await screen.findByRole('menu');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(screen.getByText('owner')).toBeInTheDocument();
  });

  it('reads and writes filters as URL search params', () => {
    expect(readFilterParams('?status=open&status=new&q=&page=2', ['status', 'q'])).toEqual({
      status: ['open', 'new'],
    });
    const next = writeFilterParams('page=2&status=old', {
      status: ['open', 'new'],
      owner: 'me',
      q: '',
    });
    expect(next.toString()).toBe('page=2&status=open&status=new&owner=me');
    expect(writeFilterParams(next, { status: null, owner: undefined }).toString()).toBe('page=2');
  });

  it('renders toggle and editable chips on the server', () => {
    const html = renderToString(
      <FilterBar search={<input aria-label="Search" />} resultCount="3 results" collapseBelow={500}>
        <FilterChip name="Unread" pressed />
        <FilterChip name="Status" value="Open" editor={<p>Pick</p>} />
      </FilterBar>,
    );
    expect(html).toContain('aria-pressed="true"');
    expect(html).toContain('mn-filter-chip__edit');
  });
});
