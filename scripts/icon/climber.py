"""Draws the climber silhouette. Coordinates live in a 108x108 grid to match
Android's adaptive-icon viewport, so the same numbers drive the vector drawable."""
from PIL import Image, ImageDraw

# Adaptive icons are 108x108 with only the central 66x66 guaranteed visible
# after masking, so the figure has to sit well inside that.
GRID = 108.0

HEAD = (58, 27)
HEAD_R = 7.0
SHOULDER = (56, 40)
HIP = (48, 66)  # Offset from the shoulder so the torso leans rather than stands.

# Arms at deliberately different heights. Symmetric arms overhead read as
# celebrating; one long reach with the other hand lower reads as mid-move.
# Both elbows clear the head's silhouette, since that collision is what made
# earlier attempts merge into a blob.
ARM_REACH = [SHOULDER, (70, 28), (77, 15)]
ARM_GRIP = [SHOULDER, (38, 44), (32, 31)]
# The trailing leg hangs, the leading leg steps out and slightly up. An
# earlier version folded the knee right back and the result read as a box
# rather than a limb, so this angle stays open.
LEG_LOW = [HIP, (38, 78), (41, 91)]
LEG_HIGH = [HIP, (65, 73), (75, 63)]

LIMB_W = 8.5
TORSO_W = 10.5

# Holds sit beyond each hand and foot, larger than the limb so the grip reads
# as landing on something. Drawn first, so the figure overlaps them.
HOLDS = [(80, 12), (29, 27), (42, 94), (79, 60)]
HOLD_R = 7.0


def _elements(holds):
    """Everything to be drawn, as (points, half-width) pairs."""
    items = [([SHOULDER, HIP], TORSO_W / 2)]
    items += [(p, LIMB_W / 2) for p in (ARM_REACH, ARM_GRIP, LEG_LOW, LEG_HIGH)]
    items.append(([HEAD], HEAD_R))
    if holds:
        items += [([h], HOLD_R) for h in HOLDS]
    return items


def _bbox(holds):
    """Bounds including stroke width, so nothing is clipped by a hair."""
    xs, ys = [], []
    for points, r in _elements(holds):
        for x, y in points:
            xs += [x - r, x + r]
            ys += [y - r, y + r]
    return min(xs), min(ys), max(xs), max(ys)


def draw(size, fg=(0, 0, 0, 255), bg=None, ss=4, holds=False, fill=0.92):
    """Renders at `ss` times size and downsamples, since PIL has no round caps.

    `fill` is the fraction of the canvas the artwork occupies. Adaptive icons
    only guarantee the central 66 of 108 survives masking, so a launcher icon
    needs roughly 0.61 while a store icon can use almost the whole square.
    """
    px = int(size * ss)
    img = Image.new("RGBA", (px, px), bg if bg else (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Scale the artwork to `fill` of the canvas and centre it, rather than
    # trusting the hand-placed coordinates to sit correctly in the frame.
    x0, y0, x1, y1 = _bbox(holds)
    scale = (px * fill) / max(x1 - x0, y1 - y0)
    ox = (px - (x1 - x0) * scale) / 2 - x0 * scale
    oy = (px - (y1 - y0) * scale) / 2 - y0 * scale

    def T(p):
        return (p[0] * scale + ox, p[1] * scale + oy)

    def dot(p, r):
        x, y = T(p)
        rr = r * scale
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=fg)

    def limb(points, width):
        d.line([T(p) for p in points], fill=fg, width=int(width * scale), joint="curve")
        # Circles stand in for the round caps and joins PIL does not draw.
        for p in points:
            dot(p, width / 2)

    if holds:
        for h in HOLDS:
            dot(h, HOLD_R)

    limb([SHOULDER, HIP], TORSO_W)
    for part in (ARM_REACH, ARM_GRIP, LEG_LOW, LEG_HIGH):
        limb(part, LIMB_W)
    dot(HEAD, HEAD_R)

    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    import sys
    white = (255, 255, 255, 255)
    draw(512, bg=white).save(sys.argv[1])
    draw(512, bg=white, holds=True).save(sys.argv[2])
    print("wrote", sys.argv[1], "and", sys.argv[2])
