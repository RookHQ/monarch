import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { Toolbar, ToolbarGroup } from './index.js';

describe('Toolbar', () => {
  it('Toolbar and ToolbarGroup forward their refs', () => {
    const toolbar = createRef<HTMLDivElement>();
    const group = createRef<HTMLDivElement>();
    render(
      <Toolbar ref={toolbar} label="Actions">
        <ToolbarGroup ref={group} aria-label="Edit">
          <Button>Bold</Button>
        </ToolbarGroup>
      </Toolbar>,
    );
    expect(toolbar.current).toHaveAttribute('role', 'toolbar');
    expect(group.current).toHaveAttribute('role', 'group');
  });

  it('moves focus with arrow keys and leaves text inputs alone', async () => {
    render(
      <Toolbar
        label="List tools"
        search={<Input aria-label="Search" />}
        end={<Button>Export</Button>}
      >
        <Button>Filter</Button>
        <Button>Sort</Button>
      </Toolbar>,
    );
    expect(screen.getByRole('toolbar', { name: 'List tools' })).toBeInTheDocument();
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Filter' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Sort' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('button', { name: 'Export' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(screen.getByRole('textbox', { name: 'Search' })).toHaveFocus();
    await user.keyboard('abc{ArrowLeft}');
    expect(screen.getByRole('textbox', { name: 'Search' })).toHaveFocus();
  });

  it('leaves Home, End and the arrows to a text field for its caret', async () => {
    render(
      <Toolbar label="List tools" search={<Input aria-label="Search" defaultValue="invoices" />}>
        <Button>Filter</Button>
        <Button>Sort</Button>
      </Toolbar>,
    );
    const user = userEvent.setup();
    const search = screen.getByRole('textbox', { name: 'Search' });
    search.focus();
    await user.keyboard('{End}');
    expect(search).toHaveFocus();
    await user.keyboard('{Home}');
    expect(search).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(search).toHaveFocus();
  });

  it('mirrors the arrow keys in a right-to-left toolbar', async () => {
    render(
      <div dir="rtl">
        <Toolbar label="List tools">
          <Button>Filter</Button>
          <Button>Sort</Button>
        </Toolbar>
      </div>,
    );
    const user = userEvent.setup();
    screen.getByRole('button', { name: 'Filter' }).focus();
    await user.keyboard('{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Sort' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('button', { name: 'Filter' })).toHaveFocus();
  });

  it('does not take Alt+Arrow, the browser shortcut for Back and Forward', () => {
    render(
      <Toolbar label="List tools">
        <Button>Filter</Button>
        <Button>Sort</Button>
      </Toolbar>,
    );
    const filter = screen.getByRole('button', { name: 'Filter' });
    filter.focus();
    expect(fireEvent.keyDown(filter, { key: 'ArrowRight', altKey: true })).toBe(true);
    expect(filter).toHaveFocus();
  });

  it('has no axe violations', async () => {
    render(
      <Toolbar label="List tools" search={<Input aria-label="Search" />}>
        <ToolbarGroup aria-label="View">
          <Button>Board</Button>
          <Button>List</Button>
        </ToolbarGroup>
      </Toolbar>,
    );
    expect(screen.getByRole('group', { name: 'View' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Toolbar label="Tools" search={<Input aria-label="Search" />}>
        <Button>Filter</Button>
      </Toolbar>,
    );
    expect(html).toContain('role="toolbar"');
  });
});
