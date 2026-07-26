#!/usr/bin/env bash
# Build the Guided Meditation Portal viral reel with ElevenLabs narration.
# Requires: ffmpeg, python3+Pillow, curl, and $ELEVENLABS_API_KEY in the env.
# Run from the repo root. Output: video/GMP_reel_final.mp4
set -euo pipefail
cd "$(dirname "$0")/.."          # repo root
VID=video; F=$VID/frames
RAW=app/src/main/res/raw/work_focus_chillout.mp3
VOICE_ID="${ELEVENLABS_VOICE_ID:-XrExE9yKIg1WjnnlVkGX}"   # Matilda (warm). Override via env.
FPS=30

echo "== 1/5 frames =="
python3 $VID/reel_frames.py

echo "== 2/5 clips =="
DUR=(3.8 4.5 5.5 5.5 5.5 4.5 5.5 5.0)
ZOOM=(in out in out in out in out)
for i in 0 1 2 3 4 5 6 7; do
  n=$((i+1)); d=${DUR[$i]}; frames=$(python3 -c "print(int($d*$FPS))")
  if [ "${ZOOM[$i]}" = "in" ]; then z="zoom+0.0006"; else z="if(eq(on,1),1.14,zoom-0.0006)"; fi
  ffmpeg -y -loglevel error -loop 1 -i $F/f$n.png \
    -vf "scale=1080:1920,zoompan=z='$z':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=$frames:s=1080x1920:fps=$FPS,format=yuv420p" \
    -t $d -r $FPS $VID/c$n.mp4
done

echo "== 3/5 xfade =="
# offsets = cumulative duration - 0.4 (xfade dur), fade=0.4 for snappy cuts
ffmpeg -y -loglevel error \
 -i $VID/c1.mp4 -i $VID/c2.mp4 -i $VID/c3.mp4 -i $VID/c4.mp4 \
 -i $VID/c5.mp4 -i $VID/c6.mp4 -i $VID/c7.mp4 -i $VID/c8.mp4 \
 -filter_complex "\
 [0][1]xfade=transition=fade:duration=0.4:offset=3.4[a];\
 [a][2]xfade=transition=fade:duration=0.4:offset=7.5[b];\
 [b][3]xfade=transition=fade:duration=0.4:offset=12.6[c];\
 [c][4]xfade=transition=fade:duration=0.4:offset=17.7[d];\
 [d][5]xfade=transition=fade:duration=0.4:offset=22.8[e];\
 [e][6]xfade=transition=fade:duration=0.4:offset=26.9[f];\
 [f][7]xfade=transition=fade:duration=0.4:offset=32.0,format=yuv420p[v]" \
 -map "[v]" -r $FPS -c:v libx264 -preset medium -crf 18 -pix_fmt yuv420p $VID/base.mp4
VDUR=$(ffprobe -v error -show_entries format=duration -of csv=p=0 $VID/base.mp4)
echo "base video: ${VDUR}s"

echo "== 4/5 ElevenLabs narration =="
read -r -d '' SCRIPT << 'TXT' || true
Stop. Your mind needs this. Guided Meditation Portal — over fifty-five tracks to calm, focus, or sleep. A breathing coach that actually works. Meet Spirit: a private guide that reads your mood, right on your device. Wake up gently — no more jarring alarms. Build a streak, and feel the change. And here's the best part: no subscriptions, ever. One tiny unlock. Everything, forever. Download Guided Meditation Portal. Your calmer life starts now.
TXT
python3 - "$VOICE_ID" "$SCRIPT" << 'PY'
import os, sys, json, urllib.request, urllib.error
vid, text = sys.argv[1], sys.argv[2]
key = os.environ["ELEVENLABS_API_KEY"]
# Pre-flight: report the account's credit balance so we can tell
# "out of credits" apart from "bad key" (both otherwise return 401).
try:
    sreq = urllib.request.Request("https://api.elevenlabs.io/v1/user/subscription",
      headers={"xi-api-key": key})
    with urllib.request.urlopen(sreq, timeout=30) as r:
        sub = json.load(r)
        used = sub.get("character_count"); lim = sub.get("character_limit")
        print(f"ElevenLabs credits: used {used} of {lim} characters "
              f"({(lim-used) if (lim is not None and used is not None) else '?'} remaining), "
              f"tier={sub.get('tier')}")
except urllib.error.HTTPError as e:
    print("Could not read subscription:", e.code, e.read().decode(errors='replace')[:300])
body = json.dumps({"text": text, "model_id": "eleven_multilingual_v2",
  "voice_settings": {"stability":0.45,"similarity_boost":0.8,"style":0.35,"use_speaker_boost":True}}).encode()
req = urllib.request.Request(f"https://api.elevenlabs.io/v1/text-to-speech/{vid}",
  data=body, headers={"xi-api-key":key,"Content-Type":"application/json","Accept":"audio/mpeg"})
try:
    with urllib.request.urlopen(req, timeout=120) as r, open("video/narration.mp3","wb") as f:
        f.write(r.read())
    print("narration.mp3 written")
except urllib.error.HTTPError as e:
    detail = e.read().decode(errors='replace')
    print(f"ElevenLabs error {e.code}: {detail[:500]}", file=sys.stderr)
    # Surface the real cause explicitly for the run log
    if "quota" in detail.lower():
        print(">>> CAUSE: OUT OF CREDITS (quota_exceeded) — top up or upgrade your ElevenLabs plan.", file=sys.stderr)
    elif e.code == 401:
        print(">>> CAUSE: KEY REJECTED (invalid/expired API key) — update the ELEVENLABS_API_KEY secret.", file=sys.stderr)
    sys.exit(1)
PY

echo "== 5/5 mix =="
# voice at full; music ducked underneath; audio locked to the FULL video length
FO=$(python3 -c "print(round($VDUR-2,2))")
ffmpeg -y -loglevel error -i $VID/base.mp4 -i $VID/narration.mp3 -ss 6 -i $RAW \
 -filter_complex "\
 [1:a]volume=1.0,adelay=600|600,apad[voice];\
 [2:a]volume=0.16,afade=t=in:st=0:d=1.5,afade=t=out:st=$FO:d=2[music];\
 [voice][music]amix=inputs=2:duration=longest,atrim=0:$VDUR,aresample=44100[a]" \
 -map 0:v -map "[a]" -c:v copy -c:a aac -b:a 192k -t $VDUR $VID/GMP_reel_final.mp4
echo "DONE -> video/GMP_reel_final.mp4"
ffprobe -v error -show_entries format=duration:stream=width,height -of default=nw=1 $VID/GMP_reel_final.mp4 2>/dev/null
