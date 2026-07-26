#!/usr/bin/env python3
"""Generate premium narration for the promo Short via the ElevenLabs API.

Reads the 8 lines from promo/lines.txt and writes promo/vo/line_N.wav
(44.1 kHz stereo). Runs on a CI runner, where the API is reachable and the key
comes from a repo secret. Fails loudly (non-zero exit) if the key is missing or
the API rejects the request, so the workflow surfaces the real reason.

Env:
  PB_ELEVENLABS_KEY / XI_API_KEY / ELEVENLABS_API_KEY  — API key (any one)
  VOICE_ID   — ElevenLabs voice id (default: Adam, a deep narrator)
  MODEL_ID   — default: eleven_multilingual_v2 (highest quality)
"""
import os, sys, json, subprocess, urllib.request, urllib.error

HERE=os.path.dirname(os.path.abspath(__file__))
VO=os.path.join(HERE,"vo"); os.makedirs(VO,exist_ok=True)

KEY=(os.environ.get("PB_ELEVENLABS_KEY") or os.environ.get("XI_API_KEY")
     or os.environ.get("ELEVENLABS_API_KEY") or "").strip()
VOICE=(os.environ.get("VOICE_ID") or "pNInz6obpgDQGcFmaJgB").strip()   # Adam
MODEL=(os.environ.get("MODEL_ID") or "eleven_multilingual_v2").strip()

if not KEY:
    sys.exit("ERROR: no ElevenLabs API key found. Add a repo secret named "
             "PB_ELEVENLABS_KEY (Settings -> Secrets and variables -> Actions).")

# Sanity-check the key first so a bad/empty key fails before any audio work.
try:
    req=urllib.request.Request("https://api.elevenlabs.io/v1/user/subscription",
                               headers={"xi-api-key":KEY})
    with urllib.request.urlopen(req,timeout=45) as r:
        sub=json.load(r)
    print(f"key OK — tier={sub.get('tier')} "
          f"chars={sub.get('character_count')}/{sub.get('character_limit')}")
except urllib.error.HTTPError as e:
    sys.exit(f"ERROR: ElevenLabs rejected the key (HTTP {e.code}): "
             f"{e.read()[:300]!r}. Check the PB_ELEVENLABS_KEY secret value.")

lines=[]
for ln in open(os.path.join(HERE,"lines.txt"),encoding="utf-8"):
    ln=ln.strip()
    if not ln or "|" not in ln: continue
    num,txt=ln.split("|",1); lines.append((int(num),txt.strip()))
lines.sort()

for num,txt in lines:
    url=(f"https://api.elevenlabs.io/v1/text-to-speech/{VOICE}"
         f"?output_format=mp3_44100_128")
    body=json.dumps({
        "text":txt,
        "model_id":MODEL,
        "voice_settings":{"stability":0.45,"similarity_boost":0.8,
                          "style":0.15,"use_speaker_boost":True},
    }).encode("utf-8")
    req=urllib.request.Request(url,data=body,headers={
        "xi-api-key":KEY,"accept":"audio/mpeg","content-type":"application/json"})
    try:
        with urllib.request.urlopen(req,timeout=120) as r:
            data=r.read()
    except urllib.error.HTTPError as e:
        sys.exit(f"ERROR: TTS failed for line {num} (HTTP {e.code}): "
                 f"{e.read()[:300]!r}")
    mp3=os.path.join(VO,f"line_{num}.mp3"); open(mp3,"wb").write(data)
    wav=os.path.join(VO,f"line_{num}.wav")
    subprocess.run(["ffmpeg","-nostdin","-y","-loglevel","error","-i",mp3,
                    "-ar","44100","-ac","2",wav],check=True)
    print(f"line {num}: {len(data)} bytes -> {os.path.basename(wav)}")

print(f"VO complete: {len(lines)} lines, voice={VOICE}, model={MODEL}")
