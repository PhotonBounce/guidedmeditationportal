#!/usr/bin/env python3
"""Generate Android mipmap icons at all required densities from a source PNG."""
from PIL import Image
import os, shutil

SOURCE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "appicon.png")
RES_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app", "src", "main", "res")

# (density-dir, size)
SIZES = [
    ("mipmap-mdpi",    48),
    ("mipmap-hdpi",    72),
    ("mipmap-xhdpi",   96),
    ("mipmap-xxhdpi",  144),
    ("mipmap-xxxhdpi", 192),
]

# Adaptive icon foreground/background need 108dp at each density (432 for xxxhdpi)
# For simplicity we write a square PNG at 432×432 for the adaptive foreground
ADAPTIVE_SIZE = 432  # xxxhdpi equivalent for adaptive layer

img = Image.open(SOURCE).convert("RGBA")
print(f"Source: {img.size[0]}x{img.size[1]} {img.mode}")

for density_dir, size in SIZES:
    out_dir = os.path.join(RES_DIR, density_dir)
    os.makedirs(out_dir, exist_ok=True)
    resized = img.resize((size, size), Image.LANCZOS)
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        out_path = os.path.join(out_dir, name)
        resized.save(out_path, "PNG")
        print(f"  Written: {out_dir}\\{name}  ({size}x{size})")

# Also write 512x512 for Play Store in appicon/
play_dir = os.path.join(os.path.dirname(RES_DIR.rstrip(os.sep).rstrip(os.sep.replace('\\','/'))), "appicon")
os.makedirs(play_dir, exist_ok=True)
img.resize((512, 512), Image.LANCZOS).save(os.path.join(play_dir, "play_store_icon_512.png"), "PNG")
print(f"  Written: {play_dir}\\play_store_icon_512.png  (512x512)")

# Write 432x432 foreground PNG for adaptive icon layer into drawable/
drawable_dir = os.path.join(RES_DIR, "drawable")
os.makedirs(drawable_dir, exist_ok=True)
img.resize((ADAPTIVE_SIZE, ADAPTIVE_SIZE), Image.LANCZOS).save(
    os.path.join(drawable_dir, "ic_launcher_foreground.png"), "PNG")
print(f"  Written: drawable/ic_launcher_foreground.png  ({ADAPTIVE_SIZE}x{ADAPTIVE_SIZE})")

print("\nDone.")
