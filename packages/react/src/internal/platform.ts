/**
 * Platform detection shared by the components that show or match keyboard
 * shortcuts (CommandPalette's hotkeys, KeyboardShortcutsSheet, AiLauncher).
 */

/**
 * Whether the browser runs on an Apple platform (macOS, iOS, iPadOS), where
 * `mod` means ⌘ rather than Ctrl. Reads `navigator.userAgentData.platform`
 * when the browser has it, else `navigator.platform`. `false` on the server.
 */
export function isApplePlatform(): boolean {
  if (typeof navigator === 'undefined') return false;
  const nav = navigator as Navigator & { userAgentData?: { platform?: string } };
  const platform = nav.userAgentData?.platform ?? nav.platform ?? '';
  return /mac|iphone|ipad|ipod/i.test(platform);
}
