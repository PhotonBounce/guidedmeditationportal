"""
Generate 7" and 10" tablet store graphics from the raw emulator screenshots.

Output:
  marketing/tablet7_XX.png   (1200x1920 — Play Store 7-inch tablet)
  marketing/tablet10_XX.png  (1600x2560 — Play Store 10-inch tablet)

Same cosmic style as the phone frames: dark gradient + stars + pink-ringed
bezel + screenshot + caption.
"""
from PIL import Image, ImageDraw, ImageFont
import os

SRC = "store_screenshots"
OUT = "marketing"
os.makedirs(OUT, exist_ok=True)

SHOTS = [
    ("01_home.png",      "Calm your mind\nanytime, anywhere"),
    ("02_breathing.png", "Breathing coach\nBox · 4-4-4-4"),
    ("03_spirit.png",    "Spirit — your\nmeditation companion"),
]

SIZES = [
    ("tablet7",  1200, 1920),
    ("tablet10", 1600, 2560),
]


def star_field(draw, w, h, seed=42):
    import random; rng = random.Random(seed)
    for _ in range(int(w * h / 9000)):
        x = rng.randint(0, w); y = rng.randint(0, h)
        r = rng.choice([1, 1, 1, 2])
        a = rng.randint(100, 255)
        draw.ellipse([x-r, y-r, x+r, y+r], fill=(255, 255, 255, a))


def rounded_mask(size, radius):
    w, h = size
    mask = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle([0, 0, w-1, h-1], radius=radius, fill=255)
    return mask


def make_frame(shot_file, caption, cw, ch):
    bezel_w = int(cw * 0.78)
    bezel_h = int(ch * 0.82)
    bezel_x = (cw - bezel_w) // 2
    bezel_y = int(ch * 0.03)
    corner_r = int(cw * 0.07)
    pad = max(14, cw // 70)

    scr_x = bezel_x + pad
    scr_y = bezel_y + pad
    scr_w = bezel_w - pad * 2
    scr_h = bezel_h - pad * 2

    bg = Image.new("RGBA", (cw, ch), (8, 8, 20, 255))
    draw = ImageDraw.Draw(bg)
    for y in range(ch):
        t = y / ch
        draw.line([(0, y), (cw, y)],
                  fill=(int(8 + 4*t), int(8 + 2*t), int(20 + 10*t), 255))

    glow = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gmax = cw // 3
    for rad in range(gmax, 0, -10):
        alpha = int(18 * (1 - rad / gmax))
        gd.ellipse([cw//2-rad, ch//2-rad, cw//2+rad, ch//2+rad],
                   fill=(220, 40, 120, alpha))
    bg = Image.alpha_composite(bg, glow)

    draw = ImageDraw.Draw(bg)
    star_field(draw, cw, ch)

    bezel_img = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bezel_img)
    bd.rounded_rectangle(
        [bezel_x-2, bezel_y-2, bezel_x+bezel_w+1, bezel_y+bezel_h+1],
        radius=corner_r+2, fill=(220, 40, 120, 255))
    bd.rounded_rectangle(
        [bezel_x, bezel_y, bezel_x+bezel_w, bezel_y+bezel_h],
        radius=corner_r, fill=(16, 16, 28, 255))
    bg = Image.alpha_composite(bg, bezel_img)

    shot = Image.open(os.path.join(SRC, shot_file)).convert("RGBA")
    sw, sh = shot.size
    crop_top = 0
    for row in range(min(120, sh)):
        row_pixels = [shot.getpixel((x, row)) for x in range(0, sw, 20)]
        avg = sum(r+g+b for r, g, b, *_ in row_pixels) / len(row_pixels) / 3
        if avg < 15:
            crop_top = row + 1
        else:
            break
    if crop_top > 10:
        shot = shot.crop((0, crop_top, sw, sh))
    shot = shot.resize((scr_w, scr_h), Image.LANCZOS)
    mask = rounded_mask((scr_w, scr_h), corner_r - pad)
    shot.putalpha(mask)
    bg.paste(shot, (scr_x, scr_y), shot)

    draw = ImageDraw.Draw(bg)
    cap_y = bezel_y + bezel_h + int(ch * 0.018)
    big = int(cw * 0.048)
    sm = int(cw * 0.037)
    try:
        font_big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", big)
        font_sm  = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", sm)
    except Exception:
        font_big = font_sm = ImageFont.load_default()

    for i, line in enumerate(caption.split("\n")):
        font = font_big if i == 0 else font_sm
        color = (255, 255, 255, 255) if i == 0 else (200, 150, 220, 255)
        bbox = draw.textbbox((0, 0), line, font=font)
        tw = bbox[2] - bbox[0]
        draw.text(((cw - tw)//2, cap_y + i*int(big*1.25)), line, font=font, fill=color)

    try:
        font_tiny = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", int(cw*0.024))
    except Exception:
        font_tiny = ImageFont.load_default()
    label = "Guided Meditation Portal"
    bbox = draw.textbbox((0, 0), label, font=font_tiny)
    tw = bbox[2] - bbox[0]
    draw.text(((cw - tw)//2, ch - int(ch*0.022)), label, font=font_tiny, fill=(150, 130, 180, 200))

    return bg.convert("RGB")


for prefix, cw, ch in SIZES:
    for idx, (shot_file, caption) in enumerate(SHOTS, 1):
        out_path = os.path.join(OUT, f"{prefix}_{idx:02d}.png")
        img = make_frame(shot_file, caption, cw, ch)
        img.save(out_path, "PNG", optimize=True)
        print(f"Saved {out_path} ({os.path.getsize(out_path)//1024} KB)")

print("Done — 6 tablet store graphics in marketing/")
