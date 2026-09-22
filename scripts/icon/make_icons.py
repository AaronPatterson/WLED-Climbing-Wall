"""Generates every icon asset from the single set of coordinates in climber.py.

The launcher icon is a vector and the store icon is a PNG, so without a shared
source they would drift the first time anyone nudged a limb. Here the same
numbers and the same bbox-fit transform drive both.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import climber
from climber import (ARM_REACH, ARM_GRIP, LEG_LOW, LEG_HIGH,
                     SHOULDER, HIP, HEAD, HEAD_R)

# Heavier than the drawing defaults: at 48px the thinner figure went spidery.
climber.LIMB_W, climber.TORSO_W = 9.8, 12.0

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(REPO, "app/src/main/res")
WHITE = (255, 255, 255, 255)

# Adaptive icons guarantee only the central 66 of 108 survives masking. 0.66
# keeps the artwork inside that, and inside a circular mask with room to spare.
LAUNCHER_FILL = 0.66
# The store icon is never masked by a launcher, only lightly rounded by Play's
# own UI, so it can sit larger in the square.
STORE_FILL = 0.80

VIEWPORT = 108.0


def transform(fill):
    """Same bbox-fit the renderer uses, in viewport units rather than pixels."""
    x0, y0, x1, y1 = climber._bbox(holds=False)
    s = (VIEWPORT * fill) / max(x1 - x0, y1 - y0)
    ox = (VIEWPORT - (x1 - x0) * s) / 2 - x0 * s
    oy = (VIEWPORT - (y1 - y0) * s) / 2 - y0 * s
    return lambda p: (p[0] * s + ox, p[1] * s + oy), s


def polyline(points, T):
    pts = [T(p) for p in points]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts)


def circle(centre, r, T, s):
    cx, cy = T(centre)
    rr = r * s
    return (f"M{cx - rr:.2f},{cy:.2f}"
            f" a{rr:.2f},{rr:.2f} 0 1,0 {2 * rr:.2f},0"
            f" a{rr:.2f},{rr:.2f} 0 1,0 {-2 * rr:.2f},0 Z")


def vector_drawable():
    T, s = transform(LAUNCHER_FILL)
    limb_w, torso_w = climber.LIMB_W * s, climber.TORSO_W * s
    strokes = [(polyline([SHOULDER, HIP], T), torso_w)]
    strokes += [(polyline(p, T), limb_w) for p in (ARM_REACH, ARM_GRIP, LEG_LOW, LEG_HIGH)]

    paths = "\n".join(
        f'    <path\n'
        f'        android:strokeColor="#FF000000"\n'
        f'        android:strokeWidth="{w:.2f}"\n'
        f'        android:strokeLineCap="round"\n'
        f'        android:strokeLineJoin="round"\n'
        f'        android:pathData="{d}" />'
        for d, w in strokes)

    head = circle(HEAD, HEAD_R, T, s)
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!--
    Generated: the same coordinates produce the Play store PNG, so edit the
    generator rather than this file if the figure ever changes.

    Limbs are strokes with round caps rather than filled outlines, which keeps
    the path data short enough to read and means the weight can be adjusted by
    changing one number instead of redrawing an outline.
-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{paths}
    <path
        android:fillColor="#FF000000"
        android:pathData="{head}" />
</vector>
'''


def main():
    os.makedirs(f"{RES}/drawable", exist_ok=True)
    os.makedirs(f"{RES}/mipmap-anydpi-v26", exist_ok=True)

    with open(f"{RES}/drawable/ic_launcher_foreground.xml", "w", newline="\n") as f:
        f.write(vector_drawable())

    with open(f"{RES}/mipmap-anydpi-v26/ic_launcher.xml", "w", newline="\n") as f:
        f.write('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
''')

    # Raster fallbacks. minSdk 26 means every device supports adaptive icons,
    # but a plain mipmap is still what turns up in places that ask for a
    # bitmap, and it costs a few kilobytes.
    for folder, px in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                       ("xxhdpi", 144), ("xxxhdpi", 192)]:
        d = f"{RES}/mipmap-{folder}"
        os.makedirs(d, exist_ok=True)
        climber.draw(px, bg=WHITE, holds=False, fill=LAUNCHER_FILL).save(f"{d}/ic_launcher.png")

    out = os.path.join(REPO, "docs/play-store-icon-512.png")
    # Play rejects transparency in the store icon, so this is flattened onto
    # white rather than left with an alpha channel.
    climber.draw(512, bg=WHITE, holds=False, fill=STORE_FILL).convert("RGB").save(out)
    print("store icon:", out)
    print("done")


if __name__ == "__main__":
    main()
