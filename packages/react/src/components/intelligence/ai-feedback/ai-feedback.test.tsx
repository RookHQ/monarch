import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiFeedback, type AiFeedbackProps } from './index.js';

function Controlled({ onChange, ...props }: Partial<AiFeedbackProps>) {
  const [value, setValue] = useState<AiFeedback | undefined>();
  return (
    <AiFeedback
      {...props}
      value={value}
      onChange={(v) => {
        setValue(v);
        onChange?.(v);
      }}
    />
  );
}

describe('AiFeedback', () => {
  it('shows labelled thumbs with aria-pressed', async () => {
    render(<AiFeedback data-testid="root" value="up" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-ai-feedback');
    expect(screen.getByRole('group', { name: 'Rate this response' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Good response' })).toHaveAttribute(
      'aria-pressed',
      'true',
    );
    expect(screen.getByRole('button', { name: 'Bad response' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    await expectNoA11yViolations();
  });

  it('thumbs down opens reasons and comment, then submits and thanks', async () => {
    const onSubmit = vi.fn();
    const onChange = vi.fn();
    render(<Controlled onSubmit={onSubmit} onChange={onChange} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Bad response' }));
    expect(onChange).toHaveBeenCalledWith('down');
    const chips = screen.getByRole('group', { name: 'What went wrong?' });
    expect(chips).toBeInTheDocument();
    await expectNoA11yViolations();
    const incomplete = screen.getByRole('button', { name: 'Incomplete' });
    const inaccurate = screen.getByRole('button', { name: 'Inaccurate' });
    await user.click(incomplete);
    await user.click(inaccurate);
    expect(incomplete).toHaveAttribute('aria-pressed', 'true');
    await user.type(screen.getByLabelText('Tell us more (optional)'), '  Missed a step.  ');
    await user.click(screen.getByRole('button', { name: 'Send feedback' }));
    expect(onSubmit).toHaveBeenCalledWith({
      value: 'down',
      reasons: ['inaccurate', 'incomplete'],
      comment: 'Missed a step.',
    });
    const thanks = screen.getByRole('status');
    expect(thanks).toHaveTextContent('Thanks for your feedback.');
    expect(thanks).toHaveFocus();
    expect(screen.queryByRole('textbox')).toBeNull();
  });

  it('thumbs up submits straight away by default without moving focus', async () => {
    const onSubmit = vi.fn();
    render(<Controlled onSubmit={onSubmit} />);
    const user = userEvent.setup();
    const up = screen.getByRole('button', { name: 'Good response' });
    await user.click(up);
    expect(onSubmit).toHaveBeenCalledWith({ value: 'up', reasons: [], comment: '' });
    expect(screen.getByRole('status')).toHaveTextContent('Thanks for your feedback.');
    expect(up).toHaveFocus();
  });

  it('asks for details on both thumbs with custom reasons', async () => {
    render(
      <Controlled
        askDetails="both"
        reasons={(v) => (v === 'up' ? [{ value: 'clear', label: 'Clear' }] : [])}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Good response' }));
    expect(screen.getByRole('group', { name: 'What went well?' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Clear' })).toBeInTheDocument();
  });

  it('pressing the pressed thumb clears the rating; Cancel returns focus to it', async () => {
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    const user = userEvent.setup();
    const down = screen.getByRole('button', { name: 'Bad response' });
    await user.click(down);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.queryByRole('textbox')).toBeNull();
    expect(down).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onChange).toHaveBeenLastCalledWith(undefined);
    expect(down).toHaveAttribute('aria-pressed', 'false');
  });

  it('shows a pending state and an error when onSubmit rejects', async () => {
    let reject: (e: Error) => void = () => {};
    const onSubmit = vi.fn(
      () =>
        new Promise<void>((_, r) => {
          reject = r;
        }),
    );
    render(<Controlled onSubmit={onSubmit} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Bad response' }));
    await user.click(screen.getByRole('button', { name: 'Send feedback' }));
    expect(screen.getByRole('form', { name: 'Rate this response' })).toHaveAttribute(
      'aria-busy',
      'true',
    );
    reject(new Error('offline'));
    expect(await screen.findByRole('alert')).toHaveTextContent('could not be sent');
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Send feedback' })).not.toHaveAttribute(
        'aria-busy',
      ),
    );
  });

  it('works uncontrolled', async () => {
    render(<AiFeedback />);
    const user = userEvent.setup();
    const up = screen.getByRole('button', { name: 'Good response' });
    await user.click(up);
    expect(up).toHaveAttribute('aria-pressed', 'true');
  });

  it('uses translated messages', () => {
    render(<AiFeedback messages={{ good: 'Gut', bad: 'Schlecht' }} />);
    expect(screen.getByRole('button', { name: 'Gut' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Schlecht' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<AiFeedback value="down" />)).toContain('mn-ai-feedback');
  });
});
