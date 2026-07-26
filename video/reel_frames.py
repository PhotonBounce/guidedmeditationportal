# Generates viral vertical (1080x1920) scene frames for the Guided Meditation Portal reel.
# Self-contained: reads assets from the repo. Run from repo root.
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageChops
import random, os, zipfile

ROOT = os.getcwd()
MK   = os.path.join(ROOT, "marketing")
OUT  = os.path.join(ROOT, "video", "frames")
os.makedirs(OUT, exist_ok=True)
W, H = 1080, 1920
rng = random.Random(11)

FB = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
def fnt(p, s): return ImageFont.truetype(p, s)

# extract logo from the committed site zip
LOGO = os.path.join(OUT, "logo.png")
if not os.path.exists(LOGO):
    with zipfile.ZipFile(os.path.join(ROOT, "meditation-portal-site.zip")) as z:
        with z.open("logo.png") as s, open(LOGO, "wb") as d:
            d.write(s.read())

PINK=(233,30,140); PINKL=(255,140,210); WHITE=(255,255,255); MUTE=(200,175,215); GOLD=(255,209,102)

def bg():
    im = Image.new("RGB",(W,H))
    px = im.load()
    top=(28,8,44); mid=(18,6,26); bot=(38,10,48)
    for y in range(H):
        t=y/H
        if t<0.5: u=t/0.5; c=tuple(int(top[i]+(mid[i]-top[i])*u) for i in range(3))
        else: u=(t-0.5)/0.5; c=tuple(int(mid[i]+(bot[i]-mid[i])*u) for i in range(3))
        for x in range(0,W): px[x,y]=c
    glow=Image.new("RGB",(W,H),(0,0,0)); gd=ImageDraw.Draw(glow)
    cx,cy,R=W//2,int(H*0.4),640
    for r in range(R,0,-8):
        a=int(52*(1-r/R)); gd.ellipse([cx-r,cy-r,cx+r,cy+r],fill=(a*230//255,a*40//255,a*120//255))
    glow=glow.filter(ImageFilter.GaussianBlur(45)); im=ImageChops.add(im,glow)
    d=ImageDraw.Draw(im)
    for _ in range(int(W*H/4800)):
        x,y=rng.randint(0,W),rng.randint(0,H); r=rng.choice([1,1,1,2]); a=rng.randint(90,240)
        d.ellipse([x-r,y-r,x+r,y+r],fill=(255,240,250,a))
    return im.convert("RGBA")

def ctext(d, cy, text, font, fill, spacing=12, glowc=None, glow_off=3):
    lines=text.split("\n"); hs=[]
    for ln in lines:
        b=d.textbbox((0,0),ln,font=font); hs.append(b[3]-b[1])
    total=sum(hs)+spacing*(len(lines)-1); y=cy-total//2
    for i,ln in enumerate(lines):
        b=d.textbbox((0,0),ln,font=font); tw=b[2]-b[0]; x=(W-tw)//2-b[0]; yy=y-b[1]
        if glowc:
            for ox,oy in [(-glow_off,0),(glow_off,0),(0,-glow_off),(0,glow_off)]:
                d.text((x+ox,yy+oy),ln,font=font,fill=glowc)
        d.text((x,yy),ln,font=font,fill=fill)
        y+=hs[i]+spacing

def phone(shot, tw, top):
    im=Image.open(os.path.join(MK,shot)).convert("RGBA")
    w,h=im.size; nh=int(h*tw/w); im=im.resize((tw,nh),Image.LANCZOS)
    return im, nh

def save(im, name): im.convert("RGB").save(os.path.join(OUT,name)); print("wrote",name)

def logo_on(s, cx, cy, sz):
    lg=Image.new("RGBA",(W,H),(0,0,0,0)); ld=ImageDraw.Draw(lg)
    ld.ellipse([cx-sz//2-40, cy-sz//2-40, cx+sz//2+40, cy+sz//2+40], fill=(233,30,140,110))
    lg=lg.filter(ImageFilter.GaussianBlur(55)); s.alpha_composite(lg)
    lo=Image.open(LOGO).convert("RGBA").resize((sz,sz),Image.LANCZOS)
    s.alpha_composite(lo,(cx-sz//2, cy-sz//2))

# 1 HOOK
s=bg(); d=ImageDraw.Draw(s)
ctext(d, 760, "STOP scrolling.", fnt(FB,92), WHITE, glowc=(120,0,60))
ctext(d, 960, "Your mind needs\na reset.", fnt(FB,78), PINKL, glowc=(80,0,40))
ctext(d, 1220, "— 15 seconds —", fnt(FR,40), MUTE)
save(s,"f1.png")

# 2 BRAND
s=bg(); d=ImageDraw.Draw(s)
logo_on(s, W//2, 720, 320); d=ImageDraw.Draw(s)
ctext(d, 1040, "Guided\nMeditation Portal", fnt(FB,84), WHITE, spacing=14)
ctext(d, 1260, "calm, not clutter", fnt(FR,42), PINKL)
save(s,"f2.png")

# 3 LIBRARY
def shot_scene(shot,title,sub,name,tw=770):
    s=bg(); d=ImageDraw.Draw(s); im,nh=phone(shot,tw,300)
    gl=Image.new("RGBA",(W,H),(0,0,0,0)); gd=ImageDraw.Draw(gl); gx=(W-tw)//2
    gd.rounded_rectangle([gx-34,270-34,gx+tw+34,270+nh+34],radius=72,fill=(233,30,140,85))
    gl=gl.filter(ImageFilter.GaussianBlur(50)); s.alpha_composite(gl)
    s.alpha_composite(im,((W-tw)//2,270)); d=ImageDraw.Draw(s)
    ctext(d,1560,title,fnt(FB,70),WHITE,glowc=(90,0,45))
    ctext(d,1690,sub,fnt(FR,40),PINKL)
    save(s,name)
shot_scene("store_01.png","55+ tracks","calm · focus · sleep · energy","f3.png")
shot_scene("store_02.png","Meet Spirit","your private AI guide — 100% on-device","f4.png")
shot_scene("store_03.png","Wake up gently","meditation alarm · never jarring","f5.png")

# 6 STREAK PUNCH
s=bg(); d=ImageDraw.Draw(s)
ctext(d, 820, "Build a streak.", fnt(FB,86), WHITE, glowc=(90,0,45))
ctext(d, 970, "Feel the change.", fnt(FB,86), PINKL, glowc=(90,0,45))
ctext(d, 1180, "🔥 breathing coach · journeys · progress", fnt(FR,36), MUTE)
save(s,"f6.png")

# 7 PRICE
s=bg(); d=ImageDraw.Draw(s)
ctext(d, 720, "No subscriptions.", fnt(FB,80), WHITE)
ctext(d, 850, "EVER.", fnt(FB,120), GOLD, glowc=(120,80,0))
ctext(d, 1050, "One $2 unlock.", fnt(FR,60), PINKL)
ctext(d, 1150, "Everything. Forever.", fnt(FR,52), WHITE)
save(s,"f7.png")

# 8 CTA
s=bg(); logo_on(s, W//2, 760, 280); d=ImageDraw.Draw(s)
ctext(d, 1040, "Download now", fnt(FB,80), WHITE, glowc=(90,0,45))
ctext(d, 1170, "Guided Meditation Portal", fnt(FR,46), PINKL)
ctext(d, 1290, "photon-bounce.com/guidedmeditation", fnt(FR,36), MUTE)
save(s,"f8.png")
print("done")
