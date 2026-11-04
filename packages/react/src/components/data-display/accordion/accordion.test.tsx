import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from './index.js';

function Example({ headingAs = 'h3' }: { headingAs?: 'h2' | 'h3' | 'h4' }) {
  return (
    <Accordion type="single" collapsible>
      <AccordionItem value="a">
        <AccordionTrigger headingAs={headingAs}>Billing</AccordionTrigger>
        <AccordionContent>Invoices are sent monthly.</AccordionContent>
      </AccordionItem>
      <AccordionItem value="b">
        <AccordionTrigger headingAs={headingAs}>Security</AccordionTrigger>
        <AccordionContent>Sign-in uses Rook ID.</AccordionContent>
      </AccordionItem>
    </Accordion>
  );
}

describe('Accordion', () => {
  it('wraps each trigger in a heading of the requested level', () => {
    const { rerender } = render(<Example />);
    expect(screen.getByRole('button', { name: 'Billing' }).closest('h3')).not.toBeNull();
    rerender(<Example headingAs="h2" />);
    expect(screen.getByRole('button', { name: 'Billing' }).closest('h2')).not.toBeNull();
  });

  it('toggles with Enter and moves between headers with arrows, Home and End', async () => {
    render(<Example />);
    const user = userEvent.setup();
    const first = screen.getByRole('button', { name: 'Billing' });
    const second = screen.getByRole('button', { name: 'Security' });
    first.focus();
    await user.keyboard('{Enter}');
    expect(first).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Invoices are sent monthly.')).toBeVisible();
    await user.keyboard('{ArrowDown}');
    expect(second).toHaveFocus();
    await user.keyboard('{Home}');
    expect(first).toHaveFocus();
    await user.keyboard('{End}');
    expect(second).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(second).toHaveAttribute('aria-expanded', 'true');
    expect(first).toHaveAttribute('aria-expanded', 'false');
  });

  it('has no axe violations and renders on the server', async () => {
    render(<Example />);
    await expectNoA11yViolations();
    expect(renderToString(<Example />)).toContain('mn-accordion__trigger');
  });
});
