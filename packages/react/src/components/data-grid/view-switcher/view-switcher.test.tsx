import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ViewSwitcher, viewSwitcherOptions, type ViewSwitcherBuiltIn } from './index.js';

describe('ViewSwitcher', () => {
  it('renders the default views as a labelled radio group', async () => {
    render(<ViewSwitcher data-testid="root" />);
    const group = screen.getByRole('radiogroup', { name: 'View' });
    expect(group).toBe(screen.getByTestId('root'));
    expect(group).toHaveClass('mn-view-switcher');
    expect(group).toHaveAttribute('data-labels', 'auto');
    const radios = screen.getAllByRole('radio');
    expect(radios.map((r) => r.textContent)).toEqual(['Table', 'Board', 'Calendar', 'Gallery']);
    expect(radios[0]).toBeChecked();
    expect(radios[0]?.querySelector('svg')).not.toBeNull();
    await expectNoA11yViolations();
  });

  it('moves and selects with arrow keys, with one tab stop', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    function Controlled() {
      const [view, setView] = useState<ViewSwitcherBuiltIn>('board');
      return (
        <ViewSwitcher
          value={view}
          onValueChange={(v) => {
            onValueChange(v);
            setView(v);
          }}
        />
      );
    }
    render(<Controlled />);
    await user.tab();
    expect(screen.getByRole('radio', { name: 'Board' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('radio', { name: 'Calendar' })).toHaveFocus();
    expect(screen.getByRole('radio', { name: 'Calendar' })).toBeChecked();
    await user.keyboard('{End}');
    expect(onValueChange).toHaveBeenLastCalledWith('gallery');
    await user.keyboard('{Home}');
    expect(onValueChange).toHaveBeenLastCalledWith('table');
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('accepts custom views, counts, hidden labels and messages', async () => {
    render(
      <ViewSwitcher<'list' | 'map'>
        aria-label="Layout"
        labels="hide"
        defaultValue="map"
        views={['list', { value: 'map', label: 'Map', count: 3 }]}
        messages={{ list: 'Rows' }}
      />,
    );
    expect(screen.getByRole('radiogroup', { name: 'Layout' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Rows' })).toHaveAttribute('title', 'Rows');
    expect(screen.getByRole('radio', { name: /^Map ?3$/ })).toBeChecked();
    await expectNoA11yViolations();
  });

  it('builds options with built-in labels and icons', () => {
    const [table, custom] = viewSwitcherOptions(['table', { value: 'x', disabled: true }]);
    expect(table?.label).toBe('Table');
    expect(table?.icon).toBeTruthy();
    expect(custom).toEqual({ value: 'x', label: 'x', disabled: true });
  });

  it('renders on the server', () => {
    const html = renderToString(<ViewSwitcher defaultValue="calendar" />);
    expect(html).toContain('mn-view-switcher');
    expect(html).toContain('Calendar');
  });
});
