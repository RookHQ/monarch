#!/usr/bin/env python3
"""Compress the docs preview PNGs of a folder in place (oxipng, and Pillow when an image is still too big).

Usage: compress-previews.py <folder> [max-bytes]   (default 150000)

Lossless first: images that already use at most 256 colours become palette images; oxipng does the rest.
Only an image that is still over the limit is reduced to 256 colours (no dithering), and the result is kept
only when it is smaller. The totals are printed.
"""
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image

folder = Path(sys.argv[1])
limit = int(sys.argv[2]) if len(sys.argv) > 2 else 150_000
oxipng = shutil.which("oxipng") or "/opt/homebrew/bin/oxipng"


def optimise(path: Path) -> None:
    subprocess.run([oxipng, "-o", "4", "--strip", "all", "-q", str(path)], check=True)


total = 0
over = []
files = sorted(folder.glob("*.png"))
for path in files:
    image = Image.open(path).convert("RGB")
    colors = image.getcolors(maxcolors=257) or []
    if len(colors) in range(1, 257):
        # Exact: the palette is the image's own colours. A quantiser merged near colours (a 252 canvas and a 242 row
        # fill became one 246 grey), which hid the rows of the stack preview.
        palette = [rgb for _, rgb in colors]
        index = {rgb: i for i, rgb in enumerate(palette)}
        exact = Image.new("P", image.size)
        exact.putpalette([c for rgb in palette for c in rgb])
        exact.putdata([index[rgb] for rgb in image.getdata()])
        exact.save(path)
    else:
        image.save(path)
    optimise(path)
    if path.stat().st_size > limit:
        before = path.read_bytes()
        image.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path)
        optimise(path)
        if path.stat().st_size >= len(before):
            path.write_bytes(before)
    size = path.stat().st_size
    total += size
    if size > limit:
        over.append((path.name, size))
print(f"{len(files)} previews, {total / 1e6:.1f} MB in total, largest {max((p.stat().st_size for p in files), default=0)} bytes")
for name, size in over:
    print(f"over {limit} bytes: {name} {size}")
