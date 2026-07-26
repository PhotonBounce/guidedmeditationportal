#!/usr/bin/env python3
"""Generate the full-frame scene backgrounds and fix the library screenshot
(paint over the paywall lock icons) for the Power of Mind promo Short.

Runs headless on CI. Repo assets come from $GITHUB_WORKSPACE (or the repo root
two levels up from this file); generated frames land in promo/frames/.
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

W, H = 1080, 1920
HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.environ.get("GITHUB_WORKSPACE") or os.path.abspath(os.path.join(HERE, ".."))
os.makedirs(os.path.join(HERE, "frames"), exist_ok=True)

GOLD=(255,201,94); AMBER=(255,179,71); CREAM=(245,238,230)
MUTED=(176,158,134); EMBER=(232,116,43); INK=(12,8,5)

FS_SERIF_B="/usr/share/fonts/truetype/freefont/FreeSerifBold.ttf"
FS_SANS_B="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FS_SANS="/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
def font(p,s): return ImageFont.truetype(p,s)

def make_bg(seed=7, glow_y=0.42, glow=1.0):
    top=np.array([18,12,7]); mid=np.array([38,26,16]); bot=np.array([14,9,6])
    ys=np.linspace(0,1,H)[:,None]
    grad=np.where(ys<0.5,
        top+(mid-top)*(ys/0.5),
        mid+(bot-mid)*((ys-0.5)/0.5))
    img=np.repeat(grad[:,None,:],W,axis=1).astype(np.float32)
    # radial ember glow
    yy,xx=np.mgrid[0:H,0:W]
    cx,cy=W*0.5,H*glow_y
    r=np.sqrt(((xx-cx)/(W*0.75))**2+((yy-cy)/(H*0.42))**2)
    halo=np.clip(1-r,0,1)**2.2
    for i,c in enumerate((EMBER[0],EMBER[1],EMBER[2])):
        img[:,:,i]+=halo*glow*(c*0.16)
    # stars
    rng=np.random.default_rng(seed)
    for _ in range(150):
        sx=rng.integers(0,W); sy=rng.integers(0,int(H*0.9))
        b=rng.uniform(0.2,0.9); rad=rng.choice([1,1,1,2])
        img[max(0,sy-rad):sy+rad+1,max(0,sx-rad):sx+rad+1,:]+=np.array([245,238,230])*b*0.5
    img=np.clip(img,0,255).astype(np.uint8)
    return Image.fromarray(img,"RGB")

def text_w(d,s,f,ls=0):
    w=0
    for ch in s: w+=d.textlength(ch,font=f)+ls
    return w-ls if s else 0

def center_text(d,cy,s,f,fill,ls=0,glow=None):
    tw=text_w(d,s,f,ls); x=(W-tw)/2
    asc,desc=f.getmetrics(); y=cy-(asc+desc)/2
    if glow:
        gi=Image.new("RGBA",(W,H),(0,0,0,0)); gd=ImageDraw.Draw(gi)
        xx=x
        for ch in s:
            gd.text((xx,y),ch,font=f,fill=glow); xx+=d.textlength(ch,font=f)+ls
        gi=gi.filter(ImageFilter.GaussianBlur(14))
        d._image.paste(gi,(0,0),gi)
    xx=x
    for ch in s:
        d.text((xx,y),ch,font=f,fill=fill); xx+=d.textlength(ch,font=f)+ls
    return tw

def draw(img):
    d=ImageDraw.Draw(img,"RGBA"); d._image=img; return d

# ---------- Scene 1: INTRO ----------
def scene1():
    img=make_bg(3,glow_y=0.36); d=draw(img)
    icon=Image.open(os.path.join(REPO,"play-store/icon_512.png")).convert("RGBA").resize((360,360))
    # glow ring
    ring=Image.new("RGBA",(W,H),(0,0,0,0)); rd=ImageDraw.Draw(ring)
    rd.ellipse([W/2-230,560-230,W/2+230,560+230],fill=(232,116,43,90))
    ring=ring.filter(ImageFilter.GaussianBlur(60)); img.paste(ring,(0,0),ring)
    img.paste(icon,(int(W/2-180),int(560-180)),icon)
    center_text(d,900,"POWER OF MIND",font(FS_SERIF_B,88),GOLD,ls=6,glow=(232,116,43,120))
    center_text(d,1010,"Break the habit. For good.",font(FS_SANS,42),CREAM)
    # thin divider
    d.line([(W/2-90,1085),(W/2+90,1085)],fill=(255,201,94,180),width=3)
    img.convert("RGB").save(os.path.join(HERE,"frames/scene1.png"))

# ---------- Scene 2: HOOK ----------
def scene2():
    img=make_bg(11,glow_y=0.5); d=draw(img)
    center_text(d,640,"vaping   ·   scrolling",font(FS_SANS,46),MUTED,ls=1)
    center_text(d,715,"drinking   ·   whatever it is",font(FS_SANS,46),MUTED,ls=1)
    center_text(d,930,"TODAY",font(FS_SERIF_B,150),CREAM,ls=4,glow=(232,116,43,110))
    center_text(d,1090,"IT ENDS.",font(FS_SERIF_B,150),GOLD,ls=4,glow=(232,116,43,150))
    img.convert("RGB").save(os.path.join(HERE,"frames/scene2.png"))

# ---------- Scene 7: 100% FREE ----------
def scene7():
    img=make_bg(21,glow_y=0.42); d=draw(img)
    center_text(d,560,"100%",font(FS_SANS_B,230),GOLD,ls=2,glow=(232,116,43,150))
    center_text(d,760,"FREE",font(FS_SANS_B,230),CREAM,ls=12,glow=(232,116,43,120))
    center_text(d,960,"The complete app.",font(FS_SERIF_B,58),CREAM)
    checks=["No subscription","No locked features","No catch"]
    y=1090
    for c in checks:
        # gold check + text, centered as a group
        f=font(FS_SANS,44); tw=text_w(d,c,f)
        gx=(W-(tw+70))/2
        d.ellipse([gx,y-4,gx+48,y+44],outline=(255,201,94,255),width=4)
        d.line([(gx+13,y+22),(gx+22,y+32),(gx+37,y+8)],fill=GOLD,width=6)
        d.text((gx+70,y-2),c,font=f,fill=CREAM)
        y+=92
    center_text(d,1500,"Supported by ads — free for everyone.",font(FS_SANS,34),MUTED)
    img.convert("RGB").save(os.path.join(HERE,"frames/scene7.png"))

# ---------- Scene 8: OUTRO / CTA ----------
def scene8():
    img=make_bg(30,glow_y=0.34); d=draw(img)
    icon=Image.open(os.path.join(REPO,"play-store/icon_512.png")).convert("RGBA").resize((300,300))
    ring=Image.new("RGBA",(W,H),(0,0,0,0)); rd=ImageDraw.Draw(ring)
    rd.ellipse([W/2-200,520-200,W/2+200,520+200],fill=(232,116,43,80))
    ring=ring.filter(ImageFilter.GaussianBlur(55)); img.paste(ring,(0,0),ring)
    img.paste(icon,(int(W/2-150),int(520-150)),icon)
    center_text(d,780,"POWER OF MIND",font(FS_SERIF_B,74),GOLD,ls=5,glow=(232,116,43,120))
    center_text(d,880,"Take your first clean day today.",font(FS_SANS,40),CREAM)
    # Google Play pill
    pill_w,pill_h=620,132; px=(W-pill_w)/2; py=1030
    d.rounded_rectangle([px,py,px+pill_w,py+pill_h],radius=66,
                        fill=(255,201,94,255))
    # play triangle
    tx,ty=px+70,py+pill_h/2
    d.polygon([(tx-14,ty-26),(tx-14,ty+26),(tx+24,ty)],fill=(20,13,8))
    d.text((px+130,py+28),"GET IT FREE ON",font=font(FS_SANS,26),fill=(40,26,12))
    d.text((px+130,py+62),"Google Play",font=font(FS_SANS_B,52),fill=(20,13,8))
    center_text(d,1260,"photon-bounce.com",font(FS_SANS,38),MUTED)
    img.convert("RGB").save(os.path.join(HERE,"frames/scene8.png"))

# ---------- Library: paint out the paywall locks ----------
def lib_nolock():
    src=Image.open(os.path.join(REPO,"play-store/screenshots/play_4_library.png")).convert("RGB")
    d=ImageDraw.Draw(src)
    # lock centers (x~747) for rows 2..6
    locks=[(747,861),(747,1010),(747,1160),(747,1312),(747,1468)]
    for (lx,ly) in locks:
        # Sample a BLANK bit of card just right of the lock (no text there),
        # stretch that vertical strip across the lock box to preserve the card's
        # subtle vertical shading, then draw a gold play triangle like row 1.
        strip=src.crop((lx+34, ly-50, lx+50, ly+50))       # 16x100 blank card
        strip=strip.resize((104, 100))
        src.paste(strip, (lx-52, ly-50))
        d.polygon([(lx-15,ly-21),(lx-15,ly+21),(lx+21,ly)],fill=(255,201,94))
    src.save(os.path.join(HERE,"frames/lib_nolock.png"))

if __name__ == "__main__":
    scene1(); scene2(); scene7(); scene8(); lib_nolock()
    print("frames written:", os.listdir(os.path.join(HERE,"frames")))
