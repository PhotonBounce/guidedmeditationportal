"""
Generate the Google Play feature graphic (1024x500) for Guided Meditation Portal.
Deep-pink cosmic theme + circular app logo + title/tagline. Pure Pillow.
"""
import os, math, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1024, 500
OUT_DIR = "marketing"
os.makedirs(OUT_DIR, exist_ok=True)
OUT = os.path.join(OUT_DIR, "feature_graphic.png")
ICON = os.path.join("app", "src", "main", "appicon", "play_store_icon_512.png")

PINK   = (233, 30, 140)
PINK_L = (255, 128, 200)
DARK   = (11, 11, 30)

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

# ---- background: diagonal gradient ----
bg = Image.new("RGB", (W, H), DARK)
px = bg.load()
c_tl = (26, 10, 34)   # deep plum top-left
c_br = (8, 8, 22)      # near-black bottom-right
for y in range(H):
    for x in range(0, W, 2):
        t = ((x / W) + (y / H)) / 2
        col = lerp(c_tl, c_br, t)
        px[x, y] = col
        if x + 1 < W:
            px[x + 1, y] = col

# ---- radial pink glow behind the logo (left) ----
glow = Image.new("RGB", (W, H), (0, 0, 0))
gd = ImageDraw.Draw(glow)
cx, cy = 250, 250
for r in range(300, 0, -4):
    a = int(70 * (1 - r / 300))
    gd.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(a * PINK[0] // 255, a * PINK[1] // 255, a * PINK[2] // 255))
glow = glow.filter(ImageFilter.GaussianBlur(40))
bg = Image.blend(bg, Image.composite(Image.new("RGB", (W, H), PINK), bg, glow.convert("L")), 0.0)
# additive blend of glow
bg = Image.eval(Image.merge("RGB", [
    Image.blend(bg.split()[i], glow.split()[i], 0.6) for i in range(3)
]), lambda v: v)

# ---- stars ----
rng = random.Random(7)
draw = ImageDraw.Draw(bg, "RGBA")
for _ in range(90):
    x, y = rng.randint(0, W), rng.randint(0, H)
    s = rng.choice([1, 1, 1, 2, 2, 3])
    a = rng.randint(60, 200)
    col = (255, 255, 255, a) if rng.random() > 0.4 else (255, 128, 200, a)
    draw.ellipse([x, y, x + s, y + s], fill=col)

# ---- circular logo with pink ring ----
D = 300
try:
    icon = Image.open(ICON).convert("RGBA").resize((D, D), Image.LANCZOS)
    mask = Image.new("L", (D, D), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, D, D], fill=255)
    lx, ly = cx - D // 2, cy - D // 2
    # soft shadow
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse([lx - 6, ly - 6, lx + D + 6, ly + D + 6], fill=(0, 0, 0, 120))
    shadow = shadow.filter(ImageFilter.GaussianBlur(12))
    bg.paste(shadow, (0, 0), shadow)
    bg.paste(icon, (lx, ly), mask)
    # pink ring
    ring = ImageDraw.Draw(bg, "RGBA")
    ring.ellipse([lx, ly, lx + D, ly + D], outline=PINK + (255,), width=6)
    ring.ellipse([lx - 5, ly - 5, lx + D + 5, ly + D + 5], outline=PINK_L + (120,), width=2)
except FileNotFoundError:
    pass

# ---- fonts ----
def font(path_opts, size):
    for p in path_opts:
        try:
            return ImageFont.truetype(p, size)
        except Exception:
            continue
    return ImageFont.load_default()

F = "C:/Windows/Fonts/"
title_f = font([F + "seguisb.ttf", F + "arialbd.ttf", F + "segoeui.ttf"], 70)
tag_f   = font([F + "segoeui.ttf", F + "arial.ttf"], 34)
sub_f   = font([F + "segoeui.ttf", F + "arial.ttf"], 25)

tx = 470  # text column start
d = ImageDraw.Draw(bg, "RGBA")

# Title (two lines, glow + white)
d.text((tx, 150), "Guided", font=title_f, fill=(255, 255, 255, 255))
d.text((tx, 225), "Meditation Portal", font=title_f, fill=(255, 255, 255, 255))

# Tagline
d.text((tx, 320), "Breathe  ·  Reflect  ·  Restore", font=tag_f, fill=PINK_L + (255,))

# Sub-line
d.text((tx, 372), "23 meditations · breathing coach · streaks · no subscriptions",
       font=sub_f, fill=(210, 180, 200, 255))

bg.convert("RGB").save(OUT, "PNG")
print("Wrote", OUT, bg.size)
