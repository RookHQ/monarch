import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiPermissionScope, aiAccessAtLeast, type AiPermissionScopeItem } from './index.js';

const scopes: AiPermissionScopeItem[] = [
  { app: 'Calendar', resources: 'Your events only', access: 'read' },
  { app: 'Documents', access: 'write' },
  { app: 'Billing', access: 'none' },
];

function Editable({ onChange }: { onChange?: (s: AiPermissionScopeItem[]) => void }) {
  const [value, setValue] = useState(scopes);
  return (
    <AiPermissionScope
      editable
      scopes={value}
      onChange={(next) => {
        setValue(next);
        onChange?.(next);
      }}
    />
  );
}

describe('AiPermissionScope', () => {
  it('lists each app with its access as text, and forwards ref and props', async () => {
    const ref = createRef<HTMLDivElement>();
    render(<AiPermissionScope ref={ref} scopes={scopes} data-testid="root" className="extra" />);
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-permission-scope', 'extra');
    expect(ref.current).toBe(root);
    const list = screen.getByRole('list', { name: 'What the AI can access' });
    const rows = within(list).getAllByRole('listitem');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('CalendarYour events only: Can read');
    expect(rows[0]).toHaveAttribute('data-access', 'read');
    expect(rows[1]).toHaveTextContent('Can read and edit');
    expect(rows[2]).toHaveTextContent('No access');
    expect(screen.queryByRole('radiogroup')).toBeNull();
    await expectNoA11yViolations();
  });

  it('edits access with a labelled None / Read / Write control per app', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Editable onChange={onChange} />);
    const group = screen.getByRole('radiogroup', { name: 'Access to Billing' });
    expect(within(group).getByRole('radio', { name: 'None' })).toBeChecked();
    await user.click(within(group).getByRole('radio', { name: 'Read' }));
    expect(onChange).toHaveBeenLastCalledWith([
      scopes[0],
      scopes[1],
      { app: 'Billing', access: 'read' },
    ]);
    expect(within(group).getByRole('radio', { name: 'Read' })).toBeChecked();
    await expectNoA11yViolations();
  });

  it('moves between levels with the arrow keys', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Editable onChange={onChange} />);
    const group = screen.getByRole('radiogroup', { name: 'Access to Calendar' });
    within(group).getByRole('radio', { name: 'Read' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(onChange).toHaveBeenLastCalledWith([
      { ...scopes[0], access: 'write' },
      scopes[1],
      scopes[2],
    ]);
    expect(within(group).getByRole('radio', { name: 'Write' })).toHaveFocus();
  });

  it('shows a safe manage link and drops unsafe ones', () => {
    const { rerender } = render(<AiPermissionScope scopes={scopes} manageHref="/settings/ai" />);
    expect(screen.getByRole('link', { name: 'Manage permissions' })).toHaveAttribute(
      'href',
      '/settings/ai',
    );
    rerender(<AiPermissionScope scopes={scopes} manageHref="javascript:alert(1)" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('shows an empty message and takes translated strings', () => {
    const { rerender } = render(<AiPermissionScope scopes={[]} />);
    expect(screen.getByText('The AI has no access to any app.')).toBeInTheDocument();
    rerender(
      <AiPermissionScope
        scopes={[{ app: 'Kalender', access: 'read' }]}
        messages={{ label: 'Zugriff', access: { read: 'Darf lesen' } }}
      />,
    );
    expect(screen.getByRole('list', { name: 'Zugriff' })).toHaveTextContent('Darf lesen');
  });

  it('compares access levels', () => {
    expect(aiAccessAtLeast('write', 'read')).toBe(true);
    expect(aiAccessAtLeast('read', 'write')).toBe(false);
    expect(aiAccessAtLeast('none', 'none')).toBe(true);
  });

  it('renders on the server', () => {
    const html = renderToString(<AiPermissionScope scopes={scopes} editable onChange={() => {}} />);
    expect(html).toContain('mn-ai-permission-scope');
    expect(html).toContain('Access to Calendar');
  });
});
