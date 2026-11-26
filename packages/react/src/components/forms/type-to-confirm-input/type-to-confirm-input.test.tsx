import { describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TypeToConfirmInput, phraseMatches } from './index.js';

describe('phraseMatches', () => {
  it('is case-sensitive and ignores surrounding whitespace', () => {
    expect(phraseMatches('acme-site', 'acme-site')).toBe(true);
    expect(phraseMatches('  acme-site\n', 'acme-site')).toBe(true);
    expect(phraseMatches('Acme-site', 'acme-site')).toBe(false);
    expect(phraseMatches('acme site', 'acme-site')).toBe(false);
    expect(phraseMatches('acme', 'acme-site')).toBe(false);
  });
});

describe('TypeToConfirmInput', () => {
  it('labels the field with the phrase and turns assistance off', async () => {
    const ref = createRef<HTMLInputElement>();
    const { container } = render(
      <TypeToConfirmInput
        ref={ref}
        phrase="acme-site"
        description="The site and its pages are deleted."
        className="extra"
        data-testid="field"
      />,
    );
    const field = screen.getByRole('textbox', { name: 'Type acme-site to confirm' });
    expect(ref.current).toBe(field);
    expect(field).toHaveAttribute('data-testid', 'field');
    expect(field).toHaveAccessibleDescription('The site and its pages are deleted.');
    expect(field).toHaveAttribute('autocomplete', 'off');
    expect(field).toHaveAttribute('autocorrect', 'off');
    expect(field).toHaveAttribute('autocapitalize', 'off');
    expect(field).toHaveAttribute('spellcheck', 'false');
    const root = container.firstElementChild!;
    expect(root).toHaveClass('mn-type-to-confirm-input', 'mn-field', 'extra');
    expect(root.querySelector('.mn-type-to-confirm-input__phrase')).toHaveTextContent('acme-site');
    await expectNoA11yViolations();
  });

  it('reports and announces match flips, case-sensitively, ignoring surrounding spaces', async () => {
    const onMatchChange = vi.fn();
    const onValueChange = vi.fn();
    const { container } = render(
      <TypeToConfirmInput
        phrase="acme-site"
        onMatchChange={onMatchChange}
        onValueChange={onValueChange}
      />,
    );
    const user = userEvent.setup();
    const field = screen.getByRole('textbox');
    const status = screen.getByRole('status');
    expect(status).toBeEmptyDOMElement();
    expect(onMatchChange).not.toHaveBeenCalled();

    await user.type(field, 'Acme-site');
    expect(onMatchChange).not.toHaveBeenCalled();
    expect(onValueChange).toHaveBeenLastCalledWith('Acme-site');

    await user.clear(field);
    await user.type(field, ' acme-site ');
    expect(onMatchChange).toHaveBeenCalledTimes(1);
    expect(onMatchChange).toHaveBeenLastCalledWith(true);
    expect(status).toHaveTextContent('Text matches');
    expect(container.firstElementChild).toHaveAttribute('data-matches', 'true');

    await user.keyboard('{Backspace}{Backspace}');
    expect(onMatchChange).toHaveBeenCalledTimes(2);
    expect(onMatchChange).toHaveBeenLastCalledWith(false);
    expect(status).toBeEmptyDOMElement();
    expect(container.firstElementChild).not.toHaveAttribute('data-matches');
  });

  it('accepts paste and uses custom announcements', async () => {
    render(
      <TypeToConfirmInput
        phrase="acme-site"
        matchAnnouncement="Name confirmed"
        mismatchAnnouncement="Name no longer matches"
      />,
    );
    const user = userEvent.setup();
    const field = screen.getByRole('textbox');
    await user.click(field);
    await user.paste('acme-site');
    expect(field).toHaveValue('acme-site');
    expect(screen.getByRole('status')).toHaveTextContent('Name confirmed');
    await user.type(field, 'x');
    expect(screen.getByRole('status')).toHaveTextContent('Name no longer matches');
  });

  it('shows the mismatch only when the parent asks, and links it to the field', async () => {
    function Form() {
      const [value, setValue] = useState('');
      const [attempted, setAttempted] = useState(false);
      return (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            setAttempted(true);
          }}
        >
          <TypeToConfirmInput
            phrase="acme-site"
            value={value}
            onValueChange={(next) => {
              setValue(next);
              setAttempted(false);
            }}
            showMismatch={attempted}
          />
          <button type="submit">Delete site</button>
        </form>
      );
    }
    render(<Form />);
    const user = userEvent.setup();
    const field = screen.getByRole('textbox');
    await user.type(field, 'acme');
    expect(field).not.toHaveAttribute('aria-invalid');
    await user.keyboard('{Enter}');
    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(field).toHaveAccessibleDescription('The text does not match.');
    await expectNoA11yViolations();
    await user.type(field, '-site');
    expect(field).not.toHaveAttribute('aria-invalid');
    await user.keyboard('{Enter}');
    expect(field).not.toHaveAttribute('aria-invalid');
  });

  it('supports a custom label, mismatch message, defaultValue and disabled', () => {
    render(
      <TypeToConfirmInput
        phrase="DELETE"
        label="Type DELETE"
        defaultValue="DELETE"
        mismatchMessage="Type DELETE exactly."
        showMismatch
        disabled
      />,
    );
    const field = screen.getByRole('textbox', { name: 'Type DELETE' });
    expect(field).toHaveValue('DELETE');
    expect(field).toBeDisabled();
    expect(screen.queryByText('Type DELETE exactly.')).not.toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<TypeToConfirmInput phrase="acme-site" />);
    expect(html).toContain('mn-type-to-confirm-input');
    expect(html).toMatch(/autocomplete="off"/i);
  });
});
