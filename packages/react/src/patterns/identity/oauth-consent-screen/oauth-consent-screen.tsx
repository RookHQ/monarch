'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, LockGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';
import { Button } from '../../../components/actions/button/index.js';
import { Badge } from '../../../components/data-display/badge/index.js';
import { InlineNotice } from '../../../components/feedback/inline-notice/index.js';
import { Avatar } from '../../../components/identity/avatar/index.js';

/** The app asking for access. */
export interface OAuthConsentApp {
  name: string;
  /** Logo URL. Unsafe URLs fall back to the initial. */
  logoSrc?: string | null | undefined;
  /** The app's domain, shown under the name, e.g. "notes.example.com". */
  domain?: string | undefined;
  /** Where the person returns after deciding, when it differs from `domain`. */
  redirectHost?: string | undefined;
  /** The identity provider has verified the app. Unverified apps get a warning. */
  verified?: boolean | undefined;
  /** The app is in testing and only invited people can use it. */
  testing?: boolean | undefined;
  /** The app's privacy policy. Only http(s) links are shown. */
  privacyUrl?: string | undefined;
  /** The app's terms. Only http(s) links are shown. */
  termsUrl?: string | undefined;
}

/** The signed-in account that would grant access. */
export interface OAuthConsentAccount {
  name?: string | undefined;
  email: string;
  avatarSrc?: string | null | undefined;
}

/** One permission the app asks for, in plain words. */
export interface OAuthConsentScope {
  /** The technical scope, e.g. `profile:read`; used as the key only. */
  id: string;
  /** What the app will be able to do, e.g. "See your name and profile photo". */
  label: ReactNode;
  /** More detail under the label. */
  description?: ReactNode;
  /** Highlight it as sensitive (for example, writing data or reading email). */
  sensitive?: boolean | undefined;
}

/** Visible strings, for translation. */
export interface OAuthConsentScreenMessages {
  /** The heading. Default `${app} wants to access your account`. */
  title: (app: string) => string;
  /** Under the domain when the redirect host differs. Default `You’ll go back to ${host}`. */
  redirect: (host: string) => string;
  /** Default "Verified". */
  verified: string;
  /** Unverified warning title. */
  unverifiedTitle: string;
  /** Unverified warning text. */
  unverified: (app: string) => string;
  /** Testing notice. */
  testing: string;
  /** Account switch link. Default "Use another account". */
  switchAccount: string;
  /** Label of the account card. Default "Signed in as". */
  signedInAs: string;
  /** Before the scopes. Default `${app} will be able to:`. */
  scopesTitle: (app: string) => string;
  /** Before held-back scopes. */
  heldBackTitle: string;
  /** Hidden text after a sensitive scope. Default "(sensitive)". */
  sensitive: string;
  /** Default "Cancel". */
  deny: string;
  /** Default "Allow". */
  allow: string;
  /** Before the policy links. Default `Before you continue, read ${app}’s`. */
  policyPrompt: (app: string) => string;
  /** Default "privacy policy". */
  privacy: string;
  /** Default "terms". */
  terms: string;
  /** Default "and". */
  and: string;
  /** Hidden note on links that open a new tab. */
  newTab: string;
  /** Shown when a callback rejects without a message. */
  error: string;
}

export const defaultOAuthConsentScreenMessages: OAuthConsentScreenMessages = {
  title: (app) => `${app} wants to access your account`,
  redirect: (host) => `You’ll go back to ${host}`,
  verified: 'Verified',
  unverifiedTitle: 'This app hasn’t been verified',
  unverified: (app) => `Only continue if you know and trust ${app}.`,
  testing: 'This app is still being tested. Only people its makers invited can use it.',
  switchAccount: 'Use another account',
  signedInAs: 'Signed in as',
  scopesTitle: (app) => `${app} will be able to:`,
  heldBackTitle: 'Not shared until the app is verified:',
  sensitive: '(sensitive)',
  deny: 'Cancel',
  allow: 'Allow',
  policyPrompt: (app) => `Before you continue, read ${app}’s`,
  privacy: 'privacy policy',
  terms: 'terms',
  and: 'and',
  newTab: '(opens in a new tab)',
  error: 'Something went wrong. Try again.',
};

export interface OAuthConsentScreenProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'title' | 'children'
> {
  /** The app asking for access. */
  app: OAuthConsentApp;
  /** The signed-in account. */
  account: OAuthConsentAccount;
  /** What the app asks for, in plain words. */
  scopes: OAuthConsentScope[];
  /** Scopes held back until the app is verified, shown separately and not granted. */
  heldBackScopes?: OAuthConsentScope[] | undefined;
  /**
   * Grants access. Return a promise for the pending state (both buttons are
   * disabled meanwhile); a rejection shows its message.
   */
  onAllow: () => unknown;
  /** Refuses access. Return a promise for the pending state. */
  onDeny: () => unknown;
  /** Switches account; shows “Use another account”. */
  onSwitchAccount?: (() => void) | undefined;
  /** Extra steps before Allow, such as choosing a workspace or confirming a code. */
  children?: ReactNode;
  /** Keep Allow disabled, e.g. until a workspace is chosen. */
  allowDisabled?: boolean | undefined;
  /** The person cannot grant access; only Cancel shows, with this reason. */
  blocked?: ReactNode;
  /** A note under the buttons, e.g. where access can be removed later. */
  footnote?: ReactNode;
  /** Heading element. Default `h1` (the consent screen is usually its own page). */
  headingLevel?: 'h1' | 'h2' | undefined;
  /** Visible strings, for translation. */
  messages?: Partial<OAuthConsentScreenMessages> | undefined;
}

function messageOf(error: unknown, fallback: string): ReactNode {
  return error instanceof Error && error.message ? error.message : fallback;
}

function httpUrl(url: string | undefined): string | undefined {
  return url && isSafeUrl(url, ['http', 'https']) ? url : undefined;
}

/**
 * The OAuth consent screen: which app is asking (logo, name, domain,
 * verification), which account would grant access with a way to switch, what
 * the app will be able to do in plain words, and equal-weight Cancel and
 * Allow buttons with pending states. Links and logos from data are sanitised.
 */
export const OAuthConsentScreen = forwardRef<HTMLElement, OAuthConsentScreenProps>(
  function OAuthConsentScreen(
    {
      app,
      account,
      scopes,
      heldBackScopes,
      onAllow,
      onDeny,
      onSwitchAccount,
      children,
      allowDisabled = false,
      blocked,
      footnote,
      headingLevel = 'h1',
      messages,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...defaultOAuthConsentScreenMessages, ...messages };
    const [busy, setBusy] = useState<'allow' | 'deny' | null>(null);
    const [error, setError] = useState<ReactNode>(null);
    const mounted = useRef(true);
    const titleId = useId();
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);

    const run = async (kind: 'allow' | 'deny') => {
      if (busy) return;
      setError(null);
      setBusy(kind);
      try {
        await (kind === 'allow' ? onAllow() : onDeny());
      } catch (failure) {
        if (mounted.current) setError(messageOf(failure, m.error));
      } finally {
        if (mounted.current) setBusy(null);
      }
    };

    const Heading = headingLevel;
    const logo = app.logoSrc && isSafeImageSrc(app.logoSrc) ? app.logoSrc : null;
    const privacy = httpUrl(app.privacyUrl);
    const terms = httpUrl(app.termsUrl);
    const external = (href: string, label: string) => (
      <a
        href={href}
        target="_blank"
        rel="noopener noreferrer"
        className="mn-oauth-consent-screen__link"
      >
        {label}
        <span className="mn-oauth-consent-screen__sr"> {m.newTab}</span>
      </a>
    );

    const scopeList = (items: OAuthConsentScope[], held: boolean) => (
      <ul className="mn-oauth-consent-screen__scopes" data-held={held || undefined}>
        {items.map((scope) => (
          <li
            key={scope.id}
            className="mn-oauth-consent-screen__scope"
            data-sensitive={scope.sensitive || undefined}
          >
            <span className="mn-oauth-consent-screen__scope-icon" aria-hidden="true">
              {held ? <LockGlyph /> : <CheckGlyph />}
            </span>
            <span className="mn-oauth-consent-screen__scope-text">
              <span className="mn-oauth-consent-screen__scope-label">
                {scope.label}
                {scope.sensitive ? (
                  <span className="mn-oauth-consent-screen__sr"> {m.sensitive}</span>
                ) : null}
              </span>
              {scope.description ? (
                <span className="mn-oauth-consent-screen__scope-description">
                  {scope.description}
                </span>
              ) : null}
            </span>
          </li>
        ))}
      </ul>
    );

    return (
      <section
        ref={ref}
        aria-labelledby={titleId}
        aria-busy={busy ? true : undefined}
        {...rest}
        className={cx('mn-oauth-consent-screen', className)}
        data-blocked={blocked ? '' : undefined}
      >
        <header className="mn-oauth-consent-screen__app">
          <Avatar
            name={app.name}
            src={logo}
            shape="square"
            size="xl"
            decorative
            className="mn-oauth-consent-screen__logo"
          />
          <Heading id={titleId} className="mn-oauth-consent-screen__title">
            {m.title(app.name)}
          </Heading>
          {app.domain ? <p className="mn-oauth-consent-screen__domain">{app.domain}</p> : null}
          {app.redirectHost && app.redirectHost !== app.domain ? (
            <p className="mn-oauth-consent-screen__redirect">{m.redirect(app.redirectHost)}</p>
          ) : null}
          {app.verified ? (
            <Badge tone="success" size="sm" dot>
              {m.verified}
            </Badge>
          ) : null}
        </header>

        {app.verified === false ? (
          <InlineNotice tone="warning" title={m.unverifiedTitle}>
            {m.unverified(app.name)}
          </InlineNotice>
        ) : null}
        {app.testing ? <InlineNotice tone="neutral">{m.testing}</InlineNotice> : null}

        <div className="mn-oauth-consent-screen__account">
          <Avatar
            name={account.name ?? account.email}
            src={account.avatarSrc ?? null}
            size="md"
            decorative
          />
          <div className="mn-oauth-consent-screen__account-text">
            <span className="mn-oauth-consent-screen__sr">{m.signedInAs} </span>
            {account.name ? (
              <span className="mn-oauth-consent-screen__account-name">{account.name}</span>
            ) : null}
            <span className="mn-oauth-consent-screen__account-email">{account.email}</span>
          </div>
          {onSwitchAccount ? (
            <Button
              size="sm"
              variant="ghost"
              disabled={busy !== null}
              className="mn-oauth-consent-screen__switch"
              onClick={onSwitchAccount}
            >
              {m.switchAccount}
            </Button>
          ) : null}
        </div>

        {blocked ? (
          <InlineNotice tone="warning" icon={<LockGlyph />}>
            {blocked}
          </InlineNotice>
        ) : (
          <>
            {scopes.length > 0 ? (
              <div className="mn-oauth-consent-screen__section">
                <p className="mn-oauth-consent-screen__section-title">{m.scopesTitle(app.name)}</p>
                {scopeList(scopes, false)}
              </div>
            ) : null}
            {heldBackScopes && heldBackScopes.length > 0 ? (
              <div className="mn-oauth-consent-screen__section">
                <p className="mn-oauth-consent-screen__section-title">{m.heldBackTitle}</p>
                {scopeList(heldBackScopes, true)}
              </div>
            ) : null}
            {children}
          </>
        )}

        {error ? (
          <p className="mn-oauth-consent-screen__error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="mn-oauth-consent-screen__actions" data-single={blocked ? '' : undefined}>
          <Button
            size="lg"
            loading={busy === 'deny'}
            disabled={busy === 'allow'}
            onClick={() => void run('deny')}
          >
            {m.deny}
          </Button>
          {blocked ? null : (
            <Button
              size="lg"
              variant="primary"
              loading={busy === 'allow'}
              disabled={busy === 'deny' || allowDisabled}
              onClick={() => void run('allow')}
            >
              {m.allow}
            </Button>
          )}
        </div>

        {privacy || terms ? (
          <p className="mn-oauth-consent-screen__policies">
            {m.policyPrompt(app.name)} {privacy ? external(privacy, m.privacy) : null}
            {privacy && terms ? ` ${m.and} ` : null}
            {terms ? external(terms, m.terms) : null}.
          </p>
        ) : null}
        {footnote ? <p className="mn-oauth-consent-screen__footnote">{footnote}</p> : null}
      </section>
    );
  },
);
