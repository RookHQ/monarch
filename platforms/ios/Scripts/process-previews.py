#!/usr/bin/env python3
"""Crops a raw simulator screenshot to the rectangle the preview host reported, and resamples it to 2x.

usage: process-previews.py <full.png> <preview-rect.txt> <screen-width-points> <out.png>

The iPhone 17 Pro captures at 3x. Previews are published at 2x: sharp on a high-density display and
about half the bytes. The crop is made at 3x first, so its edges fall on whole pixels.

A presentation (last field of the rect file is 1: a sheet, alert, dialog, popover or menu opened by the host) is
captured as the whole screen. It is cut to the presentation itself: the screen outside it is a flat dimmed colour, so
the rows that differ from that colour are grouped into bands (a gap of 20 points or more separates two), the tallest
band is the presentation (the trigger button behind it is a smaller band) and the crop is that band plus 24 points
above and below. The status bar is ignored.

A screen-level recipe is taller than the cap the host applies (the rect file says so in its last field). It is
then cut at the last gap between blocks inside the bottom 140 points, so no card or line is cut in half: a gap is a
band of rows (at least 10 points tall) with no more than two colours, the page and the canvas around it, so the
inside of a card (border, fill and canvas) never counts. The cut keeps 24 points of canvas below
the last block, the same as the padding on the other sides. If there is no gap, the crop stays at the cap.
"""
import sys

from PIL import Image, ImageChops

PADDING_POINTS = 24
WINDOW_POINTS = 140
MIN_GAP_POINTS = 10
GAP_SHARE = 0.998  # a gap row has at most two colours (page and outer canvas) bar this share of stray pixels


def gap_cut(image, scale):
    """The row (in pixels) to cut at, or None. image is the 3x crop."""
    width, height = image.size
    window = int(WINDOW_POINTS * scale)
    minimum = int(MIN_GAP_POINTS * scale)

    def is_gap(y):
        counts = sorted((count for count, _ in (image.crop((0, y, width, y + 1)).getcolors(maxcolors=width) or [])), reverse=True)
        return sum(counts[:2]) >= GAP_SHARE * width

    # Walk up from the bottom, remembering the start of each gap band; take the first band tall enough.
    y = height - 1
    while y > height - window:
        if is_gap(y):
            end = y
            while y > 0 and is_gap(y - 1):
                y -= 1
            if end - y + 1 >= minimum or end == height - 1 and end - y + 1 >= minimum // 2:
                return min(y + int(PADDING_POINTS * scale), end + 1)
        y -= 1
    return None


STATUS_BAR_POINTS = 62
BAND_GAP_POINTS = 20
DIFFERENT = 18  # a pixel differs from the dimmed screen when its channels differ by more than this in total


# The top row of a sheet that starts inside the status bar zone: set by presentation_crop, used by clear_status_bar.
SHEET_TOP = [None]


def clear_status_bar(image, scale):
    """Removes the status bar and the dynamic island from a capture whose sheet starts under them: the rows above the
    sheet become the dimmed screen colour, and the part of the island that overlaps the sheet takes the sheet colour."""
    top = SHEET_TOP[0]
    if top is None:
        return
    width, _ = image.size
    pixels = image.load()
    background = pixels[3, 3]
    for y in range(0, top):
        for x in range(width):
            pixels[x, y] = background
    for y in range(top, min(int(62 * scale), image.size[1])):
        sheet = pixels[int(width * 0.12), y]
        for x in range(int(width * 0.2), int(width * 0.8)):
            if sum(pixels[x, y]) < 480:
                pixels[x, y] = sheet


def presentation_crop(image, scale, menu=False, column_top=None):
    """The (top, bottom) rows of the presentation in a full-screen capture."""
    width, height = image.size
    # The dimmed screen outside the presentation: the top left corner, above any sheet.
    background = image.getpixel((3, 3))
    solid = Image.new("RGB", image.size, background)
    difference = ImageChops.difference(image, solid)
    r, g, b = difference.split()
    mask = ImageChops.add(ImageChops.add(r, g), b).point(lambda value: 255 if value > DIFFERENT else 0)
    first = int(STATUS_BAR_POINTS * scale)
    gap = int(BAND_GAP_POINTS * scale)

    def differs(y):
        row = mask.crop((0, y, width, y + 1))
        if y < first:
            # In the status bar zone only a sheet counts: the clock and the icons cover a small part of the row.
            return row.histogram()[255] > width // 2
        return row.getbbox() is not None

    bands, start, last = [], None, None
    for y in range(height):
        if differs(y):
            if start is None:
                start = y
            elif y - last > gap:
                bands.append((start, last))
                start = y
            last = y
    if start is not None:
        bands.append((start, last))
    if not bands:
        return 0, height
    top, bottom = max(bands, key=lambda band: band[1] - band[0])
    if menu:
        # A menu opened from a control: the control sits above the menu in its own band; keep both.
        top, bottom = bands[0][0], bands[-1][1]
        # A control that is nearly the canvas colour (a secondary button) is no band of its own: when the menu starts
        # well below the top of the column, the control is above it, so start at the column.
        if column_top is not None and top - column_top > int(40 * scale):
            top = int(column_top)
    pad = int(PADDING_POINTS * scale)
    if bottom >= height - int(40 * scale):
        # A sheet runs to the bottom of the screen. Where its lower part is empty, end the image 24 points below the
        # last content (the home indicator is ignored), so the preview is not a tall blank card.
        sheet = image.getpixel((width // 2, height - int(60 * scale)))
        margin = int(width * 0.06)
        for y in range(height - int(34 * scale), top, -1):
            row = image.crop((margin, y, width - margin, y + 1))
            if any(sum(abs(a - b) for a, b in zip(colour, sheet)) > DIFFERENT for _, colour in row.getcolors(maxcolors=width) or []):
                cut = y + pad
                if cut < bottom - int(60 * scale):
                    start = top - pad
                    if start < first:
                        SHEET_TOP[0] = top
                    return max(start, 0), cut
                break
    # Above the presentation the screen shows the status bar and the dynamic island. Padding 24 points up from a sheet
    # that starts below them would pull both into the image, so then only 4 points of the dimmed screen are kept.
    start = top - pad
    if start < first:
        SHEET_TOP[0] = top
    return max(start, 0), min(bottom + pad, height)


full, rect_file, width_points, out = sys.argv[1], sys.argv[2], float(sys.argv[3]), sys.argv[4]
fields = open(rect_file).read().split()
x, y, w, h = (float(v) for v in fields[1:5])
capped = len(fields) > 5 and fields[5] == "1"
presented = len(fields) > 6 and fields[6] == "1"
menu = presented and len(fields) > 7 and fields[7] == "menu"
image = Image.open(full).convert("RGB")
scale = image.width / width_points
box = (round(x * scale), round(y * scale), round((x + w) * scale), round((y + h) * scale))
cropped = image.crop((max(box[0], 0), max(box[1], 0), min(box[2], image.width), min(box[3], image.height)))
if presented:
    column_top = float(fields[8]) * scale if menu and len(fields) > 8 else None
    top, bottom = presentation_crop(cropped, scale, menu, column_top)
    clear_status_bar(cropped, scale)
    cropped = cropped.crop((0, top, cropped.width, bottom))
elif capped and len(fields) <= 7:
    # No natural height in the rect file: cut at the last gap. With it, the preview is taken in strips and stitched.
    cut = gap_cut(cropped, scale)
    if cut:
        cropped = cropped.crop((0, 0, cropped.width, cut))
size = (round(cropped.width * 2 / 3), round(cropped.height * 2 / 3))
cropped.resize(size, Image.LANCZOS).save(out, optimize=True)
