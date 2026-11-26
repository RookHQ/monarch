import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../actions/button/index.js';
import { ToastProvider } from '../../feedback/toast/index.js';
import { AiUndoBanner, useAiUndoToast, type AiChange } from './index.js';

const changes: AiChange[] = [
  { label: 'Status', before: 'Open', after: 'Closed' },
  { label: 'Owner', before: 'Unassigned', after: 'Sam Lee' },
];

describe('AiUndoBanner', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders title, changes and audit link; forwards props and ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiUndoBanner
        ref={ref}
        data-testid="root"
        className="x"
        title="AI changed 2 fields"
        changes={changes}
        auditHref="/audit/123"
        onUndo={() => {}}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-undo-banner', 'x');
    expect(root).toHaveAttribute('data-state', 'active');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('group', { name: 'AI change: AI changed 2 fields' })).toBe(root);
    const items = screen.getAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Status changed from Open → to Closed');
    expect(root.querySelector('del')).toHaveTextContent('Open');
    expect(root.querySelector('ins')).toHaveTextContent('Closed');
    expect(screen.getByRole('link', { name: 'View in audit log' })).toHaveAttribute(
      'rel',
      'noopener noreferrer nofollow',
    );
    await expectNoA11yViolations();
  });

  it('collapses more than three changes into a disclosure', () => {
    render(
      <AiUndoBanner
        title="AI changed 4 fields"
        changes={[...changes, ...changes]}
        onUndo={() => {}}
      />,
    );
    expect(screen.getByText('Show 4 changes').tagName).toBe('SUMMARY');
  });

  it('drops unsafe audit links', () => {
    render(<AiUndoBanner title="T" auditHref="javascript:void 0" onUndo={() => {}} />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('undoes, announces it and keeps focus in the banner', async () => {
    const onUndo = vi.fn(() => Promise.resolve());
    render(<AiUndoBanner title="AI changed 2 fields" onUndo={onUndo} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Changes undone'));
    expect(onUndo).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull();
    expect(screen.getByRole('group')).toHaveAttribute('data-state', 'undone');
    expect(screen.getByRole('group')).toHaveFocus();
  });

  it('reports a failed undo and keeps Undo', async () => {
    render(<AiUndoBanner title="T" onUndo={() => Promise.reject(new Error('x'))} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Undo' }));
    await waitFor(() =>
      expect(screen.getByRole('status')).toHaveTextContent('Couldn’t undo. Try again.'),
    );
    expect(screen.getByRole('button', { name: 'Undo' })).toBeInTheDocument();
  });

  it('expires after duration, pausing while hovered', () => {
    vi.useFakeTimers();
    const onExpire = vi.fn();
    render(<AiUndoBanner title="T" duration={5000} onExpire={onExpire} onUndo={() => {}} />);
    const root = screen.getByRole('group');
    act(() => vi.advanceTimersByTime(3000));
    fireEvent.pointerEnter(root);
    act(() => vi.advanceTimersByTime(10000));
    expect(onExpire).not.toHaveBeenCalled();
    fireEvent.pointerLeave(root);
    act(() => vi.advanceTimersByTime(1900));
    expect(onExpire).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(200));
    expect(onExpire).toHaveBeenCalledTimes(1);
    expect(root).toHaveAttribute('data-state', 'expired');
    expect(screen.queryByRole('button', { name: 'Undo' })).toBeNull();
    expect(screen.getByRole('status')).toHaveTextContent('Undo is no longer available.');
  });

  it('shows Dismiss when onDismiss is passed', async () => {
    const onDismiss = vi.fn();
    render(<AiUndoBanner title="T" onUndo={() => {}} onDismiss={onDismiss} />);
    await userEvent.setup().click(screen.getByRole('button', { name: 'Dismiss' }));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('renders on the server', () => {
    expect(
      renderToString(<AiUndoBanner title="T" changes={changes} onUndo={() => {}} />),
    ).toContain('mn-ai-undo-banner');
  });
});

describe('useAiUndoToast', () => {
  it('shows an undo toast with the changed labels as its description', async () => {
    const onUndo = vi.fn();
    let outcome: Promise<string> | undefined;
    function Demo() {
      const { undoable } = useAiUndoToast();
      return (
        <Button
          onClick={() => {
            outcome = undoable({ title: 'AI changed 2 fields', changes, onUndo });
          }}
        >
          Apply
        </Button>
      );
    }
    const user = userEvent.setup();
    render(
      <ToastProvider>
        <Demo />
      </ToastProvider>,
    );
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(await screen.findByText('AI changed 2 fields')).toBeInTheDocument();
    expect(screen.getByText('Status, Owner')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Undo' }));
    await expect(outcome).resolves.toBe('undone');
    expect(onUndo).toHaveBeenCalledTimes(1);
  });
});
