"""Re-scan the 13 cut mp3s to confirm no spoken 'long/short pause' remains."""
import os, re, glob, json
from faster_whisper import WhisperModel

RAW = os.path.join("app", "src", "main", "res", "raw")
CUT_FILES = [
    "breathing_out_tension.mp3", "fir_tip_notice_b.mp3", "heavy_backpack.mp3",
    "nowhere_to_achieve_b.mp3", "nowhere_to_go.mp3", "nowhere_you_need.mp3",
    "quiet_library.mp3", "quiet_library_b.mp3", "quiet_river_flow_b.mp3",
    "quiet_space_b.mp3", "quiet_workshop.mp3", "quiet_workshop_b.mp3",
    "vast_open_field.mp3",
]
flag = re.compile(r"long pause|short pause", re.IGNORECASE)

def main():
    print("Loading small.en...", flush=True)
    model = WhisperModel("small.en", device="cpu", compute_type="int8")
    bad = []
    for i, name in enumerate(CUT_FILES, 1):
        path = os.path.join(RAW, name)
        segments, _ = model.transcribe(path, language="en", vad_filter=True)
        hits = [f"@{s.start:.1f}s {s.text.strip()}" for s in segments if flag.search(s.text)]
        print(f"[{i}/{len(CUT_FILES)}] {name}: {'STILL FLAGGED' if hits else 'clean'}", flush=True)
        for h in hits: print("   ", h, flush=True)
        if hits: bad.append(name)
    print("\n========= CUT VERIFY SUMMARY =========", flush=True)
    print(f"{len(CUT_FILES) - len(bad)}/{len(CUT_FILES)} clean", flush=True)
    for n in bad: print("  still flagged:", n, flush=True)

if __name__ == "__main__":
    main()
