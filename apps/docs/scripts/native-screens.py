"""Turns the raw showcase captures into the docs site's images.

    python3 native-screens.py <raw folder> <apps/docs/public/native> <manifest.json>

Called by capture-native-screens.sh. For each `<platform>-<name>.<theme>.png`
in the raw folder it writes `<platform>-<name>.<theme>.webp`:
- a screen (`phone`, `tablet`, `iphone`, `ipad`): resized to the width the page
  shows it at (twice its CSS size); on Android with the status bar's height
  added above the app, filled with the app's canvas colour, so the page can
  draw the status bar there;
- a tile (`tile-*`, a 402 x 308 point box): resized to 2x, 804 x 616, so every
  tile on both platforms has the same scale;
- a card (`float-*`): trimmed to the card (the canvas around it removed) and
  resized to 2x of its size in points (iOS captures at 2x, Android at 3x). The manifest records each
image's size, which the page reads for its width and height attributes.
Fails when an image is over budget.
"""

import json
import sys
from pathlib import Path

from PIL import Image, ImageChops

BUDGET = 150 * 1024
# Output width in pixels per device, and the status bar height (dp) added on Android.
WIDTH = {"iphone": 600, "ipad": 880, "phone": 600, "tablet": 880}
DENSITY = {"phone": 3, "tablet": 2}
TILE_WIDTH = 804
# Pixels per point of the raw tiles and cards, per platform; the output is 2x.
RAW_DENSITY = {"ios": 2, "android": 3}
STATUS_BAR_DP = 32


def trim(image: Image.Image) -> Image.Image:
    """The image cut to what differs from its corner colour (the canvas around a card)."""
    canvas = Image.new("RGB", image.size, image.getpixel((1, 1)))
    diff = ImageChops.difference(image, canvas).convert("L").point(lambda v: 255 if v > 6 else 0)
    box = diff.getbbox()
    return image.crop(box) if box else image


def process(src: Path, dest: Path) -> int:
    platform, rest = src.stem.split("-", 1)
    device = rest.split(".", 1)[0].split("-", 1)[0]
    image = Image.open(src).convert("RGB")
    if device == "tile":
        return save(image.resize((TILE_WIDTH, round(image.height * TILE_WIDTH / image.width)), Image.Resampling.LANCZOS), dest)
    if device == "float":
        image = trim(image)
        scale = 2 / RAW_DENSITY[platform]
        size = (round(image.width * scale), round(image.height * scale))
        return save(image.resize(size, Image.Resampling.LANCZOS) if scale != 1 else image, dest)
    if platform == "android":
        bar = STATUS_BAR_DP * DENSITY[device]
        padded = Image.new("RGB", (image.width, image.height + bar), image.getpixel((4, 4)))
        padded.paste(image, (0, bar))
        image = padded
    width = WIDTH[device]
    height = round(image.height * width / image.width)
    return save(image.resize((width, height), Image.Resampling.LANCZOS), dest)


def save(image: Image.Image, dest: Path) -> int:
    """Writes WebP at the highest quality that stays within the budget; returns the size in bytes."""
    for quality in (84, 78, 72, 66, 60):
        image.save(dest, "WEBP", quality=quality, method=6)
        size = dest.stat().st_size
        if size <= BUDGET:
            return size
    raise SystemExit(f"native-screens: {dest.name} is {size} bytes, over the {BUDGET} byte budget")


def main() -> None:
    raw, out, manifest = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    sizes = json.loads(manifest.read_text()) if manifest.exists() else {}
    out.mkdir(parents=True, exist_ok=True)
    files = sorted(raw.glob("*-*.*.png"))
    if not files:
        raise SystemExit(f"native-screens: no screenshots in {raw}")
    for src in files:
        dest = out / (src.name[: -len(".png")] + ".webp")
        size = process(src, dest)
        with Image.open(dest) as done:
            sizes[dest.name] = {"width": done.width, "height": done.height}
            print(f"{dest.name}: {done.width}x{done.height}, {size / 1024:.1f} KB")
    manifest.write_text(json.dumps(dict(sorted(sizes.items())), indent=2) + "\n")


if __name__ == "__main__":
    main()
