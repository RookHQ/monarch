import { createRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ScopeSelector, type ScopeGroup } from './index.js';

const groups: ScopeGroup[] = [
  {
    id: 'projects',
    label: 'Projects API',
    scopes: [
      { value: 'projects:read', description: 'Read projects' },
      { value: 'projects:write', description: 'Create and update projects' },
    ],
  },
  {
    id: 'reports',
    label: 'Reports API',
    scopes: [{ value: 'reports:read' }, { value: 'reports:admin', disabled: true }],
  },
];

function Controlled(props: Partial<Parameters<typeof ScopeSelector>[0]>) {
  const [value, setValue] = useState<string[]>([]);
  return (
    <>
      <ScopeSelector groups={groups} value={value} onValueChange={setValue} {...props} />
      <output data-testid="value">{value.join(',')}</output>
    </>
  );
}

describe('ScopeSelector', () => {
  it('names the fieldset and shows a collapsed summary per group', async () => {
    render(<ScopeSelector groups={groups} defaultValue={['projects:read']} />);
    expect(screen.getByRole('group', { name: 'Scopes' })).toHaveClass('mn-scope-selector');
    const trigger = screen.getByRole('button', { name: /^Projects API/ });
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(trigger).toHaveTextContent('1 of 2 scopes');
    expect(screen.queryByRole('checkbox')).toBeNull();
    await expectNoA11yViolations();
  });

  it('expands a group and toggles scopes', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const trigger = screen.getByRole('button', { name: /^Projects API/ });
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    const panel = screen.getByRole('group', { name: 'Projects API' });
    expect(trigger).toHaveAttribute('aria-controls', panel.id);
    await user.click(within(panel).getByRole('checkbox', { name: 'projects:write' }));
    expect(screen.getByTestId('value')).toHaveTextContent('projects:write');
    await user.click(within(panel).getByRole('checkbox', { name: 'projects:write' }));
    expect(screen.getByTestId('value')).toHaveTextContent('');
    await expectNoA11yViolations();
  });

  it('selects and clears all enabled scopes of a group', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.click(screen.getByRole('button', { name: 'Select all Reports API' }));
    expect(screen.getByTestId('value')).toHaveTextContent('reports:read');
    expect(screen.getByTestId('value')).not.toHaveTextContent('reports:admin');
    await user.click(screen.getByRole('button', { name: 'Clear all Reports API' }));
    expect(screen.getByTestId('value')).toHaveTextContent('');
  });

  it('works from the keyboard', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.tab();
    expect(screen.getByRole('button', { name: /^Projects API/ })).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.tab();
    expect(screen.getByRole('button', { name: 'Select all Projects API' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('checkbox', { name: 'projects:read' })).toHaveFocus();
    await user.keyboard(' ');
    expect(screen.getByTestId('value')).toHaveTextContent('projects:read');
  });

  it('pages long lists and moves focus to the first new group', async () => {
    const user = userEvent.setup();
    const many: ScopeGroup[] = Array.from({ length: 5 }, (_, i) => ({
      id: `g${i}`,
      label: `Group ${i}`,
      scopes: [{ value: `g${i}:read` }],
    }));
    render(<ScopeSelector groups={many} pageSize={2} />);
    expect(screen.getAllByRole('listitem')).toHaveLength(2);
    await user.click(screen.getByRole('button', { name: 'Show 2 more of 3' }));
    expect(screen.getAllByRole('listitem')).toHaveLength(4);
    expect(screen.getByRole('button', { name: /^Group 2/ })).toHaveFocus();
    await user.click(screen.getByRole('button', { name: 'Show 1 more of 1' }));
    expect(screen.queryByRole('button', { name: /Show/ })).toBeNull();
    expect(screen.getByRole('button', { name: /^Group 4/ })).toHaveFocus();
  });

  it('auto-selects groups once their scopes load', () => {
    const onValueChange = vi.fn();
    const loading: ScopeGroup[] = [
      { id: 'projects', label: 'Projects API', scopes: [], loading: true },
    ];
    const { rerender } = render(
      <ScopeSelector groups={loading} onValueChange={onValueChange} autoSelect="all" />,
    );
    expect(screen.getByText('Loading scopes…')).toBeInTheDocument();
    expect(onValueChange).not.toHaveBeenCalled();
    rerender(<ScopeSelector groups={groups} onValueChange={onValueChange} autoSelect="all" />);
    expect(onValueChange).toHaveBeenLastCalledWith([
      'projects:read',
      'projects:write',
      'reports:read',
    ]);
  });

  it('with autoSelect="added" leaves initial groups alone', () => {
    const onValueChange = vi.fn();
    const { rerender } = render(
      <ScopeSelector
        groups={groups.slice(0, 1)}
        onValueChange={onValueChange}
        autoSelect="added"
      />,
    );
    expect(onValueChange).not.toHaveBeenCalled();
    rerender(<ScopeSelector groups={groups} onValueChange={onValueChange} autoSelect="added" />);
    expect(onValueChange).toHaveBeenLastCalledWith(['reports:read']);
  });

  it('disables changes but still lets groups expand', async () => {
    const user = userEvent.setup();
    render(<ScopeSelector groups={groups} disabled defaultExpanded="all" />);
    expect(screen.queryByRole('button', { name: /Select all/ })).toBeNull();
    for (const box of screen.getAllByRole('checkbox')) expect(box).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /^Projects API/ }));
    expect(screen.getByRole('button', { name: /^Projects API/ })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
  });

  it('shows the empty state and custom messages', () => {
    const ref = createRef<HTMLFieldSetElement>();
    render(
      <ScopeSelector
        ref={ref}
        groups={[]}
        messages={{ label: 'Permissions', empty: 'Choose a resource first.' }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Permissions' })).toBe(ref.current);
    expect(screen.getByText('Choose a resource first.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<ScopeSelector groups={groups} defaultExpanded={['projects']} />);
    expect(html).toContain('mn-scope-selector');
    expect(html).toContain('projects:read');
  });
});
