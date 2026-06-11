import os, re
from faster_whisper import WhisperModel
RAW = os.path.join("app", "src", "main", "res", "raw")
FILES = ["fir_tip_notice_b.mp3", "heavy_backpack.mp3", "quiet_space_b.mp3", "quiet_workshop.mp3"]
flag = re.compile(r"long pause|short pause", re.IGNORECASE)
model = WhisperModel("small.en", device="cpu", compute_type="int8")
bad = []
for i, name in enumerate(FILES, 1):
    segs, _ = model.transcribe(os.path.join(RAW, name), language="en", vad_filter=True)
    hits = [f"@{s.start:.1f}s {s.text.strip()}" for s in segs if flag.search(s.text)]
    print(f"[{i}/4] {name}: {'STILL FLAGGED' if hits else 'clean'}", flush=True)
    for h in hits: print("   ", h, flush=True)
    if hits: bad.append(name)
print("VERIFY4 DONE:", "ALL CLEAN" if not bad else f"flagged: {bad}", flush=True)
