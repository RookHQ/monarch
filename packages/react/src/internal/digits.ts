/**
 * Decimal digits from other scripts, as people type them on Arabic, Persian,
 * Devanagari, Bengali or full-width keyboards. JavaScript's `\d` only matches
 * ASCII 0-9, so a filter such as `replace(/\D/g, '')` silently drops them.
 */

/** First code point (the zero) of each supported block of ten decimal digits. */
const ZEROS = [
  0x0660, // Arabic-Indic
  0x06f0, // Extended Arabic-Indic (Persian, Urdu)
  0x0966, // Devanagari
  0x09e6, // Bengali
  0x0e50, // Thai
  0xff10, // Full-width
];

const NON_ASCII_DIGIT = /[٠-٩۰-۹०-९০-৯๐-๙０-９]/g;

/** Replaces decimal digits of other scripts with ASCII 0-9; everything else is kept. */
export function toAsciiDigits(text: string): string {
  return text.replace(NON_ASCII_DIGIT, (ch) => {
    const code = ch.charCodeAt(0);
    for (const zero of ZEROS) {
      if (code >= zero && code <= zero + 9) return String(code - zero);
    }
    return ch;
  });
}

/** Characters that only steer text direction; `Intl` adds them around signs and symbols in RTL locales. */
const BIDI_MARKS = /[؜‎‏‪-‮⁦-⁩]/g;

/** Removes invisible direction marks (LRM, RLM, ALM, embeddings and isolates). */
export function stripBidiMarks(text: string): string {
  return text.replace(BIDI_MARKS, '');
}
