import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Field, FieldLabel } from '../form-field/index.js';
import { TagInput } from './index.js';

const chips = () =>
  Array.from(document.querySelectorAll('.mn-tag-input__label')).map((n) => n.textContent);

describe('TagInput', () => {
  it('adds tags on Enter and comma, and labels each remove button', async () => {
    const onValueChange = vi.fn();
    render(<TagInput aria-label="Skills" onValueChange={onValueChange} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox', { name: 'Skills' });
    await user.type(input, 'Design{Enter}Research,');
    expect(chips()).toEqual(['Design', 'Research']);
    expect(input).toHaveValue('');
    expect(onValueChange).toHaveBeenLastCalledWith(['Design', 'Research']);
    expect(screen.getByRole('button', { name: 'Remove Design' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Added Research.');
    await expectNoA11yViolations();
  });

  it('ignores blank entries and trims whitespace', async () => {
    render(<TagInput aria-label="Tags" />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), '   {Enter},  billing  {Enter}');
    expect(chips()).toEqual(['billing']);
  });

  it('splits pasted lists on separators and new lines', async () => {
    render(<TagInput aria-label="Recipients" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox'));
    await user.paste('a@example.com, b@example.com\nc@example.com');
    expect(chips()).toEqual(['a@example.com', 'b@example.com', 'c@example.com']);
  });

  it('leaves a paste without separators to the browser', async () => {
    render(<TagInput aria-label="Tags" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('textbox'));
    await user.paste('single');
    expect(chips()).toEqual([]);
    expect(screen.getByRole('textbox')).toHaveValue('single');
  });

  it('rejects invalid values with an announced, linked message and keeps the text', async () => {
    const validate = (tag: string) => (tag.includes('@') ? null : 'Enter an email address.');
    render(<TagInput aria-label="CC" validate={validate} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.type(input, 'not-an-email{Enter}');
    expect(chips()).toEqual([]);
    expect(input).toHaveValue('not-an-email');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Enter an email address.');
    await user.type(input, 'x');
    expect(input).not.toHaveAttribute('aria-invalid');
    expect(input).not.toHaveAttribute('aria-describedby');
  });

  it('rejects duplicates case-insensitively and enforces max', async () => {
    render(<TagInput aria-label="Labels" defaultValue={['Urgent']} max={2} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.type(input, 'urgent{Enter}');
    expect(screen.getByText('“urgent” is already added.')).toBeInTheDocument();
    await user.clear(input);
    await user.type(input, 'Billing{Enter}Refund{Enter}');
    expect(chips()).toEqual(['Urgent', 'Billing']);
    expect(screen.getByText('You can add up to 2.')).toBeInTheDocument();
  });

  it('removes with Backspace: first to the last chip, then removes it', async () => {
    render(<TagInput aria-label="Tags" defaultValue={['one', 'two', 'three']} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.click(input);
    await user.keyboard('{Backspace}');
    expect(screen.getByRole('button', { name: 'Remove three' })).toHaveFocus();
    expect(chips()).toEqual(['one', 'two', 'three']);
    await user.keyboard('{Backspace}');
    expect(chips()).toEqual(['one', 'two']);
    expect(screen.getByRole('button', { name: 'Remove two' })).toHaveFocus();
    expect(screen.getByRole('status')).toHaveTextContent('Removed three.');
  });

  it('moves between chips with arrows and removes with Delete', async () => {
    render(<TagInput aria-label="Tags" defaultValue={['a', 'b', 'c']} />);
    const user = userEvent.setup();
    const input = screen.getByRole('textbox');
    await user.click(input);
    await user.keyboard('{ArrowLeft}{ArrowLeft}');
    expect(screen.getByRole('button', { name: 'Remove b' })).toHaveFocus();
    await user.keyboard('{Delete}');
    expect(chips()).toEqual(['a', 'c']);
    expect(screen.getByRole('button', { name: 'Remove c' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    expect(input).toHaveFocus();
  });

  it('removes on click and returns focus to the input', async () => {
    render(<TagInput aria-label="Tags" defaultValue={['a', 'b']} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove a' }));
    expect(chips()).toEqual(['b']);
    expect(screen.getByRole('textbox')).toHaveFocus();
  });

  it('offers suggestions in a listbox navigable by keyboard', async () => {
    render(
      <TagInput
        aria-label="Skills"
        defaultValue={['React']}
        suggestions={['React', 'Redux', 'Rust', 'Go']}
      />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('combobox', { name: 'Skills' });
    await user.type(input, 'r');
    const list = screen.getByRole('listbox', { name: 'Suggestions' });
    // Already added values are left out.
    expect(
      within(list)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual(['Redux', 'Rust']);
    expect(input).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{ArrowDown}{ArrowDown}');
    expect(input).toHaveAttribute(
      'aria-activedescendant',
      within(list).getByRole('option', { name: 'Rust' }).id,
    );
    await user.keyboard('{Enter}');
    expect(chips()).toEqual(['React', 'Rust']);
    expect(input).toHaveValue('');
    await expectNoA11yViolations();
  });

  it('adds a suggestion on click and closes on Escape', async () => {
    render(<TagInput aria-label="Skills" suggestions={['Go', 'Gleam']} />);
    const user = userEvent.setup();
    const input = screen.getByRole('combobox');
    await user.type(input, 'g');
    await user.click(screen.getByRole('option', { name: 'Gleam' }));
    expect(chips()).toEqual(['Gleam']);
    await user.type(input, 'g');
    expect(screen.getByRole('listbox')).toBeInTheDocument();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
    expect(input).toHaveAttribute('aria-expanded', 'false');
  });

  it('works controlled, including the typed text', async () => {
    function Controlled() {
      const [tags, setTags] = useState(['alpha']);
      const [text, setText] = useState('');
      return (
        <>
          <TagInput
            aria-label="Tags"
            value={tags}
            onValueChange={setTags}
            inputValue={text}
            onInputValueChange={setText}
          />
          <output>{`${tags.join('|')}/${text}`}</output>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'beta');
    expect(screen.getByText('alpha/beta')).toBeInTheDocument();
    await user.keyboard('{Enter}');
    expect(screen.getByText('alpha|beta/')).toBeInTheDocument();
  });

  it('commits on blur when addOnBlur is set', async () => {
    render(<TagInput aria-label="Tags" addOnBlur />);
    const user = userEvent.setup();
    await user.type(screen.getByRole('textbox'), 'draft');
    await user.tab();
    expect(chips()).toEqual(['draft']);
  });

  it('submits one hidden input per tag', () => {
    const { container } = render(
      <TagInput aria-label="Tags" name="tags" defaultValue={['a', 'b']} />,
    );
    const hidden = container.querySelectorAll('input[type="hidden"][name="tags"]');
    expect(Array.from(hidden).map((n) => (n as HTMLInputElement).value)).toEqual(['a', 'b']);
  });

  it('has no remove buttons when disabled (from Field) or read-only', () => {
    render(
      <Field disabled>
        <FieldLabel>Topics</FieldLabel>
        <TagInput defaultValue={['news']} />
      </Field>,
    );
    expect(screen.getByRole('textbox', { name: 'Topics' })).toBeDisabled();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(chips()).toEqual(['news']);
    render(<TagInput aria-label="Fixed" readOnly defaultValue={['kept']} />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<TagInput aria-label="Tags" defaultValue={['x']} />);
    expect(html).toContain('mn-tag-input');
    expect(html).toContain('Remove x');
  });
});
