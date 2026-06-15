"""
Deploy the photon-bounce.com HOMEPAGE update via FTP.

What it does, in order:
  1. Verifies the live theme folder `photon-bounce-aurora` exists (aborts if not).
  2. BACKS UP every live file it is about to overwrite into `homepage-backup/<timestamp>/`
     (in CI this is uploaded as a downloadable artifact).
  3. Uploads the 5 changed theme files to /wp-content/themes/photon-bounce-aurora/
     and the 5 app card images to /wp-content/uploads/photon-apps/.

Nothing else on the site is touched. All edits are additive (a "My Apps" strip +
chatbot fixes); your original homepage content is unchanged.

Usage (locally):   python homepage_deploy.py        # prompts for the FTP password
In CI:             FTP_PASS env var is supplied by the workflow_dispatch input.
The password is never stored in the repo.
"""
import io
import os
import sys
import getpass
import posixpath
from datetime import datetime
from ftplib import FTP, error_perm

HOST = "ftp.photon-bounce.com"
USER = "photonb"

SRC          = "homepage-update"
THEME        = "photon-bounce-aurora"
THEMES_DIR   = "public_html/wp-content/themes"
THEME_ROOT   = f"{THEMES_DIR}/{THEME}"
UPLOADS_ROOT = "public_html/wp-content/uploads/photon-apps"

BACKUP_DIR = os.path.join("homepage-backup", datetime.now().strftime("%Y%m%d-%H%M%S"))


def main():
    password = os.environ.get("FTP_PASS") or getpass.getpass(f"FTP password for {USER}@{HOST}: ")

    print(f"Connecting to {HOST} ...")
    ftp = FTP(HOST, timeout=60)
    ftp.login(USER, password)
    print("[OK] Logged in")

    # --- 1. Safety check: the target theme must already exist on the server ---
    try:
        raw = ftp.nlst(THEMES_DIR)
    except error_perm as e:
        print(f"ERROR: cannot list {THEMES_DIR}: {e}")
        ftp.quit(); sys.exit(1)
    themes = {posixpath.basename(t.rstrip("/")) for t in raw}
    if THEME not in themes:
        print(f"ERROR: theme '{THEME}' not found on server. Aborting (no files written).")
        print(f"       Themes present: {sorted(themes)}")
        ftp.quit(); sys.exit(1)
    print(f"[OK] Found live theme '{THEME}'")

    made = set()

    def ensure_dir(path):
        if path in made or path in ("", "/", "."):
            return
        ensure_dir(posixpath.dirname(path))
        try:
            ftp.mkd(path)
            print(f"  mkdir {path}")
        except error_perm:
            pass  # already exists
        made.add(path)

    def backup(remote, rel):
        """Download the current live file (if any) before overwriting it."""
        local = os.path.join(BACKUP_DIR, rel)
        os.makedirs(os.path.dirname(local), exist_ok=True)
        try:
            with open(local, "wb") as fh:
                ftp.retrbinary(f"RETR {remote}", fh.write)
            print(f"  backed up {remote}")
        except error_perm:
            if os.path.exists(local) and os.path.getsize(local) == 0:
                os.remove(local)
            print(f"  (no existing {remote} to back up — new file)")

    def upload(local_path, remote):
        ensure_dir(posixpath.dirname(remote))
        with open(local_path, "rb") as fh:
            ftp.storbinary(f"STOR {remote}", fh)
        print(f"  [OK] {remote}")

    # --- 2 + 3. Theme files (with backup) ---
    theme_src = os.path.join(SRC, "theme-files")
    print(f"\nTheme files -> {THEME_ROOT}/")
    for root, _dirs, files in os.walk(theme_src):
        for name in sorted(files):
            local_path = os.path.join(root, name)
            rel = os.path.relpath(local_path, theme_src).replace(os.sep, "/")
            remote = f"{THEME_ROOT}/{rel}"
            backup(remote, f"theme/{rel}")
            upload(local_path, remote)

    # --- App card images (backup any that already exist) ---
    img_src = os.path.join(SRC, "uploads-photon-apps")
    print(f"\nCard images -> {UPLOADS_ROOT}/")
    for name in sorted(os.listdir(img_src)):
        local_path = os.path.join(img_src, name)
        if not os.path.isfile(local_path):
            continue
        remote = f"{UPLOADS_ROOT}/{name}"
        backup(remote, f"uploads/{name}")
        upload(local_path, remote)

    ftp.quit()
    print("\n=== Homepage deployment complete ===")
    print("Check: https://www.photon-bounce.com/  (scroll to the 'My Apps' strip)")
    print(f"Backups of any overwritten live files are in: {BACKUP_DIR}/")


if __name__ == "__main__":
    main()
