"""Generates pet sprite sheets and gym art with the Codex CLI image tool.

Codex saves images into ~/.codex/generated_images/<session>/; after each run the newest
PNG made since the run started is copied to art/sheets/<id>.png. Already present sheets are skipped.

    python tools/gen_art.py            # everything missing
    python tools/gen_art.py otter crab # only these
"""
import shutil
import subprocess
import sys
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "art" / "sheets"
GEN = Path.home() / ".codex" / "generated_images"

STYLE = (
    "Style: detailed 16-bit pixel art like a modern cozy indie game, crisp pixels, clean dark outline, "
    "3-4 tone shading ramps, warm and cute, big shiny eyes."
)
GRID = (
    "Layout: 3 columns x 2 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. "
    "One creature per cell, centered, with clear margin; nothing touches the cell borders. No text, no grid lines, "
    "no shadows on the background, no other objects except those listed."
)

PETS = {
    "otter": ("a sleek little river otter with blue swim goggles pushed up on its head", "swimming front crawl with a small splash of blue water"),
    "lynx": ("a grey-beige lynx kitten with black ear tufts and a lilac yoga headband", "holding a deep yoga stretch (downward dog) on a small purple mat"),
    "octopus": ("a soft purple octopus with a yellow sweatband", "doing a pull-up on a small horizontal bar, holding it with two tentacles"),
    "crab": ("a round red crab with a white sweatband", "pressing a small dumbbell up in each claw"),
    "hedgehog": ("a hedgehog with a green bandana", "holding a forearm plank, very focused"),
    "kangaroo": ("a young kangaroo with red boxing gloves", "throwing a punch with a red boxing glove, dynamic"),
    "hummingbird": ("an emerald hummingbird with a pink headband", "skipping a jump rope mid-air"),
    "turtle": ("a green sea turtle with a teal shell and an orange headband", "jogging slowly, holding a small water bottle"),
    "sloth": ("a fluffy sloth with a towel around its neck", "hanging relaxed from a short bar with one arm, smiling"),
    "squirrel": ("a red squirrel with a big fluffy tail and a blue headband", "swinging a small black kettlebell between its legs"),
    "gorilla": ("a young silver-grey gorilla with a gold headband", "deadlifting a heavy barbell from the floor"),
    "ant": ("a red ant with tiny sneakers", "lifting a huge green leaf high over its head"),
    "crane": ("a red-crowned crane with a white and black plumage and a red crown", "standing on one leg in a yoga tree pose, wings in a graceful arc"),
    "mole": ("a velvety dark mole with pink paws and a tiny headlamp", "doing push-ups"),
    "owl": ("a round brown owl with big amber eyes and a purple sweatband", "doing biceps curls with two tiny dumbbells"),
    "hamster": ("a golden hamster with a white belly and a tiny red wristband", "running inside a small wooden hamster wheel"),
    "phoenix": ("a small phoenix chick with orange-gold fiery feathers and a flame crest", "rising with wings spread wide, small flames around it"),
    "mammoth": ("a woolly mammoth calf with reddish-brown fur and small curved tusks", "squatting with a barbell across its shoulders"),
    "dragon": ("a small teal dragon with cream belly, tiny wings and little horns", "pressing a kettlebell overhead, a puff of flame from its nose"),
    # the pets of the places (#21)
    "cat": ("a fluffy ginger house cat with a pink headband and white paws", "stretching in a cat-cow pose on a small round rug, a cushion beside it"),
    "fox": ("a red fox with a green running cap and white sneakers", "jogging on a park trail, a tiny water bottle in one paw"),
    "penguin": ("a round penguin with a red headband and a whistle on a cord", "leading an aerobics class, clapping its flippers above its head"),
}


def pet_prompt(desc, signature):
    return f"""Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet for a cozy fitness app pet: {desc}.
{STYLE}
{GRID}
Cells, left to right, top to bottom:
1) baby: tiny, round dumpling-shaped body, stubby limbs, very cute
2) teen: bigger, slightly more athletic
3) adult: strong and fit but still cute, standing proud
4) adult {signature}
5) adult cheering, jumping with joy, arms (or wings, or paws) up in triumph
6) adult asleep, curled up, peaceful face
The same character and palette in every cell, facing three-quarters right."""


OTHER = {
    "eggs": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 20 pet eggs for a cozy fitness app.
""" + STYLE + """
Layout: 5 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines.
Each egg is the same size and egg-shaped, but with its own pattern and colours that hint at the creature inside, in this order (left to right, top to bottom):
1 brown with cream spots and two tiny horn bumps (calf), 2 sleek brown with blue wave stripes (otter), 3 grey-beige with dark lynx spots (lynx), 4 purple with suction-cup dots (octopus), 5 red with white crab-shell bands (crab),
6 brown with spiky top (hedgehog), 7 sandy tan with a red glove stripe (kangaroo), 8 emerald iridescent small egg (hummingbird), 9 teal with hexagon shell pattern (turtle), 10 mossy green-grey with leaves (sloth),
11 rusty orange with a fluffy tail curl (squirrel), 12 silver-grey with a gold band (gorilla), 13 small red glossy (ant), 14 white with a red cap and black band (crane), 15 dark velvet with pink speckles (mole),
16 brown with feather scallops and amber spots (owl), 17 golden with white belly patch (hamster), 18 orange-gold with flame pattern, glowing (phoenix), 19 shaggy reddish-brown fur texture (mammoth), 20 teal with cream belly scales and tiny horns (dragon).""",
    "gym": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy fitness app: the inside of a small cozy basement home gym, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: warm brick walls, a small high window near the ceiling showing a strip of sky, a wooden floor with a big soft green rubber mat in the middle, warm lamp light.
IMPORTANT: keep the room mostly EMPTY: no gym machines, no people, no animals, no text, no posters. The centre of the floor and the walls on the left and right must be clear, free space (furniture will be added later).
Only small details: a radiator, a few pipes along the ceiling, a light switch, a small rug edge.""",
    "home": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy fitness app: a small cozy living room at home where someone works out, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: soft warm wallpaper, a window with light curtains, a wooden floor with a big round soft carpet in the middle, warm lamp light, the same warm cozy mood as a basement home gym.
IMPORTANT: keep the room mostly EMPTY: no people, no animals, no text. The centre of the floor must be clear, free space (characters will stand there).
Only small details near the edges: a sofa end on the far left, a floor lamp and a potted plant on the right, a small bookshelf, a pair of light dumbbells and a rolled yoga mat by the wall.""",
    "street": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy fitness app: an outdoor workout spot in a city park on a sunny day, seen from the front, side-on like a 2D game level.
""" + STYLE + """ Landscape 3:2.
The scene: blue sky with a few soft clouds at the top, green trees and bushes on the left and right edges, distant low town houses on the horizon, a sandy path and soft green grass in the front.
IMPORTANT: keep it mostly EMPTY: no people, no animals, no text. The centre of the ground must be clear, free space (characters will stand there).
Only small details near the edges: a simple pull-up bar on the far left, a park bench and a lamp post on the right, a water bottle by the bench.""",
    "group": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a background scene for a cozy fitness app: a small bright group fitness studio, seen from the front, side-on like a 2D game room.
""" + STYLE + """ Landscape 3:2.
The room: a long wall mirror on the back wall, pale wooden floor, soft daylight from tall windows, warm and friendly, the same cozy mood as a basement home gym.
IMPORTANT: keep the room mostly EMPTY: no people, no animals, no text. The centre of the floor must be clear, free space (characters will stand there).
Only small details near the edges: a stack of colourful steps and rolled mats on the left, a speaker and a small whiteboard (no writing) on the right, a row of small dumbbells on a low rack.""",
    "eggs_place": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 3 pet eggs for a cozy fitness app, in the same set as other patterned pet eggs.
""" + STYLE + """
Layout: 3 columns x 1 row of equal square cells on a flat solid pure magenta (#FF00FF) background. One egg per cell, centered, with clear margin; no text, no grid lines, no nests.
Each egg is the same size and egg-shaped, with its own pattern and colours that hint at the creature inside, in this order (left to right):
1 ginger with cream tabby stripes and a pink dot (cat), 2 russet red with a white tip and a green band (fox), 3 black and white with a red band (penguin).""",
    "items": """Use your image generation tool to create ONE image. Do nothing else (no file copying).

Image: a sprite sheet of 16 home-gym objects for a cozy fitness app, each a separate object, seen from the front (side-on like a 2D game room).
""" + STYLE + """
Layout: 4 columns x 4 rows of equal square cells on a flat solid pure magenta (#FF00FF) background. One object per cell, centered, with clear margin; no text, no grid lines, no shadows on the background.
Objects in order (left to right, top to bottom):
1 a squat rack with a barbell, 2 a treadmill, 3 a rowing machine, 4 a red punching bag hanging from a chain,
5 a potted monstera plant, 6 a framed motivational poster with a mountain (no text), 7 a retro boombox, 8 a string of warm fairy lights (horizontal),
9 a wooden shelf with three small gold trophies, 10 a tall wall mirror, 11 a rack of colorful kettlebells, 12 a rolled yoga mat and foam roller,
13 a water cooler, 14 a round standing fan, 15 a pink neon heart sign (no letters), 16 a flat weight bench with a pair of dumbbells.""",
}


def image_of(output):
    """The image made in the Codex session named in its output (other sessions may draw at the same time)."""
    m = re.search(r"session id:\s*([0-9a-f-]+)", output)
    if not m:
        return None
    pngs = sorted((GEN / m.group(1)).glob("*.png"), key=lambda p: p.stat().st_mtime)
    return pngs[-1] if pngs else None


def run(name, prompt):
    dst = OUT / f"{name}.png"
    if dst.exists():
        print("skip", name)
        return
    r = subprocess.run([shutil.which("codex.cmd") or "codex", "exec", "--skip-git-repo-check", "--sandbox", "read-only", "-"],
                       input=prompt, text=True, encoding="utf-8", capture_output=True, cwd=str(ROOT))
    p = image_of(r.stdout + r.stderr)
    if p is None:
        print("FAIL", name, r.stdout[-400:], r.stderr[-400:], flush=True)
        return
    shutil.copy(p, dst)
    print("ok", name, p, flush=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    want = sys.argv[1:]
    jobs = [(k, pet_prompt(*v)) for k, v in PETS.items()] + list(OTHER.items())
    for name, prompt in jobs:
        if want and name not in want:
            continue
        run(name, prompt)


if __name__ == "__main__":
    main()
