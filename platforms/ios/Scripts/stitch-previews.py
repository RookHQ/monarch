#!/usr/bin/env python3
"""Joins the strips of a tall preview into one image, top to bottom.

usage: stitch-previews.py <out.png> <strip.png> [<strip.png> ...]

A preview taller than the simulator screen is captured in strips (the host shows the example from `-offset`
points on); every strip has the same width and the strips meet exactly, so the join is seamless.
"""
import sys

from PIL import Image, ImageChops, ImageStat

strips = [Image.open(path).convert("RGB") for path in sys.argv[2:]]

# A strip of a lazily laid out screen (a LazyVStack, a List, a Form) can start a few points above where the last one
# ended, because the rows are measured again on each launch. Then the last rows of one strip are the first rows of
# the next. Find that overlap, when the rows are the same pixels and carry content, and drop it from the next strip.
MAX_OVERLAP = 360  # pixels, 180 points at 2x
MIN_OVERLAP = 16
trimmed = [strips[0]]
for strip in strips[1:]:
    above = trimmed[-1]
    limit = min(MAX_OVERLAP, above.height, strip.height)
    cut = 0
    for rows in range(limit, MIN_OVERLAP - 1, -1):
        block = strip.crop((0, 0, strip.width, rows))
        colours = block.getcolors(maxcolors=64)
        if colours is not None and len(colours) <= 2:
            continue  # blank canvas matches anything
        tail = above.crop((0, above.height - rows, above.width, above.height))
        if max(ImageStat.Stat(ImageChops.difference(tail, block)).mean) < 1.0:
            cut = rows
            break
    trimmed.append(strip.crop((0, cut, strip.width, strip.height)) if cut else strip)
strips = trimmed

width = strips[0].width
height = sum(strip.height for strip in strips)
joined = Image.new("RGB", (width, height))
top = 0
for strip in strips:
    joined.paste(strip, (0, top))
    top += strip.height
# A frame taller than the content leaves the canvas colour below the last block: end the image 24 points (48 pixels
# at 2x) under the last row that differs from it.
canvas = joined.getpixel((width - 1, height - 1))
rows = joined.convert("RGB")
last = height - 1
while last > 0:
    line = rows.crop((0, last, width, last + 1))
    colours = line.getcolors(maxcolors=width) or []
    if len(colours) > 1 or colours[0][1] != canvas:
        break
    last -= 1
keep = min(height, last + 1 + 48)
joined.crop((0, 0, width, keep)).save(sys.argv[1], optimize=True)
