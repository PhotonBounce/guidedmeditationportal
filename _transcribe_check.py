"""
Transcribe every meditation mp3 and flag tracks that contain spoken
TTS-script artifacts like "long pause", "pause", "(pause)", etc.

Output: a report listing each file, whether a flag phrase was found, and the
surrounding transcript snippet so you can verify by eye.
"""
import os, re, sys, glob, json
from faster_whisper import WhisperModel

RAW_DIR = os.path.join("app", "src", "main", "res", "raw")
OUT_JSON = "_transcripts.json"

# Phrases that should NEVER be spoken aloud in a finished meditation track.
FLAG_PATTERNS = [
    r"long pause",
    r"short pause",
    r"\bpause\b",
    r"\bbrackets?\b",
    r"\bellipsis\b",
    r"insert .* here",
    r"\bplaceholder\b",
]
flag_re = re.compile("|".join(FLAG_PATTERNS), re.IGNORECASE)

def main():
    files = sorted(glob.glob(os.path.join(RAW_DIR, "*.mp3")))
    if not files:
        print("No mp3 files found in", RAW_DIR); return

    print(f"Loading model (tiny.en, CPU int8)...", flush=True)
    model = WhisperModel("tiny.en", device="cpu", compute_type="int8")

    results = {}
    flagged = []
    for i, path in enumerate(files, 1):
        name = os.path.basename(path)
        print(f"[{i}/{len(files)}] {name} ...", flush=True)
        segments, info = model.transcribe(path, language="en", vad_filter=True)
        text_parts = []
        hits = []
        for seg in segments:
            t = seg.text.strip()
            text_parts.append(t)
            if flag_re.search(t):
                hits.append(f"  @{seg.start:6.1f}s: {t}")
        full = " ".join(text_parts)
        results[name] = full
        status = "FLAGGED" if hits else "clean"
        print(f"      -> {status}" + (f" ({len(hits)} hit(s))" if hits else ""), flush=True)
        for h in hits:
            print(h, flush=True)
        if hits:
            flagged.append(name)

    with open(OUT_JSON, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=1)

    print("\n==================== SUMMARY ====================", flush=True)
    print(f"Total tracks: {len(files)}", flush=True)
    print(f"Flagged (contain a script-artifact phrase): {len(flagged)}", flush=True)
    for n in flagged:
        print("  -", n, flush=True)
    print(f"\nFull transcripts written to {OUT_JSON}", flush=True)

if __name__ == "__main__":
    main()
