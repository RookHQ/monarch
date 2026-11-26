import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { Field, FieldError, FieldLabel } from '../form-field/index.js';
import { Input } from '../text-field/index.js';
import { InputGroup, InputGroupAddon, InputGroupButton } from './index.js';

describe('InputGroup', () => {
  it('keeps Field wiring on the Input and describes it by a text addon', async () => {
    render(
      <Field invalid>
        <FieldLabel>Website</FieldLabel>
        <InputGroup data-testid="group">
          <InputGroupAddon id="site-prefix">https://</InputGroupAddon>
          <Input aria-describedby="site-prefix" defaultValue="example" />
        </InputGroup>
        <FieldError>Enter a full domain.</FieldError>
      </Field>,
    );
    const input = screen.getByRole('textbox', { name: 'Website' });
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('https:// Enter a full domain.');
    const group = screen.getByTestId('group');
    expect(group).toHaveClass('mn-input-group');
    expect(group).toHaveAttribute('data-size', 'md');
    expect(screen.getByText('https://')).toHaveAttribute('data-align', 'inline-start');
    await expectNoA11yViolations();
  });

  it('attaches buttons in reading order that are reachable with Tab', async () => {
    function Search() {
      const [q, setQ] = useState('invoices');
      return (
        <InputGroup>
          <Input aria-label="Search" value={q} onChange={(e) => setQ(e.target.value)} />
          <InputGroupButton icon={<CloseGlyph />} label="Clear search" onClick={() => setQ('')} />
        </InputGroup>
      );
    }
    render(<Search />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'Search' })).toHaveFocus();
    await user.tab();
    const clear = screen.getByRole('button', { name: 'Clear search' });
    expect(clear).toHaveFocus();
    expect(clear).toHaveAttribute('data-align', 'inline-end');
    expect(clear).toHaveClass('mn-input-group__button', 'mn-icon-button');
    await user.keyboard('{Enter}');
    expect(screen.getByRole('textbox')).toHaveValue('');
    await expectNoA11yViolations();
  });

  it('renders a text button and a suffix addon with sizes', async () => {
    const onClick = vi.fn();
    render(
      <InputGroup size="sm" data-testid="group">
        <Input size="sm" aria-label="Weight" />
        <InputGroupAddon align="inline-end">kg</InputGroupAddon>
        <InputGroupButton onClick={onClick}>Apply</InputGroupButton>
      </InputGroup>,
    );
    expect(screen.getByTestId('group')).toHaveAttribute('data-size', 'sm');
    expect(screen.getByText('kg')).toHaveAttribute('data-align', 'inline-end');
    const button = screen.getByRole('button', { name: 'Apply' });
    expect(button).toHaveAttribute('data-variant', 'ghost');
    expect(button).toHaveAttribute('data-size', 'sm');
    await userEvent.setup().click(button);
    expect(onClick).toHaveBeenCalled();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <InputGroup>
        <InputGroupAddon>@</InputGroupAddon>
        <Input aria-label="Handle" />
      </InputGroup>,
    );
    expect(html).toContain('mn-input-group');
    expect(html).toContain('mn-input-group__addon');
  });
});
