/**
 * Small JSON helpers shared by the stream-format mappers and request
 * builders. Internal: not re-exported from the package.
 */
export type Json = Record<string, unknown>;

export const isObject = (v: unknown): v is Json =>
  !!v && typeof v === 'object' && !Array.isArray(v);

export function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

/** Tool arguments arrive as JSON text in pieces; a broken document stays visible as text. */
export function parseArguments(text: string): unknown {
  if (!text.trim()) return {};
  const parsed = parseJson(text);
  return parsed === undefined ? text : parsed;
}

export const num = (v: unknown) => (typeof v === 'number' && Number.isFinite(v) ? v : 0);
/** A finite, non-negative number, or undefined. */
export const optNum = (v: unknown): number | undefined =>
  typeof v === 'number' && Number.isFinite(v) && v >= 0 ? v : undefined;
export const str = (v: unknown): string | undefined => (typeof v === 'string' && v ? v : undefined);

/** Error codes from vendor formats that are worth retrying. */
const RETRYABLE_VENDOR = /rate|overload|server|timeout|unavailable|api_error|internal/i;

export function vendorRetryable(code: string): boolean {
  return RETRYABLE_VENDOR.test(code);
}

export const nowIso = () => new Date().toISOString();

/** The optional `AiUsage` detail fields, leaving out the ones the service did not report. */
export function usageDetails(
  cacheRead: unknown,
  cacheWrite: unknown,
  reasoning: unknown,
): { cacheReadTokens?: number; cacheWriteTokens?: number; reasoningTokens?: number } {
  const read = optNum(cacheRead);
  const write = optNum(cacheWrite);
  const thought = optNum(reasoning);
  return {
    ...(read !== undefined ? { cacheReadTokens: read } : {}),
    ...(write !== undefined ? { cacheWriteTokens: write } : {}),
    ...(thought !== undefined ? { reasoningTokens: thought } : {}),
  };
}
