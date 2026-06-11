"""
Surgically remove spoken "long pause" / "short pause" stage directions from
the flagged meditation mp3s.

How: word-level timestamps from faster-whisper (small.en) locate each
"long|short pause" phrase; PyAV decodes the mp3 to PCM, the phrase samples are
cut (with a touch of padding), and the result is re-encoded to mp3.

Originals are backed up to audio_backup_original/ before overwriting.
Only the unambiguous two-word stage directions are cut — bare "pause" can be
legitimate meditation language ("you are allowed to pause"), so it's reported
but left alone.
"""
import os, shutil, sys
import numpy as np
import av
from faster_whisper import WhisperModel

RAW = os.path.join("app", "src", "main", "res", "raw")
BACKUP = "audio_backup_original"

# Files flagged with "long pause"/"short pause" by the tiny.en + small.en scans
TARGETS = [
    "breathing_out_tension.mp3",
    "fir_tip_notice.mp3",
    "fir_tip_notice_b.mp3",
    "heavy_backpack.mp3",
    "nowhere_to_achieve.mp3",
    "nowhere_to_achieve_b.mp3",
    "nowhere_to_go.mp3",
    "nowhere_you_need.mp3",
    "quiet_library.mp3",
    "quiet_library_b.mp3",
    "quiet_river_flow_b.mp3",
    "quiet_space_b.mp3",
    "quiet_workshop.mp3",
    "quiet_workshop_b.mp3",
    "vast_open_field.mp3",
]

PRE_PAD  = 0.06   # seconds kept off the front of the cut (avoid clipping prior word)
POST_PAD = 0.22   # seconds cut after the phrase (swallow trailing breath of "pause")


def decode_pcm(path):
    """Decode an mp3 to float32 PCM (channels, samples) + sample rate."""
    container = av.open(path)
    stream = container.streams.audio[0]
    rate = stream.rate
    chans = stream.channels
    chunks = []
    for frame in container.decode(stream):
        arr = frame.to_ndarray()          # shape (channels, n) or (1, n*channels) packed
        if arr.dtype != np.float32:
            # normalize ints to float32
            info = np.iinfo(arr.dtype)
            arr = arr.astype(np.float32) / max(abs(info.min), info.max)
        # planar vs packed handling
        if arr.shape[0] != chans:
            arr = arr.reshape(-1, chans).T
        chunks.append(arr)
    container.close()
    pcm = np.concatenate(chunks, axis=1)
    return pcm, rate, chans


def encode_mp3(path, pcm, rate, chans, bitrate=160_000):
    """Encode float32 PCM (channels, samples) to mp3."""
    out = av.open(path, "w")
    stream = out.add_stream("libmp3lame", rate=rate)
    stream.bit_rate = bitrate
    layout = "stereo" if chans == 2 else "mono"
    FRAME = 1152                          # mp3 frame size
    n = pcm.shape[1]
    for i in range(0, n, FRAME):
        chunk = pcm[:, i:i + FRAME]
        if chunk.shape[1] == 0:
            break
        frame = av.AudioFrame.from_ndarray(
            np.ascontiguousarray(chunk), format="fltp", layout=layout)
        frame.sample_rate = rate
        for pkt in stream.encode(frame):
            out.mux(pkt)
    for pkt in stream.encode(None):
        out.mux(pkt)
    out.close()


def find_phrases(model, path):
    """Return [(start,end,text)] for each 'long|short pause' phrase."""
    segments, _ = model.transcribe(path, language="en",
                                   word_timestamps=True, vad_filter=True)
    words = []
    for seg in segments:
        for w in (seg.words or []):
            words.append((w.word.strip().strip(".,!?;:").lower(), w.start, w.end))
    spans = []
    for i in range(len(words) - 1):
        w1, s1, e1 = words[i]
        w2, s2, e2 = words[i + 1]
        if w1 in ("long", "short") and w2 == "pause":
            spans.append((s1, e2, f"{w1} {w2}"))
    return spans


def main():
    # Allow a custom file list on the command line for follow-up passes.
    targets = sys.argv[1:] if len(sys.argv) > 1 else TARGETS
    os.makedirs(BACKUP, exist_ok=True)
    print("Loading small.en (CPU int8) with word timestamps...", flush=True)
    model = WhisperModel("small.en", device="cpu", compute_type="int8")

    total_cuts = 0
    for idx, name in enumerate(targets, 1):
        src = os.path.join(RAW, name)
        if not os.path.exists(src):
            print(f"[{idx}/{len(targets)}] {name}: MISSING, skipped", flush=True)
            continue
        print(f"[{idx}/{len(targets)}] {name}: locating phrases...", flush=True)
        spans = find_phrases(model, src)
        if not spans:
            print(f"      no 'long/short pause' found by word-scan — left unchanged", flush=True)
            continue

        pcm, rate, chans = decode_pcm(src)
        n = pcm.shape[1]
        keep = np.ones(n, dtype=bool)
        for (s, e, txt) in spans:
            a = max(0, int((s - PRE_PAD) * rate))
            b = min(n, int((e + POST_PAD) * rate))
            keep[a:b] = False
            print(f"      cut {txt!r} @ {s:6.1f}s–{e:6.1f}s", flush=True)
        cut_pcm = pcm[:, keep]

        # Backup once, then overwrite
        bpath = os.path.join(BACKUP, name)
        if not os.path.exists(bpath):
            shutil.copy2(src, bpath)
        encode_mp3(src, cut_pcm, rate, chans)
        removed = (n - cut_pcm.shape[1]) / rate
        total_cuts += len(spans)
        print(f"      saved — {len(spans)} cut(s), {removed:.1f}s removed", flush=True)

    print("\n================ CUT SUMMARY ================", flush=True)
    print(f"Files processed: {len(targets)} | total phrases cut: {total_cuts}", flush=True)
    print(f"Originals backed up in {BACKUP}/", flush=True)
    print("NOTE: 'breathing_out_tension_b.mp3' says a bare 'pause' (stage direction)", flush=True)
    print("      that can't be auto-distinguished — re-record that one.", flush=True)


if __name__ == "__main__":
    main()
