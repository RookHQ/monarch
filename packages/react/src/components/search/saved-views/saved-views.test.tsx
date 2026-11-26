import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SavedViews, type SavedView } from './index.js';

const VIEWS: SavedView[] = [
  { id: 'all', name: 'All open', count: 42, readOnly: true, group: 'Built-in' },
  { id: 'mine', name: 'Assigned to me', count: 7, isDefault: true, group: 'My views' },
  { id: 'urgent', name: 'Urgent', count: 3, shared: true, group: 'My views' },
];

const open = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole('button', { name: /^View:/ }));
  return screen.findByRole('menu');
};

describe('SavedViews', () => {
  it('shows the current view, count and modified marker in the trigger', async () => {
    render(<SavedViews views={VIEWS} value="mine" modified />);
    const trigger = screen.getByRole('button', { name: /View:/ });
    expect(trigger).toHaveTextContent('View: Assigned to me 7 Edited');
    expect(trigger).toHaveAttribute('data-modified', 'true');
    await expectNoA11yViolations();
  });

  it('switches views (uncontrolled) through radio items grouped by heading', async () => {
    const onValueChange = vi.fn();
    render(<SavedViews views={VIEWS} defaultValue="all" onValueChange={onValueChange} />);
    const user = userEvent.setup();
    const menu = await open(user);
    expect(within(menu).getByText('Built-in')).toBeInTheDocument();
    expect(within(menu).getByText('My views')).toBeInTheDocument();
    expect(within(menu).getByRole('menuitemradio', { name: /All open/ })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(
      within(menu).getByRole('menuitemradio', { name: /Assigned to me ?, Default view/ }),
    ).toBeInTheDocument();
    await expectNoA11yViolations(menu);
    await user.click(within(menu).getByRole('menuitemradio', { name: /Urgent/ }));
    expect(onValueChange).toHaveBeenCalledWith('urgent');
    expect(screen.getByRole('button', { name: /View:/ })).toHaveTextContent('Urgent');
  });

  it('offers save changes, discard, rename, default and delete only where allowed', async () => {
    const onSaveChanges = vi.fn();
    const onDiscardChanges = vi.fn();
    const onSetDefault = vi.fn();
    const props = {
      views: VIEWS,
      modified: true,
      onSaveChanges,
      onDiscardChanges,
      onSetDefault,
      onRename: vi.fn(),
      onDelete: vi.fn(),
      onSaveNew: vi.fn(),
    };
    const { rerender } = render(<SavedViews {...props} value="all" />);
    const user = userEvent.setup();
    let menu = await open(user);
    // Read-only view: no save changes, rename or delete.
    expect(within(menu).queryByRole('menuitem', { name: /Save changes/ })).toBeNull();
    expect(within(menu).queryByRole('menuitem', { name: 'Rename…' })).toBeNull();
    expect(within(menu).getByRole('menuitem', { name: 'Save as new view…' })).toBeInTheDocument();
    await user.click(within(menu).getByRole('menuitem', { name: 'Discard changes' }));
    expect(onDiscardChanges).toHaveBeenCalledOnce();
    rerender(<SavedViews {...props} value="urgent" />);
    menu = await open(user);
    await user.click(within(menu).getByRole('menuitem', { name: 'Save changes to “Urgent”' }));
    expect(onSaveChanges).toHaveBeenCalledWith('urgent');
    menu = await open(user);
    await user.click(within(menu).getByRole('menuitem', { name: 'Set as default' }));
    expect(onSetDefault).toHaveBeenCalledWith('urgent');
    rerender(<SavedViews {...props} value="mine" modified={false} />);
    menu = await open(user);
    expect(within(menu).queryByRole('menuitem', { name: 'Set as default' })).toBeNull();
    expect(within(menu).queryByRole('menuitem', { name: 'Discard changes' })).toBeNull();
  });

  it('saves a new view with validation and returns focus to the trigger', async () => {
    function Demo() {
      const [views, setViews] = useState(VIEWS);
      const [value, setValue] = useState<string | null>('all');
      return (
        <SavedViews
          views={views}
          value={value}
          onValueChange={setValue}
          modified
          onSaveNew={async (name) => {
            const id = name.toLowerCase();
            setViews((v) => [...v, { id, name }]);
            setValue(id);
          }}
        />
      );
    }
    render(<Demo />);
    const user = userEvent.setup();
    const menu = await open(user);
    await user.click(within(menu).getByRole('menuitem', { name: 'Save as new view…' }));
    const dialog = await screen.findByRole('dialog', { name: 'Save view' });
    const input = within(dialog).getByRole('textbox', { name: /Name/ });
    await user.click(within(dialog).getByRole('button', { name: 'Save' }));
    expect(within(dialog).getByText('Enter a name.')).toBeInTheDocument();
    expect(input).toHaveAttribute('aria-invalid', 'true');
    await user.type(input, 'urgent');
    await user.keyboard('{Enter}');
    expect(within(dialog).getByText('A view with this name already exists.')).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, 'Waiting on customer{Enter}');
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    const trigger = screen.getByRole('button', { name: /View:/ });
    expect(trigger).toHaveTextContent('Waiting on customer');
    await waitFor(() => expect(trigger).toHaveFocus());
  });

  it('keeps the dialog open with an error when saving fails', async () => {
    render(
      <SavedViews
        views={VIEWS}
        value="urgent"
        onRename={() => Promise.reject(new Error('nope'))}
      />,
    );
    const user = userEvent.setup();
    const menu = await open(user);
    await user.click(within(menu).getByRole('menuitem', { name: 'Rename…' }));
    const dialog = await screen.findByRole('dialog', { name: 'Rename view' });
    const input = within(dialog).getByRole('textbox', { name: /Name/ });
    expect(input).toHaveValue('Urgent');
    await user.clear(input);
    await user.type(input, 'Hot{Enter}');
    expect(
      await within(dialog).findByText('The view could not be saved. Try again.'),
    ).toBeInTheDocument();
  });

  it('confirms before deleting', async () => {
    const onDelete = vi.fn();
    render(<SavedViews views={VIEWS} value="urgent" onDelete={onDelete} />);
    const user = userEvent.setup();
    const menu = await open(user);
    await user.click(within(menu).getByRole('menuitem', { name: 'Delete view…' }));
    const dialog = await screen.findByRole('alertdialog');
    expect(dialog).toHaveTextContent('Delete “Urgent”?');
    await user.click(within(dialog).getByRole('button', { name: 'Delete view' }));
    expect(onDelete).toHaveBeenCalledWith('urgent');
  });

  it('opens from the keyboard', async () => {
    render(<SavedViews views={VIEWS} value="mine" />);
    const user = userEvent.setup();
    screen.getByRole('button', { name: /View:/ }).focus();
    await user.keyboard('{Enter}');
    await screen.findByRole('menu');
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('menu')).toBeNull());
    expect(screen.getByRole('button', { name: /View:/ })).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<SavedViews views={VIEWS} value="urgent" modified />);
    expect(html).toContain('mn-saved-views');
    expect(html).toContain('Urgent');
    expect(html).toContain('Edited');
  });
});
