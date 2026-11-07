import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { InfoGlyph, WarningGlyph, BotGlyph } from '../../../internal/icons.js';

/** How much harm a wrong answer could do where the disclaimer appears. */
export type AiDisclaimerRisk = 'low' | 'medium' | 'high';

/** The default wording per risk. English defaults. */
export type AiDisclaimerMessages = Record<AiDisclaimerRisk, string>;

export const AI_DISCLAIMER_MESSAGES: AiDisclaimerMessages = {
  low: 'AI can make mistakes. Check important info.',
  medium: 'AI can make mistakes. Check important details before you rely on them.',
  high: 'AI-generated content can be wrong. Have a qualified person review it before you act on it.',
};

export interface AiDisclaimerProps extends HTMLAttributes<HTMLDivElement> {
  /**
   * `low` (default): a quiet line under a composer or answer. `medium`: adds an
   * info icon and stronger wording. `high`: a warning-toned note for answers
   * people might act on (money, health, legal, deleting data).
   */
  risk?: AiDisclaimerRisk | undefined;
  /** Replaces the default wording for this instance. */
  children?: ReactNode;
  /** Replaces the default wording per risk, for localisation. */
  messages?: Partial<AiDisclaimerMessages> | undefined;
}

const ICONS = { low: BotGlyph, medium: InfoGlyph, high: WarningGlyph } as const;

/**
 * A short reminder that AI output can be wrong, worded and styled for how much
 * a mistake would matter: "AI can make mistakes. Check important info."
 */
export const AiDisclaimer = forwardRef<HTMLDivElement, AiDisclaimerProps>(function AiDisclaimer(
  { risk = 'low', children, messages, className, ...rest },
  ref,
) {
  const Icon = ICONS[risk];
  const text = children ?? { ...AI_DISCLAIMER_MESSAGES, ...messages }[risk];
  return (
    <div
      ref={ref}
      role="note"
      {...rest}
      className={cx('mn-ai-disclaimer', className)}
      data-risk={risk}
    >
      <Icon className="mn-ai-disclaimer__icon" />
      <span className="mn-ai-disclaimer__text">{text}</span>
    </div>
  );
});
