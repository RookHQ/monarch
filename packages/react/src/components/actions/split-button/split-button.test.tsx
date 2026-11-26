import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { DropdownMenuItem } from '../../overlays/dropdown-menu/index.js';
import { SplitButton } from './index.js';

describe('SplitButton', () => {
  it('runs the main action and opens the alternatives from a named menu button', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    const onDraft = vi.fn();
    render(
      <SplitButton
        variant="primary"
        onClick={onSave}
        items={[{ label: 'Save as draft', onSelect: onDraft }, { label: 'Save and close' }]}
      >
        Save
      </SplitButton>,
    );
    expect(screen.getByRole('group', { name: 'Save' })).toHaveClass('mn-split-button');
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSave).toHaveBeenCalledOnce();

    await user.tab();
    const arrow = screen.getByRole('button', { name: 'More options for Save' });
    expect(arrow).toHaveFocus();
    expect(arrow).toHaveAttribute('aria-haspopup', 'menu');
    expect(arrow).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard('{Enter}');
    const menu = await screen.findByRole('menu');
    await waitFor(() =>
      expect(screen.getByRole('menuitem', { name: 'Save as draft' })).toHaveFocus(),
    );
    await expectNoA11yViolations();
    await user.keyboard('{Enter}');
    expect(onDraft).toHaveBeenCalledOnce();
    await waitFor(() => expect(menu).not.toBeInTheDocument());
    expect(arrow).toHaveFocus();
    expect(onSave).toHaveBeenCalledOnce();
  });

  it('accepts menu children, custom messages and wrapper class names', async () => {
    const user = userEvent.setup();
    render(
      <SplitButton
        className="custom"
        messages={{ moreOptions: 'Other ways to send' }}
        menuContent={<DropdownMenuItem>Schedule</DropdownMenuItem>}
      >
        Send
      </SplitButton>,
    );
    expect(screen.getByRole('group')).toHaveClass('mn-split-button', 'custom');
    await user.click(screen.getByRole('button', { name: 'Other ways to send' }));
    expect(await screen.findByRole('menuitem', { name: 'Schedule' })).toBeInTheDocument();
  });

  it('disables both halves when disabled, and only the arrow with menuDisabled', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    const { rerender } = render(
      <SplitButton disabled onClick={onClick} items={[{ label: 'Other' }]}>
        Save
      </SplitButton>,
    );
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'More options for Save' })).toBeDisabled();
    rerender(
      <SplitButton menuDisabled onClick={onClick} items={[{ label: 'Other' }]}>
        Save
      </SplitButton>,
    );
    await user.click(screen.getByRole('button', { name: 'Save' }));
    expect(onClick).toHaveBeenCalledOnce();
    await user.click(screen.getByRole('button', { name: 'More options for Save' }));
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('keeps the arrow focusable but closed while loading', async () => {
    const user = userEvent.setup();
    render(
      <SplitButton loading items={[{ label: 'Other' }]}>
        Save
      </SplitButton>,
    );
    const arrow = screen.getByRole('button', { name: 'More options for Save' });
    expect(arrow).not.toBeDisabled();
    arrow.focus();
    await user.keyboard('{Enter}');
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SplitButton items={[{ label: 'Other' }]}>Save</SplitButton>);
    expect(html).toContain('mn-split-button');
    expect(html).toContain('More options for Save');
  });
});
