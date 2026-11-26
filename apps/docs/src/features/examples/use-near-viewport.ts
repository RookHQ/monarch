import { useEffect, useState, type RefObject } from 'react';

/**
 * `true` once the element comes within `margin` of the viewport, and from then
 * on. Live previews mount only then: a page with dozens of examples renders
 * the ones being read first, and the prerendered HTML carries none of them.
 * Where the observer is missing, everything mounts at once.
 */
export function useNearViewport(ref: RefObject<Element | null>, margin = '600px'): boolean {
  const [near, setNear] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el || near) return;
    if (typeof IntersectionObserver === 'undefined') {
      setNear(true);
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting)) setNear(true);
      },
      { rootMargin: `${margin} 0px` },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [ref, near, margin]);
  return near;
}
