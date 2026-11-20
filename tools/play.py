"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/<n>-<name>.png (1080x2340 captures of the demo build, see tools/capture.sh)
and writes the 512 px icon, the 1024x500 feature graphics and six 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen loses the phone's status bar
and runs off the bottom edge under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONTS = ROOT / "app/src/main/res/font"

CREAM = (0xF5, 0xEF, 0xE6)
SAND = (0xEB, 0xDF, 0xCF)
INK = (0x2A, 0x24, 0x20)
ORANGE = (0xE8, 0x61, 0x3C)
ORANGE_DEEP = (0xC9, 0x4A, 0x2A)
LIGHT = (0xFF, 0xF6, 0xEC)
STATUS_BAR = 110  # the clock and icons at the top of every capture

# The order they appear in the store, with the caption over each: a line in ink and one in orange.
PAGES = [
    ("1-gym", ("Pets train", "alongside you"), ("Питомцы тренируются", "вместе с вами")),
    ("2-log", ("Every set,", "every week"), ("Каждый подход,", "каждая неделя")),
    ("3-pets", ("Twenty pets", "to find and raise"), ("Двадцать питомцев:", "найдите и вырастите")),
    ("4-stats", ("Workouts, tonnage", "and records"), ("Тренировки, тоннаж", "и рекорды")),
    ("5-exercise", ("275 exercises", "and your progress"), ("275 упражнений", "и ваш прогресс")),
    ("6-card", ("Cards to share:", "the week, a record, a pet"), ("Открытки: неделя,", "рекорд, питомец")),
]


def nunito(size, weight="bold"):
    return ImageFont.truetype(str(FONTS / f"nunito_{weight}.ttf"), size)


def fit(text, size, width, weight="bold"):
    while nunito(size, weight).getlength(text) > width:
        size -= 2
    return nunito(size, weight)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), r, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def lay(g, card, x, y, r=44, edge=SAND, width=4):
    """A screen on the board: rounded corners, a soft warm shadow and a thin edge."""
    sh = Image.new("RGBA", (card.width + 120, card.height + 120), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle((60, 78, 60 + card.width, 78 + card.height), r, fill=(90, 50, 30, 70))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(26)), (x - 60, y - 60))
    g.alpha_composite(rounded(card, r), (x, y))
    ImageDraw.Draw(g).rounded_rectangle((x - 2, y - 2, x + card.width + 1, y + card.height + 1), r, outline=edge, width=width)


def screen(lang, name):
    im = Image.open(SHOTS / lang / f"{name}.png").convert("RGB")
    return im.crop((0, STATUS_BAR, im.width, im.height))


def screenshot(lang, name, caption):
    W, H = 1080, 1920
    g = Image.new("RGBA", (W, H), CREAM)
    d = ImageDraw.Draw(g)
    # A warm orange band behind the caption, the colour of the app's buttons.
    d.rectangle((0, 0, W, 300), fill=ORANGE)
    d.ellipse((-200, 180, W + 200, 420), fill=ORANGE)
    size = min(fit(line, 84, W - 120).size for line in caption)
    d.text((W / 2, 112), caption[0], font=nunito(size), fill=LIGHT, anchor="mm")
    d.text((W / 2, 212), caption[1], font=nunito(size), fill=INK, anchor="mm")
    w, top = 860, 380
    im = screen(lang, name)
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    # The screen runs on past the bottom edge.
    lay(g, im.crop((0, 0, w, H - top + 60)), (W - w) // 2, top)
    return g.convert("RGB")


def icon():
    """Play rounds the icon itself: the logo goes on a full square of its own orange, no transparent corners."""
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((512, 512), Image.LANCZOS)
    square = Image.new("RGBA", (512, 512), logo.getpixel((256, 4)))
    square.alpha_composite(logo)
    return square.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = Image.new("RGBA", (W, H), ORANGE)
    d = ImageDraw.Draw(g)
    d.ellipse((560, -260, 1400, 760), fill=ORANGE_DEEP)
    logo = Image.open(ROOT / "docs/logo.png").convert("RGBA").resize((132, 132), Image.LANCZOS)
    lay(g, logo, 62, 62, r=30, edge=LIGHT, width=3)
    lines = ("A workout log", "with pets that train too") if lang == "en" else ("Дневник тренировок", "с питомцами-качками")
    d.text((60, 222), "Pumpling", font=nunito(76), fill=LIGHT)
    d.rounded_rectangle((64, 326, 300, 332), 3, fill=INK)
    f = fit(max(lines, key=len), 38, 360, "semibold")
    for i, l in enumerate(lines):
        d.text((64, 352 + i * 48), l, font=f, fill=LIGHT if i == 0 else INK)
    # The basement gym with the team, cut from the first screen.
    im = Image.open(SHOTS / lang / "1-gym.png").convert("RGB").crop((36, 365, 1044, 860))
    im = im.resize((round(im.width * 240 / im.height), 240), Image.LANCZOS)
    lay(g, im, W - im.width - 48, (H - im.height) // 2, r=28, edge=LIGHT, width=4)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon().save(OUT / "icon-512.png", optimize=True)
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(lang, name, en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
