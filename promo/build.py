#!/usr/bin/env python3
"""Assemble the Power of Mind 60s vertical Short (1080x1920, HD).

VO-duration-aware: each scene is auto-stretched to fit its narration line, so
swapping in a different voice (e.g. premium ElevenLabs) can never make a line
overrun its scene or bleed into the next. VO files live in promo/vo/ as
line_1..line_8 (.wav preferred, .mp3/.m4a accepted).
"""
import os, subprocess, json

HERE=os.path.dirname(os.path.abspath(__file__))
REPO=os.environ.get("GITHUB_WORKSPACE") or os.path.abspath(os.path.join(HERE,".."))
SS=os.path.join(REPO,"play-store/screenshots")
FR=os.path.join(HERE,"frames")
VO=os.path.join(HERE,"vo")
os.makedirs(os.path.join(HERE,"scenes"),exist_ok=True)
FPS=30; T=0.6            # xfade duration
LEAD=0.45               # VO starts this long after its scene begins
TAIL=0.9                # min silence between end of VO and end of scene

def run(cmd):
    p=subprocess.run(cmd,capture_output=True,text=True)
    if p.returncode!=0:
        print("CMD FAILED:"," ".join(cmd[:6]),"...")
        print(p.stderr[-1500:]); raise SystemExit(1)
    return p

def dur(f):
    return float(subprocess.check_output(["ffprobe","-v","error","-show_entries",
        "format=duration","-of","csv=p=0",f]).strip())

def vo_file(i):
    for ext in ("wav","mp3","m4a"):
        p=os.path.join(VO,f"line_{i+1}.{ext}")
        if os.path.exists(p): return p
    raise SystemExit(f"missing VO for line {i+1} in {VO}")

# scene: (image, base_duration, motion 'in'/'out').  8 scenes <-> 8 VO lines.
scenes=[
 (f"{FR}/scene1.png",          5.8,"in"),
 (f"{FR}/scene2.png",          7.2,"out"),
 (f"{SS}/play_1_quiz.png",     8.4,"in"),
 (f"{SS}/play_3_dashboard.png",7.0,"out"),
 (f"{SS}/play_5_player.png",   8.0,"in"),
 (f"{FR}/lib_nolock.png",      7.8,"out"),
 (f"{FR}/scene7.png",          8.8,"in"),
 (f"{FR}/scene8.png",         10.2,"in"),
]

# ---------- Measure VO, size each scene to fit its line ----------
vo_paths=[vo_file(i) for i in range(len(scenes))]
vo_dur=[dur(p) for p in vo_paths]
sdur=[max(base, vo_dur[i]+LEAD+TAIL) for i,(img,base,mo) in enumerate(scenes)]
for i,(img,base,mo) in enumerate(scenes):
    print(f"scene {i+1}: base={base:.1f}s vo={vo_dur[i]:.2f}s -> {sdur[i]:.2f}s")

# ---------- Pass A: per-scene silent clips with Ken Burns ----------
clip_paths=[]
for i,(img,base,mo) in enumerate(scenes):
    out=f"{HERE}/scenes/clip_{i+1}.mp4"; clip_paths.append(out)
    d=sdur[i]; n=int(round(d*FPS))
    if mo=="in":
        z="min(1.001+0.00058*on,1.14)"
    else:
        z="if(lte(on,1),1.14,max(1.14-0.00058*on,1.001))"
    vf=(f"scale=2160:3840:flags=lanczos,"
        f"zoompan=z='{z}':d={n}:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':"
        f"s=1080x1920:fps={FPS},setsar=1,format=yuv420p")
    run(["ffmpeg","-nostdin","-y","-loglevel","error","-loop","1","-i",img,
         "-t",f"{d:.3f}","-vf",vf,"-r",str(FPS),
         "-c:v","libx264","-crf","18","-preset","medium","-pix_fmt","yuv420p",out])
    print(f"clip {i+1}: {dur(out):.2f}s  ({mo})")

# ---------- Pass B: xfade chain ----------
inputs=[]
for c in clip_paths: inputs+=["-i",c]
fc=[]; prev="[0:v]"; offset=0.0
starts=[0.0]
for i in range(1,len(clip_paths)):
    offset+=sdur[i-1]-T
    starts.append(offset)
    lbl=f"[x{i}]"
    fc.append(f"{prev}[{i}:v]xfade=transition=fade:duration={T}:offset={offset:.3f}{lbl}")
    prev=lbl
filter_v=";".join(fc)
video_only=f"{HERE}/video_only.mp4"
run(["ffmpeg","-nostdin","-y","-loglevel","error",*inputs,
     "-filter_complex",filter_v,"-map",prev,
     "-c:v","libx264","-crf","18","-preset","medium","-pix_fmt","yuv420p",
     "-r",str(FPS),video_only])
TOTAL=dur(video_only)
print(f"video_only: {TOTAL:.2f}s")

# VO placement: each line starts LEAD after its scene's visible start.
vo_at_ms=[int(round((starts[i]+LEAD)*1000)) for i in range(len(scenes))]

# ---------- Pass C: music bed (copyright-safe synth pad, A-minor) ----------
music=f"{HERE}/music.wav"
run(["ffmpeg","-nostdin","-y","-loglevel","error",
     "-f","lavfi","-i",f"sine=frequency=110:duration={TOTAL:.2f}",     # A2
     "-f","lavfi","-i",f"sine=frequency=164.81:duration={TOTAL:.2f}",  # E3
     "-f","lavfi","-i",f"sine=frequency=220:duration={TOTAL:.2f}",     # A3
     "-f","lavfi","-i",f"sine=frequency=329.63:duration={TOTAL:.2f}",  # E4 shimmer
     "-filter_complex",
     "[0]volume=0.9[a];[1]volume=0.5[b];[2]volume=0.35[c];[3]volume=0.12[d];"
     "[a][b][c][d]amix=inputs=4:normalize=0,"
     "tremolo=f=0.12:d=0.5,aecho=0.8:0.9:900:0.35,lowpass=f=1100,"
     f"afade=t=in:st=0:d=2.5,afade=t=out:st={TOTAL-2.5:.2f}:d=2.5,"
     "volume=0.22[m]","-map","[m]",music])

# ---------- Pass D: audio timeline (music + placed VO) ----------
n=len(scenes)
aud_inputs=["-i",music]
for i in range(n): aud_inputs+=["-i",vo_paths[i]]
parts=[]
for i in range(n):
    ms=vo_at_ms[i]
    parts.append(f"[{i+1}]aresample=44100,aformat=channel_layouts=stereo,"
                 f"adelay={ms}|{ms},volume=1.0[v{i}]")
mixin="".join(f"[v{i}]" for i in range(n))
parts.append(f"[0]aformat=channel_layouts=stereo[m];"
             f"[m]{mixin}amix=inputs={n+1}:normalize=0:dropout_transition=0,"
             f"alimiter=limit=0.97,aresample=44100[a]")
audio=f"{HERE}/audio.m4a"
run(["ffmpeg","-nostdin","-y","-loglevel","error",*aud_inputs,
     "-filter_complex",";".join(parts),"-map","[a]",
     "-c:a","aac","-b:a","192k","-t",f"{TOTAL:.2f}",audio])

# ---------- Pass E: mux ----------
final=f"{HERE}/PowerOfMind_Short.mp4"
run(["ffmpeg","-nostdin","-y","-loglevel","error","-i",video_only,"-i",audio,
     "-map","0:v","-map","1:a","-c:v","copy","-c:a","aac","-b:a","192k",
     "-movflags","+faststart","-t",f"{TOTAL:.2f}",final])

info=json.loads(subprocess.check_output(["ffprobe","-v","error","-show_format",
    "-show_streams","-of","json",final]))
v=[s for s in info["streams"] if s["codec_type"]=="video"][0]
sz=os.path.getsize(final)/1e6
print(f"\nFINAL: {final}")
print(f"  {v['width']}x{v['height']}  {v.get('r_frame_rate')}  "
      f"{float(info['format']['duration']):.2f}s  {sz:.1f} MB  codec={v['codec_name']}")
