import os
from faster_whisper import WhisperModel
path = os.path.join("app","src","main","res","raw","quiet_space_b.mp3")
model = WhisperModel("small.en", device="cpu", compute_type="int8")
segs, _ = model.transcribe(path, language="en", word_timestamps=True, vad_filter=True)
for s in segs:
    if s.end < 200 or s.start > 235: continue
    for w in (s.words or []):
        print(f"{w.start:7.2f}-{w.end:7.2f}  {w.word!r}", flush=True)
print("INSPECT DONE", flush=True)
