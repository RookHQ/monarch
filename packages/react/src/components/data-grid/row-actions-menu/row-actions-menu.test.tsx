import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RowActionsMenu, type RowAction } from './index.js';

const actions = (overrides: Partial<Record<string, Partial<RowAction>>> = {}): RowAction[] => [
  { id: 'delete', label: 'Delete', tone: 'danger', onSelect: vi.fn(), ...overrides.delete },
  { id: 'open', label: 'Open', onSelect: vi.fn(), ...overrides.open },
  { id: 'rename', label: 'Rename', shortcut: 'F2', onSelect: vi.fn(), ...overrides.rename },
  { id: 'pin', label: 'Pin', inline: true, onSelect: vi.fn() },
];

describe('RowActionsMenu', () => {
  it('opens from the keyboard, lists danger items last and returns focus', async () => {
    const user = userEvent.setup();
    const list = actions();
    render(<RowActionsMenu actions={list} rowLabel="Report 12" />);
    const trigger = screen.getByRole('button', { name: 'Actions for Report 12' });
    expect(trigger).toHaveClass('mn-row-actions-menu');
    expect(trigger).toHaveAttribute('aria-haspopup', 'menu');
    trigger.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    const items = within(menu).getAllByRole('menuitem');
    expect(items.map((i) => i.textContent)).toEqual(['Open', 'RenameF2', 'Delete']);
    expect(items[2]).toHaveAttribute('data-tone', 'danger');
    expect(within(menu).getByRole('separator')).toBeInTheDocument();
    await expectNoA11yViolations(menu);
    await user.keyboard('{ArrowDown}{Enter}');
    expect(list[2]?.onSelect).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(trigger).toHaveFocus();
  });

  it('asks for confirmation before a destructive action and runs it inside the dialog', async () => {
    const user = userEvent.setup();
    let finish: () => void = () => {};
    const onDelete = vi.fn(() => new Promise<void>((r) => (finish = r)));
    render(
      <RowActionsMenu
        rowLabel="Report 12"
        actions={actions({
          delete: {
            onSelect: onDelete,
            confirm: { title: 'Delete Report 12?', confirmLabel: 'Delete report' },
          },
        })}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Actions for Report 12' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete Report 12?' });
    expect(onDelete).not.toHaveBeenCalled();
    const confirm = within(dialog).getByRole('button', { name: 'Delete report' });
    await user.click(confirm);
    expect(onDelete).toHaveBeenCalledTimes(1);
    finish();
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Actions for Report 12' })).toHaveFocus(),
    );
  });

  it('does not run a confirmed action when cancelled', async () => {
    const user = userEvent.setup();
    const onDelete = vi.fn();
    render(
      <RowActionsMenu
        actions={actions({ delete: { onSelect: onDelete, confirm: { title: 'Delete?' } } })}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Row actions' }));
    await user.click(await screen.findByRole('menuitem', { name: 'Delete' }));
    await user.click(await screen.findByRole('button', { name: 'Cancel' }));
    expect(onDelete).not.toHaveBeenCalled();
  });

  it('skips disabled items, renders nothing without menu items and accepts messages', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <RowActionsMenu
        rowLabel="Row 1"
        messages={{ trigger: (r) => `More for ${r}` }}
        actions={actions({ open: { disabled: true } })}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'More for Row 1' }));
    expect(await screen.findByRole('menuitem', { name: 'Open' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
    await user.keyboard('{Escape}');
    rerender(<RowActionsMenu actions={[{ id: 'x', label: 'X', inline: true, onSelect() {} }]} />);
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<RowActionsMenu actions={actions()} rowLabel="Row 1" />);
    expect(html).toContain('mn-row-actions-menu');
    expect(html).toContain('Actions for Row 1');
  });
});
