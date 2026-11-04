import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Input, Textarea } from './index.js';

describe('Input', () => {
  it('lets explicit props win outside a Field', () => {
    render(<Input aria-label="Search" invalid />);
    expect(screen.getByRole('textbox', { name: 'Search' })).toHaveAttribute('aria-invalid', 'true');
  });

  it('renders accessibly with sizes and states', async () => {
    render(
      <main>
        <Input size="sm" aria-label="Small" />
        <Input aria-label="Disabled" disabled />
        <Textarea aria-label="Notes" />
      </main>,
    );
    expect(screen.getByRole('textbox', { name: 'Small' })).toHaveAttribute('data-size', 'sm');
    expect(screen.getByRole('textbox', { name: 'Disabled' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Input aria-label="Name" />);
    expect(html).toContain('mn-input');
  });
});

describe('Textarea', () => {
  it('defaults to three rows and is resizable', () => {
    render(<Textarea aria-label="Notes" />);
    const area = screen.getByRole('textbox', { name: 'Notes' });
    expect(area).toHaveAttribute('rows', '3');
    expect(area).toHaveAttribute('data-resizable');
  });

  it('renders on the server', () => {
    const html = renderToString(<Textarea aria-label="Notes" />);
    expect(html).toContain('mn-textarea');
  });
});
