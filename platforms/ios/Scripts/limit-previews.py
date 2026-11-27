#!/usr/bin/env python3
"""Keeps every docs preview at or under a byte limit (D9: 150 KB).

usage: limit-previews.py <dir> [limit-bytes]

A PNG over the limit is first reduced to a 256-colour palette (median cut, no dithering, so flat fills stay
flat); if that is still over, it is scaled down by 10 percent steps. oxipng runs afterwards on the files it
changed. Prints each changed file. Exit 1 when a file still exceeds the limit.
"""
import os
import subprocess
import sys

from PIL import Image

directory = sys.argv[1]
limit = int(sys.argv[2]) if len(sys.argv) > 2 else 150_000
changed, failed = [], []
for name in sorted(os.listdir(directory)):
    if not name.endswith(".png"):
        continue
    path = os.path.join(directory, name)
    if os.path.getsize(path) <= limit:
        continue
    image = Image.open(path).convert("RGB")
    scale = 1.0
    while True:
        work = image if scale == 1.0 else image.resize((round(image.width * scale), round(image.height * scale)), Image.LANCZOS)
        work.quantize(colors=256, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE).save(path, optimize=True)
        subprocess.run(["oxipng", "-o", "max", "--strip", "safe", "-q", path], check=False)
        if os.path.getsize(path) <= limit or scale < 0.5:
            break
        scale -= 0.1
    changed.append(name)
    print(f"limit-previews: {name} {os.path.getsize(path)} bytes (scale {scale:.1f})")
    if os.path.getsize(path) > limit:
        failed.append(name)
sys.exit(1 if failed else 0)
