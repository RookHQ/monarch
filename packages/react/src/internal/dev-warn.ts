const seen = new Set<string>();

function isDev() {
  const env = (globalThis as { process?: { env?: Record<string, string | undefined> } }).process
    ?.env;
  return env?.NODE_ENV !== 'production';
}

/** Logs a Monarch usage warning once per message, and never in production builds. */
export function warnOnce(message: string) {
  if (!isDev() || seen.has(message)) return;
  seen.add(message);
  console.warn(`[monarch] ${message}`);
}
