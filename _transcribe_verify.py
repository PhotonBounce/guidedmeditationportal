"""
Re-verify the 9 tracks that came back "clean" on tiny.en using the more
accurate small.en model, to be sure they don't also contain the spoken
"long pause" / "pause" TTS artifact.
"""
import os, re, glob, json
from faster_whisper import WhisperModel

RAW_DIR = os.path.join("app", "src", "main", "res", "raw")

CLEAN_SUSPECTS = [
    "heavier_feet.mp3",
    "heavy_backpack_b.mp3",
    "nowhere_to_go.mp3",
    "nowhere_to_go_b.mp3",
    "nowhere_you_need_b.mp3",
    "quiet_beach_breath.mp3",
    "quiet_beach_breath_b.mp3",
    "quiet_river_flow.mp3",
    "quiet_space.mp3",
]

FLAG_PATTERNS = [r"long pause", r"short pause", r"\bpause\b",
                 r"\bbrackets?\b", r"\bplaceholder\b", r"insert .* here"]
flag_re = re.compile("|".join(FLAG_PATTERNS), re.IGNORECASE)

def main():
    print("Loading model (small.en, CPU int8)...", flush=True)
    model = WhisperModel("small.en", device="cpu", compute_type="int8")

    results = {}
    flagged = []
    for i, name in enumerate(CLEAN_SUSPECTS, 1):
        path = os.path.join(RAW_DIR, name)
        if not os.path.exists(path):
            print(f"[{i}/{len(CLEAN_SUSPECTS)}] {name} MISSING", flush=True); continue
        print(f"[{i}/{len(CLEAN_SUSPECTS)}] {name} ...", flush=True)
        segments, info = model.transcribe(path, language="en", vad_filter=True)
        parts, hits = [], []
        for seg in segments:
            t = seg.text.strip()
            parts.append(t)
            if flag_re.search(t):
                hits.append(f"  @{seg.start:6.1f}s: {t}")
        results[name] = " ".join(parts)
        print(f"      -> {'FLAGGED' if hits else 'clean'}", flush=True)
        for h in hits:
            print(h, flush=True)
        if hits:
            flagged.append(name)

    with open("_transcripts_verify.json", "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=1)

    print("\n==================== VERIFY SUMMARY ====================", flush=True)
    print(f"Re-checked {len(CLEAN_SUSPECTS)} 'clean' tracks with small.en", flush=True)
    print(f"Newly flagged: {len(flagged)}", flush=True)
    for n in flagged:
        print("  -", n, flush=True)
    if not flagged:
        print("All 9 confirmed CLEAN.", flush=True)

if __name__ == "__main__":
    main()
