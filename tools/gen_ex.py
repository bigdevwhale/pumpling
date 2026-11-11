"""Draws an illustration for every exercise in the catalogue with the Codex CLI image tool.

Nine exercises a sheet (3×3) keeps the order reliable. Sheets land in art/ex/sheet_NN.png, and
art/ex/sheets.json lists which ids each one holds; `python tools/cut.py` then cuts them into
res/drawable-nodpi/ex_<id>.webp. Existing sheets are skipped, so a failed one is simply rerun.

    python tools/gen_ex.py          # all missing sheets
    python tools/gen_ex.py 3 7      # only these sheet numbers (redraw)
"""
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "ex"
GEN = Path.home() / ".codex" / "generated_images"
PER = 9

rows = [l.split("\t") for l in (ROOT / "app/src/main/assets/exercises.tsv").read_text(encoding="utf-8").splitlines() if l.strip() and not l.startswith("#")]
ex = [(r[0], r[2]) for r in rows]
sheets = [ex[i:i + PER] for i in range(0, len(ex), PER)]

STYLE = ("Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, 3-4 tone shading, warm colours, "
         "exactly the style of the attached reference pictures. Equipment drawn simply and clearly (barbell with red plates, dumbbells, machines in grey).")

sys.path.insert(0, str(ROOT / "tools"))
import gen_art  # noqa: E402
DESC = dict(gen_art.PETS)
DESC["bull"] = ("a chubby bull calf with small horns and a red sweatband", "")
# which pets demonstrate each family, taking turns
BY_CAT = {"legs": ["bull", "mammoth", "squirrel"], "push": ["crab", "ant", "bull"], "pull": ["octopus", "gorilla", "sloth"],
          "core": ["hedgehog", "owl", "mole"], "cardio": ["otter", "turtle", "hamster"], "flex": ["lynx", "crane", "sloth"],
          "jump": ["kangaroo", "hummingbird", "dragon"], "hiit": ["hummingbird", "phoenix", "squirrel"], "bal": ["crane", "lynx", "owl"]}
CAT = {r[0]: r[7] for r in rows}
turn = {}
PET = {}
for i, _ in ex:
    c = CAT[i]; k = turn.get(c, 0); PET[i] = BY_CAT[c][k % len(BY_CAT[c])]; turn[c] = k + 1
REF = ROOT / "app/src/main/res/drawable-nodpi"


def prompt(items):
    cells = "\n".join(f"{i + 1}) the {PET[x]} ({DESC[PET[x]][0]}) doing: {name}" for i, (x, name) in enumerate(items))
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: an exercise illustration sheet for a fitness app. Each cell shows one of the app's pet characters (they look exactly like in the
attached reference pictures, which are named after them) performing one exercise with correct, clear form, mid-movement,
in the clearest view for that exercise (usually from the side), so a person can understand how to do it.
{STYLE}
Layout: 3 columns x 3 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One illustration per cell,
centred, with clear margin; nothing touches the cell borders. No text, no labels, no numbers, no grid lines, no floor shadows.
Cells, left to right, top to bottom (exactly {len(items)}; leave any remaining cells empty magenta):
{cells}"""


def image_of(output):
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "sheets.json").write_text(json.dumps([[i for i, _ in s] for s in sheets], indent=0), encoding="utf-8")
    args = sys.argv[1:]
    part = None
    if args and "/" in args[0]:
        a, b = args.pop(0).split("/"); part = (int(a), int(b))   # «0/2»: this process takes every second sheet
    want = {int(a) for a in args}
    for n, items in enumerate(sheets):
        dst = OUT / f"sheet_{n:02d}.png"
        if part and n % part[1] != part[0]:
            continue
        if (want and n not in want) or (not want and dst.exists()):
            continue
        refs = []
        for pet in sorted({PET[x] for x, _ in items}):
            f = OUT / f"ref_{pet}.png"
            if not f.exists():
                from PIL import Image
                Image.open(REF / f"pet_{pet}_2.webp").convert("RGBA").save(f)
            refs += ["-i", str(f)]
        r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only"] + refs + ["-"],
                           input=prompt(items), text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
        p = image_of(r.stdout + r.stderr)
        if p is None:
            print("FAIL", n, r.stdout[-300:], flush=True)
            continue
        shutil.copy(p, dst)
        print("ok", n, ", ".join(i for i, _ in items), flush=True)


if __name__ == "__main__":
    main()
