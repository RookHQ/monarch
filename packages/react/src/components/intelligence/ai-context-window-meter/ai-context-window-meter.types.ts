import type { AiUsage } from '../ai-conversation/index.js';

/** One part of what fills the context window, e.g. "System prompt" or "Attachments". */
export interface AiContextWindowPart {
  label: string;
  tokens: number;
}

/** Where a context-usage figure comes from. */
export type AiContextUsageSource = 'reported' | 'counted' | 'estimated';

/** How full the context window is, as `useAiContextUsage` works it out. AiContextWindowMeter takes it as `usage`. */
export interface AiContextUsage {
  /** Tokens in the context window now. */
  used: number;
  /** The model's context window size, in tokens. */
  limit: number;
  /**
   * True when any part of `used` is a guess: the default estimator was used,
   * or a count is still on its way. The meter then says "About …".
   */
  approximate: boolean;
  /** True while an asynchronous `countTokens` has not answered for every message. */
  counting: boolean;
  /**
   * `reported`: the service's own figure from the latest reply's `usage`
   * (plus counts for anything newer). `counted`: your `countTokens`.
   * `estimated`: the default estimator.
   */
  source: AiContextUsageSource;
  /** What the tokens are made of, when that is known. */
  breakdown?: readonly AiContextWindowPart[] | undefined;
  /** The usage the latest reply reported, with cache and reasoning tokens when the service gave them. */
  reported?: AiUsage | undefined;
}
