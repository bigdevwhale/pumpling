"""Cuts the Codex sheets in art/sheets into app drawables.

    python tools/cut.py

Pets (3×2): pet_<id>_<0..5>.webp – baby, teen, adult, signature move, cheer, asleep.
Items (4×4): item_<id>.webp. Eggs (5×4): egg_<species>.webp. Gym: gym_bg.webp.
The magenta background is keyed out; each frame is cropped to its creature.
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHEETS = ROOT / "art" / "sheets"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

PETS = ["bull", "otter", "lynx", "octopus", "crab", "hedgehog", "kangaroo", "hummingbird", "turtle", "sloth",
        "squirrel", "gorilla", "ant", "crane", "mole", "owl", "hamster", "phoenix", "mammoth", "dragon"]
ITEMS = ["rack", "treadmill", "rower", "bag", "plant", "poster", "boombox", "lights",
         "trophies", "mirror", "kettle", "mat", "cooler", "fan", "neon", "bench"]
MAX = 320


def key(im):
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            m = min(r, b) - g  # how magenta
            if m > 90:
                px[x, y] = (0, 0, 0, 0)
            elif m > 40:
                # a fringe: pull the pink out and fade it
                k = (m - 40) / 50
                px[x, y] = (int(r - (r - g) * k), g, int(b - (b - g) * k), int(255 * (1 - k)))
    return im


def frames(sheet, cols, rows):
    im = key(Image.open(sheet))
    w, h = im.size
    cw, ch = w / cols, h / rows
    out = []
    for j in range(rows):
        for i in range(cols):
            cell = im.crop((int(i * cw), int(j * ch), int((i + 1) * cw), int((j + 1) * ch)))
            box = cell.getchannel("A").point(lambda v: 255 if v > 30 else 0).getbbox()
            cell = cell.crop(box) if box else cell
            s = MAX / max(cell.size)
            if s < 1:
                cell = cell.resize((max(1, round(cell.width * s)), max(1, round(cell.height * s))), Image.LANCZOS)
            out.append(cell)
    return out


def save(im, name):
    im.save(OUT / f"{name}.webp", "WEBP", quality=90, method=6)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for p in PETS:
        f = SHEETS / f"{p}.png"
        if not f.exists():
            print("missing", p)
            continue
        for n, fr in enumerate(frames(f, 3, 2)):
            save(fr, f"pet_{p}_{n}")
        print("pet", p)
    if (SHEETS / "items.png").exists():
        for name, fr in zip(ITEMS, frames(SHEETS / "items.png", 4, 4)):
            save(fr, f"item_{name}")
        print("items")
    if (SHEETS / "eggs.png").exists():
        for name, fr in zip(PETS, frames(SHEETS / "eggs.png", 5, 4)):
            save(fr, f"egg_{name}")
        print("eggs")
    if (SHEETS / "gym.png").exists():
        g = Image.open(SHEETS / "gym.png").convert("RGB")
        g = g.resize((960, round(960 * g.height / g.width)), Image.LANCZOS)
        g.save(OUT / "gym_bg.webp", "WEBP", quality=88, method=6)
        print("gym")


if __name__ == "__main__":
    main()
