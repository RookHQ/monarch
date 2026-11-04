import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { Input } from '../../../components/forms/text-field/index.js';
import { Toolbar, ToolbarGroup } from './index.js';

describe('Toolbar', () => {
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
