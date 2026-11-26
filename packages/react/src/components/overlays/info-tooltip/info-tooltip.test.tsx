import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InfoTooltip } from './index.js';

const text = 'Share of tracked keywords where you rank on page one.';

function Example(props: Partial<Parameters<typeof InfoTooltip>[0]>) {
  return (
    <p>
      Visibility score
      <InfoTooltip label="About visibility score" {...props}>
        {text}
      </InfoTooltip>
      <button type="button">Next</button>
    </p>
  );
}

describe('InfoTooltip', () => {
  it('is a named button that toggles a note on click and keeps focus', async () => {
    render(<Example />);
    const user = userEvent.setup();
    const button = screen.getByRole('button', { name: 'About visibility score' });
    expect(button).toHaveAttribute('aria-expanded', 'false');
    await expectNoA11yViolations();
    await user.click(button);
    const note = await screen.findByRole('note', { name: 'About visibility score' });
    expect(note).toHaveTextContent(text);
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(button).toHaveFocus();
    await expectNoA11yViolations();
    await user.click(button);
    await waitFor(() => expect(screen.queryByRole('note')).not.toBeInTheDocument());
  });

  it('announces the text through a persistent live region', async () => {
    render(<Example />);
    const user = userEvent.setup();
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    await user.click(screen.getByRole('button', { name: 'About visibility score' }));
    await waitFor(() => expect(status).toHaveTextContent(text));
    await user.keyboard('{Escape}');
    await waitFor(() => expect(status).toBeEmptyDOMElement());
  });

  it('opens with Enter and Space; Escape closes and leaves focus on the button', async () => {
    render(<Example />);
    const user = userEvent.setup();
    await user.tab();
    const button = screen.getByRole('button', { name: 'About visibility score' });
    expect(button).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(await screen.findByRole('note')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() => expect(screen.queryByRole('note')).not.toBeInTheDocument());
    expect(button).toHaveFocus();
    await user.keyboard(' ');
    expect(await screen.findByRole('note')).toBeInTheDocument();
    // Tab moves on to the next control and does not get trapped.
    await user.tab();
    expect(screen.getByRole('button', { name: 'Next' })).toHaveFocus();
  });

  it('closes on an outside click', async () => {
    render(
      <>
        <Example defaultOpen />
        <div data-testid="outside">Elsewhere</div>
      </>,
    );
    const user = userEvent.setup();
    expect(screen.getByRole('note')).toBeInTheDocument();
    await user.click(screen.getByTestId('outside'));
    await waitFor(() => expect(screen.queryByRole('note')).not.toBeInTheDocument());
  });

  it('opens on tap (pointer down and up without hover)', async () => {
    render(<Example />);
    const user = userEvent.setup();
    await user.pointer({
      keys: '[TouchA]',
      target: screen.getByRole('button', { name: 'About visibility score' }),
    });
    expect(await screen.findByRole('note')).toBeInTheDocument();
  });

  it('does not open on hover alone', async () => {
    render(<Example />);
    const user = userEvent.setup();
    await user.hover(screen.getByRole('button', { name: 'About visibility score' }));
    expect(screen.queryByRole('note')).not.toBeInTheDocument();
  });

  it('controlled: reports changes and follows open', async () => {
    const onOpenChange = vi.fn();
    const { rerender } = render(<Example open={false} onOpenChange={onOpenChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'About visibility score' }));
    expect(onOpenChange).toHaveBeenCalledWith(true);
    expect(screen.queryByRole('note')).not.toBeInTheDocument();
    rerender(<Example open onOpenChange={onOpenChange} />);
    expect(screen.getByRole('note')).toBeInTheDocument();
  });

  it('uses a default label, a help glyph and a custom icon', () => {
    const { rerender } = render(<InfoTooltip>{text}</InfoTooltip>);
    const button = screen.getByRole('button', { name: 'More information' });
    expect(button).toHaveAttribute('data-icon', 'info');
    rerender(<InfoTooltip icon="help">{text}</InfoTooltip>);
    expect(button).toHaveAttribute('data-icon', 'help');
    rerender(<InfoTooltip iconNode={<svg data-testid="custom" />}>{text}</InfoTooltip>);
    expect(screen.getByTestId('custom').parentElement).toHaveAttribute('aria-hidden', 'true');
  });

  it('forwards the ref to the button', () => {
    let node: HTMLButtonElement | null = null;
    render(
      <InfoTooltip
        ref={(el) => {
          node = el;
        }}
      >
        {text}
      </InfoTooltip>,
    );
    expect(node).toBe(screen.getByRole('button'));
  });

  it('renders on the server', () => {
    const html = renderToString(<Example />);
    expect(html).toContain('mn-info-tooltip');
    expect(html).toContain('aria-label="About visibility score"');
  });
});
