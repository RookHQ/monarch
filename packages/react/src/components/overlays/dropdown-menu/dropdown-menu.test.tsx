import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from './index.js';

describe('DropdownMenu', () => {
  it('opens from the keyboard and moves with arrow keys', async () => {
    const onArchive = vi.fn();
    render(
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button>Actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Rename</DropdownMenuItem>
          <DropdownMenuItem onSelect={onArchive}>Archive</DropdownMenuItem>
          <DropdownMenuItem tone="danger" shortcut="⌘⌫">
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Actions' }).focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    await waitFor(() => expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus());
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('menuitem', { name: 'Archive' })).toHaveFocus();
    await expectNoA11yViolations(menu);
    await user.keyboard('{Enter}');
    expect(onArchive).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('marks destructive items with data-tone and hides their icon from assistive technology', async () => {
    const onDelete = vi.fn();
    render(
      <DropdownMenu defaultOpen>
        <DropdownMenuTrigger asChild>
          <Button>Actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Rename</DropdownMenuItem>
          <DropdownMenuItem tone="danger" icon={<svg />} onSelect={onDelete}>
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );
    const user = userEvent.setup();
    const remove = await screen.findByRole('menuitem', { name: 'Delete' });
    expect(remove).toHaveAttribute('data-tone', 'danger');
    expect(remove.querySelector('.mn-menu__icon')).toHaveAttribute('aria-hidden');
    expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveAttribute(
      'data-tone',
      'default',
    );
    await user.keyboard('{End}');
    expect(remove).toHaveFocus();
    expect(remove).toHaveAttribute('data-highlighted');
    await user.keyboard('{Enter}');
    expect(onDelete).toHaveBeenCalledOnce();
  });

  it('renders on the server (closed)', () => {
    const html = renderToString(
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button>Actions</Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem>Rename</DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>,
    );
    expect(html).toContain('aria-haspopup="menu"');
  });
});
