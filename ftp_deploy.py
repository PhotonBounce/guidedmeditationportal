"""
Deploy the Guided Meditation Portal website to photon-bounce.com/ausis.

Usage (on your PC, from the repo root):
    python ftp_deploy.py

Uploads everything inside meditation-portal-site.zip to public_html/ausis/.
The password is asked at runtime (or set the FTP_PASS environment variable)
so credentials never live in the repository.
"""
import io
import os
import getpass
import zipfile
import posixpath
from ftplib import FTP

HOST = "ftp.photon-bounce.com"
USER = "photonb"
ZIP = "meditation-portal-site.zip"
REMOTE_ROOT = "public_html/ausis"


def main():
    password = os.environ.get("FTP_PASS") or getpass.getpass(f"FTP password for {USER}@{HOST}: ")

    print(f"Connecting to {HOST}...")
    ftp = FTP(HOST, timeout=30)
    ftp.login(USER, password)
    print("[OK] Logged in")

    made = set()

    def ensure_dir(path):
        if path in made or path in ("", "/"):
            return
        ensure_dir(posixpath.dirname(path))
        try:
            ftp.mkd(path)
            print(f"  mkdir {path}")
        except Exception:
            pass  # already exists
        made.add(path)

    with zipfile.ZipFile(ZIP) as z:
        names = [n for n in z.namelist() if not n.endswith("/")]
        print(f"Uploading {len(names)} files from {ZIP} -> {REMOTE_ROOT}/")
        for name in names:
            remote = posixpath.join(REMOTE_ROOT, name)
            ensure_dir(posixpath.dirname(remote))
            ftp.storbinary(f"STOR {remote}", io.BytesIO(z.read(name)))
            print(f"  [OK] {remote}")

    ftp.quit()
    print("=== Deployment complete: https://www.photon-bounce.com/ausis/ ===")


if __name__ == "__main__":
    main()
