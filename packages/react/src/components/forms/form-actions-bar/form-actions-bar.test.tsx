import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { FormActionsBar } from './index.js';

function deferred() {
  let resolve!: () => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<void>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

const save = () => screen.getByRole('button', { name: 'Save changes' });

describe('FormActionsBar', () => {
  it('is a labelled region; Save and Discard are enabled only with unsaved changes', async () => {
    const { rerender } = render(
      <FormActionsBar dirty={false} onSave={() => {}} onDiscard={() => {}} />,
    );
    const bar = screen.getByRole('region', { name: 'Form actions' });
    expect(bar).toHaveAttribute('data-sticky');
    expect(save()).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Discard' })).toBeDisabled();
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    rerender(<FormActionsBar dirty onSave={() => {}} onDiscard={() => {}} />);
    expect(save()).toBeEnabled();
    expect(screen.getByRole('status')).toHaveTextContent('You have unsaved changes');
    expect(bar).toHaveAttribute('data-dirty');
    await expectNoA11yViolations();
  });

  it('shows the pending state while onSave runs and the error when it fails', async () => {
    const user = userEvent.setup();
    const first = deferred();
    const onSave = vi.fn(() => first.promise);
    render(<FormActionsBar dirty onSave={onSave} />);
    await user.click(save());
    expect(save()).toHaveAttribute('aria-busy', 'true');
    await user.click(save());
    expect(onSave).toHaveBeenCalledOnce();
    await act(async () => first.reject(new Error('Name is already taken')));
    expect(await screen.findByRole('alert')).toHaveTextContent('Name is already taken');
    expect(save()).not.toHaveAttribute('aria-busy');
    onSave.mockImplementation(() => Promise.resolve());
    await user.click(save());
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
    await expectNoA11yViolations();
  });

  it('uses errorMessage and the default message for unreadable errors', async () => {
    const user = userEvent.setup();
    const { rerender } = render(<FormActionsBar dirty onSave={() => Promise.reject('boom')} />);
    await user.click(save());
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Your changes could not be saved. Try again.',
    );
    rerender(
      <FormActionsBar
        dirty
        onSave={() => Promise.reject(new Error('x'))}
        errorMessage={() => 'Mapped error'}
      />,
    );
    await user.click(save());
    expect(await screen.findByText('Mapped error')).toBeInTheDocument();
  });

  it('asks before discarding; Keep editing cancels and Discard changes calls onDiscard', async () => {
    const user = userEvent.setup();
    const onDiscard = vi.fn();
    render(<FormActionsBar dirty onSave={() => {}} onDiscard={onDiscard} />);
    const discard = screen.getByRole('button', { name: 'Discard' });
    await user.click(discard);
    const dialog = await screen.findByRole('alertdialog', { name: 'Discard unsaved changes?' });
    expect(screen.getByRole('button', { name: 'Keep editing' })).toHaveFocus();
    await user.keyboard('{Enter}');
    await waitFor(() => expect(dialog).not.toBeInTheDocument());
    expect(onDiscard).not.toHaveBeenCalled();
    await waitFor(() => expect(discard).toHaveFocus());
    await user.click(discard);
    await user.click(await screen.findByRole('button', { name: 'Discard changes' }));
    await waitFor(() => expect(onDiscard).toHaveBeenCalledOnce());
  });

  it('discards at once with confirmDiscard={false}', async () => {
    const user = userEvent.setup();
    const onDiscard = vi.fn();
    render(<FormActionsBar dirty confirmDiscard={false} onDiscard={onDiscard} />);
    await user.click(screen.getByRole('button', { name: 'Discard' }));
    expect(onDiscard).toHaveBeenCalledOnce();
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
  });

  it('submits its form when there is no onSave', async () => {
    const user = userEvent.setup();
    function Form() {
      const [name, setName] = useState('');
      const [submitted, setSubmitted] = useState('');
      return (
        <>
          <form
            id="profile"
            onSubmit={(event) => {
              event.preventDefault();
              setSubmitted(name);
            }}
          >
            <label htmlFor="name">Name</label>
            <input id="name" value={name} onChange={(e) => setName(e.target.value)} />
          </form>
          <FormActionsBar dirty={name !== submitted} form="profile" />
          <output>{submitted}</output>
        </>
      );
    }
    render(<Form />);
    await user.type(screen.getByRole('textbox', { name: 'Name' }), 'Ada');
    expect(save()).toHaveAttribute('type', 'submit');
    await user.click(save());
    expect(document.querySelector('.mn-form-actions-bar__message')).toBeEmptyDOMElement();
    expect(document.querySelector('output')).toHaveTextContent('Ada');
    expect(save()).toBeDisabled();
  });

  it('hides when clean with hideWhenClean, and shows a custom status', () => {
    const { rerender } = render(<FormActionsBar dirty={false} hideWhenClean />);
    expect(screen.queryByRole('region')).not.toBeInTheDocument();
    rerender(<FormActionsBar dirty hideWhenClean status="3 fields changed" pending />);
    expect(screen.getByRole('region')).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('3 fields changed');
    expect(save()).toHaveAttribute('aria-busy', 'true');
  });

  it('renders on the server', () => {
    const html = renderToString(<FormActionsBar dirty onSave={() => {}} onDiscard={() => {}} />);
    expect(html).toContain('mn-form-actions-bar');
    expect(html).toContain('Save changes');
  });
});
