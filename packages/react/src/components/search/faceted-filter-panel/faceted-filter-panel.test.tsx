import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FacetedFilterPanel, facetActiveCount, type Facet, type FacetValue } from './index.js';

const FACETS: Facet[] = [
  {
    id: 'status',
    label: 'Status',
    type: 'checkbox',
    options: [
      { value: 'open', label: 'Open', count: 12 },
      { value: 'waiting', label: 'Waiting', count: 3 },
      { value: 'closed', label: 'Closed', count: 0 },
    ],
  },
  {
    id: 'type',
    label: 'Type',
    type: 'checkbox',
    maxVisible: 2,
    options: ['Bug', 'Task', 'Story', 'Epic'].map((l) => ({ value: l.toLowerCase(), label: l })),
  },
  { id: 'age', label: 'Age', type: 'range', min: 0, unit: 'days' },
];

describe('FacetedFilterPanel', () => {
  it('renders labelled groups with counts and no axe violations', async () => {
    render(<FacetedFilterPanel data-testid="root" facets={FACETS} />);
    expect(screen.getByTestId('root')).toHaveClass('mn-faceted-filter-panel');
    expect(screen.getByRole('group', { name: 'Filters' })).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Open 12' })).not.toBeChecked();
    expect(screen.getByRole('button', { name: 'Status' })).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('spinbutton', { name: 'Minimum' })).toHaveAccessibleDescription('days');
    expect(screen.getByRole('button', { name: 'Clear all' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('toggles options and clears a group (uncontrolled)', async () => {
    const onValueChange = vi.fn();
    render(<FacetedFilterPanel facets={FACETS} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Open 12' }));
    await user.click(screen.getByRole('checkbox', { name: 'Waiting 3' }));
    expect(onValueChange).toHaveBeenLastCalledWith({ status: ['open', 'waiting'] });
    expect(screen.getByRole('button', { name: 'Status 2 selected' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    await user.click(screen.getByRole('button', { name: 'Clear Status filter' }));
    expect(onValueChange).toHaveBeenLastCalledWith({});
    expect(screen.getByRole('checkbox', { name: 'Open 12' })).not.toBeChecked();
  });

  it('moves focus to the group toggle when its Clear button goes away', async () => {
    render(<FacetedFilterPanel facets={FACETS} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('checkbox', { name: 'Open 12' }));
    await user.click(screen.getByRole('button', { name: 'Clear Status filter' }));
    expect(screen.queryByRole('button', { name: 'Clear Status filter' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Status' })).toHaveFocus();
  });

  it('toggles a checkbox with Space and collapses a group with Enter', async () => {
    render(<FacetedFilterPanel facets={FACETS} />);
    const user = userEvent.setup();
    const box = screen.getByRole('checkbox', { name: 'Open 12' });
    box.focus();
    await user.keyboard(' ');
    expect(box).toBeChecked();
    const toggle = screen.getByRole('button', { name: /^Status/ });
    toggle.focus();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('checkbox', { name: 'Open 12' })).toBeNull();
    await user.keyboard('{Enter}');
    expect(screen.getByRole('checkbox', { name: 'Open 12' })).toBeChecked();
  });

  it('shows more options on request and keeps checked ones visible', async () => {
    render(<FacetedFilterPanel facets={FACETS} defaultValue={{ type: ['epic'] }} />);
    const user = userEvent.setup();
    expect(screen.getByRole('checkbox', { name: 'Epic' })).toBeChecked();
    expect(screen.queryByRole('checkbox', { name: 'Story' })).toBeNull();
    const more = screen.getByRole('button', { name: 'Show 1 more' });
    await user.click(more);
    expect(screen.getByRole('checkbox', { name: 'Story' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Show fewer' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('edits a range and flags a minimum above the maximum', async () => {
    const onValueChange = vi.fn();
    render(<FacetedFilterPanel facets={FACETS} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('spinbutton', { name: 'Minimum' }), '10');
    expect(onValueChange).toHaveBeenLastCalledWith({ age: { min: 10, max: undefined } });
    await user.type(screen.getByRole('spinbutton', { name: 'Maximum' }), '5');
    expect(screen.getByRole('alert')).toHaveTextContent('The minimum is above the maximum.');
    await user.clear(screen.getByRole('spinbutton', { name: 'Minimum' }));
    await user.clear(screen.getByRole('spinbutton', { name: 'Maximum' }));
    expect(onValueChange).toHaveBeenLastCalledWith({});
  });

  it('is controllable and clears everything', async () => {
    function Controlled() {
      const [value, setValue] = useState<FacetValue>({ status: ['open'], age: { max: 30 } });
      return <FacetedFilterPanel facets={FACETS} value={value} onValueChange={setValue} />;
    }
    render(<Controlled />);
    const user = userEvent.setup();
    expect(screen.getByRole('spinbutton', { name: 'Maximum' })).toHaveValue(30);
    await user.click(screen.getByRole('button', { name: 'Clear all' }));
    expect(screen.getByRole('checkbox', { name: 'Open 12' })).not.toBeChecked();
    expect(screen.getByRole('spinbutton', { name: 'Maximum' })).toHaveValue(null);
  });

  it('counts active filters per facet', () => {
    expect(facetActiveCount(FACETS[0]!, { status: ['a', 'b'] })).toBe(2);
    expect(facetActiveCount(FACETS[2]!, { age: { min: 1 } })).toBe(1);
    expect(facetActiveCount(FACETS[2]!, {})).toBe(0);
  });

  it('renders on the server', () => {
    const html = renderToString(<FacetedFilterPanel facets={FACETS} />);
    expect(html).toContain('mn-faceted-filter-panel');
    expect(html).toContain('Status');
  });
});
