export default function Page() {
  return (
    <main>
      <p className="text-red-500">raw palette</p>
      {/* monarch-ignore-next-line color/no-raw-palette -- brand-mandated partner badge */}
      <p className="bg-blue-600">suppressed</p>
      {/* monarch-ignore-next-line color/no-raw-palette */}
      <p className="bg-green-600">suppression without reason</p>
      <a href="#top">anchor, not a colour</a>
      <img src="/logo.svg" />
      <div onClick={() => alert('hi')}>clickable div</div>
      <input className="outline-none" />
      <span tabIndex={2}>positive</span>
    </main>
  );
}
