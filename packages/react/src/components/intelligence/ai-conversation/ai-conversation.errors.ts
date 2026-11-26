/**
 * Error classification shared by the hook, the transports and the mappers:
 * which codes may be retried, how HTTP statuses map to codes, and the
 * user-facing text of the errors Monarch raises itself (never a raw
 * `Error.message` such as "Failed to fetch").
 */

/** Codes Monarch's transports and mappers produce. Services may send others. */
export type AiErrorCode =
  | 'network'
  | 'timeout'
  | 'rate-limited'
  | 'auth'
  | 'bad-request'
  | 'server'
  | 'overloaded'
  | 'upload-failed'
  | 'attachments-unsupported'
  | 'tool-result-failed'
  | 'unknown';

/** User-facing text of the errors Monarch raises itself. English defaults. */
export interface AiConversationMessages {
  /** The connection dropped or the request could not be sent. */
  connectionLost: string;
  /** The service sent nothing for longer than `idleTimeoutMs`. */
  idleTimeout: string;
  /** HTTP 429. */
  rateLimit: string;
  /** HTTP 401 and 403. */
  auth: string;
  /** HTTP 400 and 422. */
  badRequest: string;
  /** HTTP 5xx, or a service error without a message. */
  unavailable: string;
  /** Another HTTP failure. */
  unreachable: string;
  /** An attachment could not be uploaded. */
  uploadFailed: string;
  /** Files were attached but the conversation has no way to send them (`uploadFile`). */
  attachmentsUnsupported: string;
  /** A file is larger than `inlineFiles.maxBytes`. */
  fileTooLarge: (name: string) => string;
  /** The transport could not continue after a tool result. */
  toolResultFailed: string;
}

export const AI_CONVERSATION_MESSAGES: AiConversationMessages = {
  connectionLost: 'The connection was lost.',
  idleTimeout: 'The assistant stopped responding.',
  rateLimit: 'Too many requests. Wait a moment and try again.',
  auth: 'You are not signed in, or you do not have access to the assistant.',
  badRequest: 'The assistant could not accept this request.',
  unavailable: 'The assistant is unavailable.',
  unreachable: 'The assistant could not be reached.',
  uploadFailed: 'An attachment could not be uploaded.',
  attachmentsUnsupported: 'Attachments cannot be sent in this conversation.',
  fileTooLarge: (name) => `${name} is too large to attach.`,
  toolResultFailed: 'The tool result could not be sent.',
};

/** Merges partial strings over the English defaults. */
export function conversationMessages(
  partial?: Partial<AiConversationMessages>,
): AiConversationMessages {
  return { ...AI_CONVERSATION_MESSAGES, ...partial };
}

const RETRYABLE = new Set<string>([
  'network',
  'timeout',
  'rate-limited',
  // Older code from `createSseTransport`, still understood.
  'rate-limit',
  'server',
  'overloaded',
  'overloaded_error',
  'rate_limit_error',
  'api_error',
]);

/**
 * True when a failed turn may be tried again unchanged: network drops,
 * timeouts, rate limits, overload and server errors (`http-5xx`). Unknown
 * codes are not retryable, so a broken request is not repeated.
 */
export function isRetryableErrorCode(code: string | undefined): boolean {
  if (!code) return false;
  if (RETRYABLE.has(code)) return true;
  const http = /^http-(\d{3})$/.exec(code);
  return http ? Number(http[1]) >= 500 : false;
}

/** Code, retryability and message key for an HTTP failure status. */
export interface AiHttpErrorClass {
  code: string;
  retryable: boolean;
  message: keyof Pick<
    AiConversationMessages,
    'rateLimit' | 'auth' | 'badRequest' | 'unavailable' | 'unreachable'
  >;
}

/**
 * Maps an HTTP status: 401/403 → `auth`, 400/422 → `bad-request` (neither
 * retryable), 429 → `rate-limited` (retryable), 5xx → `http-5xx`
 * (retryable), any other status → `http-<status>` (not retryable).
 */
export function classifyHttpStatus(status: number): AiHttpErrorClass {
  if (status === 401 || status === 403) return { code: 'auth', retryable: false, message: 'auth' };
  if (status === 400 || status === 422)
    return { code: 'bad-request', retryable: false, message: 'badRequest' };
  if (status === 429) return { code: 'rate-limited', retryable: true, message: 'rateLimit' };
  if (status >= 500) return { code: `http-${status}`, retryable: true, message: 'unavailable' };
  return { code: `http-${status}`, retryable: false, message: 'unreachable' };
}

/**
 * Milliseconds to wait from a `Retry-After` header (seconds or an HTTP date),
 * or null when there is none.
 */
export function parseRetryAfter(
  header: string | null | undefined,
  now = Date.now(),
): number | null {
  if (!header) return null;
  const trimmed = header.trim();
  if (/^\d+(\.\d+)?$/.test(trimmed)) return Math.max(0, Number(trimmed) * 1000);
  const date = Date.parse(trimmed);
  return Number.isNaN(date) ? null : Math.max(0, date - now);
}
