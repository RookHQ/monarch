import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SpaceSwitcher, type SpaceOption } from './index.js';

const spaces: SpaceOption[] = [
  { id: 's-eng', name: 'Engineering', description: '42 members' },
  { id: 's-mkt', name: 'Marketing' },
  { id: 's-ops', name: 'Operations' },
];

describe('SpaceSwitcher', () => {
  it('names the trigger with the current Space and starts on All spaces', async () => {
    render(<SpaceSwitcher spaces={spaces} />);
    expect(screen.getByRole('button', { name: 'Space: All spaces' })).toHaveAttribute(
      'aria-haspopup',
      'dialog',
    );
    await expectNoA11yViolations();
  });

  it('opens a searchable list with the current Space selected', async () => {
    const user = userEvent.setup();
    render(
      <SpaceSwitcher
        spaces={spaces}
        defaultValue="s-mkt"
        manageHref="https://id.example.com/spaces"
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Space: Marketing' }));
    const dialog = await screen.findByRole('dialog', { name: 'Space' });
    const list = within(dialog).getByRole('listbox');
    expect(within(list).getByRole('option', { name: /Marketing/ })).toHaveAttribute(
      'aria-selected',
      'true',
    );
    expect(within(list).getByRole('option', { name: /All spaces/ })).toHaveAttribute(
      'aria-selected',
      'false',
    );
    expect(within(dialog).getByRole('combobox', { name: 'Search spaces…' })).toHaveFocus();
    const manage = within(dialog).getByRole('link', { name: 'Manage Spaces (opens in a new tab)' });
    expect(manage).toHaveAttribute('target', '_blank');
    await expectNoA11yViolations();
  });

  it('filters by typing and chooses with the keyboard', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    render(<SpaceSwitcher spaces={spaces} onValueChange={onValueChange} />);
    const trigger = screen.getByRole('button', { name: 'Space: All spaces' });
    trigger.focus();
    await user.keyboard('{Enter}');
    await user.keyboard('ope');
    const list = await screen.findByRole('listbox');
    expect(within(list).queryByRole('option', { name: /Marketing/ })).not.toBeInTheDocument();
    await user.keyboard('{ArrowDown}{Enter}');
    expect(onValueChange).toHaveBeenCalledWith('s-ops');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Space: Operations' })).toHaveFocus();
  });

  it('All spaces clears the choice; without allowAll it is not offered', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const { unmount } = render(
      <SpaceSwitcher spaces={spaces} defaultValue="s-eng" onValueChange={onValueChange} />,
    );
    await user.click(screen.getByRole('button', { name: 'Space: Engineering' }));
    await user.click(await screen.findByRole('option', { name: /All spaces/ }));
    expect(onValueChange).toHaveBeenCalledWith(null);
    expect(screen.getByRole('button', { name: 'Space: All spaces' })).toBeInTheDocument();
    unmount();

    render(<SpaceSwitcher spaces={spaces} value="s-eng" allowAll={false} />);
    await user.click(screen.getByRole('button', { name: 'Space: Engineering' }));
    const list = await screen.findByRole('listbox');
    expect(within(list).queryByRole('option', { name: /All spaces/ })).not.toBeInTheDocument();
  });

  it('is controlled', async () => {
    const user = userEvent.setup();
    const onValueChange = vi.fn();
    const { rerender } = render(
      <SpaceSwitcher spaces={spaces} value="s-eng" onValueChange={onValueChange} />,
    );
    await user.click(screen.getByRole('button', { name: 'Space: Engineering' }));
    await user.click(await screen.findByRole('option', { name: /Marketing/ }));
    expect(onValueChange).toHaveBeenCalledWith('s-mkt');
    expect(screen.getByRole('button', { name: 'Space: Engineering' })).toBeInTheDocument();
    rerender(<SpaceSwitcher spaces={spaces} value="s-mkt" onValueChange={onValueChange} />);
    expect(screen.getByRole('button', { name: 'Space: Marketing' })).toBeInTheDocument();
  });

  it('treats a Space that no longer exists as All spaces', () => {
    render(<SpaceSwitcher spaces={spaces} value="s-gone" />);
    expect(screen.getByRole('button', { name: 'Space: All spaces' })).toBeInTheDocument();
  });

  it('New Space runs the action after closing; Escape returns focus', async () => {
    const user = userEvent.setup();
    const onCreate = vi.fn();
    render(<SpaceSwitcher spaces={spaces} onCreate={onCreate} />);
    const trigger = screen.getByRole('button', { name: 'Space: All spaces' });
    await user.click(trigger);
    await user.click(await screen.findByRole('button', { name: 'New Space' }));
    expect(onCreate).toHaveBeenCalled();
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    await user.click(trigger);
    await screen.findByRole('dialog');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(trigger).toHaveFocus();
  });

  it('hides itself with no Spaces, but shows loading', async () => {
    const { container, rerender } = render(<SpaceSwitcher spaces={[]} />);
    expect(container).toBeEmptyDOMElement();
    rerender(<SpaceSwitcher spaces={[]} loading />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Space: All spaces' }));
    expect(await screen.findByText('Loading spaces…')).toBeInTheDocument();
  });

  it('collapsed: icon only, with the name in a tooltip', async () => {
    render(<SpaceSwitcher spaces={spaces} defaultValue="s-ops" collapsed />);
    const trigger = screen.getByRole('button', { name: 'Space: Operations' });
    expect(trigger).toHaveAttribute('data-collapsed');
    await userEvent.setup().hover(trigger);
    expect(await screen.findByRole('tooltip')).toHaveTextContent('Space: Operations');
  });

  it('translates', () => {
    render(
      <SpaceSwitcher spaces={spaces} messages={{ label: 'Espace', all: 'Tous les espaces' }} />,
    );
    expect(screen.getByRole('button', { name: 'Espace: Tous les espaces' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<SpaceSwitcher spaces={spaces} defaultValue="s-eng" />);
    expect(html).toContain('mn-space-switcher');
    expect(html).toContain('Engineering');
  });
});
