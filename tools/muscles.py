"""art/sheets/muscles.png (flat colour-keyed chart) → res/drawable-nodpi/muscle_idx.png.
Each pixel's red channel holds its class: 0 background, 1 skin, 2 outline, 3+ a muscle (order of MUSCLES);
green holds the line darkness (0..255) so outlines stay smooth. Alpha marks the figure."""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parent.parent
KEYS = [("chest", (255, 0, 0)), ("delts", (255, 255, 0)), ("bi", (0, 0, 255)), ("tri", (0, 255, 255)), ("fore", (255, 0, 255)),
        ("abs", (0, 255, 0)), ("lats", (255, 128, 0)), ("upper", (128, 0, 255)), ("lower", (128, 64, 0)), ("glutes", (255, 128, 192)),
        ("quads", (0, 128, 0)), ("hams", (0, 0, 128)), ("calves", (0, 128, 128))]
MUSCLES = [k for k, _ in KEYS]
im = Image.open(ROOT / "art/sheets/muscles.png").convert("RGB")
im = im.resize((560, round(im.height * 560 / im.width)), Image.LANCZOS)
W, H = im.size; px = im.load()
out = Image.new("RGBA", (W, H)); op = out.load()
for y in range(H):
    for x in range(W):
        r, g, b = px[x, y]
        lum = (r * 3 + g * 6 + b) / 10
        if r > 235 and g > 235 and b > 235:
            op[x, y] = (0, 0, 0, 0); continue
        best, bd = None, 1e9
        for i, (_, (kr, kg, kb)) in enumerate(KEYS):
            d = (r - kr) ** 2 + (g - kg) ** 2 + (b - kb) ** 2
            if d < bd: best, bd = i, d
        sk = (r - 200) ** 2 + (g - 200) ** 2 + (b - 200) ** 2
        if lum < 70 and max(r, g, b) - min(r, g, b) < 60:
            op[x, y] = (2, int(255 - lum), 0, 255)
        elif bd < 4500 and bd < sk:
            op[x, y] = (3 + best, 0, 0, 255)
        elif max(r, g, b) - min(r, g, b) < 40:
            op[x, y] = (1, int(max(0, 200 - lum)), 0, 255)   # skin, darker near lines
        else:
            op[x, y] = (2, 120, 0, 255)
# lines and stray specks inside a muscle take that muscle's class, keeping their darkness in green,
# so a lit muscle shows its own inner lines in a darker shade of its colour
from collections import Counter
cls = [[op[x, y][0] if op[x, y][3] else 0 for x in range(W)] for y in range(H)]
for y in range(H):
    for x in range(W):
        c = cls[y][x]
        if c == 0 or c == 1: continue
        cnt = Counter(cls[j][i] for j in range(max(0, y - 3), min(H, y + 4)) for i in range(max(0, x - 3), min(W, x + 4)) if cls[j][i] >= 3)
        if not cnt: continue
        top, n = cnt.most_common(1)[0]
        total = sum(1 for j in range(max(0, y - 3), min(H, y + 4)) for i in range(max(0, x - 3), min(W, x + 4)) if cls[j][i] != 0)
        if c == 2 and n * 10 >= total * 6:
            op[x, y] = (top, op[x, y][1], 0, 255)
        elif c >= 3 and top != c and cnt[c] * 2 < n:
            op[x, y] = (top, 150, 0, 255)
out.save(ROOT / "app/src/main/res/drawable-nodpi/muscle_idx.png")
print(W, H, MUSCLES)
