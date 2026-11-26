import { describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { TermsAcceptance, type TermsDocument } from './index.js';

const DOCS: TermsDocument[] = [
  { label: 'Terms of Service', href: '/legal/terms', version: '2026-11-01' },
  { label: 'Privacy Policy', href: '/legal/privacy', version: '7', id: 'privacy' },
];

const box = () =>
  screen.getByRole('checkbox', { name: 'I agree to the Terms of Service and Privacy Policy' });

describe('TermsAcceptance', () => {
  it('labels the box with linked documents, unticked and required by default', async () => {
    render(<TermsAcceptance documents={DOCS} data-testid="root" locale="en" />);
    expect(screen.getByTestId('root')).toHaveClass('mn-terms-acceptance');
    expect(box()).not.toBeChecked();
    expect(box()).toHaveAttribute('aria-required', 'true');
    const terms = screen.getByRole('link', { name: 'Terms of Service' });
    expect(terms).toHaveAttribute('href', '/legal/terms');
    expect(terms).not.toHaveAttribute('target');
    expect(screen.getByRole('link', { name: 'Privacy Policy' })).toHaveAttribute(
      'href',
      '/legal/privacy',
    );
    await expectNoA11yViolations();
  });

  it('joins three documents with the locale list format and supports target', () => {
    render(
      <TermsAcceptance
        locale="en-US"
        target="_blank"
        documents={[...DOCS, { label: 'Cookie Policy', href: '/legal/cookies' }]}
      />,
    );
    expect(screen.getByRole('checkbox')).toHaveAccessibleName(
      /^I agree to the Terms of Service.*, Privacy Policy.*, and Cookie Policy/,
    );
    expect(screen.getByRole('link', { name: /Cookie Policy/ })).toHaveAttribute(
      'rel',
      'noopener noreferrer',
    );
  });

  it('toggles with Space and reports versions and time; clicking a link does not tick it', async () => {
    const onAcceptedChange = vi.fn();
    render(<TermsAcceptance documents={DOCS} onAcceptedChange={onAcceptedChange} />);
    const user = userEvent.setup();
    const link = screen.getByRole('link', { name: 'Terms of Service' });
    link.addEventListener('click', (event) => event.preventDefault());
    await user.click(link);
    expect(box()).not.toBeChecked();
    box().focus();
    await user.keyboard(' ');
    expect(box()).toBeChecked();
    expect(onAcceptedChange).toHaveBeenCalledWith(true, {
      versions: { '/legal/terms': '2026-11-01', privacy: '7' },
      at: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
    });
    await user.click(box());
    expect(onAcceptedChange).toHaveBeenLastCalledWith(false, expect.any(Object));
  });

  it('blocks a native form submit while unticked, shows the error and focuses the box', async () => {
    const onSubmit = vi.fn((event: SubmitEvent) => event.preventDefault());
    render(
      <form aria-label="Sign up" onSubmit={(e) => onSubmit(e.nativeEvent as SubmitEvent)}>
        <TermsAcceptance documents={DOCS} name="terms" versionsName="terms_versions" />
        <button type="submit">Create account</button>
      </form>,
    );
    const user = userEvent.setup();
    const form = screen.getByRole('form', { name: 'Sign up' }) as HTMLFormElement;
    expect(form.querySelector('input[type="checkbox"][required]')).not.toBeNull();
    expect(form.checkValidity()).toBe(false);
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(box()).toHaveAttribute('aria-invalid', 'true');
    expect(box()).toHaveAccessibleDescription('Accept the terms to continue');
    expect(box()).toHaveFocus();
    await expectNoA11yViolations();

    await user.click(box());
    expect(box()).not.toHaveAttribute('aria-invalid');
    expect(screen.queryByText('Accept the terms to continue')).not.toBeInTheDocument();
    expect(form.checkValidity()).toBe(true);
    const data = new FormData(form);
    expect(data.get('terms')).toBe('accepted');
    expect(JSON.parse(String(data.get('terms_versions')))).toEqual({
      '/legal/terms': '2026-11-01',
      privacy: '7',
    });
    await user.click(screen.getByRole('button', { name: 'Create account' }));
    expect(onSubmit).toHaveBeenCalledOnce();
  });

  it('posts nothing while unticked, and is optional with required={false}', () => {
    render(
      <form aria-label="Sign up">
        <TermsAcceptance documents={DOCS} name="terms" required={false} />
      </form>,
    );
    const form = screen.getByRole('form') as HTMLFormElement;
    expect(form.checkValidity()).toBe(true);
    expect(new FormData(form).get('terms')).toBeNull();
    expect(box()).not.toHaveAttribute('aria-required', 'true');
  });

  it('works controlled, and shows a controlled error', async () => {
    function Controlled() {
      const [accepted, setAccepted] = useState(false);
      return (
        <>
          <TermsAcceptance
            documents={DOCS}
            checked={accepted}
            invalid={!accepted}
            requiredMessage="Tick the box to sign up."
            onAcceptedChange={setAccepted}
          />
          <button type="button" onClick={() => setAccepted(false)}>
            Clear
          </button>
        </>
      );
    }
    render(<Controlled />);
    const user = userEvent.setup();
    expect(box()).toHaveAccessibleDescription('Tick the box to sign up.');
    await user.click(box());
    expect(box()).toBeChecked();
    expect(box()).not.toHaveAttribute('aria-invalid');
    await user.click(screen.getByRole('button', { name: 'Clear' }));
    expect(box()).not.toBeChecked();
  });

  it('uses a custom label and can be disabled', () => {
    render(
      <TermsAcceptance
        documents={DOCS.slice(0, 1)}
        label={(links) => <>I have read and accept the {links}</>}
        disabled
      />,
    );
    const custom = screen.getByRole('checkbox', {
      name: 'I have read and accept the Terms of Service',
    });
    expect(custom).toBeDisabled();
  });

  it('renders on the server', () => {
    const html = renderToString(<TermsAcceptance documents={DOCS} locale="en" />);
    expect(html).toContain('mn-terms-acceptance');
    expect(html).toContain('href="/legal/privacy"');
    expect(html).toContain('Terms of Service');
  });
});
