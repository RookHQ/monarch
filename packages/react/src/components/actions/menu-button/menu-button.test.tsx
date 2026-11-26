import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DropdownMenuItem } from '../../overlays/dropdown-menu/index.js';
import { MenuButton } from './index.js';

describe('MenuButton', () => {
  it('follows the menu button pattern: aria attributes, Enter opens on the first item, Escape returns focus', async () => {
    const user = userEvent.setup();
    const onArchive = vi.fn();
    render(
      <MenuButton
        label="Actions"
        items={[
          { label: 'Rename', onSelect: () => {} },
          { label: 'Archive', onSelect: onArchive },
          'separator',
          { label: 'Delete', tone: 'danger', shortcut: '⌘⌫' },
        ]}
      />,
    );
    const button = screen.getByRole('button', { name: 'Actions' });
    expect(button).toHaveAttribute('aria-haspopup', 'menu');
    expect(button).toHaveAttribute('aria-expanded', 'false');
    button.focus();
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(menu).toHaveAttribute('aria-labelledby', button.id);
    await waitFor(() => expect(screen.getByRole('menuitem', { name: 'Rename' })).toHaveFocus());
    expect(screen.getByRole('separator')).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /Delete/ })).toHaveAttribute('data-tone', 'danger');
    await expectNoA11yViolations(menu);
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    expect(button).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    await screen.findByRole('menu');
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onArchive).toHaveBeenCalledOnce();
    await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
  });

  it('accepts DropdownMenu children and controlled open state', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(
      <MenuButton label="Export" open onOpenChange={onOpenChange}>
        <DropdownMenuItem>CSV</DropdownMenuItem>
        <DropdownMenuItem>PDF</DropdownMenuItem>
      </MenuButton>,
    );
    expect(await screen.findByRole('menuitem', { name: 'CSV' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('renders safe links and drops unsafe ones', async () => {
    render(
      <MenuButton
        label="Open"
        defaultOpen
        items={[
          { label: 'Docs', href: 'https://example.com/docs' },
          { label: 'Bad', href: 'javascript:alert(1)' },
        ]}
      />,
    );
    const docs = await screen.findByRole('menuitem', { name: 'Docs' });
    expect(docs.tagName).toBe('A');
    expect(docs).toHaveAttribute('href', 'https://example.com/docs');
    const bad = screen.getByRole('menuitem', { name: 'Bad' });
    expect(bad.tagName).not.toBe('A');
    expect(bad).not.toHaveAttribute('href');
  });

  it('does not render a disabled entry as a link', async () => {
    render(
      <MenuButton
        label="Open"
        defaultOpen
        items={[{ label: 'Billing', href: 'https://example.com/billing', disabled: true }]}
      />,
    );
    const billing = await screen.findByRole('menuitem', { name: 'Billing' });
    expect(billing.tagName).not.toBe('A');
    expect(billing).not.toHaveAttribute('href');
    expect(billing).toHaveAttribute('aria-disabled', 'true');
  });

  it('icon-only form uses the label as its accessible name and has no chevron', () => {
    render(
      <MenuButton label="More actions" iconOnly icon={<svg data-testid="icon" />} items={[]} />,
    );
    const button = screen.getByRole('button', { name: 'More actions' });
    expect(button).toHaveAttribute('data-icon-only');
    expect(button).toHaveClass('mn-icon-button');
    expect(button.querySelectorAll('svg')).toHaveLength(1);
  });

  it('does not open while disabled or loading; loading stays focusable', async () => {
    const user = userEvent.setup();
    const { rerender } = render(
      <MenuButton label="Actions" disabled items={[{ label: 'Rename' }]} />,
    );
    await user.click(screen.getByRole('button', { name: 'Actions' }));
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    rerender(<MenuButton label="Actions" loading items={[{ label: 'Rename' }]} />);
    const button = screen.getByRole('button', { name: 'Actions' });
    button.focus();
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    await user.click(button);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('renders on the server (closed)', () => {
    const html = renderToString(<MenuButton label="Actions" items={[{ label: 'Rename' }]} />);
    expect(html).toContain('mn-menu-button');
    expect(html).toContain('aria-haspopup="menu"');
    expect(html).not.toContain('Rename');
  });
});
