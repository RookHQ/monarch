import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { VisuallyHidden } from './index.js';

describe('VisuallyHidden', () => {
  it('keeps text in the accessible name', async () => {
    render(
      <button type="button">
        Save <VisuallyHidden>changes to the ticket</VisuallyHidden>
      </button>,
    );
    expect(screen.getByRole('button', { name: 'Save changes to the ticket' })).toBeInTheDocument();
    expect(screen.getByText('changes to the ticket')).toHaveClass('mn-visually-hidden');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<VisuallyHidden>hidden</VisuallyHidden>);
    expect(html).toContain('mn-visually-hidden');
  });
});
