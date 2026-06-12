"""
Wrap the 3 raw emulator screenshots in a clean store-graphic frame.

Output: marketing/store_XX.png  (1080x2160 — Play Store phone screenshot size)
Each image:  dark cosmic background + rounded phone bezel + screenshot + caption
"""
from PIL import Image, ImageDraw, ImageFont
import os, math, struct, zlib

SRC  = "store_screenshots"
OUT  = "marketing"
os.makedirs(OUT, exist_ok=True)

SHOTS = [
    ("01_home.png",      "Calm your mind\nanytime, anywhere"),
    ("02_breathing.png", "Breathing coach\nBox · 4-4-4-4"),
    ("03_spirit.png",    "Spirit — your\nmeditation companion"),
]

# Canvas size (Play Store phone screenshot)
CW, CH = 1080, 2160

# Phone bezel dimensions
BEZEL_W = 880
BEZEL_H = 1820
BEZEL_X = (CW - BEZEL_W) // 2
BEZEL_Y = 60
CORNER_R = 80

# Screen inset inside bezel
SCREEN_PAD = 18
SCR_X = BEZEL_X + SCREEN_PAD
SCR_Y = BEZEL_Y + SCREEN_PAD
SCR_W = BEZEL_W - SCREEN_PAD * 2
SCR_H = BEZEL_H - SCREEN_PAD * 2


def star_field(draw, w, h, seed=42):
    import random; rng = random.Random(seed)
    for _ in range(220):
        x = rng.randint(0, w); y = rng.randint(0, h)
        r = rng.choice([1, 1, 1, 2])
        a = rng.randint(100, 255)
        draw.ellipse([x-r, y-r, x+r, y+r], fill=(255,255,255,a))


def rounded_mask(size, radius):
    w, h = size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, w-1, h-1], radius=radius, fill=255)
    return mask


def make_frame(shot_file, caption):
    # Background gradient (dark navy → black)
    bg = Image.new("RGBA", (CW, CH), (8, 8, 20, 255))
    draw = ImageDraw.Draw(bg)
    # Subtle gradient
    for y in range(CH):
        t = y / CH
        r = int(8 + 4*t); g = int(8 + 2*t); b = int(20 + 10*t)
        draw.line([(0,y),(CW,y)], fill=(r,g,b,255))

    # Soft pink glow blob (matches app aesthetic)
    glow = Image.new("RGBA", (CW, CH), (0,0,0,0))
    gd = ImageDraw.Draw(glow)
    for rad in range(300, 0, -10):
        alpha = int(18 * (1 - rad/300))
        gd.ellipse([CW//2-rad, CH//2-rad, CW//2+rad, CH//2+rad],
                   fill=(220, 40, 120, alpha))
    bg = Image.alpha_composite(bg, glow)

    # Stars
    draw = ImageDraw.Draw(bg)
    star_field(draw, CW, CH)

    # Bezel (dark charcoal, pink border)
    bezel_img = Image.new("RGBA", (CW, CH), (0,0,0,0))
    bd = ImageDraw.Draw(bezel_img)
    # Pink ring (2px)
    bd.rounded_rectangle(
        [BEZEL_X-2, BEZEL_Y-2, BEZEL_X+BEZEL_W+1, BEZEL_Y+BEZEL_H+1],
        radius=CORNER_R+2, fill=(220,40,120,255))
    # Bezel body
    bd.rounded_rectangle(
        [BEZEL_X, BEZEL_Y, BEZEL_X+BEZEL_W, BEZEL_Y+BEZEL_H],
        radius=CORNER_R, fill=(16,16,28,255))

    bg = Image.alpha_composite(bg, bezel_img)

    # Screenshot inside bezel
    shot = Image.open(os.path.join(SRC, shot_file)).convert("RGBA")
    # Crop the top status bar area if it's a near-black strip (breathing screen)
    sw, sh = shot.size
    crop_top = 0
    for row in range(min(120, sh)):
        row_pixels = [shot.getpixel((x, row)) for x in range(0, sw, 20)]
        avg = sum(r+g+b for r,g,b,*_ in row_pixels) / len(row_pixels) / 3
        if avg < 15:
            crop_top = row + 1
        else:
            break
    if crop_top > 10:
        shot = shot.crop((0, crop_top, sw, sh))
    shot = shot.resize((SCR_W, SCR_H), Image.LANCZOS)
    mask = rounded_mask((SCR_W, SCR_H), CORNER_R - SCREEN_PAD)
    shot.putalpha(mask)
    bg.paste(shot, (SCR_X, SCR_Y), shot)

    # Caption text below phone
    draw = ImageDraw.Draw(bg)
    cap_y = BEZEL_Y + BEZEL_H + 30
    lines = caption.split("\n")
    try:
        font_big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 52)
        font_sm  = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 40)
    except Exception:
        font_big = font_sm = ImageFont.load_default()

    for i, line in enumerate(lines):
        font = font_big if i == 0 else font_sm
        color = (255, 255, 255, 255) if i == 0 else (200, 150, 220, 255)
        bbox = draw.textbbox((0, 0), line, font=font)
        tw = bbox[2] - bbox[0]
        draw.text(((CW - tw)//2, cap_y + i*64), line, font=font, fill=color)

    # App name tiny at very bottom
    try:
        font_tiny = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 28)
    except Exception:
        font_tiny = ImageFont.load_default()
    label = "Guided Meditation Portal"
    bbox = draw.textbbox((0,0), label, font=font_tiny)
    tw = bbox[2]-bbox[0]
    draw.text(((CW-tw)//2, CH-44), label, font=font_tiny, fill=(150,130,180,200))

    return bg.convert("RGB")


for idx, (shot_file, caption) in enumerate(SHOTS, 1):
    out_path = os.path.join(OUT, f"store_{idx:02d}.png")
    img = make_frame(shot_file, caption)
    img.save(out_path, "PNG", optimize=True)
    print(f"Saved {out_path} ({os.path.getsize(out_path)//1024} KB)")

print("Done — 3 framed store graphics in marketing/")
