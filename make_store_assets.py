"""
Generate all Google Play store graphic assets from raw ADB screenshots.

Outputs
-------
screenshots/phone/     8 phone screenshots  @ 1080x1920 (9:16)
screenshots/tablet7/   8 tablet-7" shots    @ 1200x1920 (9:16, letterboxed)
screenshots/tablet10/  8 tablet-10" shots   @ 1600x2560 (9:16, letterboxed)
screenshots/feature_graphic.png             1024x500
"""

from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os, sys, textwrap

# ── paths ────────────────────────────────────────────────────────────────────
REPO       = os.path.dirname(os.path.abspath(__file__))
RAW_DIR    = os.path.join(REPO, "screenshots")
PHONE_DIR  = os.path.join(RAW_DIR, "phone")
T7_DIR     = os.path.join(RAW_DIR, "tablet7")
T10_DIR    = os.path.join(RAW_DIR, "tablet10")
ICON_PATH  = os.path.join(REPO, "appicon.png")
if not os.path.exists(ICON_PATH):
    ICON_PATH = os.path.join(REPO, "app", "src", "main", "res", "drawable", "appicon.png")

for d in [PHONE_DIR, T7_DIR, T10_DIR]:
    os.makedirs(d, exist_ok=True)

# ── best 8 phone shots (chosen for variety / no debug clutter) ────────────────
PICKS = [
    ("page_main_new.png",     "01_sounds"),
    ("page_mix_new.png",      "02_mix_studio"),
    ("page_aria_new.png",     "03_aria_guide"),
    ("page_settings_new.png", "04_settings"),
    ("page_about_new.png",    "05_about"),
]

PHONE_W,  PHONE_H  = 1080, 1920
T7_W,     T7_H     = 1200, 1920   # 9:16 – fits in 7-inch spec (320-3840 each side)
T10_W,    T10_H    = 1600, 2560   # stays within 3840 limit
FEAT_W,   FEAT_H   = 1024, 500

# brand colours matching the app
BG_DARK   = (11,  11,  30)
BG_MID    = (16,  17,  46)
ACCENT    = (167, 139, 250)   # iris purple
GOLD      = (245, 200,  66)
WHITE     = (248, 250, 252)

# ── helper: crop raw 1080×2408 → 1080×1920 (top-centred, skip status bar) ───
def crop_phone(path) -> Image.Image:
    img = Image.open(path).convert("RGB")
    w, h = img.size
    if w == PHONE_W and h == PHONE_H:
        return img
    # centre-crop vertically, keeping top-of-content
    top = max(0, (h - PHONE_H) // 3)   # slightly above centre = keeps header
    return img.crop((0, top, PHONE_W, top + PHONE_H))

# ── helper: fit phone screenshot into a larger canvas (tablet frame) ─────────
def make_tablet(phone_img: Image.Image, tw: int, th: int) -> Image.Image:
    canvas = Image.new("RGB", (tw, th), BG_DARK)
    # scale phone screenshot to fill height
    scale = th / PHONE_H
    new_w = int(PHONE_W * scale)
    scaled = phone_img.resize((new_w, th), Image.LANCZOS)
    x_off = (tw - new_w) // 2
    canvas.paste(scaled, (x_off, 0))
    # dark side bars
    return canvas

# ── process phone + tablet screenshots ───────────────────────────────────────
print("Processing screenshots...")
phone_imgs = []
for (src_name, dst_stem) in PICKS:
    src = os.path.join(RAW_DIR, src_name)
    if not os.path.exists(src):
        print(f"  SKIP (not found): {src_name}")
        continue
    phone = crop_phone(src)
    # phone
    out_p = os.path.join(PHONE_DIR, f"{dst_stem}.png")
    phone.save(out_p, "PNG", optimize=True)
    phone_imgs.append(phone)
    # tablet 7"
    t7 = make_tablet(phone, T7_W, T7_H)
    t7.save(os.path.join(T7_DIR, f"{dst_stem}.png"), "PNG", optimize=True)
    # tablet 10"
    t10 = make_tablet(phone, T10_W, T10_H)
    t10.save(os.path.join(T10_DIR, f"{dst_stem}.png"), "PNG", optimize=True)
    print(f"  OK  {dst_stem}")

# ── feature graphic 1024×500 ─────────────────────────────────────────────────
print("\nGenerating feature graphic...")

feat = Image.new("RGB", (FEAT_W, FEAT_H), BG_DARK)
draw = ImageDraw.Draw(feat)

# gradient background (simple vertical bands via horizontal lines)
for y in range(FEAT_H):
    t = y / FEAT_H
    r = int(BG_DARK[0] * (1-t) + BG_MID[0] * t)
    g = int(BG_DARK[1] * (1-t) + BG_MID[1] * t)
    b = int(BG_DARK[2] * (1-t) + BG_MID[2] * t + 20 * t)
    draw.line([(0, y), (FEAT_W, y)], fill=(r, g, b))

# subtle star dots
import random
random.seed(42)
for _ in range(120):
    x = random.randint(0, FEAT_W)
    y = random.randint(0, FEAT_H)
    size = random.choice([1, 1, 1, 2])
    alpha = random.randint(60, 180)
    c = (alpha, alpha, min(255, alpha + 40))
    draw.ellipse([x, y, x+size, y+size], fill=c)

# accent glow arc (top-right)
for r_val in range(180, 60, -5):
    alpha_val = int(30 * (180 - r_val) / 120)
    col = (ACCENT[0], ACCENT[1], ACCENT[2])
    draw.arc([FEAT_W - 280, -80, FEAT_W + 120, 320], 90, 270,
             fill=col, width=1)

# app icon (left side) — masked to a circle
icon_size = 180
icon_x, icon_y = 50, (FEAT_H - icon_size) // 2

if os.path.exists(ICON_PATH):
    icon = Image.open(ICON_PATH).convert("RGBA").resize((icon_size, icon_size), Image.LANCZOS)
    circle_mask = Image.new("L", (icon_size, icon_size), 0)
    ImageDraw.Draw(circle_mask).ellipse([0, 0, icon_size, icon_size], fill=255)
    feat.paste(icon, (icon_x, icon_y), circle_mask)
else:
    # fallback: purple circle with Z
    draw.ellipse([icon_x, icon_y, icon_x+icon_size, icon_y+icon_size], fill=ACCENT)
    draw.text((icon_x + icon_size//2, icon_y + icon_size//2), "Z",
              fill=WHITE, anchor="mm")

# text (right of icon)
text_x = icon_x + icon_size + 36
baseline = FEAT_H // 2

# try to load a font, fall back to default
def load_font(size):
    for path in [
        r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arial.ttf",
        r"C:\Windows\Fonts\calibri.ttf",
    ]:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except:
                pass
    return ImageFont.load_default()

def load_bold_font(size):
    for path in [
        r"C:\Windows\Fonts\segoeuib.ttf",
        r"C:\Windows\Fonts\arialbd.ttf",
        r"C:\Windows\Fonts\calibrib.ttf",
    ]:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except:
                pass
    return ImageFont.load_default()

font_title  = load_bold_font(34)
font_tag    = load_font(24)
font_sub    = load_font(17)

title_line1 = "Guided Meditation"
title_line2 = "Portal"
tag_text    = "Breathe · Reflect · Restore"
sub_line1   = "Narrated meditations &"
sub_line2   = "calming soundscapes"

# title (two lines, tight leading)
draw.text((text_x, baseline - 86), title_line1, font=font_title, fill=ACCENT)
draw.text((text_x, baseline - 48), title_line2, font=font_title, fill=ACCENT)

# gold underline accent under "Portal"
title_bbox = draw.textbbox((text_x, baseline - 48), title_line2, font=font_title)
title_w = title_bbox[2] - title_bbox[0]
draw.rectangle([text_x, baseline - 6, text_x + title_w, baseline - 2], fill=GOLD)

# tagline
draw.text((text_x, baseline + 14), tag_text, font=font_tag, fill=WHITE)

# subtitle (two short lines)
draw.text((text_x, baseline + 48), sub_line1, font=font_sub, fill=(180, 180, 220))
draw.text((text_x, baseline + 70), sub_line2, font=font_sub, fill=(180, 180, 220))

# phone preview (rightmost) — use first phone screenshot
if phone_imgs:
    preview_h = 340
    preview_w = int(PHONE_W * preview_h / PHONE_H)
    preview = phone_imgs[0].resize((preview_w, preview_h), Image.LANCZOS)
    px = FEAT_W - preview_w - 20
    py = (FEAT_H - preview_h) // 2
    feat.paste(preview, (px, py))
    # thin border
    draw.rectangle([px-1, py-1, px+preview_w, py+preview_h], outline=ACCENT, width=1)

feat_path = os.path.join(RAW_DIR, "feature_graphic.png")
feat.save(feat_path, "PNG", optimize=True)
print(f"  Feature graphic: {feat_path}  ({FEAT_W}x{FEAT_H})")

# == summary ==================================================================
print("\n== Asset summary ==========================================")
for label, folder, expected in [
    ("Phone 1080x1920 (9:16)", PHONE_DIR,  "1080x1920"),
    ("Tablet-7  1200x1920",    T7_DIR,    "1200x1920"),
    ("Tablet-10 1600x2560",    T10_DIR,   "1600x2560"),
]:
    files = sorted(os.listdir(folder))
    total_kb = sum(os.path.getsize(os.path.join(folder,f))//1024 for f in files)
    print(f"  {label}: {len(files)} files  ({total_kb} KB total)  -> {folder}")

feat_kb = os.path.getsize(feat_path) // 1024
print(f"  Feature graphic 1024x500: {feat_kb} KB  -> {feat_path}")
print("\nDone. Open screenshots\\ in Explorer to review.")
