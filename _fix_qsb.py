"""Targeted cut: remove residual spoken 'long pause' hiding in the
221.3s-226.1s gap of quiet_space_b.mp3 (between 'remain open.' and 'Now')."""
import os, sys, importlib.util
spec = importlib.util.spec_from_file_location("cutmod", "_cut_long_pause.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module.__self__ if False else spec.loader.exec_module(m)
import numpy as np

src = os.path.join("app","src","main","res","raw","quiet_space_b.mp3")
pcm, rate, chans = m.decode_pcm(src)
n = pcm.shape[1]
a, b = int(221.3*rate), int(226.1*rate)
keep = np.ones(n, dtype=bool); keep[a:b] = False
m.encode_mp3(src, pcm[:, keep], rate, chans)
print(f"cut {(b-a)/rate:.1f}s from quiet_space_b ({a/rate:.1f}-{b/rate:.1f}s); new len {(keep.sum())/rate:.0f}s", flush=True)
print("QSB FIX DONE", flush=True)
