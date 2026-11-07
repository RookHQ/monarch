import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AppAccessPicker, AppAccessSummary, type AppAccessOption } from './index.js';

const APPS: AppAccessOption[] = [
  { id: 'notes', name: 'Notes', description: 'Shared notes and docs', badge: 'Beta' },
  { id: 'planner', name: 'Planner', logoSrc: 'https://example.com/planner.png' },
  { id: 'billing', name: 'Billing', disabled: true },
];

function Controlled({ initial = [] as string[], onChange = (_: string[]) => {} }) {
  const [value, setValue] = useState(initial);
  return (
    <AppAccessPicker
      apps={APPS}
      value={value}
      onValueChange={(next) => {
        setValue(next);
        onChange(next);
      }}
    />
  );
}

describe('AppAccessPicker', () => {
  it('renders a named group of labelled checkboxes', async () => {
    render(<Controlled initial={['planner']} />);
    expect(screen.getByRole('group', { name: 'App access' })).toHaveClass('mn-app-access-picker');
    const notes = screen.getByRole('checkbox', { name: /Notes/ });
    expect(notes).not.toBeChecked();
    expect(notes).toHaveAccessibleDescription('Shared notes and docs');
    expect(screen.getByRole('checkbox', { name: 'Planner' })).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Billing' })).toBeDisabled();
    expect(screen.getByText('1 of 3 selected')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('toggles with the label, the keyboard and select all / clear all', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Controlled initial={['billing']} onChange={onChange} />);
    await user.click(screen.getByText('Notes'));
    expect(onChange).toHaveBeenLastCalledWith(['billing', 'notes']);
    screen.getByRole('checkbox', { name: 'Planner' }).focus();
    await user.keyboard(' ');
    expect(onChange).toHaveBeenLastCalledWith(['billing', 'notes', 'planner']);
    // Billing is fixed (disabled) and stays selected through Clear all.
    await user.click(screen.getByRole('button', { name: 'Clear all' }));
    expect(onChange).toHaveBeenLastCalledWith(['billing']);
    await user.click(screen.getByRole('button', { name: 'Select all' }));
    expect(onChange).toHaveBeenLastCalledWith(['billing', 'notes', 'planner']);
  });

  it('disables everything, shows loading and empty states', () => {
    const { rerender } = render(
      <AppAccessPicker apps={APPS} value={[]} onValueChange={() => {}} disabled />,
    );
    for (const box of screen.getAllByRole('checkbox')) expect(box).toBeDisabled();
    rerender(<AppAccessPicker apps={APPS} value={[]} onValueChange={() => {}} loading />);
    expect(screen.getByRole('group')).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('Loading apps');
    rerender(<AppAccessPicker apps={[]} value={[]} onValueChange={() => {}} />);
    expect(screen.getByText('No apps are available yet.')).toBeInTheDocument();
  });

  it('takes help text and translated messages', () => {
    render(
      <AppAccessPicker
        apps={APPS}
        value={[]}
        onValueChange={() => {}}
        hideLabel
        description="Applied when they accept."
        messages={{ label: 'Apps', selectAll: 'Alle auswählen' }}
      />,
    );
    const group = screen.getByRole('group', { name: 'Apps' });
    expect(group).toHaveAccessibleDescription('Applied when they accept.');
    expect(screen.getByRole('button', { name: 'Alle auswählen' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AppAccessPicker apps={APPS} value={['notes']} onValueChange={() => {}} />,
    );
    expect(html).toContain('mn-app-access-picker');
    expect(html).toContain('Planner');
  });
});

describe('AppAccessSummary', () => {
  it('shows sorted names, then +N with the rest spoken', async () => {
    render(
      <AppAccessSummary data-testid="s" apps={APPS} value={['planner', 'notes', 'billing']} />,
    );
    const summary = screen.getByTestId('s');
    expect(summary).toHaveTextContent('Billing');
    expect(summary).toHaveTextContent('+1');
    expect(summary.textContent).toContain(', and 1 more: Planner');
    await expectNoA11yViolations();
  });

  it('names the empty state and unknown ids', () => {
    const { rerender } = render(<AppAccessSummary data-testid="s" apps={APPS} value={[]} />);
    expect(screen.getByTestId('s')).toHaveTextContent('No app access');
    rerender(<AppAccessSummary data-testid="s" apps={APPS} value={['legacy-app']} />);
    expect(screen.getByTestId('s')).toHaveTextContent('legacy-app');
  });

  it('renders on the server', () => {
    expect(renderToString(<AppAccessSummary apps={APPS} value={['notes']} />)).toContain(
      'mn-app-access-summary',
    );
  });
});
