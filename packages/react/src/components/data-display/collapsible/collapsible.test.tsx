import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
  type CollapsibleProps,
} from './index.js';

function Advanced(props: CollapsibleProps) {
  return (
    <Collapsible data-testid="root" {...props}>
      <CollapsibleTrigger chevron>Advanced settings</CollapsibleTrigger>
      <CollapsibleContent data-testid="content">
        <label>
          Retry limit <input />
        </label>
      </CollapsibleContent>
    </Collapsible>
  );
}

describe('Collapsible', () => {
  it('follows the disclosure pattern: closed content is hidden and out of the tab order', async () => {
    render(
      <main>
        <Advanced />
      </main>,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Advanced settings' });
    const content = screen.getByTestId('content');
    expect(trigger).toHaveAttribute('type', 'button');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(trigger).toHaveAttribute('aria-controls', content.id);
    expect(content).not.toBeVisible();
    expect(trigger.querySelector('.mn-collapsible__chevron')).toHaveAttribute(
      'aria-hidden',
      'true',
    );
    await user.tab();
    expect(trigger).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('textbox', { hidden: true })).not.toHaveFocus();
    await expectNoA11yViolations();
  });

  it('opens and closes with click, Enter and Space', async () => {
    const onOpenChange = vi.fn();
    render(<Advanced onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Advanced settings' });
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByTestId('content')).toBeVisible();
    expect(screen.getByTestId('root')).toHaveAttribute('data-state', 'open');
    expect(onOpenChange).toHaveBeenLastCalledWith(true);
    await user.tab();
    expect(screen.getByRole('textbox', { name: 'Retry limit' })).toHaveFocus();
    await user.tab({ shift: true });
    await user.keyboard('{Enter}');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByTestId('content')).not.toBeVisible();
    await user.keyboard(' ');
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
    await expectNoA11yViolations();
  });

  it('keeps children mounted, so input survives closing', async () => {
    render(<Advanced defaultOpen />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), '5');
    await user.click(screen.getByRole('button', { name: 'Advanced settings' }));
    await user.click(screen.getByRole('button', { name: 'Advanced settings' }));
    expect(screen.getByRole('textbox')).toHaveValue('5');
  });

  it('can be controlled', async () => {
    function Controlled() {
      const [open, setOpen] = useState(false);
      return (
        <>
          <button type="button" onClick={() => setOpen(true)}>
            Expand all
          </button>
          <Advanced open={open} onOpenChange={setOpen} />
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Expand all' }));
    expect(screen.getByRole('button', { name: 'Advanced settings' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    expect(screen.getByTestId('content')).toBeVisible();
  });

  it('does not toggle when disabled', async () => {
    const onOpenChange = vi.fn();
    render(<Advanced disabled onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Advanced settings' });
    expect(trigger).toBeDisabled();
    await user.click(trigger);
    expect(onOpenChange).not.toHaveBeenCalled();
    expect(screen.getByTestId('root')).toHaveAttribute('data-disabled');
  });

  it('renders the child as the trigger with asChild, and respects preventDefault', async () => {
    render(
      <Collapsible>
        <CollapsibleTrigger asChild onClick={(e) => e.shiftKey && e.preventDefault()}>
          <button type="button" className="custom">
            Details
          </button>
        </CollapsibleTrigger>
        <CollapsibleContent>Body</CollapsibleContent>
      </Collapsible>,
    );
    const user = userEvent.setup();
    const trigger = screen.getByRole('button', { name: 'Details' });
    expect(trigger).toHaveClass('custom', 'mn-collapsible__trigger');
    expect(trigger).not.toHaveAttribute('data-plain');
    await user.keyboard('{Shift>}');
    await user.click(trigger);
    await user.keyboard('{/Shift}');
    expect(trigger).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger);
    expect(trigger).toHaveAttribute('aria-expanded', 'true');
  });

  it('throws a clear error outside Collapsible', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<CollapsibleTrigger>Orphan</CollapsibleTrigger>)).toThrow(
      'CollapsibleTrigger must be used inside <Collapsible>.',
    );
    vi.restoreAllMocks();
  });

  it('renders on the server, open and closed', () => {
    const closed = renderToString(<Advanced />);
    expect(closed).toContain('mn-collapsible');
    expect(closed).toContain('aria-expanded="false"');
    expect(closed).toMatch(/hidden=""/);
    const open = renderToString(<Advanced defaultOpen />);
    expect(open).toContain('aria-expanded="true"');
    expect(open).not.toMatch(/hidden=""/);
  });
});
